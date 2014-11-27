/**
 * -------------------- Queue ---------------------------------------------------------------
 * @param maxSize - Maximum number of elements in the queue. If undefined, queue size is unlimited.
 * Modifiable propety. If the size is reduced, surplus elements will not be removed.
 * @param comparator - Function which takes two elements as arguments and returns true if they are
 * considered to be the same element. Used by remove & contains function.
 */
function Queue(maxSize, comparator) {
	var queue = [];
	this.maxSize = maxSize;
	
	/**
	 * Adds an element to the queue. If the queue is full, the oldest element will be removed.
	 * @returns the removed element or undefined, if no element was removed.
	 */
	this.add = function(task) {
		var result = undefined;
		if(this.maxSize !== undefined && queue.length >= this.maxSize) {
			result = queue.pop();
		}
		queue.unshift(task);
		return result;
	};
	
	var containsRemove = function(task, remove) {
		for (var i = 0; i < queue.length; i++) {
			if (comparator(queue[i], task)) {
				if(remove) queue.splice(i, 1);
				return true;
			}
		}
		return false;
	};
	
	/**
	 * Compares the given element with the queued elements by using it as second parameter with the
	 * given taskComparator.
	 * @returns true, if the element was found
	 */
	this.contains = function(task) {
		return containsRemove(task, false);
	};
	
	/**
	 * Compares the given element with the queued elements by using it as second parameter with the
	 * given taskComparator and removes it if found.
	 * @returns true, if the element was found and removed
	 */
	this.remove = function(task) {
		return containsRemove(task, true);
	};
	
	/**
	 * @return array with all elements ordered by insertion order (latest first, oldest last).
	 */
	this.get = function() {
		return queue.slice();
	};
}