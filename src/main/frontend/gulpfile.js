/* jshint node: true */
'use strict';

/**
 * Commented out watch code, could prove useful at some point.
 */

var gulp = require('gulp');
var babel = require("gulp-babel");
var sass = require("gulp-sass");
//var files = require('fs');
//var path = require('path');
//var watch = require('gulp-watch');

/**
 * ########################
 * Paths & Files
 * ########################
 */

//JavaScript

var jsDir = "javascript/";
var jsDest = "../webapp/resources/javascript/compiled/";
var jsFilter = "**/*.js";

//SASS

var sassDir = "sass/";
var sassDest = "../webapp/resources/css/compiled/";
var sassFilter = "**/*.scss";

/**
 * ########################
 * Gulp Build
 * ########################
 */

gulp.task('build-js', function () {
	return gulp
		.src(jsDir + jsFilter)
		.pipe(babel())
		.pipe(gulp.dest(jsDest));
});

gulp.task('build-sass', function () {
	return gulp
		.src(sassDir + sassFilter)
		.pipe(sass().on('error', sass.logError))
		.pipe(gulp.dest(sassDest));
});

gulp.task('build', ['build-js', 'build-sass']);


//gulp.task('watch-scripts', function () {
//	var options = {
//		unlink: '-'.red + 'Removed',
//		add: '+'.green + 'Added',
//		change: '*'.cyan + 'Changed'
//	};
//
//	return watch(config.src.scripts.dir + config.src.scripts.files, function (vinyl) {
//		vinyl.path = vinyl.path.replace(new RegExp('\\' + path.sep, 'g'), '/');
//		var to = vinyl.path.split(config.src.scripts.dir)[1].split('.');
//		var ext = to.pop();
//		to = to.join('.');
//		var toPath = to.split('/');
//		toPath.pop()
//
//		console.log(options[vinyl.event] + ' ' + to.magenta + '...');
//
//		if (vinyl.event === 'unlink') {
//			files.unlinkSync("js/" + to + ".js");
//		} else {
//			gulp.src(config.src.scripts.dir + to + "." + ext)
//				.pipe(scriptsCompiler())
//				.pipe(gulp.dest(config.dist.scripts + toPath));
//		}
//	});
//});
//
//gulp.task('watch-styles', function () {
//	var options = {
//		unlink: '-'.red + 'Removed',
//		add: '+'.green + 'Added',
//		change: '*'.cyan + 'Changed'
//	};
//
//	return watch(config.src.styles.dir + config.src.styles.files, function (vinyl) {
//		vinyl.path = vinyl.path.replace(new RegExp('\\' + path.sep, 'g'), '/');
//		var to = vinyl.path.split(config.src.styles.dir)[1].split('.');
//		to.pop();
//		to = to.join('.');
//		var toPath = to.split('/');
//		toPath.pop()
//
//		console.log(options[vinyl.event] + ' ' + to.magenta + '...');
//
//		gulp
//			.src(config.src.styles.dir + config.src.styles.start)
//			.pipe(stylesCompiler())
//			.pipe(gulp.dest(config.dist.styles))
//	});
//});
//
//gulp.task('watch', ['watch-scripts', 'watch-styles']);