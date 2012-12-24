goog.provide("polaris.helper");

goog.require("goog.string");

polaris.helper.convertFileIdToUrlFragment = function(fileId) {
  return goog.string.format("#page=code&file=%d&offset=0", fileId);
}

polaris.helper.convertJumpTargetToUrlFragment = function(jumpTarget) {
  return goog.string.format(
      "#page=code&file=%d&offset=%d",
      jumpTarget.fileId,
      jumpTarget.position.line);
}

polaris.helper.dropPackageName = function(fullTypeName) {
  var pos = fullTypeName.lastIndexOf(".");
  return fullTypeName.substring(pos + 1);
}

polaris.helper.dropPackageAndClassName = function(fullMemberName) {
  var pos = fullMemberName.lastIndexOf("#");
  return fullMemberName.substring(pos + 1);
}

polaris.helper.codeOffsetToLineNum = function(code, offset) {
  if (offset >= code.length) {
    console.log("Offset " + offset + " is out of range");
    offset = code.length;
  }
  return goog.string.countOf(code.substring(0, offset), "\n");
}

polaris.helper.removeStart = function(s, t) {
  if (goog.string.startsWith(s, t)) {
    return s.substring(t.length);
  } else {
    return s;
  }
}

polaris.helper.getLast = function(a) {
  if (a.length == 0) {
    return null;
  } else {
    return a[a.length - 1];
  }
}
