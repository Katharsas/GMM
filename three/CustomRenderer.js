/**
 * Class CustomRenderer.
 * Dependencies: JQuery, Three.js, OrbitControls.js
 */
function CustomRenderer() {

	var $canvas, width, height;
	var scene, camera, renderer, controls;
	var boxMesh;
	var directionalLight;
	
	var directionalLightRadians = 0;
	var shadowsEnabled = true;
	var rotateLight = true;

	var createScene = function () {
		$canvas = $('canvas#canvas');
		$(window).resize(function() {waitForFinalEvent(updateCanvasSize, 50, "canvresz");});
		
		scene = new THREE.Scene();

		camera = new THREE.PerspectiveCamera( 40, 1, 1, 10000 );
		camera.position.y = 20;
		camera.position.z = 50;
		
		controls = new THREE.OrbitControls( camera );
		
		//ambient light
		var ambientLight = new THREE.AmbientLight(0x181818);
		scene.add(ambientLight);

		// directional lighting
		directionalLight = new THREE.DirectionalLight(0xffffff);
		directionalLight.position.set(100, 100, 100);
		
		if(shadowsEnabled) {
			directionalLight.castShadow = true;
			
			directionalLight.shadowCameraNear = 100;
			directionalLight.shadowCameraFar = 300;
			
			directionalLight.shadowCameraLeft = -50;
			directionalLight.shadowCameraRight = 50;
			directionalLight.shadowCameraTop = 50;
			directionalLight.shadowCameraBottom = -50;
			
			directionalLight.shadowDarkness = 0.5;
			directionalLight.shadowBias = 0.0001;
			
			directionalLight.shadowMapWidth = 2048;
			directionalLight.shadowMapHeight = 2048;
			
			directionalLight.shadowCameraVisible = true;
		}
		scene.add(directionalLight);
	};
	
	var createRenderer = function() {
		renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
		if(shadowsEnabled) {
			renderer.shadowMapType = THREE.PCFSoftShadowMap;
			renderer.shadowMapEnabled = true;
			renderer.shadowMapSoft = true;
		}
	};
	
	var loadModel = function() {
		var callbackFinished = function(geometry, materials) {
//			var material = new THREE.MeshBasicMaterial( { color: 0xff0000, wireframe: true } );
			var material = new THREE.MeshLambertMaterial( );
			var mesh = new THREE.Mesh( geometry, material );
			if(shadowsEnabled) {
				mesh.castShadow = true;
				mesh.receiveShadow = true;
			}
			scene.add(mesh);
		};
		var loader = new THREE.JSONLoader();
		loader.load('./models/original.js', callbackFinished);
	};

	var render = function() {
		requestAnimationFrame(render);
		
		if(rotateLight) {
			directionalLightRadians += 0.002;
			var sin = Math.sin(directionalLightRadians) * 100;
			var cos = Math.cos(directionalLightRadians) * 100;
			directionalLight.position.set(sin, 100, cos);
		}
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

var cubeRenderer = new CustomRenderer();