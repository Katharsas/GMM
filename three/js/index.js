var $canvasControls = $("#canvasControls");

PreviewRenderer($canvasControls);



$("#toggleWireframe").on("click", function(event) {
	var height = parseInt($canvasControls.css("height"));
	$canvasControls.css("height", height + "%");
});