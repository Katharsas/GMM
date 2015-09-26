import square from "./provider1";
import arg from "./provider2";
import { Module } from "./provider3";

import $ from "./lib/jquery";


var x = square(arg);
var $div = $("#hello");
console.log(x);
console.log($div);
console.log(Module.member);