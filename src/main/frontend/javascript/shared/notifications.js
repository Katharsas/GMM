import $ from "../lib/jquery";
import Ajax from "./ajax";
import TaskDialogs from "./TaskDialog";
// import DataChangeNotifier from "./DataChangeNotifier";
import EventListener from "./EventListener";
import { contextUrl, allVars } from "./default";

const template = `
<div class="notific"><span></span>
<div/>
`

const getTaskNotificText = function(taskIdLink, taskName, changeType, userName) {
    return `
    <span class="notific-task-user">${userName}</span> 
    <span class="notific-task-change ${changeType}">${changeType}</span>
    <span class="notific-task-name"}>${taskName}</span> 
    `
}

let active = false;

/** @type {JQuery} */ let $toggle;
/** @type {JQuery} */ let $notifications;

const init = function() {
    if (!allVars.isUserLoggedIn) {return;}

     // Initialize menu bar button for notifications toggle

    $toggle = $("#notifications-toggle");
    const $count = $toggle.find("#notifications-count");
    const alarmClass = "notifications-alarm";
    
    const initToggle = function() {

        const updateToggleAlarm = function() {
            Ajax.get(contextUrl + "/notifics/has")
            .then(function(numberOfNewNotifics){
                if (numberOfNewNotifics > 0) {
                    $toggle.addClass(alarmClass);
                    $count.text(numberOfNewNotifics);
                }
            });
        }

        $toggle.addClass("enabled");
        updateToggleAlarm();

        EventListener.subscribe(EventListener.events.NotificationChangeEvent, function() {
            if (active) {
                $toggle.click();
                $toggle.click();
            } else {
                updateToggleAlarm();
            }
        });
    }

    // Initialize the notification list and notifications themselves

    $notifications = $("#notifications");
    const $new = $notifications.find("#notifications-new");
    const $old = $notifications.find("#notifications-old");

    const initNotifications = function() {

        const updateOldNotifics = function() {
            return updateNotifics(contextUrl + "/notifics/old", $old);
        }
        const updateNewNotifics = function() {
            return updateNotifics(contextUrl + "/notifics/new", $new)
            .then(function(newNotificCount) {
                const hasNewNotifics = newNotificCount > 0;
                $mark.toggle(hasNewNotifics);
                $toggle.toggleClass(alarmClass, hasNewNotifics);
                $count.text(newNotificCount);
                return newNotificCount;
            });
        };

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
    
                     if (isTaskNotific) {
                        if (existsMap[item.taskIdLink] === true) {
                            bindTaskDialog($item, item.taskIdLink);
                        }
                     }
                     $list.prepend($item);
                }
                const notificCount = $list.children().length;
                $list.toggle(notificCount > 0);
                return notificCount;
            });
        }

        const bindTaskDialog = function($notific, idLink) {
            $notific.addClass("notific-taskExists");
            $notific.addClass("clickable");
            $notific.attr("data-id", idLink);
        }

        const onClickTaskName = function() {
            const $taskName = $(this);
            const idLink = $taskName.data("id");
            TaskDialogs.openDialog(idLink);
        }

        $notifications.resizable({ handles: 'e', minWidth: 100 });
        $new.on("click", ".notific.notific-taskExists", onClickTaskName);
        $old.on("click", ".notific.notific-taskExists", onClickTaskName);

        const $mark = $notifications.find("#notifications-markRead");
        $mark.on("click", function() {
            Ajax.post(contextUrl + "/notifics/markRead")
            .then(function() {
                updateNewNotifics();
                updateOldNotifics();
            });
        });

        const $clear = $notifications.find("#notifications-clearRead");
        $clear.on("click", function() {
            Ajax.post(contextUrl + "/notifics/clearRead")
            .then(updateNewNotifics)
            .then(function(newNotificCount) {
                if (newNotificCount <= 0) {
                    $toggle.click();
                }
            });
        });

        $toggle.on("click", function() {
            $toggle.toggleClass("activeTab");
            active = !active;
            $notifications.toggle(active);
            if (active) {
                updateNewNotifics();
                updateOldNotifics();
            } else {
                $new.empty();
                $old.empty();
            }
        });
    }
    
    initToggle();
    initNotifications();
};

const Notifications = { init };

export default Notifications;