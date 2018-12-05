import { contextUrl, allVars } from "./default";

/**
 * Possible remote events:
 * - TaskDataChangeEvent
 * - NotificationChangeEvent
 * 
 * Messages that contain an "@" like "TaskDataChangeEvent@user15" are supposed
 * to be only interesting for the specified user.
 */
const EventListener = function() {
	
	const events = {
		TaskDataChangeEvent : "TaskDataChangeEvent",
		NotificationChangeEvent : "NotificationChangeEvent",
		WorkbenchChangeEvent : "WorkbenchChangeEvent",
		PinnedListChangeEvent : "PinnedListChangeEvent",
		AssetFileOperationsChangeEvent : "AssetFileOperationsChangeEvent"
	}

	const callbacks = {};

	let currentPromise = Promise.resolve();

	for (const [_, eventName] of Object.entries(events)) {
		callbacks[eventName] = [];
	}

	const urlBase = location.hostname + (location.port ? ":" + location.port : "")
	const protocol = location.protocol === "https:" ? "wss://" : "ws://";
	const webSocket = new WebSocket(protocol + urlBase + contextUrl + "/websocket/notifier");

	webSocket.onerror = function(event) {
		console.error("Websocket connection error!");
		console.error(event);
	}

	const onMessageReceived = function(message) {
		var messageParts = message.split("@");
		var eventName = messageParts[0];
		var isTargeted = messageParts.length > 1;
		if (isTargeted) {
			var userId = messageParts[1];
			if (userId !== allVars.currentUser.idLink) {
				return;
			}
		}
		console.info("EventListener: Received event '" + message + "'");
		
		for (const callback of callbacks[eventName]) {
			currentPromise = currentPromise.then(callback);
		}
	}

	webSocket.onmessage = function(data) {
		onMessageReceived(data.data);
	}

	return {
		/**
		 * Callbacks are executed in order of subscription.
		 * @param {string} eventName
		 * @param {Function} callback - No argument, may return a promise if it
		 * 		wants to block the execution of following handlers.
		 */
		subscribe : function(eventName, callback) {
			callbacks[eventName].push(callback);
		},
		events : events,
		/**
		 * @param {string} eventName
		 */
		trigger : function(eventName) {
			//console.log("Event triggered by client: " + eventName);
			onMessageReceived(eventName);
		}
	};
}();

export default EventListener;