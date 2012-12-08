goog.provide("polaris.ClassStructureView");

goog.require("goog.array");
goog.require("goog.string");
goog.require("goog.ui.tree.TreeControl");
goog.require("polaris.helper");

// TODO: handle click event to expand tree on folders

polaris.ClassStructureView = function(types, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.tree_ = tree = new goog.ui.tree.TreeControl("");
  tree.setIsUserCollapsible(false);
  goog.array.forEach(types, function(type) {
    polaris.ClassStructureView.addTypeToTree_(type, tree);
  });
}
goog.inherits(polaris.ClassStructureView, goog.ui.Component);

/** @override */
polaris.ClassStructureView.prototype.enterDocument = function() {
  this.tree_.render(this.getElement());
}

/** @override */
polaris.ClassStructureView.prototype.exitDocument = function() {
}

polaris.ClassStructureView.addTypeToTree_ = function(type, tree) {
  // TODO: handle inner class
  classNode = tree.createNode();
  classNode.setHtml(polaris.ClassStructureView.createClassLink_(type));
  classNode.onClick_ = goog.nullFunction;
  classNode.expand();
  tree.add(classNode);
  if (type.fields) {
    goog.array.forEach(type.fields, function(field) {
        fieldNode = tree.createNode();
        fieldNode.setHtml(polaris.ClassStructureView.createFieldLink_(field));
        fieldNode.onClick_ = goog.nullFunction;
        classNode.add(fieldNode);
    });
  }
  if (type.methods) {
    goog.array.forEach(type.methods, function(method) {
        methodNode = tree.createNode();
        methodNode.setHtml(polaris.ClassStructureView.createMethodLink_(method));
        methodNode.onClick_ = goog.nullFunction;
        classNode.add(methodNode);
    });
  }
}

polaris.ClassStructureView.createClassLink_ = function(type) {
  return goog.string.format(
      "<a href=\"%s\" class=\"class_link\">%s</a>",
      polaris.helper.convertJumpTargetToUrlFragment(type.jumpTarget),
      goog.string.htmlEscape(polaris.helper.dropPackageName(type.handle.name)));
}

polaris.ClassStructureView.createFieldLink_ = function(field) {
  return goog.string.format(
      "<a href=\"%s\" class=\"field_link\">%s</a>",
      polaris.helper.convertJumpTargetToUrlFragment(field.jumpTarget),
      goog.string.htmlEscape(polaris.helper.dropPackageAndClassName(field.handle.name)));
}

polaris.ClassStructureView.createMethodLink_ = function(method) {
  var name = polaris.helper.dropPackageAndClassName(method.handle.name);
  var text;
  if (name == "<init>") {
    text = "constructor";
  } else if (name == "<cinit>") {
    text = "static-block";
  } else {
    text = name;
  }
  text += "(";
  if (method.parameters) {
    goog.array.forEach(method.parameters, function(param, index) {
        if (index > 0) {
          text += ", ";
        }
        text += polaris.helper.dropPackageName(param.type.name);
    });
  }
  text += "): ";
  text += polaris.helper.dropPackageName(method.returnType.name);
  return goog.string.format(
      "<a href=\"%s\" class=\"method_link\">%s</a>",
      polaris.helper.convertJumpTargetToUrlFragment(method.jumpTarget),
      text);
}

