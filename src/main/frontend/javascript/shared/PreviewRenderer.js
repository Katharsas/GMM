/// <reference path="../lib/three.d.ts" />

import $ from "../lib/jquery";
import THREE from '../lib/three';
import { CanvasRenderer, ShadingType } from './CanvasRenderer';
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
 * @param {jquery} $canvasContainer - TODO change to preview container and TODO attach listeners
 * 		for rendering option changes
 */
var PreviewRenderer = (function() {

	/**
	 * Proxy for adding and removing event listeners (emulates native dom element from jquery object).
	 */
	var NodeProxy = function($nodes) {
		var nodeArray = [];
		for (var node of $nodes.toArray()) {
			nodeArray.push(node);
		}
		this.addEventListener = function(...args) {
			for (var node of nodeArray) {
				node.addEventListener(...args);
			}
		};
		this.removeEventListener = function(...args) {
			for (var node of nodeArray) {
				node.removeEventListener(...args);
			}
		};
	};
	
	var createCamera = function() {
		var camera = new THREE.PerspectiveCamera( 40, 1, 1, 500 );
		camera.position.y = 10;
		camera.position.z = 20;
		return camera;
	};

	var createControls  = function($canvas, camera, animationCallbacks) {
		var canvas = $canvas.length === 1 ? $canvas[0] : new NodeProxy($canvas);
		var controls = new THREE.OrbitControls(camera, document, canvas);
		controls.target.y = 5;
		var restrictToBounds = function(number, bound) {
			return Math.min(Math.max(number, -bound), bound);
		};
		$canvas.on("renderOptionsChange", function(event) {
			var options = event.detail;
			controls.autoRotate = options.rotateCamera;
			controls.autoRotateSpeed = options.rotateCameraSpeed;
		});
		animationCallbacks.push(function() {
			// restrict sideways pan
			controls.target.x = restrictToBounds(controls.target.x, 0);
			controls.target.z = restrictToBounds(controls.target.z, 0);
			controls.update();// needed for zoom
		});
		return controls;
	};
	
	/**
	 * PreviewRenderer
	 */
	return function($canvasContainer) {
		
		var $canvasAreas = $canvasContainer.find(".task-preview-visual");
		var isStopped = false;
		
		var animationCallbacks = [];
		
		var camera = createCamera();
		createControls($canvasAreas.find("canvas"), camera, animationCallbacks);
		
		var renderers = [];
		
		$canvasAreas.each(function() {
			var $area = $(this);
			var data = {
				geometryPath : $area.attr("data-url"),
				$canvas : $area.find("canvas"),
				camera : camera
			};
			// stop any parent event handlers when inside canvas
			data.$canvas.on("click", function(event) {
				event.stopPropagation();
				event.preventDefault();
			});
			var renderer = new CanvasRenderer(data, animationCallbacks);
			renderers.push(renderer);
		});
		
		// see typedef RenderOptions in CanvasRenderer.js
		var options = {
			shading : "matcap",
			wireframe : false,
			rotateCamera : true,
			rotateCameraSpeed : 0.7,
			shadowsEnabled : false,
			rotateLight : true,
		};

		var dispatch = function() {
			$canvasAreas.each(function() {
				var $area = $(this);
				var event = new CustomEvent("renderOptionsChange", { detail : options });
				$area.find("canvas")[0].dispatchEvent(event);
			});
		};
		
		var render = function() {
			if (!isStopped) {
				requestAnimationFrame(render);
				for(var i = 0; i < animationCallbacks.length; i++) {
					animationCallbacks[i]();
				}
				for(var renderer of renderers) {
					renderer.render();
				}
			}
		};
		
		dispatch();
		render();
		
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
				for(var renderer of renderers) {
					renderer.destroy();
				}
			}
		};
	};
})();

export default PreviewRenderer;