/*
 * Dependencies: JQuery, Three.js, OrbitControls.js, jqueryResize.js
 *
 * TODO: proper cleanup on task collapse: https://github.com/mrdoob/three.js/issues/7391 (event handlers!)
 */

/**
 * Renders a pair of 3D models, which share render options, camera & controls. Each 3D model
 * is rendered into a canvas using PreviewRenderer#CanvasRenderer.
 * 
 * @event PreviewRenderer # renderOptionsChange
 * @type {CustomEvent}
 * @property {object} detail - contains all render options:
 * 		{boolean} shadowsEnabled - initial:true
 * 		{boolean} showWireframe - initial:false
 * 		{boolean} rotateLight - initial:true
 * 		{boolean} rotateCamera - initial:true
 * 		{double} rotateCameraSpeed - initial:0.7
 * 
 * @fires PreviewRenderer # renderOptionsChange
 * @listens PreviewRenderer # renderOptionsChange
 * 
 * @param {jquery} $canvasContainer - TODO change to preview container and TODO attach listeners
 * 		for rendering option changes
 */
var PreviewRenderer = (function() {
	
	/**
	 * Can render a single 3D model into a single canvas.
	 * @listens PreviewRenderer#renderOptionsChange
	 * 
	 * @param {object} data:
	 * 		{jquery} $canvas - HTML canvas node into which this renderer will draw.
	 * 		{string} geometryPath - file path to the json file containing 3D model.
	 * 		{object} camera - the three.js camera used for rendering the 3D model.
	 * @param {function[]} animationCallbacks - All functions that need to be called
	 * 		per frame will be pushed into this array.
	 */
	var CanvasRenderer = (function() {
		
		var createScene = function ($canvas, animationCallbacks) {
			var scene = new THREE.Scene();
			// ambient light
			var ambientLight = new THREE.AmbientLight(0x444444);
			scene.add(ambientLight);
			// directional light
			var lightDistance = 120;
			var directionalLight = new THREE.DirectionalLight(0xbbbbbb);
			directionalLight.position.set(lightDistance, lightDistance, lightDistance);
			scene.add(directionalLight);
			// directional light visual
			var shadowVisualizer = setupShadows(directionalLight, animationCallbacks);
			scene.add(shadowVisualizer);
			
			var rotateLight;
			$canvas.on("renderOptionsChange", function(event) {
				var options = event.detail;
				var shadowsEnabled = options.shadowsEnabled && !options.showWireframe;
				directionalLight.castShadow = shadowsEnabled;
				shadowVisualizer.visible = shadowsEnabled;
				rotateLight = options.rotateLight;
			});

			var lightRadians = 0;
			animationCallbacks.push(function() {
				if(rotateLight) {
					lightRadians += 0.003;
					var sin = Math.sin(lightRadians) * lightDistance;
					var cos = Math.cos(lightRadians) * lightDistance;
					directionalLight.position.set(sin, lightDistance, cos);
				}
			});
			return scene;
		};
		
		var setupShadows = function(light, animationCallbacks) {
			var shadowCamera = light.shadow.camera;
			// shadow casting boundaries
			shadowCamera.near = 100;
			shadowCamera.far = 250;
			var shadowPadding = 30;
			shadowCamera.left = -shadowPadding;
			shadowCamera.right = shadowPadding;
			shadowCamera.top = shadowPadding;
			shadowCamera.bottom = -shadowPadding;
			// quality
			light.shadow.bias = 0.0001;
			var resolution = 2048;
			light.shadow.mapSize.width = resolution;
			light.shadow.mapSize.height = resolution;
			
			var shadowVisualizer = new THREE.DirectionalLightHelper(light, shadowPadding);
			animationCallbacks.push(function() {
				if(shadowVisualizer)
				shadowVisualizer.update();
			});
			return shadowVisualizer;
		};
		
		var createRenderer = function($canvas) {
			var renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
			renderer.shadowMap.type = THREE.PCFSoftShadowMap;
			renderer.shadowMap.enabled = true;
			renderer.shadowMapSoft = true;
			return renderer;
		};
		
		/**
		 * Load geometry from json and create a new material.
		 * Make mesh from geometry and material and add to scene.
		 */
		var loadMeshIntoScene = function($canvas, jsonPath, scene) {
			var loader = new THREE.JSONLoader();

			// we need to register event handler immediatly but load is async
			var initOptions;
			var setInitOptions = function(event) {
				initOptions = event.detail;
			}
			// save options until we can register proper handler
			$canvas.on("renderOptionsChange", setInitOptions);
			
			loader.load(jsonPath, function(geometry, materials) {
				var material = new THREE.MeshLambertMaterial();
				var mesh = new THREE.Mesh(geometry, material);
				scene.add(mesh);
				// proper handler
				$canvas.on("renderOptionsChange", function(event) {
					setOptions(event.detail, material, mesh);
				});
				// remove init handler and apply init options
				$canvas.off("renderOptionsChange", setInitOptions);
				if(initOptions) {
					setOptions(initOptions, material, mesh);
				}
			});
			var setOptions = function(options, material, mesh) {
				// shadows
				var shadowsEnabled = options.shadowsEnabled && !options.showWireframe;
				mesh.castShadow = shadowsEnabled;
				mesh.receiveShadow = shadowsEnabled;
				// wireframe
				material.wireframe = options.showWireframe;
			}
		};
		
		var initCanvas = function($canvas, renderer, camera) {
			var width, height;
			var updateCanvasSize = function() {
				width = $canvas.width();
				height = $canvas.height();

				camera.aspect = $canvas.width() / $canvas.height();
				camera.updateProjectionMatrix();
				renderer.setSize(width, height, false);
			};
			
			// prevent user from scrolling while zooming
			$canvas.on('scroll', function(event) {
				event.preventDefault();
				event.stopPropagation();
			});
			// needs jqueryResize to work
			$($canvas).resize(function() {
				waitForFinalEvent(updateCanvasSize, 50, "canvresz");
			});
			updateCanvasSize();
		};

		/**
		 * Allows to call a function only after a certain amount of time passed without the same function being called again.
		 * This is useful to delay expensive event handlers, that get fired rapidly, until events "calm down".
		 * @param {callback} callback
		 * @param {int} ms - the time that must pass until callback will be called
		 * @param {uniqueId} - if the same unqiueId gets passed twice before time ran out on the first, timer will be reset
		 */
		var waitForFinalEvent = (function() {
			var timers = {};
			return function(callback, ms, uniqueId) {
				if (!uniqueId) {
					uniqueId = "Don't call this twice without a uniqueId";
				}
				if (timers[uniqueId]) {
					clearTimeout(timers[uniqueId]);
				}
				timers[uniqueId] = setTimeout(callback, ms);
			};
		})();

		/**
		 * CanvasRenderer
		 */
		return function (data, animationCallbacks) {
			
			var $canvas = data.$canvas;
			var scene = createScene($canvas, animationCallbacks);
			loadMeshIntoScene($canvas, data.geometryPath, scene);
			var renderer = createRenderer($canvas);
			initCanvas($canvas, renderer, data.camera);
			
			return {
				render : function() {
					renderer.render(scene, data.camera);
				},
				destroy : function() {
					renderer.forceContextLoss();
					renderer.context = null;
					renderer.domElement = null;
					renderer = null;
				}
			}
		}
		// CanvasRenderer End
		// ####################################################################################
	})();
	
	
	var createCamera = function() {
		var camera = new THREE.PerspectiveCamera( 40, 1, 1, 500 );
		camera.position.y = 10;
		camera.position.z = 20;
		return camera;
	}

	var createControls  = function($canvas, camera, animationCallbacks) {
		var controls = new THREE.OrbitControls(camera, $('#canvasControls')[0]);
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
	}
	
	/**
	 * PreviewRenderer
	 */
	return function($canvasContainer) {
		
		var animationCallbacks = [];

		var $bothCanvas = $canvasContainer.find("canvas");
		var camera = createCamera();
		var controls = createControls($bothCanvas, camera, animationCallbacks);
		
		// TODO paths
		var data1 = {
			geometryPath : "./models/original.json",
			$canvas : $($bothCanvas[0]),
			camera : camera
		};
		var data2 = {
			geometryPath : "./models/new.json",
			$canvas : $($bothCanvas[1]),
			camera : camera
		};
		
		var renderer1 = new CanvasRenderer(data1, animationCallbacks);
		var renderer2 = new CanvasRenderer(data2, animationCallbacks);
		
		var options = {
			shadowsEnabled : true,
			rotateLight : true,
			showWireframe : false,
			rotateCamera : true,
			rotateCameraSpeed : 0.7
		}

		var setOptions = new CustomEvent("renderOptionsChange", { detail : options });
		var dispatch = function() {
			$bothCanvas[0].dispatchEvent(setOptions);
			$bothCanvas[1].dispatchEvent(setOptions);
		}
		dispatch();

		var render = function() {
			requestAnimationFrame(render);
			for(var i = 0; i < animationCallbacks.length; i++) {
				animationCallbacks[i]();
			}
			renderer1.render();
			renderer2.render();
		};

		render();
		
		// ####################### Test-UI #######################
		// TODO

		$("#toggleShadows").click(function() {
			options.shadowsEnabled = !options.shadowsEnabled;
			dispatch();
		});
		$("#toggleWireframe").click(function() {
			options.showWireframe = !options.showWireframe;
			dispatch();
		});
	}
})();

PreviewRenderer($("#canvasControls"));
