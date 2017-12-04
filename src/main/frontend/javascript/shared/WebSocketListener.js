import { contextUrl } from "./default";

var WebSocketListener = function() {
	
	var callbacks = {};

	var urlBase = location.hostname + (location.port ? ":" + location.port : "")
	var webSocket = new WebSocket("ws://" + urlBase + contextUrl + "/notifier");

	webSocket.onmessage = function(data) {
		console.log(data);
		var eventName = data.data;
		callbacks[eventName]();
	}

	return {
		subscribe : function(eventName, callback) {
			callbacks[eventName] = callback;
		}
	};
};

export default WebSocketListener;