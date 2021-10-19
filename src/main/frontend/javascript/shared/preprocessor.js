import $ from "../lib/jquery";
import Errors from "./Errors";

/**
 * HtmlPreProcessor
 * --------------------------------------------------------------------
 */

const urlToSvg = {};//cache
const urlToSvgPromise = {};//svgs currently getting fetched

/**
 * Copies relevant information from img to svg and replaces img element.
 * @param {HTMLImageElement} img
 * @param {SVGElement} svg
 */
const replaceImgWithSvg = function(img, svg) {
	if (typeof img.id !== "undefined") {
		svg.id = img.id;
	}
	if (typeof img.className !== "undefined") {
		// className property is read-only on svg
		svg.setAttribute("class", img.className + " replaced-svg");
	}
	img.replaceWith(svg);
};

/**
 * Replace all SVG images with inline SVG to allow coloring svgs with css.
 * @param {HTMLElement} parent 
 */
const replaceSvgImages = function(parent) {

	const promises = [];
	const images = parent.querySelectorAll("img.svg");
	for (let img of images) {
		const imgSrc = img.getAttribute("src");
		const imgUrl = imgSrc ? imgSrc : img.getAttribute("data-src");

		if (urlToSvg[imgUrl] !== undefined) {
			// get from cache
			replaceImgWithSvg(img, urlToSvg[imgUrl].cloneNode(true));
		} else {
			let fetching = urlToSvgPromise[imgUrl];

			// if not already being fetched, we populate cache
			if (fetching === undefined) {
				fetching = $.get(imgUrl, function(data) {
					const $svg = $(data).find('svg');
					// Remove any invalid XML tags as per http://validator.w3.org
					$svg.removeAttr('xmlns:a');
					
					urlToSvg[imgUrl] = $svg[0];

					// cleanup
					urlToSvgPromise[imgUrl] = undefined;
					
				}, 'xml');
				urlToSvgPromise[imgUrl] = fetching;
			}

			// get from cache after fetched
			const done = fetching.then(() => {
				replaceImgWithSvg(img, urlToSvg[imgUrl].cloneNode(true));
			});
			promises.push(done);
		}
	}
	return Promise.all(promises);
}

/**
 * Makes all buttons focusable.
 * @param {HTMLElement} parent 
 */
const prepareButtons = function(parent) {
	const buttons = parent.querySelectorAll(".button, .clickable");
	for (let button of buttons) {
		button.setAttribute("tabindex", "0");
	}
};

const makeDraggable = function($range) {
	$range.findSelf(".draggable").fixedDraggable();
};

export default {
	/**
	 * For completely unprocessed elements.
	 * @param {JQuery} parent
	 */
	apply : function($parent) {
		const parent = $parent[0];
		const promise = replaceSvgImages(parent);
		prepareButtons(parent);
		makeDraggable($parent);
		return promise;
	},

	/**
	 * For elements that were cloned from already processed elements.
	 */
	applyOnlyDataAndEvents : function($range) {
		makeDraggable($range);
	},

	/**
	 * Lazy load images.
	 */
	lazyload : function($range) {
		const $images = $range.findSelf("img.lazyload");
		$images.each(function(){
			const $image = $(this);
			$image.attr("src", $image.data("src"));
			$image.removeAttr("data-src");
			$image.removeClass("lazyload");
		});
	}
};
