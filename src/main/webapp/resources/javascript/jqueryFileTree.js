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
// Usage: $('.fileTreeDemo').fileTree( options, callback )
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
// Options:  directoryClickable - whether or not to call the callback function on directory click
//
if(jQuery) (function($){
	
	$.extend($.fn, {
		fileTree: function(options, callback) {
			// Defaults
			if( !options ) options = {};
			if( options.root === undefined ) options.root = '/';
			if( options.script === undefined ) options.script = 'jqueryFileTree.php';
			if( options.folderEvent === undefined ) options.folderEvent = 'click';
			if( options.expandSpeed === undefined ) options.expandSpeed= 500;
			if( options.collapseSpeed === undefined ) options.collapseSpeed= 500;
			if( options.expandEasing === undefined ) options.expandEasing = null;
			if( options.collapseEasing === undefined ) options.collapseEasing = null;
			if( options.multiFolder === undefined ) options.multiFolder = true;
			if( options.loadMessage === undefined ) options.loadMessage = 'Loading...';
			if( options.directoryClickable === undefined ) options.directoryClickable = true;
			
			$(this).each( function() {
				
				function showTree(c, root) {
					$(c).addClass('wait');
					$(".jqueryFileTree.start").remove();
					$.post(options.script, { dir: root })
						.done(function(data) {
							$(c).find('.start').html('');
							$(c).removeClass('wait').append(data);
							if( options.root == root ) $(c).find('UL:hidden').show(); else $(c).find('UL:hidden').slideDown({ duration: options.expandSpeed, easing: options.expandEasing });
							bindTree(c);
						})
						.fail(showException);
				}
				
				function bindTree(t) {
					$(t).find('LI A').bind(options.folderEvent, function() {
						if( $(this).parent().hasClass('directory') ) {
							if( $(this).parent().hasClass('collapsed') ) {
								// Expand
								if( !options.multiFolder ) {
									$(this).parent().parent().find('UL').slideUp({ duration: options.collapseSpeed, easing: options.collapseEasing });
									$(this).parent().parent().find('LI.directory').removeClass('expanded').addClass('collapsed');
								}
								$(this).parent().find('UL').remove(); // cleanup
								showTree( $(this).parent(), $(this).attr('rel'));// removed some pointless matching and escaping, which broke inner folders
								$(this).parent().removeClass('collapsed').addClass('expanded');
							} else {
								// Collapse
								$(this).parent().find('UL').slideUp({ duration: options.collapseSpeed, easing: options.collapseEasing });
								$(this).parent().removeClass('expanded').addClass('collapsed');
							}
							if(options.directoryClickable) {
								callback($(this));
								return false;
							}
						} else {
							callback($(this));
							return false;
						}
					});
					// Prevent A from triggering the # on non-click events
					if( options.folderEvent.toLowerCase != 'click' ) $(t).find('LI A').bind('click', function() { return false; });
				}
				
				// Loading message
				$(this).html('<ul class="jqueryFileTree start"><li class="wait">' + options.loadMessage + '<li></ul>');
				
				// Get the initial file list
				showTree( $(this), escape(options.root) );
			});
		}
	});
	
})(jQuery);