goog.provide("polaris.CodeView");

goog.require("goog.array");
goog.require("goog.bind");
goog.require("goog.dom.xml");
goog.require("goog.dom.NodeIterator");
goog.require("goog.ui.Component");
goog.require("polaris.PageController");

polaris.CodeView = function(annotation, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.xml_ = goog.dom.xml.loadXml(annotation);
  polaris.CodeView.traverse_(this.xml_, goog.bind(polaris.CodeView.translate_, this.xml_));
};
goog.inherits(polaris.CodeView, goog.ui.Component);

/** @override */
polaris.CodeView.prototype.enterDocument = function() {
  var domHelper = this.getDomHelper();
  var root = this.getElement();
  var pre = domHelper.createDom("pre");
  root.appendChild(pre);
  pre.innerHTML = goog.dom.xml.serialize(this.xml_);
};

/** @override */
polaris.CodeView.prototype.exitDocument = function() {
};

polaris.CodeView.traverse_ = function(root, callback) {
  goog.array.forEach(root.childNodes, function(node) {
      var newNode = callback(node);
      if (newNode != null) {
        root.replaceChild(newNode, node);
      }
      polaris.CodeView.traverse_(node, callback);
  });
}

polaris.CodeView.translate_ = function(node, doc) {
  if (node.tagName == "source") {
    node.tagName = "div";
  } else if (node.tagName == "type-usage") {
    var resolved = (node.getAttribute("resolved") == "true");
    var typeId = Number(node.getAttribute("type-id"));
    var newNode = null;
    if (resolved) {
      newNode = goog.dom.createElement(goog.dom.TagName.A);
      newNode.setAttribute("href",
          polaris.PageController.createUrlFragment("goto", {type: typeId}));
    } else {
      newNode = goog.dom.createElement(goog.dom.TagName.SPAN);
    }
    newNode.textContent = node.textContent;
    return newNode;
  }
  return null;
};

