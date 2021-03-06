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

const subscriberToEventHandler = new Map();

/**
 * @param {function} eventsHandler 
 *      Parameter: ClientDataChangeEvent[]
 *      Returns: Promise of the finished handler action
 */
const registerSubscriber = function(id, eventsHandler) {
    console.debug("DataChangeNotifier: Registered handler with id '" + id + "'.");
    subscriberToEventHandler.set(id, eventsHandler);
}

const unregisterSubscriber = function(id) {
    console.debug("DataChangeNotifier: Unregistered handler with id '" + id + "'.");
    subscriberToEventHandler.delete(id);
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
        const asyncTasks = [];
        for (const [handlerId, eventsHandler] of subscriberToEventHandler) {
            console.debug("DataChangeNotifier: Calling handler with id '" + handlerId + "'.");
            asyncTasks.push(function() {
                return eventsHandler(events);
            });
        }
        return runSerial(asyncTasks);
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