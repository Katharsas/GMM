import rollupResolve from 'rollup-plugin-node-resolve';
import babel from 'rollup-plugin-babel';

function glsl() {

	return {

		transform( code, id ) {

			if ( /\.glsl$/.test( id ) === false ) return;

			var transformedCode = 'export default ' + JSON.stringify(
				code
					.replace( /[ \t]*\/\/.*\n/g, '' ) // remove //
					.replace( /[ \t]*\/\*[\s\S]*?\*\//g, '' ) // remove /* */
					.replace( /\n{2,}/g, '\n' ) // # \n+ to \n
			) + ';';
			return {
				code: transformedCode,
				map: { mappings: '' }
			};

		}

	};

}

export default {
	entry: 'javascript/lib/threeSmall.js',
	indent: '\t',
	plugins: [
		rollupResolve(),
		glsl(),
		babel({
			babelrc: false,
			exclude: 'node_modules/**',
			presets: [ 
				["es2015", { "modules" : false } ]
			],
		}),
	],
	format: 'umd',
};
