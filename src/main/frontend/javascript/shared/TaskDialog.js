import $ from "../lib/jquery";
import Ajax from "../shared/ajax";
import Dialogs, { centerDialog } from "../shared/dialogs";
import HtmlPreProcessor from "./preprocessor";
import { switchPinOperation } from "../tasks/Task";
import DataChangeNotifier from "../shared/DataChangeNotifier";
import lozad from 'lozad';
import { contextUrl, runSerial } from "../shared/default";

let $taskDialogContainer;
let $taskDialogTemplate;

// const taskHeaderContainerSelector = ".taskDialog-task-header";
// const taskBodyContainerSelector = ".taskDialog-task-body";
const closeSelector = ".taskDialog-close";

$(document).ready(function() {
    $taskDialogContainer = $("#taskDialog-container");
    $taskDialogTemplate = $("#taskDialog-template");
});

const idLinkToDialog = {};

const onTaskDataChange = function(event) {

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
        if (idLink in idLinkToDialog) {
            // TODO bring existing to focus
            return;
        }
        const dialog = TaskDialog(taskCache, taskBinders, idLink, function(idLink) {
            delete idLinkToDialog[idLink];
        });
        idLinkToDialog[idLink] = dialog;
    };

    const update = function() {
        
    };

    const init = function() {
        DataChangeNotifier.registerSubscriber("TaskDialogs", function(events) {
            for (const event of events) {
                onTaskDataChange(event);
            }
        });
        taskCache.subscribePinnedEvent(function(idLink, isPinned) {
            if (idLink in idLinkToDialog) {
                idLinkToDialog[idLink].onPinned(isPinned);
            }
        });
    };

    const TaskDialog = function(cache, binders, id, closeCallback) {
        
        const $dialog = $taskDialogTemplate.clone();
        $dialog.removeAttr("id");
        $dialog.resizable({ handles: 'e, s, w, ne, se, sw, nw', minWidth: 400 });

        const $closeButton = $dialog.find(closeSelector);

        const $taskContainer = $dialog.find(".taskDialog-task");

        const onRemoved = function() {
            $dialog.remove();
            closeCallback(id);
        };

        const onEdited = function() {
            $taskContainer.empty();
            attachTask();
        };
        
        const onPinned = function(isPinned) {
            switchPinOperation($taskContainer, isPinned);
        }

        const attachTask = function() {
            return cache.makeAvailable([id])
            .then(function(idLinksMissing){
                const $header = cache.getTaskHeader(id);
                const $body = cache.getTaskBody(id);

                binders.bindHeader($header);
                binders.bindBody(id, $body);

                $header.removeClass("list-element");
                $header.removeClass("collapsed");
                $header.findSelf(".task-header").removeClass("clickable");
                $taskContainer.append($header);
                $header.append($body)
                $body.show();
            });
        };

        const bindEvents = function() {
            $closeButton.click(function() {
                onRemoved();
            });
        };

        bindEvents();
        attachTask().then(function() {
            $taskDialogContainer.append($dialog);
            HtmlPreProcessor.apply($dialog);
            $dialog.show();
            const numberOfExisting = $taskDialogContainer.children().length;
            centerDialog($dialog, numberOfExisting, $dialog.outerWidth(), $dialog.innerHeight())
        });

        return {
            onRemoved : onRemoved,
            onEdited : onEdited,
            onPinned : onPinned
        };
    };

    init();

    TaskDialogs.openDialog = openDialog;
    TaskDialogs.update = update;

    return TaskDialogs;
}

export { TaskDialogsInit };
export default TaskDialogs;