/*
 * Class CustomRenderer.
 * Dependencies: JQuery, Three.js, OrbitControls.js
 *
 * TODO: proper cleanup on task collapse: https://github.com/mrdoob/three.js/issues/7391 (event handlers!)
 */
function CanvasRenderer(data, options, animationCallbacks) {
	
	var createScene = function () {
		var scene = new THREE.Scene();
		
		//ambient light
		var ambientLight = new THREE.AmbientLight(0x444444);
		scene.add(ambientLight);

		// directional lighting
		var lightDistance = 120;
		var directionalLight = new THREE.DirectionalLight(0xbbbbbb);
		directionalLight.position.set(lightDistance, lightDistance, lightDistance);
		
		if(options.shadowsEnabled) {
			enableShadows(scene, directionalLight);
		}
		var lightRadians = 0;
		animationCallbacks.push(function() {
			if(options.rotateLight) {
				lightRadians += 0.003;
				var sin = Math.sin(lightRadians) * lightDistance;
				var cos = Math.cos(lightRadians) * lightDistance;
				directionalLight.position.set(sin, lightDistance, cos);
			}
		});
		scene.add(directionalLight);
		return scene;
	};
	
	var enableShadows = function(scene, light) {
		light.castShadow = true;
		
		var shadowCamera = light.shadow.camera;
		shadowCamera.near = 100;
		shadowCamera.far = 250;
		var shadowPadding = 30;
		shadowCamera.left = -shadowPadding;
		shadowCamera.right = shadowPadding;
		shadowCamera.top = shadowPadding;
		shadowCamera.bottom = -shadowPadding;
		
		light.shadow.bias = 0.0001;
		
		var resolution = 2048;
		light.shadow.mapSize.width = resolution;
		light.shadow.mapSize.height = resolution;
		
		// light.shadowCameraVisible = true;
		var shadowVisualizer = new THREE.DirectionalLightHelper(light, shadowPadding);
		animationCallbacks.push(function() {
			shadowVisualizer.update();
		});
		scene.add(shadowVisualizer);
	};
	
	var createRenderer = function($canvas) {
		var renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
		if(options.shadowsEnabled) {
			renderer.shadowMap.type = THREE.PCFSoftShadowMap;
			renderer.shadowMap.enabled = true;
			renderer.shadowMapSoft = true;
		}
		return renderer;
	};
	
	/**
	 * Load geometry from json and create a new material.
	 * Make mesh from geometry and material and add to scene.
	 */
	var loadMeshIntoScene = function(jsonPath, scene) {
		var loader = new THREE.JSONLoader();
		loader.load(jsonPath, function(geometry, materials) {
			var material = new THREE.MeshLambertMaterial();
			if (options.showWireframe) {
				material.wireframe = true;
			}
			var mesh = new THREE.Mesh( geometry, material );
			if(options.shadowsEnabled && !options.showWireframe) {
				mesh.castShadow = true;
				mesh.receiveShadow = true;
			}
			scene.add(mesh);
		});
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
		// TODO: container instead of window
		$(window).resize(function() {
			waitForFinalEvent(updateCanvasSize, 50, "canvresz");
		});
		updateCanvasSize();
	}

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

	var scene = createScene();
	loadMeshIntoScene(data.geometryPath, scene);
	var renderer = createRenderer(data.$canvas);
	initCanvas(data.$canvas, renderer, data.camera);
	
	return {
		render : function() {
			renderer.render(scene, data.camera);
		}
	}
}

//############################
// PreviewRenderer
// ############################

var animationCallbacks = [];

var options = {
		shadowsEnabled : true,
		rotateLight : true,
		showWireframe : false,
		rotateCamera : true,
		rotateCameraSpeed : 0.7
	}

var createCamera = function() {
	var camera = new THREE.PerspectiveCamera( 40, 1, 1, 500 );
	camera.position.y = 10;
	camera.position.z = 20;
	return camera;
}

var createControls  = function(camera) {
	var controls = new THREE.OrbitControls(camera);
	controls.target.y = 5;
	controls.autoRotate = options.rotateCamera;
	controls.autoRotateSpeed = options.rotateCameraSpeed;
	
	var restrictToBounds = function(number, bound) {
		return Math.min(Math.max(number, -bound), bound);
	};
	animationCallbacks.push(function() {
		// restrict sideways pan
		controls.target.x = restrictToBounds(controls.target.x, 0);
		controls.target.z = restrictToBounds(controls.target.z, 0);
		controls.update();// needed for zoom
	});
	return controls;
}

var camera = createCamera();
var controls = createControls(camera);

var data1 = {
	geometryPath : './models/original.js',
	$canvas : $('canvas#canvas1'),
	camera : camera
};
var data2 = {
	geometryPath : './models/original.js',
	$canvas : $('canvas#canvas2'),
	camera : camera
};

var renderer1 = new CanvasRenderer(data1, options, animationCallbacks);
var renderer2 = new CanvasRenderer(data2, options, animationCallbacks);

var render = function() {
	requestAnimationFrame(render);
	for(var i = 0; i < animationCallbacks.length; i++) {
		animationCallbacks[i]();
	}
	renderer1.render();
	renderer2.render();
};

render();