/**
 * Used to build custom three.js lib which only includes needed things and add that to window.
 * When this lib is used normally in a html script tag, it can be retrieved by any module from
 * global window.THREE. Allows for a custom tweaked very compact three.js lib.
 */
// 

// import * as THREE from 'three';
// window.THREE = THREE;

import 'three/src/polyfills.js';

import * as materials from 'three/src/materials/Materials.js';
import * as constants from 'three/src/constants';
// PreviewRenderer dependencies:
import { Scene } from 'three/src/scenes/Scene.js';
import { AmbientLight } from 'three/src/lights/AmbientLight.js';
import { DirectionalLight } from 'three/src/lights/DirectionalLight.js';
import { PointLight } from 'three/src/lights/PointLight.js';
import { DirectionalLightHelper } from 'three/src/helpers/DirectionalLightHelper.js';
import { WebGLRenderer } from 'three/src/renderers/WebGLRenderer.js';
import { JSONLoader } from 'three/src/loaders/JSONLoader.js';
import { Mesh } from 'three/src/objects/Mesh.js';
import { PerspectiveCamera } from 'three/src/cameras/PerspectiveCamera.js';
import { EdgesGeometry } from 'three/src/geometries/EdgesGeometry.js';
import { LineSegments } from 'three/src/objects/LineSegments.js';
import { Line } from 'three/src/objects/Line.js'
import { Object3D } from 'three/src/core/Object3D.js';
// OrbitControls dependencies:
import { EventDispatcher } from 'three/src/core/EventDispatcher.js';
import { Vector2 } from 'three/src/math/Vector2';
import { Vector3 } from 'three/src/math/Vector3';
import { Quaternion } from 'three/src/math/Quaternion';
// MatcapMaterial dependencies:
import { TextureLoader } from 'three/src/loaders/TextureLoader';

window.THREE = {
    Scene,
    AmbientLight,
    DirectionalLight,
    PointLight,
    DirectionalLightHelper,
    WebGLRenderer,
    JSONLoader,
    Mesh,
    PerspectiveCamera,
    EdgesGeometry,
    LineSegments,
    Line,
    Object3D,
    EventDispatcher,
    Vector2,
    Vector3,
    Quaternion,
    TextureLoader
}
for (let material in materials) {
    window.THREE[material] = materials[material];
}
for (let constant in constants) {
    window.THREE[constant] = constants[constant];
}