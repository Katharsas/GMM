/* jshint node:true */
"use strict";

var fs = require('fs');

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

var rollup = require('rollup');
var uglifyjs = require("uglify-js");

var threeConfig = require("./three-rollup.config");

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
var jsMinify = false;

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
	console.log(args.toString());

	// notify.onError({
	// 	title: "Compile Error in line <%= error.lineNumber %>",
	// 	message: "<%= error.message %>",
	// 	onLast: true,
	// }).apply(this, args);
	this.emit("end"); // Keep gulp from hanging on this task
}

function buildScript(files) {
	var tasks = files.map(function(file) {
		var configBrowserify = {
				entries: [jsDir + file],
				debug: jsSourceMaps
			};
		var configBabelify = {
				plugins: ["transform-es2015-modules-commonjs"],
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

gulp.task("three", function () {
	var minify = true;
	var outputFile = jsDest + "three.bundle.js";

	return rollup.rollup(threeConfig.input("javascript/lib/threeSmall.js"))
		.then(function(bundle) {
			return bundle.generate(threeConfig.output());
		}).then(function(rollupResult) {
			var result = uglifyjs.minify(rollupResult.code, {
				compress : minify,
				mangle: minify,
				output: {
					beautify: !minify,
					preamble: "// threejs.org/license"
				}
			});
			fs.writeFileSync(outputFile, result.code);
		});
});

gulp.task("build_js", function () {
	//multiple files: http://fettblog.eu/gulp-browserify-multiple-bundles/
	return buildScript(
		["tasks.js",
		 "links.js",
		 "admin.js",
		 "profile.js",
		 "login.js"]
	);
});

gulp.task("build_sass", function () {
	return gulp
		.src(sassDir + sassFilter)
		.pipe(gulpif(cssSourceMaps, sourcemaps.init()))
		.pipe(sass().on("error", sass.logError))
		.pipe(gulpif(cssSourceMaps, sourcemaps.write()))
		.pipe(gulp.dest(sassDest));
});

gulp.task("build", ["build_js", "build_sass"]);
