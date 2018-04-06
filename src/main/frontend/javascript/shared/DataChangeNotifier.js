import Ajax from "../shared/ajax";
import EventListener from "../shared/EventListener";
import { contextUrl, runSerial } from "../shared/default";

let thisEventUrl;

let currentPromise = Promise.resolve();
let updatesPending = 0;

/**
 * @param {string} eventUrl - used to get events about task data changes.
 */
const DataChangeNotifierInit = function(eventUrl) {
    thisEventUrl = eventUrl;
}

const subscriberToEventHandler = {};

/**
 * @param {function} eventsHandler 
 *      Parameter: ClientDataChangeEvent[]
 *      Returns: Promise of the finished handler action
 */
const registerSubscriber = function(id, eventsHandler) {
    subscriberToEventHandler[id] = eventsHandler;
}

const unregisterSubscriber = function(id) {
    delete subscriberToEventHandler[id];
}

/** 
 * Will trigger a new update. There can only be at most one running and
 * one planned update (to run after the currently running one) at a time.
 * 
 * Returns the promise of finishing the triggered update or the planned update
 * (even if this call itself did not cause a new update).
 */
const triggerUpdate = function() {
    if (updatesPending > 1) {
        return currentPromise;
    }
    updatesPending++;
    currentPromise = currentPromise.then(function() {
        return Ajax.get(contextUrl + thisEventUrl);
    })
    .then(function(events) {
        if (events.length <= 0) {
            return Promise.resolve();
        }
        console.log("Received data change events from server:");
        console.log(events);
        const asyncTasks = [];
        for (const [_, eventsHandler] of Object.entries(subscriberToEventHandler)) {
            asyncTasks.push(eventsHandler(events));
        }
        return Promise.all(asyncTasks);
    })
    .then(function() {
        updatesPending--;
    });
    return currentPromise;
}

EventListener.subscribe(EventListener.events.TaskDataChangeEvent, triggerUpdate);

const DataChangeNotifier = {
    registerSubscriber : registerSubscriber,
    unregisterSubscriber : unregisterSubscriber,
}

export { DataChangeNotifierInit };
export default DataChangeNotifier;