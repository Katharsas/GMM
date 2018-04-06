import $ from "../lib/jquery";
import Ajax from "../shared/ajax";
import Dialogs from "../shared/dialogs";
import lozad from 'lozad';
import { contextUrl, runSerial } from "../shared/default";

let $taskDialogContainer;
let $taskDialogTemplate;

const taskHeaderContainerSelector = ".taskDialog-task-header";
const taskBodyContainerSelector = ".taskDialog-task-body";
const closeSelector = ".taskDialog-close";

$(document).ready(function() {
    $taskDialogContainer = $("#taskDialog-container");
    $taskDialogTemplate = $("#taskDialog-template");
});

const idLinkToDialog = {};

const onCacheChange = function(event) {

    if (event.eventType === "edited") {
        cache.updateCache();
    }
    // TODO then
    for (const [taskId, dialog] of Object.entries(idLinkToDialog)) {
        if (event.changedIds.includes(taskId)) {
            switch (event.eventType) {
                case "EDITED" :
                    dialog.onEdited();
                break;
                case "REMOVED" :
                    dialog.onRemoved();
                    delete idLinkToDialog[taskId];
                break;
                default :
                    // return new Promise.resolve();
                break;
            }
        }
    }
}

let TaskDialogs = {};

const TaskDialogsInit = function(taskCache, taskBinders, eventUrl) {

    const openDialog = function(idLink) {
        dialog = TaskDialog(taskCache, taskBinders, idLink, function(idLink) {
            delete idLinkToDialog[idLink];
        });
        idLinkToDialog[idLink] = dialog;
    }

    const update = function() {
        
    }

    const init = function() {

    }

    const TaskDialog = function(cache, binders, id, closeCallback) {
        
        const $dialog = $taskDialogTemplate.clone();
        const $closeButton =$dialog.find(closeSelector);

        const $headerContainer = $dialog.find(taskHeaderContainerSelector);
        const $bodyContainer = $dialog.find(taskBodyContainerSelector);

        const onRemoved = function() {
            $dialog.remove();
            closeCallback(id);
        };

        const onEdited = function() {
            $headerContainer.empty();
            $bodyContainer.empty();
            attachTask();
        };

        const attachTask = function() {
            const $header = cache.getTaskHeader(id);
            const $body = cache.getTaskBody(id);

            binders.bindHeader($task);
            binders.bindBody(id, $task, $body);

            $headerContainer.attach($header);
            $bodyContainer.attach($body)
        };

        const bindEvents = function() {
            $closeButton.click(function() {
                onRemoved();
            });
        };

        bindEvents();
        attachTask();
        $taskDialogContainer.attach($dialog);

        return {
            onRemoved : onRemoved,
            onEdited : onEdited,
        };
    }

    TaskDialogs = {
        openDialog : openDialog,
        update : update,
    }

    return TaskDialogs;
}

export { TaskDialogsInit };
export default TaskDialogs;