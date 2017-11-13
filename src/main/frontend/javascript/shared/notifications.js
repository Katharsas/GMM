import $ from "../lib/jquery";
import Ajax from "./ajax";
import { contextUrl, allVars } from "./default";

var template = `
<div class="notific"><span></span>
<div/>
`

var getTaskNotificText = function(taskIdLink, taskName, changeType, userName) {
    return `
    Task <span class="notific-task-name" ${taskIdLink === null ? "" : `data-id="${taskIdLink}"`}>${taskName}</span> 
    was <span class="notific-task-change">${changeType}</span> 
    by <span class="notific-task-user">${userName}</span>.`
}

var init = function() {

    /** @type {JQuery} */ var $toggle = $("#notifications-toggle");

    if (!allVars.isUserLoggedIn) {return;}

    /** @type {JQuery} */ var $notifications = $("#notifications");
    /** @type {JQuery} */ var $clear = $notifications.find("#notifications-clear");

    var $new = $notifications.find("#notifications-new");
    var $old = $notifications.find("#notifications-old");

    var updateOldNotifics = function() {
        return updateNotifics(contextUrl + "/notifics/old")
        .then(function(items) {
            for (let item of items) {
                /** @type {JQuery} */ let $item = $(template);
                let notificHtml = (item.taskName === undefined) ?
                        item.text : getTaskNotificText(item.taskIdLink, item.taskName, item.changeType, item.userName);
                $item.find("span").html(notificHtml);
                $old.prepend($item);
            }
        });
    }
    var updateNewNotifics = function() {
        return updateNotifics(contextUrl + "/notifics/new", $new);
    };

    var updateNotifics = function(url, $list) {
        return Ajax.post(url)
        .then(function(items) {
            for (let item of items) {
                 /** @type {JQuery} */ let $item = $(template);
                 let notificHtml = (item.taskName === undefined) ? item.text :
                        getTaskNotificText(item.taskIdLink, item.taskName, item.changeType, item.userName);
                 $item.find("span").html(notificHtml);
                 $list.prepend($item);
            }
        });
    }

    var clearNotifications = function() {
        return Ajax.post(contextUrl + "/notifics/clear")
        .then(function() {
            $toggle.click();
        });
    }

    $toggle.on("click", function() {
        $toggle.toggleClass("activeTab");
        var active = $toggle.hasClass("activeTab");
        $notifications.toggle(active);
        if (active) {
            Promise.resolve()
            .then(updateOldNotifics)
            .then(updateNewNotifics);
        } else {
            $new.empty();
            $old.empty();
        }
    });
    $clear.on("click", function() {
        clearNotifications();
    })
};

var Notifications = { init };

export default Notifications;