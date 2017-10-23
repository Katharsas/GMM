/**
 * Used to build custom three.js lib which only includes needed things and add that to window.
 * When this lib is used normally in a html script tag, it can be retrieved by any module from
 * global window.THREE. Allows for a custom tweaked very compact three.js lib.
 */

import 'three/src/polyfills.js';

export * from 'three/src/materials/Materials.js';
export * from 'three/src/constants';

export { Vector2 } from 'three/src/math/Vector2';
export { Vector3 } from 'three/src/math/Vector3';
export { Quaternion } from 'three/src/math/Quaternion';
export { Object3D } from 'three/src/core/Object3D.js';
export { Line } from 'three/src/objects/Line.js'
export { LineSegments } from 'three/src/objects/LineSegments.js';
// PreviewRenderer dependencies:
export { Scene } from 'three/src/scenes/Scene.js';
export { AmbientLight } from 'three/src/lights/AmbientLight.js';
export { DirectionalLight } from 'three/src/lights/DirectionalLight.js';
export { PointLight } from 'three/src/lights/PointLight.js';
export { DirectionalLightHelper } from 'three/src/helpers/DirectionalLightHelper.js';
export { WebGLRenderer } from 'three/src/renderers/WebGLRenderer.js';
export { JSONLoader } from 'three/src/loaders/JSONLoader.js';
export { Mesh } from 'three/src/objects/Mesh.js';
export { PerspectiveCamera } from 'three/src/cameras/PerspectiveCamera.js';
export { EdgesGeometry } from 'three/src/geometries/EdgesGeometry.js';
export { BoxGeometry } from 'three/src/geometries/BoxGeometry.js';
export { BoxHelper } from 'three/src/helpers/BoxHelper.js';
// OrbitControls dependencies:
export { EventDispatcher } from 'three/src/core/EventDispatcher.js';
// MatcapMaterial dependencies:
export { TextureLoader } from 'three/src/loaders/TextureLoader';