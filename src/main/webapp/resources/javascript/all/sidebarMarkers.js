/**
 * -------------------- SidebarMarkers -----------------------------------------
 * Allows you to create markers for elements of the list in a sidebar.
 * Markers are little GUI elements in the sidebar, which position themselves next
 * to the element they mark, if they can. If the element is not on screen, they 
 * stick to the bottom or top of the sidebar, but they are always visible.
 * 
 * They can be used as quickjump to selected elements of a list for example.
 * Multiple sidemarkers are possible. Its also possible to register real sidebars
 * to integrate them with the markers.
 * 
 * This implementation is dependent on special sidebarMarkers CSS.
 * 
 * @param markerFactory - function which returns a jQuery marker object
 * 
 * @see http://jsfiddle.net/Ldmy2uzo/6/ (author Jan Mothes)
 * @see http://jsfiddle.net/Ldmy2uzo/3/ (author Florian Reuschel)
 * @author Jan Mothes
 */
var SidebarMarkers = function(markerFactory, markerMargin) {
	//static sidebars, id to jQuery mapping, used to caluclate height offset
	const topBars = {};
	const bottomBars = {};
	
	//task ids ordered by position in the list.
	const ids = [];
	
	//maps id selectors to corresponding jQuery elements
	const elements = {};
	
	//maps id selectors to onScroll handlers 
	const handlers = {};
	
	//maps id selectors of elements to jQuery objects of markers which belong to these elements
	const markers = {};
	
	const reorderIds = function() {
		ids.sort(function(id1, id2) {
			return elements[id1].offset().top - elements[id2].offset().top;
		});
	};
	
	return {
		/**
		 * Register a fixed sidebar so markers to not float over it.
		 * 
		 * @param sideBar:String - id of sidebar, may have dynamic height
		 * @param isOnTop:boolean - sidebar position (top or bottom)
		 */
		registerSidebar : function(sideBarId, isOnTop) {
			if(isOnTop) topBars[sideBarId] = $(sideBarId);
			else 	bottomBars[sideBarId] = $(sideBarId);
		},
		
		/**
		 * Unregister fixed sidebar, markers will ignore it.
		 * @see function registerSidebar
		 */
		 unregisterSidebar : function(sideBarId) {
			delete topBars[sideBarId];
			delete bottomBars[sideBarId];
		},
		
		/**
		 * Manually update marker psotions (for example when animating sidebars).
		 */
		update : function() {
			Object.keys(handlers).forEach(function(key) {
				handlers[key]();
			});
		},
		
		/**
		 * TODO: add sidebar-marked to list element to make it relative, remove
		 * @param id - id of list element to which a marker needs to be added
		 */
		addMarker : function(id) {
			//add to arrays/maps
			ids.push(id);
			elements[id] = $(id);
			reorderIds();
			
			var $marked = elements[id];
			var $marker = markerFactory().addClass('sidebar-marker');
			//var $marker = $('<div>').addClass('sidebar-marker').html('Marker');
			markers[id] = $marker;
	
			// create marker and place it inside of the marked element so it will automatically have the right position
			$marked.prepend($marker);
			
			// resposition method (event handler)
			var repositionMarker = function() {
				// if marked element is out of view, put the marker at the top or
				// bottom of the page (always towards the marked element)
				var viewY = $(window).height();
				var scrollY = $(window).scrollTop();
				var markedPosY = $marked.offset().top;
				var markedY = $marked.outerHeight();
				var markerY = $marker.outerHeight();
				
				//calculate index for offset from other markers
				var index = ids.indexOf(id);
				var indexInverted = ids.length - index - 1;
				
				//offset from registered sidebars
				var topBarOffset = 0;
				Object.keys(topBars).forEach(function(key) {
					topBarOffset += topBars[key].outerHeight();
				});
				var bottomBarOffset = 0;
				Object.keys(bottomBars).forEach(function(key) {
					bottomBarOffset += bottomBars[key].outerHeight();
				});

				//complete offset from top/bottom view border
				var bottomOffset = bottomBarOffset + indexInverted * (markerY + markerMargin) + markerMargin;
				var topOffset = topBarOffset + index * (markerY + markerMargin) + markerMargin;
				
				// marked element is too low => marker to bottom
				if (scrollY + viewY - bottomOffset < markedPosY + markerY) {
					$marker.addClass("sidebar-marker-fixed");
					$marker.css("top", "auto");
					$marker.css("bottom", bottomOffset + "px");
				}
				// marked element is too high => marker to top
				else if (scrollY + topOffset > markedPosY) {
					$marker.addClass("sidebar-marker-fixed");
					$marker.css("bottom", "auto");
					$marker.css("top", topOffset + "px");
				}
				// marked element is on view => marker to marked lement
				else {
					$marker.removeClass('sidebar-marker-fixed');
					$marker.removeAttr("style");
				}
			};
			// bind reposition method to scroll event
			$(window).scroll(repositionMarker);
			handlers[id] = repositionMarker;
	
			// update positions of all markers
			this.update();
		},
		
		/**
		 * @param id - id of list element to from which the marker needs to be removed
		 */
		removeMarker : function(id) {
			//remove marker html
			markers[id].remove();
			
			//remove from arrays/maps
			var index = ids.indexOf(id);
			array.splice(index, 1);
			delete elements[id];
			
			//unbind reposition method
			$(window).off("onscroll", handlers[id]);
		}
	};
};