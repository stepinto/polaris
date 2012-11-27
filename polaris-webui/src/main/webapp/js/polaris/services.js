goog.provide("polaris.services");

goog.require("goog.net.XhrIo");
goog.require("goog.string");
goog.require("goog.uri.utils");

polaris.services.search = function(query, from, to, callback) {
  var parameters = {
    query: query,
    from: from,
    to: to
  };
  polaris.services.exec_("search", parameters, callback);
};

polaris.services.complete = function(query, n, callback) {
  var parameters = {
    query: query,
    limit: n
  }
  polaris.services.exec_("complete", parameters, callback);
}

polaris.services.readSourceCode = function(fileId, callback) {
  var parameters = {
    fileId: fileId
  };
  polaris.services.exec_("code", parameters, callback);
};

polaris.services.readType = function(typeId, callback) {
  var parameters = {
    typeId: typeId
  }
  polaris.services.exec_("type", parameters, callback);
};

polaris.services.exec_ = function(command, parameters, callback) {
  var url = goog.string.format("/ajax/%s?%s",
      goog.string.urlEncode(command),
      goog.uri.utils.buildQueryDataFromMap(parameters));
  goog.net.XhrIo.send(url, function(e) {
      callback(e.target.getResponseJson());
  });
};
