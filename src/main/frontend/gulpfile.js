/* jshint node:true */
"use strict";

/**
 * Commented out watch code, could prove useful at some point.
 */

var notify = require("gulp-notify");

var gulp = require("gulp");
var gulpif = require('gulp-if');
var sass = require("gulp-sass");
var sourcemaps = require("gulp-sourcemaps");

var uglify = require('gulp-uglify');
var browserify = require("browserify");
var babelify = require("babelify");
var source = require("vinyl-source-stream");
var buffer = require('vinyl-buffer');
var rename = require("gulp-rename");
var eventStream = require("event-stream");

//var path = require("path");

/**
 * ########################
 * Paths & Files
 * ########################
 */

//JavaScript

var jsDir = "javascript/";
var jsDest = "../webapp/resources/javascript/compiled/";
//var jsFilter = "**/*.js";

var jsSourceMaps = false;
var jsMinify = true;

//SASS

var sassDir = "sass/";
var sassDest = "../webapp/resources/css/compiled/";
var sassFilter = "**/*.scss";

var cssSourceMaps = false;

/**
 * ########################
 * Gulp Build
 * ########################
 */

function handleErrors() {
	var args = Array.prototype.slice.call(arguments);
	notify.onError({
		title: "Compile Error in line <%= error.lineNumber %>",
		message: "<%= error.message %>"
	}).apply(this, args);
	this.emit("end"); // Keep gulp from hanging on this task
}

function buildScript(files) {
	var tasks = files.map(function(file) {
		var configBrowserify = {
				entries: [jsDir + file],
				debug: jsSourceMaps
			};
		var configBabelify = {
				plugins: ["transform-es2015-modules-commonjs",
				          "transform-es2015-parameters",
				          "transform-es2015-spread",
				          "transform-es2015-for-of"],
			};
		return browserify(configBrowserify)
			.transform(babelify, configBabelify)
			.bundle()
			.on("error", handleErrors)
			.pipe(source(file))
			.pipe(gulpif(jsMinify, buffer()))
			.pipe(gulpif(jsMinify, uglify()))
			.on("error", handleErrors)
			.pipe(rename({
				extname: ".bundle.js"
			}))
			.pipe(gulp.dest(jsDest));
	});
	 return eventStream.merge.apply(null, tasks);
}

gulp.task("build-js", function () {
	//multiple files: http://fettblog.eu/gulp-browserify-multiple-bundles/
	return buildScript(
		[
		 "tasks.js",
		 "links.js",
		 "admin.js",
		 "profile.js",
		 "login.js"]
	);
});

gulp.task("build-sass", function () {
	return gulp
		.src(sassDir + sassFilter)
		.pipe(gulpif(cssSourceMaps, sourcemaps.init()))
		.pipe(sass().on("error", sass.logError))
		.pipe(gulpif(cssSourceMaps, sourcemaps.write()))
		.pipe(gulp.dest(sassDest));
});

gulp.task("build", ["build-js", "build-sass"]);
