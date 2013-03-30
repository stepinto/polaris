'use strict';

/* Services */

angular.module('polarisServices', [])
  .factory('CodeSearch', function($http, Utils) {
    var execute = function(method, req, callback) {
      $http.post('/api/' + method, req).success(callback);
    };
    return {
      search: function (query, rankFrom, rankTo, callback) {
        var req = {'query': query, 'rankFrom': rankFrom, 'rankTo': rankTo};
        execute('search', req, callback);
      },
      complete: function(query, limit, callback) {
        var req = {'query': query, 'limit': limit};
        execute('complete', req, callback);
      },
      readSourceByPath: function (project, path, callback) {
        var req = {'projectName': project, 'fileName': path};
        execute('source', req, callback);
      },
      readSourceById: function(id, callback) {
        var req = {'fileId': Number(id)};
        execute('source', req, callback);
      },
      listFiles: function(project, path, callback) {
        var FILE_HANDLE_KIND_ORDINALS = {'DIRECTORY': 0, 'NORMAL_FILE': 1};
        var req = {'projectName': project, 'directoryName': path};
        execute('listFiles', req, function(resp) {
          if (resp.children) {
            resp.children.sort(function(left, right) {
              var leftKindOrdinal = FILE_HANDLE_KIND_ORDINALS[left.kind];
              var rightKindOrdinal = FILE_HANDLE_KIND_ORDINALS[right.kind];
              if (leftKindOrdinal != rightKindOrdinal) {
                return leftKindOrdinal - rightKindOrdinal;
              }
              if (left.project != right.project) {
                return Utils.strcmp(left.project, right.project);
              }
              return Utils.strcmp(left.path, right.path);
            });
            callback(resp);
          }
        });
      },
      getTypeById: function(typeId, callback) {
        var req = {'typeId': Number(typeId)};
        execute('getType', req, callback);
      },
      getMethodById: function(methodId, callback) {
        var req = {'methodId': Number(methodId)};
        execute('getMethod', req, callback);
      },
      listUsages: function(kind, id, callback) {
        var req = {'kind': kind, 'id': Number(id)};
        execute('listUsages', req, callback);
      },
      listTypesInFile: function(fileId, callback) {
        var req = {'fileId': Number(fileId), 'limit': 2147483647};
        execute('listTypesInFile', req, callback);
      },
      getFileHandle: function(project, path, callback) {
        var req = {'project': project, 'path': path};
        execute('getFileHandle', req, callback);
      }
    };
  })
  .factory('Utils', function() {
    return {
      'startsWith': function(s, t) {
        return s.indexOf(t) == 0;
      },
      'endsWith': function(s, t) {
        return s.lastIndexOf(t) + t.length == s.length;
      },
      'getFirst': function(a) {
        if (a.length == 0) {
          return true;
        }
        else {
          return a[0];
        }
      },
      'getLast': function(a) {
        if (a.length == 0) {
          return null;
        } else {
          return a[a.length - 1];
        }
      },
      'getBaseName': function(path) {
        var parts = path.split('/');
        for (var i = parts.length - 1; i >= 0; i--) {
          var part = parts[i];
          if (part != "") {
            return part;
          }
        }
        return null;
      },
      'str2bool': function(s) {
        return s != 'false';
      },
      'removeStart': function(s, t) {
        if (this.startsWith(s, t)) {
          return s.substring(t.length);
        } else {
          return s;
        }
      },
      'countLines': function(s) {
        var pos = -1;
        var count = 0;
        while ((pos = s.indexOf('\n', pos + 1)) != -1) {
          count++;
        }
        if (!this.endsWith(s, '\n')) {
          count++;
        }
        return count;
      },
      'getSimpleName': function(s) {
        var pos = s.lastIndexOf('.');
        if (pos == -1) {
          return s;
        }
        return s.substring(pos + 1);
      },
      'getDisplayNameOfTypeHandle': function(type) {
        if (type.kind == 'PRIMITIVE') {
          var n = type.primitive.kind.toLowerCase();
          if (n == 'integer') { return 'int'; }
          return n;
        } else if (type.kind == 'CLASS') {
          return this.getSimpleName(type.clazz.name);
        } else {
          console.log("Unknown kind of type handle: " + type);
          return null;
        }
      },
      'getDisplayNameOfMethodHandle': function(method) {
        var name = this.getSimpleName(method.name);
        if (name == '<cinit>') {
          return "static-block";
        } else if (name == '<init>') {
          return 'constructor';
        } else {
          return name;
        }
      },
      'getDisplayNameOfFieldHandle': function(field) {
        return this.getSimpleName(field.name);
      },
      'escapeHTML': function(s) {
        var t = '';
        for (var i = 0; i < s.length; i++) {
          var ch = s[i];
          if (ch == '&') {
            t += '&amp;';
          } else if (ch == '<') {
            t += '&lt;';
          } else if (ch == '>') {
            t += '&gt;';
          } else if (ch == '\'') {
            t += '&apos;';
          } else {
            t += ch;
          }
        }
        return t;
      },
      'strcmp': function(s, t) {
        if (s == t) {
          return 0;
        } else if (s < t) {
          return -1;
        } else {
          return 1;
        }
      }
    };
  })
  .factory('LinkBuilder', function() {
    return {
      'source': function(jumpTarget) {
        var url = this.sourceFromHandle(jumpTarget.file);
        if (jumpTarget.span) {
          url += '&line=' + jumpTarget.span.from.line;
        }
        return url;
      },
      'sourceFromProjectAndPath': function(project, path) {
        return '/source?project=' + project + '&path=' + path;
      },
      'sourceFromHandle': function(fileHandle) {
        return this.sourceFromProjectAndPath(fileHandle.project, fileHandle.path);
      }
    };
  });
