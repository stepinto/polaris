goog.provide("polaris.SearchBox");

goog.require("goog.ui.Component");
goog.require("goog.ui.ac.ArrayMatcher");
goog.require("goog.ui.ac.AutoComplete");
goog.require("polaris.services");

polaris.SearchBox = function(opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
}
goog.inherits(polaris.SearchBox, goog.ui.Component);

/** @override */
polaris.SearchBox.prototype.canDecorate = function(element) {
  return element.tagName == goog.dom.TagName.INPUT;
}

/** @override */
polaris.SearchBox.prototype.render = function() {
  throw new Error("must call decoreate()");
}

/** @override */
polaris.SearchBox.prototype.enterDocument = function() {
  var matcher = new polaris.SearchBox.ArrayMatcher(["hello", "world"], true);
  var renderer = new goog.ui.ac.Renderer();
  var inputHandler = new goog.ui.ac.InputHandler(null, null, false);
  var autoComplete = new goog.ui.ac.AutoComplete(matcher, renderer, inputHandler);
  inputHandler.attachAutoComplete(autoComplete);
  inputHandler.attachInputs(this.getElement());
}

/** @override */
polaris.SearchBox.prototype.exitDocument = function() {
}

polaris.SearchBox.prototype.getValue = function() {
  return this.getElement().value;
}

polaris.SearchBox.ArrayMatcher = function() {
}

polaris.SearchBox.ArrayMatcher.prototype.requestMatchingRows = function(query, n, callback) {
  polaris.services.complete(query, n, function(resp) {
      callback(query, resp.entries);
  });
}

polaris.SearchBox.ArrayMatcher.prototype.shouldRequestMatches = function() {
  return true;
}
