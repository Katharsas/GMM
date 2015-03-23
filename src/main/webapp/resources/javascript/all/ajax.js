/**
 * Ajax convenience functions, expecting JSON response from server (if any) 
 * and return jqXHR when possible. Includes form submitting.
 * 
 * @author Jan Mothes
 */
var Ajax = (function() {
	var getAjaxDefaultSettings = function(url, data, settings) {
		if (url === undefined || url === null) return undefined;
		var result = {
			url: url,
			headers: {accept:"application/json,*/*;q=0.8"},
		};
		if (data !== undefined && data !== null) {
			result.data = data;
		}
		if (settings !== undefined && settings !== null) {
			for(var key in settings) {
				result[key] = settings[key];
			}
		}
		return result;
	};
	var remove = function (url, data, $form) {
		var settings = getAjaxDefaultSettings(url, data, {
			type: "DELETE",
		});
		return (($form === undefined) ?
				$.ajax(settings) : $form.ajaxSubmit(settings).data('jqxhr'))
				.fail(showException);
	};
	return {
		getAjaxDefaultSettings : getAjaxDefaultSettings,
		
		/**
		 * POST
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param data - data for server, pass null if you don't need it
		 * @param $form - optional, will cause this form the be submitted
		 */
		post : function(url, data, $form) {
			var settings = getAjaxDefaultSettings(url, data, {
				type: "POST",
			});
			return (($form === undefined) ?
					$.ajax(settings) : $form.ajaxSubmit(settings).data('jqxhr'))
					.fail(showException);
		},
		
		/**
		 * GET
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param data - data for server, pass null if you don't need it
		 * @param $form - optional, will cause this form the be submitted
		 */
		get : function (url, data, $form) {
			var settings = getAjaxDefaultSettings(url, data, {
				type: "GET",
			});
			return (($form === undefined) ?
					$.ajax(settings) : $form.ajaxSubmit(settings).data('jqxhr'))
					.fail(showException);
		},
		
		/**
		 * DELETE
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param data - data for server, pass null if you don't need it
		 * @param $form - optional, will cause this form the be submitted
		 */
		remove : remove,
		"delete" : remove,
		
		/**
		 * Upload File
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param file - Single file. See HTML 5 File API
		 */
		upload : function(url, file) {
			var formData = new FormData();
			formData.append('file', file);
			var settings = Ajax.getAjaxDefaultSettings(url, formData, {
		    	processData: false,
		    	contentType: false,
		    	type: "POST"
			});
			return $.ajax(settings).fail(showException);
		}
	};
})();