/* jshint esnext:true */
import $ from "../lib/jquery";

/**
 * @author Florian Reuschel
 */
$.fn.fixedDraggable = function() {
	this.each(function () {
	
		var $move = $(this);
		var initialOffsetX;
		var initialOffsetY;

		// function which will be bound to mouseover
		var moveContainer = function(event) {
			//newMovePos = (newMousePos+scroll) - initialOffset - scroll
			//           = newMousePos - initialOffset
			var left = event.pageX - initialOffsetX - $(window).scrollLeft();
			var top = event.pageY - initialOffsetY - $(window).scrollTop();

			// use CSS3-transform-Property for better performance
			$move.css("transform",
					"translateX(" + left + "px) " +
					"translateY(" + top + "px)");
		};

		$move.mousedown(function(event) {
			if(event.target !== this) return;
			
			// set top/left to zero and use translation instead
			var left =  $move.offset().left - $(window).scrollLeft();
	        var top = $move.offset().top - $(window).scrollTop();
			$move.css({
				"transform": "translateX(" + left + "px) translateY(" + top + "px)",
				"left": 0,
				"top": 0
			});
			
			//initialOffset = (mousePos+scroll) - (movePos+scroll)
			//              = mousePos - movePos
			initialOffsetX = event.pageX - $move.offset().left;
			initialOffsetY = event.pageY - $move.offset().top;

			// only move, do not trigger other events
			event.preventDefault();

			// bind to mousemove
			$(window).mousemove(moveContainer);

			// trigger only on next mouseup
			$(window).one("mouseup", function() {
				// unbind from mousemove        
				$(window).off("mousemove", moveContainer);
			});
		});
	});
};
export default {};