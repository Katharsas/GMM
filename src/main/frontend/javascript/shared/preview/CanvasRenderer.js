/// <reference path="../../lib/three.d.ts" />

import $ from "../../lib/jquery";
import { contextUrl, waitForFinalEvent } from "../default";
import THREE from '../../lib/three';
import MatcapMaterial from './MatcapMaterial';
import uuid from 'uuid/v1'; // fast, not secure random!, see https://github.com/mcollina/hyperid

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

var createScene = (function () {
    /** Create scene with lights & needed for solid shading.
     * @param {THREE.Object3D[]} objects - The objects to put inside the scene.
     * @param {function[]} animationCallbacks - All functions that need to be called per frame will be pushed into this array.
     */
    var createSceneFunc = function($canvas, objects, animationCallbacks) {
        var scene = new THREE.Scene();
        for (var object of objects) {
            scene.add(object);
        }
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

/**
 * @param {string} jsonPath - Url to json mesh file.
 */
var loadMesh= function($canvas, jsonPath) {
    var loader = new THREE.JSONLoader();

    var wireOffsetSettings = {
        polygonOffset: true,
        polygonOffsetFactor: 0.2,
        polygonOffsetUnits: 1
    }
    var matcapMaterial = new MatcapMaterial(contextUrl + "/res/gfx/matcap/softred.jpg", wireOffsetSettings);
    var solidMaterial = new THREE.MeshLambertMaterial(Object.assign(wireOffsetSettings, { 
        color: 0xdddddd
    }));

     /**
     * @param {RenderOptions} options 
     * @param {THREE.Mesh} mesh 
     */
    var setOptions = function(options, mesh) {
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
    };

    return new Promise(function(resolve, reject) {

        loader.load(jsonPath, function(geometry, materials) {
            geometry.computeBoundingBox();
            var mesh = new THREE.Mesh(geometry);
            //geometry.uvsNeedUpdate = true;
            
            //var boundingBox = geometry.boundingBox.clone();
            
            $canvas.on("renderOptionsChange", function(event) {
                setOptions(event.detail, mesh);
            });
            resolve(mesh);
        });
    });
   
};

/**
 * @param {THREE.Mesh} mesh 
 */
var createWireframe = function($canvas, mesh) {
    // wireframe (see https://stackoverflow.com/questions/31539130/display-wireframe-and-solid-color)
    var wireGeometry = new THREE.EdgesGeometry(mesh.geometry);
    var wireMaterial = new THREE.LineBasicMaterial({
        color: 0xffffff,
        linewidth: 1,
    });
    // TODO Bugreport LineSegments not having a proper prototype chain (LineSegments -> Line -> Object3D missing)
    // probably something wrong with custom three.js build (classes not supported by webpack?)
    var wireframe = new THREE.Line(wireGeometry, wireMaterial);
    wireframe.isLineSegments = true;
    $canvas.on("renderOptionsChange", function(event) {
        /** @type {RenderOptions} */
        var options = event.detail;
        wireframe.visible = options.wireframe;
    });
    return wireframe;
}

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
 * @param {THREE.WebGLRenderer} renderer 
 */
var destroyRenderer = function(renderer) {
    renderer.forceContextLoss();
    renderer.context = null;
    renderer.domElement = null;
    renderer = null;
};

/**
 * @typedef Size2D
 * @property {number} width 
 * @property {number} height
 * 
 * @param {number} width
 * @param {number} height
 */
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
 * @typedef CanvasRenderer
 * @type {object}
 * @property {function(string) : Promise<THREE.Mesh>} loadMesh
 * @property {function(THREE.Mesh) : THREE.LineSegments} createWireframe
 * @property {function(THREE.Object3D[], any[]) : THREE.Scene} createScene
 * @property {function() : THREE.WebGLRenderer} createRenderer
 * @property {function(THREE.WebGLRenderer) : void} destroyRenderer
 * @property {function(Size2D, THREE.Camera, THREE.WebGLRenderer) : Size2D} updateCanvasSizeIfChanged
 * @property {function(THREE.WebGLRenderer, THREE.Scene, THREE.Camera) : void} render
 */

/**
 * Can render a single 3D model into a single canvas.
 * @listens PreviewRenderer#renderOptionsChange
 * @param {JQuery} $canvas
 * @returns {CanvasRenderer}
 */
var CanvasRenderer = function ($canvas) {
    
    var thisId = uuid();
    // checks if canvas size changed and updates stuff accordingly
    var checkResize = function($canvas, canvasSize, camera, renderer) {
        var $parent = $canvas.parent();
        var newCanvasSize = new Size2D($parent.width(), $parent.height());
        if(!canvasSize.equals(newCanvasSize)) {
            canvasSize = newCanvasSize;
            waitForFinalEvent(function() {
                updateCanvasSize(canvasSize, camera, renderer);
            }, 50, thisId);
        }
        return newCanvasSize;
    };
    
    return {
        /**
         * @returns {Promise} - parameter: {THREE.Geometry}
         */
        loadMesh : function(geometryPath) {
            return loadMesh($canvas, geometryPath);
        },
        /**
         * @returns {THREE.LineSegments}
         */
        createWireframe : function(mesh) {
            return createWireframe($canvas, mesh);
        },
        /**
         * @returns {THREE.Scene}
         */
        createScene : function(objects, animationCallbacks) {
            // create scene, create mesh from geometry, prepare materials, add stuff to scene
            return createScene($canvas, objects, animationCallbacks);
        },
        /**
         * @returns {THREE.WebGLRenderer}
         */
        createRenderer : function() {
            return createRenderer($canvas);
        },
        destroyRenderer : function(renderer) {
            destroyRenderer(renderer);
        },
        /**
         * Can be called every frame, has independent internal update rate.
         * @returns {Size2D} - New size if changed, old size if not.
         */
        updateCanvasSizeIfChanged : function(canvasSize, camera, renderer) {
            return checkResize($canvas, canvasSize, camera, renderer);
        },
        /**
         * Call every frame to render to canvas.
         * @param {THREE.WebGLRenderer} renderer
         */
        render : function(renderer, scene, camera) {
            renderer.render(scene, camera);
        },
    };
};

export { CanvasRenderer, ShadingType, Size2D };