import $ from "../lib/jquery";
import Ajax from "./ajax";
import TaskDialogs from "./TaskDialog";
import { contextUrl, allVars } from "./default";

const template = `
<div class="notific"><span></span>
<div/>
`

const getTaskNotificText = function(taskIdLink, taskName, changeType, userName) {
    return `
    <span class="notific-task-user">${userName}</span> 
    <span class="notific-task-change ${changeType}">${changeType}</span>
    <span class="notific-task-name" data-id="${taskIdLink}"}>${taskName}</span> 
    `
}

/** @type {JQuery} */ let $toggle;
/** @type {JQuery} */ let $notifications;

const init = function() {

    if (!allVars.isUserLoggedIn) {return;}

    $toggle = $("#notifications-toggle");
    $notifications = $("#notifications");

    $notifications.resizable({ handles: 'e', minWidth: 100 });

    /** @type {JQuery} */ const $mark = $notifications.find("#notifications-markRead");
    /** @type {JQuery} */ const $clear = $notifications.find("#notifications-clear");

    var $new = $notifications.find("#notifications-new");
    var $old = $notifications.find("#notifications-old");

    const onClickTaskName = function() {
        const $taskName = $(this);
        const idLink = $taskName.data("id");
        console.log(this);
        TaskDialogs.openDialog(idLink);
    }

    $new.on("click", ".notific.notific-taskExists .notific-task-name", onClickTaskName);
    $old.on("click", ".notific.notific-taskExists .notific-task-name", onClickTaskName);

    const updateOldNotifics = function() {
        return updateNotifics(contextUrl + "/notifics/old", $old);
    }
    const updateNewNotifics = function() {
        return updateNotifics(contextUrl + "/notifics/new", $new)
        .then(function(isVisible) {
            $mark.toggle(isVisible);
        });
    };

    const bindTaskDialog = function($notific) {
        $notific.addClass("notific-taskExists");
        $notific.addClass("clickable");
    }
    const unbindTaskDialog = function($notific) {
        $notific.removeClass("notific-taskExists");
        $notific.removeClass("clickable");
    }

    const updateNotifics = function(url, $list) {
        return Ajax.post(url)
        .then(function(result) {
            const existsMap = result.idLinkToExists;
            $list.empty();
            for (const item of result.notifications) {
                 /** @type {JQuery} */ const $item = $(template);
                 const isTaskNotific = (item.taskName !== undefined)
                 const notificHtml = isTaskNotific ?
                        getTaskNotificText(item.taskIdLink, item.taskName, item.changeType, item.userName) : item.text;
                 $item.find("span").html(notificHtml);

                 // TODO Make sure deletions during runtime also deactivate task dialogs.
                 if (isTaskNotific) {
                    if (existsMap[item.taskIdLink] === true) {
                        bindTaskDialog($item);
                    }
                 }
                 $list.prepend($item);
            }
            const shouldBeVisible = $list.children().length > 0;
            $list.toggle(shouldBeVisible);
            return shouldBeVisible;
        });
    }

    var clearNotifications = function() {
        return Ajax.post(contextUrl + "/notifics/clearRead")
        .then(function() {
            $toggle.click();
        });
    }

    $toggle.on("click", function() {
        $toggle.toggleClass("activeTab");
        var active = $toggle.hasClass("activeTab");
        $notifications.toggle(active);
        if (active) {
            updateOldNotifics();
            updateNewNotifics();
        } else {
            $new.empty();
            $old.empty();
        }
    });
    $clear.on("click", function() {
        clearNotifications();
    })
    $mark.on("click", function() {
        // TODO Server needs to reorder old notifics after adding new ones to them (by creation date)

        Ajax.post(contextUrl + "/notifics/markRead")
        .then(function() {
            updateOldNotifics();
            updateNewNotifics();
        });
    })
};

var Notifications = { init };

export default Notifications;