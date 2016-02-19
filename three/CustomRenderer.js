/**
 * Class CustomRenderer.
 * Dependencies: JQuery, Three.js, OrbitControls.js
 *
 * TODO: proper cleanup on task collapse: https://github.com/mrdoob/three.js/issues/7391 (event handlers!)
 */
function CustomRenderer(jsonPath) {

	var $canvas, width, height;
	var scene, camera, renderer, controls;
	var boxMesh;
	var directionalLight, shadowVisualizer;
	
	var lightRadians = 0;
	var lightDistance = 120;
	
	var shadowsEnabled = true;
	var rotateLight = true;
	var showWireframe = false;
	var rotateCamera = true;
	
	var createScene = function () {
		$canvas = $('canvas#canvas');
		// prevent user from scrolling while zooming
		$canvas.on('scroll', function(event) {
			event.preventDefault();
			event.stopPropagation();
		});
		$(window).resize(function() {waitForFinalEvent(updateCanvasSize, 50, "canvresz");});
		
		scene = new THREE.Scene();

		camera = new THREE.PerspectiveCamera( 40, 1, 1, 500 );
		camera.position.y = 10;
		camera.position.z = 20;
		
		controls = new THREE.OrbitControls( camera );
		controls.target.y = 5;
		controls.autoRotate = rotateCamera;
		
		//ambient light
		var ambientLight = new THREE.AmbientLight(0x444444);
		scene.add(ambientLight);

		// directional lighting
		directionalLight = new THREE.DirectionalLight(0xbbbbbb);
		directionalLight.position.set(lightDistance, lightDistance, lightDistance);
		
		if(shadowsEnabled) {
			enableShadows(directionalLight);
		}
		scene.add(directionalLight);
	};
	
	var enableShadows = function(light) {
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
		
		var shadowMap = 2048;
		light.shadow.mapSize.width = shadowMap;
		light.shadow.mapSize.height = shadowMap;
		
		// light.shadowCameraVisible = true;
		shadowVisualizer = new THREE.DirectionalLightHelper(light, shadowPadding);
		scene.add(shadowVisualizer);
	};
	
	var createRenderer = function() {
		renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
		if(shadowsEnabled) {
			renderer.shadowMap.type = THREE.PCFSoftShadowMap;
			renderer.shadowMap.enabled = true;
			renderer.shadowMapSoft = true;
		}
	};
	
	var loadModel = function() {
		var callbackFinished = function(geometry, materials) {
//			var material = new THREE.MeshBasicMaterial( { color: 0xff0000, wireframe: true } );
			var material = new THREE.MeshLambertMaterial();
			if (showWireframe) {
				material.wireframe = true;
			}
			var mesh = new THREE.Mesh( geometry, material );
			if(shadowsEnabled && !showWireframe) {
				mesh.castShadow = true;
				mesh.receiveShadow = true;
			}
			scene.add(mesh);
		};
		var loader = new THREE.JSONLoader();
		loader.load(jsonPath, callbackFinished);
	};
	
	var restrictToBounds = function(number, bound) {
		return Math.min(Math.max(number, -bound), bound);
	};

	var render = function() {
		requestAnimationFrame(render);
		
		if(rotateLight) {
			lightRadians += 0.003;
			var sin = Math.sin(lightRadians) * lightDistance;
			var cos = Math.cos(lightRadians) * lightDistance;
			directionalLight.position.set(sin, lightDistance, cos);
		}
		if (shadowVisualizer !== undefined) shadowVisualizer.update();
		// restrict sideways pan
		controls.target.x = restrictToBounds(controls.target.x, 0);
		controls.target.z = restrictToBounds(controls.target.z, 0);
		controls.update();// needed for zoom
		renderer.render(scene, camera);
	};

	var updateCanvasSize = function() {
		width = $canvas.width();
		height = $canvas.height();

		camera.aspect = $canvas.width() / $canvas.height();
		camera.updateProjectionMatrix();
		renderer.setSize(width, height, false);
	};

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

	createScene();
	loadModel();
	createRenderer();
	updateCanvasSize();
	render();
}
var renderer = new CustomRenderer('./models/original.js');
// var renderer = new CustomRenderer('./models/testData_hut.js');