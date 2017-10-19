/// <reference path="../lib/three.d.ts" />

import $ from "../lib/jquery";
import { contextUrl, waitForFinalEvent } from "./default";
import THREE from '../lib/three';
import MatcapMaterial from './MatcapMaterial';

/** @module CanvasRenderer */

 /**
 * Shading type enum.
 * @enum {string}
 */
const ShadingType = {
    Matcap: "matcap",
    Solid: "solid",
    None: "none"
};

/**
 * @typedef RenderOptions
 * @type {object}
 * @property {ShadingType} shading
 * @property {boolean} wireframe
 * @property {boolean} rotateCamera
 * @property {double} rotateCameraSpeed
 * Only relevant if shading is set to Solid:
 * @property {boolean} shadowsEnabled
 * @property {boolean} rotateLight
 */

/** Called on every frame.
 * @callback animationCallback
 */

 /** Create scene with lights & needed for solid shading.
 * @param {jquery} $canvas
 * @param {animationCallback[]} animationCallbacks 
 */
var createScene = (function () {
    var createSceneFunc = function($canvas, animationCallbacks) {
        var scene = new THREE.Scene();
        // lights & directional light shadow visualizer
        var ambientLight = new THREE.AmbientLight(0x333333);
        scene.add(ambientLight);
    
        var lightDistance = 120;
        var directionalLight = new THREE.DirectionalLight(0xbbbbbb);
        directionalLight.position.set(lightDistance, lightDistance, lightDistance);
        scene.add(directionalLight);
    
        var shadowPadding = 30;
        var shadowVisualizer = createShadowVisualizer(directionalLight, shadowPadding, animationCallbacks);
        scene.add(shadowVisualizer);

        setupShadows(directionalLight, shadowPadding);
        
        var rotateLight;
    
        var lightRadians = 0;
        animationCallbacks.push(function() {
            if(rotateLight) {
                lightRadians += 0.003;
                var sin = Math.sin(lightRadians) * lightDistance;
                var cos = Math.cos(lightRadians) * lightDistance;
                directionalLight.position.set(sin, lightDistance, cos);
            }
        });
    
        $canvas.on("renderOptionsChange", function(event) {
            /**@type {RenderOptions} */
            let options = event.detail;
            // enable lights/shadow only when shading type is solid

            let isShadingSolid = options.shading === ShadingType.Solid;
            ambientLight.visible = isShadingSolid;
            directionalLight.visible = isShadingSolid;

            let isShadowsVisible = isShadingSolid && options.shadowsEnabled;
            shadowVisualizer.visible = isShadowsVisible;
            directionalLight.castShadow = isShadowsVisible;

            rotateLight = isShadingSolid && options.rotateLight;
        });
        
        return scene;
    }

    var createShadowVisualizer = function(light, shadowPadding, animationCallbacks) {
        var shadowVisualizer = new THREE.DirectionalLightHelper(light, shadowPadding);
        animationCallbacks.push(function() {
            if(shadowVisualizer.visible) {
                shadowVisualizer.update();
            }
        });
        return shadowVisualizer;
    };

    var setupShadows = function(light, shadowPadding) {
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
    };

    return createSceneFunc;
})();


var createRenderer = function($canvas) {
    var renderer = new THREE.WebGLRenderer({canvas:$canvas[0], antialias: true});
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.shadowMapSoft = true;

    $canvas.on("renderOptionsChange", function(event) {
        /**@type {RenderOptions} */
        var options = event.detail;
        var isShadowsEnabled = (options.shading === ShadingType.Solid) && options.shadowsEnabled;
        renderer.shadowMap.enabled = isShadowsEnabled;
    });
    return renderer;
};

/**
 * Load geometry from json and create a new material.
 * Make mesh from geometry and material and add to scene.
 */
var loadMeshIntoScene = function($canvas, jsonPath, scene) {
    var loader = new THREE.JSONLoader();

    // to not miss out on the first renderOptionsChange event,
    // we should register event handler immediatly, but cant because load is async
    //  => cache events for loader, until loader can register proper listener
    var chached;
    var cacheRenderOptions = function(event) {
        chached = event.detail;
    };
    $canvas.on("renderOptionsChange", cacheRenderOptions);

    var wireOffsetSettings = {
        polygonOffset: true,
        polygonOffsetFactor: 0.2,
        polygonOffsetUnits: 1
    }
    var matcapMaterial = new MatcapMaterial(contextUrl + "/res/gfx/matcap/softred.jpg", wireOffsetSettings);
    var solidMaterial = new THREE.MeshLambertMaterial(Object.assign(wireOffsetSettings, { 
        color: 0xdddddd
    }));

    loader.load(jsonPath, function(geometry, materials) {
        var mesh = new THREE.Mesh(geometry);
        mesh.
        geometry.uvsNeedUpdate = true;

        scene.add(mesh);
        // wireframe (see https://stackoverflow.com/questions/31539130/display-wireframe-and-solid-color)
        var wireGeometry = new THREE.EdgesGeometry(geometry);
        var wireMaterial = new THREE.LineBasicMaterial({
             color: 0xffffff,
             linewidth: 1,
        });
        // TODO Bugreport LineSegments not having a proper prototype chain (LineSegments -> Line -> Object3D missing)
        var wireframe = new THREE.Line(wireGeometry, wireMaterial);
        wireframe.isLineSegments = true;
        scene.add(wireframe);
        // proper handler
        $canvas.on("renderOptionsChange", function(event) {
            setOptions(event.detail, mesh, wireframe);
        });
        // remove cache-handler and apply options from cache if event already occured
        $canvas.off("renderOptionsChange", cacheRenderOptions);
        if(chached) {
            setOptions(chached, mesh, wireframe);
        }
    });
    /**
     * @param {RenderOptions} options 
     * @param {THREE.Mesh} mesh 
     * @param {THREE.LineSegments} wire 
     */
    var setOptions = function(options, mesh, wire) {
        // material
        mesh.visible = options.shading !== ShadingType.None;
        if (options.shading === ShadingType.Matcap) {
            mesh.material = matcapMaterial;
        } else if (options.shading === ShadingType.Solid) {
            mesh.material = solidMaterial;
        }
        // shadows
        var shadowsEnabled = (options.shading === ShadingType.Solid) && options.shadowsEnabled;
        mesh.castShadow = shadowsEnabled;
        mesh.receiveShadow = shadowsEnabled;
        // wireframe
        wire.visible = options.wireframe;        
    };
};

function Size2D(width, height) {
    this.width = width;
    this.height = height;
    this.equals = function(size) {
        return size.width === width && size.height === height;
    };
}

/**
 * @param {Size2D} canvasSize - width & height used to update camera & renderer
 */
var updateCanvasSize = function(canvasSize, camera, renderer) {
    var width = canvasSize.width;
    var height = canvasSize.height;

    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height, false);
};



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
var CanvasRenderer = function (data, animationCallbacks) {
    
    var $canvas = data.$canvas;
    var geometryPath = data.geometryPath;
    var camera = data.camera;
    var scene = createScene($canvas, animationCallbacks);
    loadMeshIntoScene($canvas, geometryPath, scene);
    var renderer = createRenderer($canvas);
    
    var canvasSize = new Size2D(0, 0);
    // checks if canvas size changed and updates stuff accordingly
    var checkResize = function() {
        var $parent = $canvas.parent();
        var newCanvasSize = new Size2D($parent.width(), $parent.height());
        if(!canvasSize.equals(newCanvasSize)) {
            console.log(newCanvasSize);
            canvasSize = newCanvasSize;
            waitForFinalEvent(function() {
                updateCanvasSize(canvasSize, camera, renderer);
            }, 50, geometryPath);
        }
    };
    
    return {
        render : function() {
            checkResize();
            renderer.render(scene, camera);
        },
        destroy : function() {
            renderer.forceContextLoss();
            renderer.context = null;
            renderer.domElement = null;
            renderer = null;
        }
    };
};

export { CanvasRenderer, ShadingType };