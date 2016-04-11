/*
 * TODO: NOT IN SYNC WITH PREVIEWRENDERER
 */


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
			var shadowPadding = 30;
			var shadowVisualizer =
				createShadowVisualizer(directionalLight, shadowPadding, animationCallbacks);
			scene.add(shadowVisualizer);
			
			var rotateLight;
			$canvas.on("renderOptionsChange", function(event) {
				var options = event.detail;
				var shadowsEnabled = options.shadowsEnabled && !options.showWireframe;
				directionalLight.castShadow = shadowsEnabled;
				setupShadows(directionalLight, shadowPadding);
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
		
		var createShadowVisualizer = function(light, shadowPadding, animationCallbacks) {
			var shadowVisualizer = new THREE.DirectionalLightHelper(light, shadowPadding);
			animationCallbacks.push(function() {
				if(shadowVisualizer)
				shadowVisualizer.update();
			});
			return shadowVisualizer;
		};
		
		var setupShadows = function(light, shadowPadding) {
			if(light.castShadow) {
				var shadowCamera = light.shadow.camera;
				// shadow casting boundaries
				shadowCamera.near = 100;
				shadowCamera.far = 250;
				shadowCamera.left = -shadowPadding;
				shadowCamera.right = shadowPadding;
				shadowCamera.top = shadowPadding;
				shadowCamera.bottom = -shadowPadding;
				// quality
				light.shadow.bias = 0.0001;
				var resolution = 2048;
				light.shadow.mapSize.width = resolution;
				light.shadow.mapSize.height = resolution;
			}
		};
		
		var createRenderer = function($canvas) {
			var renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
			$canvas.on("renderOptionsChange", function(event) {
				var options = event.detail;
				var shadowsEnabled = options.shadowsEnabled && !options.showWireframe;
				if(shadowsEnabled) {
					renderer.shadowMap.type = THREE.PCFSoftShadowMap;
					renderer.shadowMap.enabled = true;
					renderer.shadowMapSoft = true;
				}
			});
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
			};
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
			};
		};
		
		var initCanvas = function($canvas, funcId, renderer, camera) {
			var width, height;
			var updateCanvasSize = function() {
				width = $canvas.width();
				height = $canvas.height();

				camera.aspect = $canvas.width() / $canvas.height();
				camera.updateProjectionMatrix();
				renderer.setSize(width, height, false);
			};
			// needs jqueryResize to work
			$($canvas).resize(function() {
				waitForFinalEvent(updateCanvasSize, 50, funcId);
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
			var geometryPath = data.geometryPath;
			var scene = createScene($canvas, animationCallbacks);
			loadMeshIntoScene($canvas, geometryPath, scene);
			var renderer = createRenderer($canvas);
			initCanvas($canvas, geometryPath, renderer, data.camera);
			
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
			};
		};
		// CanvasRenderer End
		// ####################################################################################
	})();
	
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
		
		if($canvasAreas.length > 0) {
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
		}
		
		var options = {
			shadowsEnabled : false,
			rotateLight : true,
			showWireframe : false,
			rotateCamera : true,
			rotateCameraSpeed : 0.7
		};

		var dispatch = function() {
			$canvasAreas.each(function() {
				var $area = $(this);
				var event = new CustomEvent("renderOptionsChange", { detail : options });
				$area.find("canvas")[0].dispatchEvent(event);
			});
		};
		
		var render = function() {
			requestAnimationFrame(render);
			for(var i = 0; i < animationCallbacks.length; i++) {
				animationCallbacks[i]();
			}
			for(var renderer of renderers) {
				renderer.render();
			}
		};
		
		dispatch();
		render();
		
		return {
			options: options,
			update: function() {
				dispatch();
			},
			destroy: function() {
				for(var renderer of renderers) {
					renderer.destroy();
				}
			}
		};
	};
})();
