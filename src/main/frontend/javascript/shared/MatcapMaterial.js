/// <reference path="../lib/three.d.ts" />
// from https://www.npmjs.com/package/three-matcap-material
// License: ISC (see https://spdx.org/licenses/ISC.html)

import THREE from '../lib/three';

const vs = `
varying vec2 vN;

void main() {

    vec4 p = vec4( position, 1. );

    vec3 e = normalize( vec3( modelViewMatrix * p ) );
    vec3 n = normalize( normalMatrix * normal );

    vec3 r = reflect( e, n );
    float m = 2. * sqrt( 
        pow( r.x, 2. ) + 
        pow( r.y, 2. ) + 
        pow( r.z + 1., 2. ) 
    );
    vN = r.xy / m + .5;

    gl_Position = projectionMatrix * modelViewMatrix * p;

}
`

const fs = `
uniform sampler2D tMatCap;

varying vec2 vN;

void main() {
    
    vec3 base = texture2D( tMatCap, vN ).rgb;
    gl_FragColor = vec4( base, 1. );

}
`

const loader = new THREE.TextureLoader();

export default function(matcapImage, settings) {
    // see node_modules/three-matcab-material/assets for more textures
    var internalSettings = {
        uniforms: { 
            tMatCap: { 
                type: 't', 
            },
        },
        vertexShader: vs,
        fragmentShader: fs
    };
    var material =  new THREE.ShaderMaterial(Object.assign(internalSettings, settings));
    
    loader.load(matcapImage, function(texture){
        material.uniforms.tMatCap.value = texture;
    });
    return material;
};
