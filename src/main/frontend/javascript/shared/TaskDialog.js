import $ from "../lib/jquery";
import { centerDialog } from "../shared/dialogs";
import HtmlPreProcessor from "./preprocessor";
import { switchPinOperation } from "../tasks/Task";
import DataChangeNotifier from "../shared/DataChangeNotifier";

let $taskDialogContainer;
let $taskDialogTemplate;

// const taskHeaderContainerSelector = ".taskDialog-task-header";
// const taskBodyContainerSelector = ".taskDialog-task-body";
const closeSelector = ".taskDialog-close";

$(document).ready(function() {
    $taskDialogContainer = $("#taskDialog-container");
    $taskDialogTemplate = $("#taskDialog-template");
    $taskDialogTemplate.css("z-index", "150");
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

const TaskDialogsInit = function(taskCache, taskBinders) {

    const openDialog = function(idLink, closeOthers) {
        if (idLink in idLinkToDialog) {
            idLinkToDialog[idLink].bringToForeground();
        } else {
            if (closeOthers) {
                for (const idLink in idLinkToDialog) {
                    idLinkToDialog[idLink].onRemoved();
                }
            }
            const dialog = TaskDialog(taskCache, taskBinders, idLink, function(idLink) {
                delete idLinkToDialog[idLink];
            });
            idLinkToDialog[idLink] = dialog;
        }
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
        let $task = null;
        $dialog.removeAttr("id");
        $dialog.resizable({ handles: 'e, w,', minWidth: 500 });

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
            .then(function(){
                $task = cache.getTaskHeader(id);
                const $body = cache.getTaskBody(id);

                binders.bindHeader($task);
                binders.bindBody(id, $body, () => $task.focus());
                HtmlPreProcessor.lazyload($body);

                $task.removeClass("list-element");
                $task.removeClass("collapsed");
                $task.findSelf(".task-header").removeClass("clickable");
                $task.addClass("expanded");
                $taskContainer.append($task);
                $task.append($body);
                $body.show();

                $task.on("keyup", function (event) {
                    if (event.key === "Escape") {
                        onRemoved();
                        event.stopPropagation();
                    }
                });
            });
        };

        const bindEvents = function() {
            $closeButton.click(function() {
                onRemoved();
            });
        };

        const bringToForeground = function() {
            $taskDialogContainer.find(".taskDialog").css('z-index', 150);
            $dialog.css('z-index', 151);
            $task.focus();
        }

        bindEvents();
        attachTask().then(function() {
            $taskDialogContainer.append($dialog);
            HtmlPreProcessor.apply($dialog);
            $dialog.show();
            $dialog.on("mousedown", function() {
                bringToForeground();
            });
            bringToForeground();
            const numberOfExisting = $taskDialogContainer.children().length;
            centerDialog($dialog, numberOfExisting, $dialog.outerWidth(), $dialog.innerHeight())
        });

        return {
            onRemoved : onRemoved,
            onEdited : onEdited,
            onPinned : onPinned,
            bringToForeground : bringToForeground,
        };
    };

    init();

    TaskDialogs.openDialog = openDialog;
    TaskDialogs.update = update;

    return TaskDialogs;
}

export { TaskDialogsInit };
export default TaskDialogs;