function IllegalArgumentException(message) {
    this.name = "IllegalArgumentException";
    this.message = message;
    this.stack = (new Error()).stack;
}
IllegalArgumentException.prototype = Object.create(Error.prototype);
IllegalArgumentException.prototype.constructor = IllegalArgumentException;

function IllegalStateException(message) {
    this.name = "IllegalStateException";
    this.message = message;
    this.stack = (new Error()).stack;
}
IllegalStateException.prototype = Object.create(Error.prototype);
IllegalStateException.prototype.constructor = IllegalStateException;

export default {
    IllegalArgumentException,
    IllegalStateException
};

