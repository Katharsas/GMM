import Ajax from "../shared/ajax";
import EventListener from "../shared/EventListener";
import Dialogs from "../shared/dialogs";
import { contextUrl } from "../shared/default";

let thisStatustUrl;

let isNewAssetFileOperationsEnabled;

const subscriberToChangeHandler = new Map();

/**
 * @param {string} statusUrl - used to get events about task data changes.
 */
const AssetFileOperationsNotifierInit = function(statusUrl) {
    thisStatustUrl = statusUrl;
    onEvent();
}

/**
 * @param {function(boolean):void} changeHandler 
 *      Parameter: {boolean} isNewAssetFileOperationsEnabled
 */
const registerSubscriber = function(changeHandler, id) {
    console.debug("AssetFileOperationsNotifier: Registered handler with id '" + id + "'.");
    subscriberToChangeHandler.set(id, changeHandler);
}

/**
 * 
 * @param {JQuery} $button 
 * @param {string} inputEventType 
 * @param {function():void} eventHandler
 */
const registerNewAssetOperation = function($button, inputEventType, eventHandler) {
    $button.on(inputEventType, function() {
        if (isNewAssetFileOperationsEnabled) {
            eventHandler();
        } else {
            Dialogs.alert(null, "This action is not available while asset operations are running on server.")
        }
    });
}

const onEvent = function() {
    Ajax.get(contextUrl + thisStatustUrl)
    .then(function(isFileOperationsEnabled){
        if (isNewAssetFileOperationsEnabled !== isFileOperationsEnabled) {
            isNewAssetFileOperationsEnabled = isFileOperationsEnabled;
            for (const [_, changeHandler] of subscriberToChangeHandler) {
                changeHandler(isFileOperationsEnabled);
            }
        }
    });
}

EventListener.subscribe(EventListener.events.AssetFileOperationsChangeEvent, onEvent);


const AssetFileOperationsNotifier = {
    registerSubscriber : registerSubscriber,
    registerNewAssetOperation : registerNewAssetOperation,
}

export { AssetFileOperationsNotifierInit };
export default AssetFileOperationsNotifier;