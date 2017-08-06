/**
 * Used to build custom three.js lib which only includes needed things and add that to window.
 * When this lib is used normally in a html script tag, it can be retrieved by any module from
 * global window.THREE. Allows for a custom tweaked very compact three.js lib.
 */
// 

// import * as THREE from 'three';
// window.THREE = THREE;

import 'three/src/polyfills.js';

// PreviewRenderer dependencies:
import { Scene } from 'three/src/scenes/Scene.js';
import { AmbientLight } from 'three/src/lights/AmbientLight.js';
import { DirectionalLight } from 'three/src/lights/DirectionalLight.js';
import { DirectionalLightHelper } from 'three/src/helpers/DirectionalLightHelper.js';
import { WebGLRenderer } from 'three/src/renderers/WebGLRenderer.js';
import { JSONLoader } from 'three/src/loaders/JSONLoader.js';
import { MeshLambertMaterial } from 'three/src/materials/Materials.js';
import { Mesh } from 'three/src/objects/Mesh.js';
import { PerspectiveCamera } from 'three/src/cameras/PerspectiveCamera.js';
// OrbitControls dependencies:
import { EventDispatcher } from 'three/src/core/EventDispatcher.js';
import { Vector2 } from 'three/src/math/Vector2';
import { Vector3 } from 'three/src/math/Vector3';
import { Quaternion } from 'three/src/math/Quaternion';
import { MOUSE } from 'three/src/constants';

window.THREE = {
    Scene,
    AmbientLight,
    DirectionalLight,
    DirectionalLightHelper,
    WebGLRenderer,
    JSONLoader,
    MeshLambertMaterial, 
    Mesh,
    PerspectiveCamera,
    EventDispatcher,
    Vector2,
    Vector3,
    Quaternion,
    MOUSE,
}