/**
 * Class CustomRenderer.
 * Dependencies: JQuery, Three.js, OrbitControls.js
 */
function CustomRenderer() {

	var $canvas, width, height;
	var scene, camera, renderer, controls;
	var boxMesh;

	var createScene = function () {
		$canvas = $('canvas#canvas');
		$(window).resize(function() {waitForFinalEvent(updateCanvasSize, 50, "canvresz");});
		
		scene = new THREE.Scene();

		camera = new THREE.PerspectiveCamera( 40, 1, 1, 10000 );
		camera.position.y = 20;
		camera.position.z = 50;
		
		controls = new THREE.OrbitControls( camera );
		// controls.position0.set( 0, 0, 0 );
		// controls.reset();
		// use this to invoke render on listener, but animation invokes rendering anyway.
		// controls.addEventListener( 'change', render );
		// whatever
		// controls.target.set( 0, 0, 0 );
		
//		var geometry = new THREE.BoxGeometry( 2, 2, 2 );
//		var material = new THREE.MeshBasicMaterial( { color: 0xff0000, wireframe: false } );
//		var material = new THREE.MeshLambertMaterial( );
//		boxMesh = new THREE.Mesh( geometry, material );

//		scene.add( boxMesh );
		
		//ambient light
		var ambientLight = new THREE.AmbientLight(0x111122);
		scene.add(ambientLight);

		// directional lighting
		var directionalLight = new THREE.DirectionalLight(0xffffff);
		directionalLight.position.set(10, 10, 10).normalize();
		scene.add(directionalLight);
	};
	
	var createRenderer = function() {
		renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
	};
	
	var loadModel = function() {
		var callbackFinished = function(geometry, materials) {
//			var material = new THREE.MeshBasicMaterial( { color: 0xff0000, wireframe: true } );
			var material = new THREE.MeshLambertMaterial( );
			var mesh = new THREE.Mesh( geometry, material );
			scene.add(mesh);
		};
		var loader = new THREE.JSONLoader();
		loader.load('./ship.js', callbackFinished);
	};

	var render = function() {
		requestAnimationFrame(render);

//		boxMesh.rotation.y += 0.005;
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