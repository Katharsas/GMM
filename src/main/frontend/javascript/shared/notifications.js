import $ from "../lib/jquery";
import Ajax from "./ajax";
import { contextUrl, allVars } from "./default";

var template = `
<div class="notific"><span></span>
<div/>
`

var init = function() {

    /** @type {JQuery} */ var $toggle = $("#notifications-toggle");

    if (!allVars.isUserLoggedIn) {return;}

    /** @type {JQuery} */ var $notifications = $("#notifications");
    /** @type {JQuery} */ var $clear = $notifications.find("#notifications-clear");

    var $new = $notifications.find("#notifications-new");
    var $old = $notifications.find("#notifications-old");
    
    var updateOldNotifics = function() {
        return Ajax.get(contextUrl + "/notifics/old")
        .then(function(items) {
            for (let item of items) {
                /** @type {JQuery} */ let $item = $(template);
                $item.find("span").text(item.text);
                $old.prepend($item);
            }
        });
    }
    var updateNewNotifics = function() {
        return Ajax.post(contextUrl + "/notifics/new")
        .then(function(items) {
            for (let item of items) {
                /** @type {JQuery} */ let $item = $(template);
                $item.find("span").text(item.text);
                $new.prepend($item);
            }
        });
    };
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