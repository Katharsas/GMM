/// <reference path="../../lib/three.d.ts" />

import $ from "../../lib/jquery";
import THREE from '../../lib/three';
import { CanvasRenderer, ShadingType, Size2D } from './CanvasRenderer';
// Be aware that OrbitControls.js adds properties to THREE too.

/**
 * @event RenderOptionsChange
 * @type {CustomEvent}
 * @property {RenderOptions} detail
 */

/**
 * Renders a pair of 3D models, which share render options, camera & controls. Each 3D model
 * is rendered into a canvas using PreviewRenderer#CanvasRenderer.
 * 
 * @fires RenderOptionsChange
 * @listens RenderOptionsChange
 * 
 * @param {JQuery} $canvasContainer - TODO change to preview container and TODO attach listeners
 * 		for rendering option changes
 */
var PreviewRenderer = (function() {

	/**
	 * Proxy for adding and removing event listeners (emulates native dom element from jquery object).
	 */
	var NodeProxy = function($nodes) {
		var nodeArray = [];
		for(var node of $nodes.toArray()) {
			nodeArray.push(node);
		}
		this.addEventListener = function(...args) {
			for(var node of nodeArray) {
				node.addEventListener(...args);
			}
		};
		this.removeEventListener = function(...args) {
			for(var node of nodeArray) {
				node.removeEventListener(...args);
			}
		};
	};

	var createCamera = function(zDistance) {
		var camera = new THREE.PerspectiveCamera(40, 1, 1, 500);
		camera.position.y = zDistance/2;
		camera.position.z = zDistance;
		return camera;
	};

	var createControls = function($canvas, camera, lookAt, animationCallbacks) {
		var canvas = $canvas.length === 1 ? $canvas[0] : new NodeProxy($canvas);
		var controls = new THREE.OrbitControls(camera, document, canvas);
		controls.target.copy(lookAt);
		$canvas.on("renderOptionsChange", function(event) {
			var options = event.detail;
			controls.autoRotate = options.rotateCamera;
			controls.autoRotateSpeed = options.rotateCameraSpeed;
		});
		animationCallbacks.push(function() {
			// restrict sideways pan
			controls.target.x = lookAt.x;
			controls.target.z = lookAt.z;
			controls.update();// needed for zoom
		});
		return controls;
	};

	// some helper functions to make THREE vector methods side-effect-free
	/** @returns {THREE.Vector3} */
	var add = function(v1, v2) {
		return v1.clone().add(v2);
	};
	/** @returns {THREE.Vector3} */
	var sub = function(v1, v2) {
		return v1.clone().sub(v2);
	};
	/** @returns {THREE.Vector3} */
	var multiplyScalar = function(v1, s) {
		return v1.clone().multiplyScalar(s);
	};

	/**
	 * PreviewRenderer
	 */
	return function($canvasContainer) {

		var $canvasAreas = $canvasContainer.find(".task-preview-visual");
		var isStopped = false;

		var animationCallbacks = [];
		var camera;

		/**@typedef ViewState
		 * @property {CanvasRenderer} view
		 * @property {THREE.Scene} scene
		 * @property {THREE.WebGLRenderer} renderer
		 * @property {Size2D} viewSize
		 */

		/** @type {ViewState[]} */
		var viewStates = [];

		/**
		 * @typedef LoadResult
		 * @property {JQuery} $canvas
		 * @property {CanvasRenderer} view
		 * @property {THREE.Mesh} mesh
		 */

		 /** @type {Promise<LoadResult>[]} */
		var meshLoadPromises = [];

		$canvasAreas.each(function() {
			var $area = $(this);
			var $canvas = $area.find("canvas");
			var jsonUrl = $area.attr("data-url");
			// stop any parent event handlers when inside canvas
			$canvas.on("click", function(event) {
				event.stopPropagation();
				event.preventDefault();
			});
			/**@type {CanvasRenderer}*/
			var view = new CanvasRenderer($canvas);
			var meshPromise = view.loadMesh(jsonUrl);
			meshLoadPromises.push(meshPromise.then(mesh => ({ $canvas, view, mesh })));
		});
		Promise.all(meshLoadPromises).then(function(loadResult) {

			// camera, controls
			var cameraDistances = [];
			var cameraTargets = [];

			for(let { $canvas, view, mesh } of loadResult) {

				let bounds = mesh.geometry.boundingBox;
				let center = multiplyScalar(add(bounds.min, bounds.max), 0.5);
				let meshPos = mesh.position;
				let centerAbs = add(mesh.position, center);
				let height = meshPos.y + centerAbs.y / 2;

				let cameraTarget = new THREE.Vector3(centerAbs.x, height, centerAbs.z);
				cameraTargets.push(cameraTarget);

				let boundsSize = sub(bounds.max, bounds.min);
				let cameraDistance = Math.pow(boundsSize.length() * 3, 0.65);

				cameraDistances.push(cameraDistance);
			}

			var debugPos;
			{
				let camDistance = cameraDistances.reduce((a, b) => a + b) / cameraDistances.length;
				let camTarget = multiplyScalar(cameraTargets.reduce((a, b) => add(a, b)), 1/cameraTargets.length);
	
				camera = createCamera(camDistance);
				createControls($canvasAreas.find("canvas"), camera, camTarget, animationCallbacks);
				debugPos = camTarget;
			}

			// scene, renderer
			for(let { $canvas, view, mesh } of loadResult) {

				let wire = view.createWireframe(mesh);
				let sceneChildren = [mesh, wire];

				let debug = false;
				if (debug) {
					let debugCubeSize = 0.2;
					let debugCube = new THREE.BoxGeometry(debugCubeSize, debugCubeSize, debugCubeSize);
					let debugCubeMesh = new THREE.Mesh(debugCube, new THREE.MeshBasicMaterial({ color: 0xffff00 }));
					debugCubeMesh.position.copy(debugPos);
					sceneChildren.push(debugCubeMesh);
				}
				
				let scene = view.createScene(sceneChildren, animationCallbacks);
				let renderer = view.createRenderer();

				viewStates.push({
					view: view,
					scene: scene,
					renderer: renderer,
					viewSize: new Size2D(0, 0)
				});
			}
			dispatch();
			render();
		});

		// see typedef RenderOptions in CanvasRenderer.js
		var options = {
			shading: "matcap",
			wireframe: false,
			rotateCamera: true,
			rotateCameraSpeed: 0.7,
			shadowsEnabled: false,
			rotateLight: true,
		};

		var dispatchToArea = function($area) {
			var event = new CustomEvent("renderOptionsChange", { detail: options });
			$area.find("canvas")[0].dispatchEvent(event);
		};

		var dispatch = function() {
			$canvasAreas.each(function() {
				var $area = $(this);
				dispatchToArea($area);
			});
		};

		var render = function() {
			if (!isStopped) {
				requestAnimationFrame(render);
				for(var i = 0; i < animationCallbacks.length; i++) {
					animationCallbacks[i]();
				}
				for(var viewState of viewStates) {
					var newSize = viewState.view.updateCanvasSizeIfChanged(viewState.viewSize, camera, viewState.renderer);
					viewState.viewSize = newSize;
					viewState.view.render(viewState.renderer, viewState.scene, camera);
				}
			}
		};

		return {
			/**
			 * @param {CanvasRenderer.RenderOptions} changedOptions
			 */
			setOptions: function(changedOptions) {
				for(var option in changedOptions) {
					options[option] = changedOptions[option];
				}
				dispatch();
			},
			getOption: function(option) {
				return options[option];
			},
			destroy: function() {
				isStopped = true;
				for(let viewState of viewStates) {
					viewState.view.destroyRenderer(viewState.renderer);
				}
			}
		};
	};
})();

export default PreviewRenderer;