import $ from "../lib/jquery";

/**
 * This moddule contains code that is needed to modify a task during lifettime of a task,
 * while TaskEventBindings contains code that initializes a task only once.
 */

/**
 * 
 * @param {jQuery} $taskOperationsParent - any element containing the tasks's operations or the operations element itself.
 * @param {boolean} isPinned - true, false or undefined/null if both should be hidden.
 */
var switchPinOperation = function($taskOperationsParent, isPinned) {
    if (isPinned === undefined) {
        isPinned = null;
    }
    var $operations = $taskOperationsParent.findSelf(".task-operations");
    $operations.find(".task-operations-unpin").toggle(isPinned !== null && isPinned);
    $operations.find(".task-operations-pin").toggle(isPinned !== null && !isPinned);
};

export { switchPinOperation };