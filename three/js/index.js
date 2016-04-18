
var $canvasControls = $("<div></div>").attr("id", "canvasControls");

var $preview1 = $("<div/>")
	.addClass("task-preview-visual")
	.attr("data-url", "./models/original.json")
	.append($("<canvas></canvas>"));

var $preview2 = $("<div/>")
	.addClass("task-preview-visual")
	.attr("data-url", "./models/new.json")
	.append($("<canvas></canvas>"));

$canvasControls.append($preview1, $preview2);



var renderer = PreviewRenderer($canvasControls);

$("body").prepend($canvasControls);

renderer.update();
