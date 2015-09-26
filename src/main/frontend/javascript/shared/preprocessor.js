/* jshint esnext:true */
import $ from "../lib/jquery";

/**
 * HtmlPreProcessor
 * --------------------------------------------------------------------
 */
export default (function() {
	var urlTo$svg = {};//cache
	var urlTo$imgs = {};//asynch cache
	
	/**
	 * Copies relevant information from $img to $svg and replaces the code.
	 */
	var replaceImgWithSvg = function($img, $svg) {
		var imgID = $img.attr('id');
	    var imgClass = $img.attr('class');
	    
	    // Add replaced image's ID to the new SVG
        if(typeof imgID !== 'undefined') {
            $svg = $svg.attr('id', imgID);
        }
        // Add replaced image's classes to the new SVG
        if(typeof imgClass !== 'undefined') {
            $svg = $svg.attr('class', imgClass+' replaced-svg');
        }
        
        $img.replaceWith($svg);
	};
	
	var replaceSvgImages = function($range) {
		/* Replace all SVG images with inline SVG
		 * from: http://stackoverflow.com/questions/11978995/how-to-change-color-of-svg-image-using-css-jquery-svg-image-replacement
		 */
		$range.find('img.svg').each(function(){
			
		    var $img = $(this);
		    var imgURL = $img.attr('src');
		    
		    //look into cache
		    if (urlTo$svg[imgURL] !== undefined) {
		    	replaceImgWithSvg($img, urlTo$svg[imgURL].clone());
		    }
		    //look into async cache
		    else if(urlTo$imgs[imgURL] !== undefined) {
		    	//queue image for replacing when first request ready
		    	urlTo$imgs[imgURL].push($img);
		    }
		    else {
		    	//initialize async cache queue
		    	urlTo$imgs[imgURL] = [];
		    	
		    	$.get(imgURL, function(data) {
			        var $svg = $(data).find('svg');
			        // Remove any invalid XML tags as per http://validator.w3.org
			        $svg = $svg.removeAttr('xmlns:a');
			        
			        //put svg into cache
			        urlTo$svg[imgURL] = $svg;
			        
			        replaceImgWithSvg($img, $svg);

			        //process asynch cache queue
			        urlTo$imgs[imgURL].forEach(function($img) {
			        	replaceImgWithSvg($img, $svg.clone());
			        });
			        //destroy asynch cache queue
			        delete urlTo$imgs[imgURL];
			        
			    }, 'xml');
		    }
		});
	};
	
	/**
	 * Makes all buttons focusable and binds click event to enter key.
	 */
	var prepareButtons = function($range) {
		$range.find(".button").each(function(){
			var $button = $(this);
			$button.attr("tabindex", "0");
			$button.onEnter(function() {
				$button.click();
			});
		});
	};
	
	return {
		apply : function($range) {
			replaceSvgImages($range);
			prepareButtons($range);
		}
	};
})();
