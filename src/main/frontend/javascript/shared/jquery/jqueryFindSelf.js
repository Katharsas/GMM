import $ from "../../lib/jquery";

/**
 * http://stackoverflow.com/a/31586013
 * 
 * @param {Selector} selector - If null, return the current set of matched elements.
 * 		Else, return elements from find(elements) and add current set if current selector
 * 		matches the given selector.
 */
$.fn.findSelf = function(selector) {
	if (selector === null) return this;
	else {
		var result = this.find(selector);
	    return (this.is(selector)) ?
	        result.add(this) : result;
	}
};