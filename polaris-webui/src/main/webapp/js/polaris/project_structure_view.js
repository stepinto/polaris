goog.provide("polaris.ProjectStructureView");

goog.require("goog.ui.Component");
goog.require("polaris.helper");
goog.require("polaris.services");

polaris.ProjectStructureView = function(project, path, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.project_ = project;
  this.tree_ = new goog.ui.tree.TreeControl("");
  this.populateNodes_(path);
}
goog.inherits(polaris.ProjectStructureView, goog.ui.Component);

/** @override */
polaris.ProjectStructureView.prototype.enterDocument = function() {
  this.tree_.render(this.getElement());
}

/** @override */
polaris.ProjectStructureView.prototype.exitDocument = function() {
}

polaris.ProjectStructureView.prototype.populateNodes_ = function(path) {
  this.doPopulateNodes_(path, -1, this.tree_);
}

polaris.ProjectStructureView.prototype.doPopulateNodes_ = function(path, pos, parentNode) {
  nextPos = path.indexOf("/", pos + 1);
  if (nextPos == -1) {
    return;
  }
  var prefix = path.substring(0, nextPos + 1);
  var this_ = this;
  parentNode.expand();
  polaris.services.listFiles(this.project_, prefix, function(resp) {
      if (resp.directories) {
        goog.array.forEach(resp.directories, function(dir) {
          var node = this_.tree_.createNode();
          node.setHtml(polaris.helper.removeStart(dir, prefix));
          parentNode.add(node);
          if (goog.string.endsWith(dir, "/") && goog.string.startsWith(path, dir)) {
          this_.doPopulateNodes_(path, nextPos, node);
          }
          });
      }
      if (resp.files) {
        goog.array.forEach(resp.files, function(file) {
          var node = this_.tree_.createNode();
          node.onClick_ = goog.nullFunction;
          node.setHtml(polaris.ProjectStructureView.createFileLink(file));
          parentNode.add(node);
        });
      }
  });
}

polaris.ProjectStructureView.createFileLink = function(file) {
  return goog.string.format("<a href=\"%s\">%s</a>",
      polaris.helper.convertFileIdToUrlFragment(file.id),
      polaris.helper.getLast(file.path.split("/")));
}

