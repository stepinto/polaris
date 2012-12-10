goog.provide("polaris.PathBar");

goog.require("goog.array");
goog.require("goog.dom.TagName");
goog.require("goog.ui.Component");
goog.require("polaris.helper");

polaris.PathBar = function(path, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.path_ = path;
}
goog.inherits(polaris.PathBar, goog.ui.Component);

/** @override */
polaris.PathBar.prototype.enterDocument = function() {
  var root = this.getElement();
  var domHelper = this.getDomHelper();
  var pathParts = polaris.helper.removeStart(this.path_, "/").split("/");
  var ul = domHelper.createDom(goog.dom.TagName.UL, {"style": "padding-left: 0px"});
  root.appendChild(ul);
  goog.array.forEach(pathParts, function(part, index) {
      var style = "display: inline; list-style: none; padding: 0px 1px 0px 1px";
      var li1 = domHelper.createDom(goog.dom.TagName.LI, {"style": style});
      li1.innerText = "/";
      ul.appendChild(li1);
      var li2 = domHelper.createDom(goog.dom.TagName.LI, {"style": style});
      li2.innerText = part;
      ul.appendChild(li2);
  });
}

/** @override */
polaris.PathBar.prototype.exitDocument = function() {
}

