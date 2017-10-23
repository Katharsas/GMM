// import rollupResolve from 'rollup-plugin-node-resolve';
var rollupResolve = require('rollup-plugin-node-resolve');

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

// export default { input... }
exports.input = function(target) {
	return {
		input: target,
		plugins: [
			rollupResolve(),
			glsl()
		],
	};
};
exports.output = function() {
	return {
		indent: '\t',
		format: 'iife',
		name: 'THREE'
	};
};