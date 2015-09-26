/* jshint node: true */
'use strict';

/**
 * Commented out watch code, could prove useful at some point.
 */

var notify = require("gulp-notify");

var gulp = require('gulp');
var sass = require("gulp-sass");
var sourcemaps = require("gulp-sourcemaps");

var browserify = require('browserify');
var babelify = require('babelify');
var source = require('vinyl-source-stream');

//var path = require('path');

/**
 * ########################
 * Paths & Files
 * ########################
 */

//JavaScript

var jsDir = "javascript/example/";
var jsDest = "../webapp/resources/javascript/compiled/";
//var jsFilter = "**/*.js";

//SASS

var sassDir = "sass/";
var sassDest = "../webapp/resources/css/compiled/";
var sassFilter = "**/*.scss";

/**
 * ########################
 * Gulp Build
 * ########################
 */

function handleErrors() {
	var args = Array.prototype.slice.call(arguments);
	notify.onError({
		title: "Compile Error",
		message: "<%= error.message %>"
	}).apply(this, args);
	this.emit("end"); // Keep gulp from hanging on this task
}

function buildScript(file) {
	var props = {entries: [jsDir + file], debug: true};
	var bundler = browserify(props);
	bundler.transform(babelify);
	var stream = bundler.bundle();
	return stream.on("error", handleErrors)
		.pipe(source("bundle_" + file))
		.pipe(gulp.dest(jsDest));
}

gulp.task("build-js", function () {
	//multiple files: http://fettblog.eu/gulp-browserify-multiple-bundles/
	return buildScript("consumer.js");
});

gulp.task('build-sass', function () {
	return gulp
		.src(sassDir + sassFilter)
		.pipe(sourcemaps.init())
		.pipe(sass().on('error', sass.logError))
		.pipe(sourcemaps.write())
		.pipe(gulp.dest(sassDest));
});

gulp.task('build', ['build-js', 'build-sass']);