


var IllegalArgumentError = function(message) {
	this.message = message;
};
IllegalArgumentError.prototype = new Error();

var Errors = {};
Errors.IllegalArgumentError = IllegalArgumentError;

//for (var constructor in Errors) {
//	console.log(constructor);
//	constructor.prototype = new Error();
//}

export default Errors;

