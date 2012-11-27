goog.provide("polaris.PageController");

goog.require("goog.array");
goog.require("goog.bind");
goog.require("goog.events");
goog.require("goog.string");
goog.require("goog.History");

polaris.PageController = function() {
  this.history_ = new goog.History;
  this.history_.setEnabled(true);
  this.pages = [];
  goog.events.listen(this.history_, goog.history.EventType.NAVIGATE,
      goog.bind(this.onNavigate_, this));
  goog.events.listen(window, goog.events.EventType.LOAD, goog.bind(this.onLoad_, this));
}

polaris.PageController.prototype.history_ = null;

polaris.PageController.createUrlFragment = function(page, parameters) {
  return goog.string.format("#page=%s&%s", page, 
      goog.uri.utils.buildQueryDataFromMap(parameters));
}

polaris.PageController.prototype.registerPage = function(name, loadCallback, dom) {
  this.pages.push({
    name: name,
    load: loadCallback,
    dom: dom
  });
};

polaris.PageController.prototype.showPage = function(name, parameters) {
  var token = "page=" + goog.string.urlEncode(name);
  for (var key in parameters) {
    token += "&" + goog.string.urlEncode(key) + "=" + goog.string.urlEncode(parameters[key]);
  }
  this.showPageByUrlFragment(token);
};

polaris.PageController.prototype.showPageByUrlFragment = function(fragment) {
  if (goog.string.startsWith(fragment, "#")) {
    fragment = fragment.substring(1);
  }
  this.history_.setToken(fragment);
}

polaris.PageController.prototype.handleHistory_ = function(token) {
  var page = "index";
  var parameters = {}
  goog.array.forEach(token.split("&"), function(part) {
    var pos = part.indexOf("=");
    var key;
    var value;
    if (pos == -1) {
      key = part;
      value = "";
    } else {
      key = part.substring(0, pos);
      value = part.substring(pos + 1);
    }
    if (key == "page") {
      page = value;
    } else {
      parameters[key] = value;
    }
  });
  this.doShowPage_(page, parameters);
};

polaris.PageController.prototype.doShowPage_ = function(pageName, parameters) {
  var page = goog.array.find(this.pages, function(p) { return p.name == pageName; });
  if (page == null) {
    page = this.pages[0];
    console.log("fallback to index (requested page " + page.name + ")");
  } else {
    console.log("switch to page " + page.name);
  }
  goog.array.forEach(this.pages, function(p) {
    var visible = (p == page);
    goog.style.showElement(p.dom, visible);
  });
  page.load(parameters);
};

polaris.PageController.prototype.onLoad_ = function() {
  var fragment = goog.uri.utils.getFragment(document.location.href);
  if (fragment == null) {
    fragment = "page=index";
  }
  this.handleHistory_(fragment);
};

polaris.PageController.prototype.onNavigate_ = function(e) {
  this.handleHistory_(e.token);
};

