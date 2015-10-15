/* jshint esnext:true */
import $ from "../lib/jquery";
import Dialogs from "./dialogs";
import { contextUrl } from "./default";

/**
 * Ajax
 * --------------------------------------------------------------------
 * Ajax convenience functions, expecting JSON response from server (if any) 
 * and return jqXHR when possible. Includes form submitting.
 * 
 * @author Jan Mothes
 */
var Ajax = (function() {
	//CSRF tokens must be included into POST/PUT/DELETE to GMM
	//Just throwing it into every request that goes to GMM.
	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");
	
	var getAjaxDefaultSettings = function(url, data, settings) {
		if (url === undefined || url === null) return undefined;
		var result = {
			url: url,
			headers: {accept:"application/json,*/*;q=0.8"},
		};
		if (url.startsWith(contextUrl)) {
			result.headers[header] = token;
		}
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

	return {
		
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
					.fail(Dialogs.showException);
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
					.fail(Dialogs.showException);
		},
		
		/**
		 * Upload File
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param file - Single file. See HTML 5 File API
		 */
		upload : function(url, file) {
			var formData = new FormData();
			formData.append('file', file);
			var settings = getAjaxDefaultSettings(url, formData, {
		    	processData: false,
		    	contentType: false,
		    	type: "POST"
			});
			return $.ajax(settings).fail(Dialogs.showException);
		}
	};
})();
export default Ajax;
global.Ajax = Ajax;