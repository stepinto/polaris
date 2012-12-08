goog.provide("polaris.CodeView");

goog.require("goog.array");
goog.require("goog.bind");
goog.require("goog.dom.xml");
goog.require("goog.dom.NodeIterator");
goog.require("goog.ui.Component");
goog.require("polaris.PageController");

polaris.CodeView = function(source, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.source_ = source.source;
  this.xml_ = goog.dom.xml.loadXml(source.annotatedSource);
  this.lineNumSpans_ = [];
  polaris.CodeView.traverse_(this.xml_, goog.bind(polaris.CodeView.translate_, this.xml_));
};
goog.inherits(polaris.CodeView, goog.ui.Component);

/** @override */
polaris.CodeView.prototype.enterDocument = function() {
  var domHelper = this.getDomHelper();
  var root = this.getElement();

  var lineNumPre = domHelper.createDom(goog.dom.TagName.PRE, {
      "class": "fl",
      "style": "margin-top: 0px"
  });
  var lineNum = goog.string.countOf(this.source_, '\n') + 1;
  for (var i = 0; i < lineNum; i++) {
    var span = domHelper.createDom(goog.dom.TagName.SPAN);
    span.innerText = goog.string.format("%3d", i);
    lineNumPre.appendChild(span);
    lineNumPre.appendChild(domHelper.createDom(goog.dom.TagName.BR));
    this.lineNumSpans_.push(span);
  }
  root.appendChild(lineNumPre);

  var codePre = domHelper.createDom(goog.dom.TagName.PRE, {
      "style": "margin-left: 30"
  });
  codePre.innerHTML = goog.dom.xml.serialize(this.xml_);
  root.appendChild(codePre);
};

/** @override */
polaris.CodeView.prototype.exitDocument = function() {
};

polaris.CodeView.prototype.getDisplayOffsetOfLine = function(line) {
  return this.lineNumSpans_[line].offsetTop;
}

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

