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
const Ajax = (function() {

	let requestsInFlight = 0;
	const onSendCallbacks = [];
	const onReceiveCallbacks = [];

	//CSRF tokens must be included into POST/PUT/DELETE to GMM
	//Just throwing it into every request that goes to GMM.
	const token = $("meta[name='_csrf']").attr("content");
	const header = $("meta[name='_csrf_header']").attr("content");
	
	const getAjaxDefaultSettings = function(url, data, settings) {
		if (url === undefined || url === null) return undefined;
		const result = {
			url: url,
			headers: {accept:"application/json;q=1.0,*/*;q=0.8"},
		};
		if (url.startsWith(contextUrl)) {
			result.headers[header] = token;
		}
		if (data !== undefined && data !== null) {
			result.data = data;
		}
		if (settings !== undefined && settings !== null) {
			for(const key in settings) {
				result[key] = settings[key];
			}
		}
		return result;
	};
	
	const failHandler = function(responseData) {
		const httpStatus = responseData.status;
		if(httpStatus === 403) {
			Dialogs.alert(function(){
				location.reload();
			}, "Server answer: 403 Forbidden or Timeout.<br> Confirm to reload page.");
		} else if (httpStatus === 404) {
			Dialogs.alert(function(){
				location.reload();
			}, "Server answer: 404 Not Found.<br> Confirm to reload page.");
		} else {
			Dialogs.showException(responseData);
		}
	};

	const onSend = function() {
		for (const onSendCb of onSendCallbacks) {
			onSendCb(requestsInFlight);
		}
		requestsInFlight++;
	}

	const onReceive = function(responseData) {
		requestsInFlight--;
		for (const onReceiveCb of onReceiveCallbacks) {
			onReceiveCb(requestsInFlight);
		}
		return responseData;
	}

	return {
		
		/**
		 * POST
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param data - data for server, pass null if you don't need it
		 * @param $form - optional, will cause this form the be submitted
		 */
		post : function(url, data, $form) {
			onSend();
			const settings = getAjaxDefaultSettings(url, data, {
				type: "POST",
			});
			const jqPromise = (($form === undefined) ?
					$.ajax(settings) : $form.ajaxSubmit(settings).data('jqxhr'))
					.fail(failHandler)
					.always(onReceive);
			return Promise.resolve(jqPromise);
		},
		
		/**
		 * GET
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param data - data for server, pass null if you don't need it
		 * @param $form - optional, will cause this form the be submitted
		 */
		get : function (url, data, $form) {
			onSend();
			const settings = getAjaxDefaultSettings(url, data, {
				type: "GET",
			});
			const jqPromise = (($form === undefined) ?
					$.ajax(settings) : $form.ajaxSubmit(settings).data('jqxhr'))
					.fail(failHandler)
					.always(onReceive);
			return Promise.resolve(jqPromise);
		},
		
		/**
		 * Upload File
		 * --------------------------------------------------------------------
		 * @param url - server url
		 * @param file - Single file. See HTML 5 File API
		 */
		upload : function(url, file) {
			onSend();
			const formData = new FormData();
			formData.append('file', file);
			const settings = getAjaxDefaultSettings(url, formData, {
		    	processData: false,
		    	contentType: false,
		    	type: "POST"
			});
			const jqPromise = $.ajax(settings)
					.fail(failHandler)
					.always(onReceive);
			return Promise.resolve(jqPromise);
		},

		registerOnSend : function(onSend) {
			onSendCallbacks.push(onSend);
		},
		registerOnReceive : function(onReceive) {
			onReceiveCallbacks.push(onReceive);
		}
	};
})();
export default Ajax;
global.Ajax = Ajax;