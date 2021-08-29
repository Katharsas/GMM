/* jshint node:true */
"use strict";

var fs = require('fs');

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

var rollup = require('rollup-stream');
//var uglifyjs = require("uglify-js");

var closureCompiler = require('google-closure-compiler').gulp();

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
	this.emit("end"); // Keep gulp from hanging on this task
}

function buildScript(files) {
	var tasks = files.map(function(file) {
		var configBrowserify = {
				entries: [jsDir + file],
				debug: jsSourceMaps
			};
		var presets = [];
		if (jsMinify) {
			presets.push("@babel/preset-env")
		}
		var configBabelify = {
				plugins: ["@babel/plugin-transform-modules-commonjs"],
				presets: presets
			};
		return browserify(configBrowserify)
			.transform(babelify, configBabelify)
			.bundle()
			.on("error", handleErrors)
			.pipe(source(file))
			.pipe(gulpif(jsMinify, buffer()))
			.pipe(gulpif(jsMinify, uglify()))// TODO if jsMinify, babel ES5 instead o ES6
			.on("error", handleErrors)
			.pipe(rename({
				extname: ".bundle.js"
			}))
			.pipe(gulp.dest(jsDest));
	});
	eventStream.merge.apply(null, tasks);
	return Promise.resolve();
}

gulp.task("three", function () {
	const minify = true;
	const outputFileName = "three.bundle.js";
	const configPath = "./three_build_config/";
	const rollupConfig = require(configPath + "rollup.config");

	const rollupStreamConfig = {
		// use newest rollup version instead of rollup-stream's dependency
		rollup: require('rollup'),
		// will be passed as arg to rollup.rollup(..)
		...rollupConfig.input("javascript/lib/threeSmall.js"),
		// will be passed as arg to bundle.generate(..)
		output : rollupConfig.output()
	};
	
	return rollup(rollupStreamConfig)
		.pipe(source(outputFileName))
		.pipe(buffer())
		.on("error", handleErrors)
		.pipe(closureCompiler({
			// copied from command line options of three.js build
			warning_level: 'VERBOSE',
			jscomp_off: ['globalThis', 'checkTypes'],
			language_in: 'ECMASCRIPT6_STRICT',
			language_out: 'ECMASCRIPT6_STRICT',
			externs: configPath + 'externs.js',
			output_wrapper: '// threejs.org/license\n%output%\n',
		},{
			platform: ['java']
		}))
		.on("error", handleErrors)
		.pipe(rename(outputFileName))
		.pipe(gulp.dest(jsDest));
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

gulp.task("build", gulp.series("build_js", "build_sass"));
