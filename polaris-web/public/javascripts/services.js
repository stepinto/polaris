'use strict';

var rpcCount = 0;

/* Services */
angular.module('polarisServices', [])
  .factory('Protos', function() {
    var defs = {
      'protos/search.proto': [
        'CompleteRequest',
        'CompleteResponse',
        'SourceRequest',
        'SourceResponse',
        'GetFileHandleRequest',
        'GetFileHandleResponse',
        'ListFilesRequest',
        'ListFilesResponse',
        'GetTypeRequsest',
        'GetTypeResponse',
        'GetMethodRequest',
        'GetMethodResponse',
        'ListTypesInFileRequest',
        'ListTypesInFileResponse',
        'ListUsagesRequest',
        'ListUsagesResponse',
        'Hit'
      ],
      'protos/parser.proto': [
        'ClassType',
        'FileHandle',
        'Usage',
        'TypeUsage',
        'MethodUsage',
        'VariableUsage',
        'TypeKind',
        'PrimitiveType']
    };
    var ret = {};
    $.each(defs, function(protoFile, messages) {
      var pb = dcodeIO.ProtoBuf.protoFromFile('protos/search.proto');
      $.each(messages, function(i, message) {
        ret[message] = pb.build(message);
      });
    });
    return ret;
  })
  .factory('CodeSearch', function($http, $rootScope, Utils, Protos) {
    var now = function() {
      return new Date().getTime();
    };
    var execute = function(method, req, callback) {
      var startTime = now();
      $http.post('/api/' + method, req).success(function (resp) {
        var latency = now() - startTime;
        console.log('RPC #' + rpcCount + ' ' + method +
          ' status: ' + resp.status + ' latency: ' + latency + ' ms');
        rpcCount++;
        callback(resp);
      });
    };
    var execute2 = function(method, req, decoder, callback) {
      // We have to use XMLHttpRequest directly here, because $http seems to not
      // support receiving binary responses.
      var startTime = now();
      var xmlReq = new XMLHttpRequest();
      xmlReq.open('POST', '/api/' + method, true);
      xmlReq.responseType = 'arraybuffer';
      xmlReq.send(new Uint8Array(req.toArrayBuffer()));
      xmlReq.onload = function(e) {
        var latency = now() - startTime;
        var out = xmlReq.response;
        var resp = decoder.decode(out);
        rpcCount++;
        console.log('RPC #' + rpcCount + ' ' + method +
          ' status: ' + resp.status + ' latency: ' + latency + ' ms');
        callback(resp);
        $rootScope.$apply();
      };
    }
    return {
      search: function (query, rankFrom, rankTo, callback) {
        var req = {'query': query, 'rankFrom': rankFrom, 'rankTo': rankTo};
        execute('search', req, callback);
      },
      complete: function(query, limit, callback) {
        var req = new Protos.CompleteRequest();
        req.query = query;
        req.limit = limit;
        execute2('complete', req, Protos.CompleteResponse, callback);
      },
      readSourceByPath: function (project, path, callback) {
        var req = new Protos.SourceRequest();
        req.projectName = project;
        req.fileName = path;
        execute2('source', req, Protos.SourceResponse, callback);
      },
      readSourceById: function(id, callback) {
        var req = new Protos.SourceRequest();
        req.fileId = Number(id);
        execute2('source', req, Protos.SourceResponse, callback);
      },
      listFiles: function(project, path, callback) {
        var req = new Protos.ListFilesRequest();
        req.projectName = project;
        req.directoryName = path;
        execute2('listFiles', req, Protos.ListFilesResponse, function(resp) {
          if (resp.children) {
            resp.children.sort(function(left, right) {
              if (left.kind != right.kind) {
                return left.kind - right.kind;
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
        var req = new Protos.GetTypeRequest();
        req.typeId = typeId;
        execute2('getType', req, Protos.GetTypeResponse, callback);
      },
      getMethodById: function(methodId, callback) {
        var req = new Protos.GetMethodRequest();
        req.methodId = methodId;
        execute2('getMethod', req, Protos.GetMethodResponse, callback);
      },
      listUsages: function(kind, id, callback) {
        var req = new Protos.ListUsagesRequest();
        req.kind = kind;
        req.id = id;
        execute2('listUsages', req, Protos.ListUsagesResponse, callback);
      },
      listTypesInFile: function(fileId, callback) {
        var req = new Protos.ListTypesInFileRequest();
        req.fileId = fileId;
        req.limit = 2147483647;
        execute2('listTypesInFile', req, Protos.ListTypesInFileResponse, callback);
      },
      getFileHandle: function(project, path, callback) {
        var req = new Protos.GetFileHandleRequest();
        req.project = project;
        req.path = path;
        execute2('getFileHandle', req, Protos.GetFileHandleResponse, callback);
      }
    };
  })
  .factory('Utils', function(Protos) {
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
        if (type.kind == Protos.TypeKind.PRIMITIVE) {
          var result = null;
          $.each(Protos.PrimitiveType.Kind, function(name, value) {
            if (value == type.primitive.kind) {
              result = name.toLowerCase();
            }
          });
          if (result == null) {
            console.log('Unknown primitive type: ' + type);
            result = 'UnknownPrimitive#' + type;
          }
          return result;
        } else if (type.kind == Protos.TypeKind.CLASS) {
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
      },
      'replaceAll': function(str, s, t) {
        return str.replace(new RegExp(s, 'g'), t);
      },

      // Returns type/method/variable id of a given usage.
      'getEntityIdOfUsage': function(usage) {
        if (usage.kind == Protos.Usage.Kind.TYPE) {
          if (usage.type.type.clazz.resolved) {
            return usage.type.type.clazz.id;
          } else {
            console.log("Unresolved type: " + usage.type.type);
            return 0;
          }
        } else if (usage.kind == Protos.Usage.Kind.METHOD) {
          return usage.method.method.id;
        } else if (usage.kind == Protos.Usage.Kind.VARIABLE) {
          return usage.variable.variable.id;
        }
        console.log('Found usage of unknown kind:', usage);
        return 0;
      },
      'ab2str': function(buf) {
        return String.fromCharCode.apply(null, new Uint8Array(buf));
      },
      'str2ab': function(str) {
        var buf = new ArrayBuffer(str.length*2); // 2 bytes for each char
        var bufView = new Uint16Array(buf);
        for (var i=0, strLen=str.length; i<strLen; i++) {
          bufView[i] = str.charCodeAt(i);
        }
        return buf;
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
