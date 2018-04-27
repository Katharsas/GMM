const path = require('path');

var jsDir = "./javascript/";
var jsDest = "../webapp/resources/javascript/compiled";

module.exports = {
    mode: "none",
    entry: jsDir + 'tasks.js',
    output: {
        filename: 'tasks.bundle.js',
        path: path.resolve(__dirname, jsDest)
    }
};