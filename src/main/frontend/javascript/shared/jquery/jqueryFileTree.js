import $ from "../../lib/jquery";
import Ajax from "../ajax";

// jQuery File Tree Plugin
//
// Version 1.01
//
// Cory S.N. LaViska
// A Beautiful Site (http://abeautifulsite.net/)
// 24 March 2008
//
// Visit http://abeautifulsite.net/notebook.php?article=58 for more information
//
// Usage: $('.fileTreeDemo').fileTree( options, callbackOnSelect )
//
// Options:  root           - root folder to display; default = /
//           script         - location of the serverside AJAX file to use; default = jqueryFileTree.php
//           folderEvent    - event to trigger expand/collapse; default = click
//           expandSpeed    - default = 500 (ms); use -1 for no animation
//           collapseSpeed  - default = 500 (ms); use -1 for no animation
//           expandEasing   - easing function to use on expand (optional)
//           collapseEasing - easing function to use on collapse (optional)
//           multiFolder    - whether or not to limit the browser to one subfolder at a time
//           loadMessage    - Message to display while initial tree loads (can be HTML)
//
// History:
//
// 1.01 - updated to work with foreign characters in directory/file names (12 April 2008)
// 1.00 - released (24 March 2008)
//
// TERMS OF USE
// 
// This plugin is dual-licensed under the GNU General Public License and the MIT License and
// is copyright 2008 A Beautiful Site, LLC. 
//
//
// MODIFIED FOR PRIVATE PROJECT, NOT ORIGINAL VERSION:
//
// Options:  fileClickable - whether or not to call the callbackOnSelect function on file (not directory) click 
//			 directoryClickable - whether or not to call the callbackOnSelect function on directory click
//			 url - alternative name for "script" option 
//
$.extend($.fn, {
	fileTree: function(options, callbackOnSelect, callbackOnCollapse) {
		// Defaults
		if( !options ) options = {};
		if( options.root === undefined ) options.root = '';
		if( options.script === undefined ) {
			if ( options.url === undefined ) options.script = 'jqueryFileTree.php';
			else options.script = options.url;
		}
		if( options.folderEvent === undefined ) options.folderEvent = 'click';
		if( options.expandSpeed === undefined ) options.expandSpeed= 300;
		if( options.collapseSpeed === undefined ) options.collapseSpeed= 300;
		if( options.expandEasing === undefined ) options.expandEasing = null;
		if( options.collapseEasing === undefined ) options.collapseEasing = null;
		if( options.multiFolder === undefined ) options.multiFolder = true;
		if( options.loadMessage === undefined ) options.loadMessage = 'Loading...';
		if( options.fileClickable === undefined ) options.fileClickable = true;
		if( options.directoryClickable === undefined ) options.directoryClickable = true;
		
		$(this).each( function() {
			
			function showTree(c, root) {
				$(c).addClass('wait');
				$(".jqueryFileTree.start").remove();
				Ajax.post(options.script, { dir: root })
					.then(function(data) {
						$(c).find('.start').html('');
						$(c).removeClass('wait').append(data[0]);
						if( options.root == root ) {
							$(c).find('UL:hidden').show();
						} else {
							$(c).find('UL:hidden').slideDown({
								duration: options.expandSpeed,
								easing: options.expandEasing });
						}
						bindTree(c);
					});
			}
			
			function bindTree(t) {
				$(t).find('LI A').bind(options.folderEvent, function() {
					var $parent = $(this).parent();
					if( $parent.hasClass('directory') ) {
						if( $parent.hasClass('collapsed') ) {
							// Expand
							if( !options.multiFolder ) {
								$parent.parent().find('UL').slideUp({
									duration: options.collapseSpeed,
									easing: options.collapseEasing });
								$parent.parent().find('LI.directory')
									.removeClass('expanded').addClass('collapsed');
							}
							$parent.find('UL').remove(); // cleanup
							// removed some pointless matching and escaping, which broke inner folders
							showTree( $parent, $(this).attr('rel'));
							$parent.removeClass('collapsed').addClass('expanded');
						} else {
							// Collapse
							$parent.find('UL').slideUp({
								duration: options.collapseSpeed, easing: options.collapseEasing,
								complete: function() {
									if(callbackOnCollapse !== undefined) callbackOnCollapse($parent);}
								});
							$parent.removeClass('expanded').addClass('collapsed');
						}
						if(options.directoryClickable) {
							callbackOnSelect($(this));
							return false;
						}
					} else {
						if (options.fileClickable) {
							callbackOnSelect($(this));
							return false;
						}
					}
				});
				// Prevent A from triggering the # on non-click events
				if( options.folderEvent.toLowerCase != 'click' ) {
					$(t).find('LI A').bind('click', function() { return false; });
				}
			}
			
			// Loading message
			$(this).html('<ul class="jqueryFileTree start"><li class="wait">' + options.loadMessage + '<li></ul>');
			
			// Get the initial file list
			showTree( $(this), escape(options.root) );
		});
	}
});
export default {};