'use strict';

/* Services */

angular.module('polarisServices', [])
  .factory('CodeSearch', function($http) {
    return {
      search: function (query, rankFrom, rankTo, callback) {
        var req = {'query': query, 'rankFrom': rankFrom, 'rankTo': rankTo};
        $http.post('/api/search', req).success(callback);
      },
      complete: function(query, limit, callback) {
        var req = {'query': query, 'limit': limit};
        $http.post('/api/complete', req).success(callback);
      },
      readSourceByPath: function (project, path, callback) {
        var req = {'projectName': project, 'fileName': path};
        $http.post('/api/source', req).success(callback);
      },
      readSourceById: function(id, callback) {
        var req = {'fileId': Number(id)};
        $http.post('/api/source', req).success(callback);
      },
      listFiles: function(project, path, callback) {
        var req = {'projectName': project, 'directoryName': path};
        $http.post('/api/layout', req).success(callback);
      },
      getTypeById: function(typeId, callback) {
        var req = {'typeId': Number(typeId)};
        $http.post('/api/getType', req).success(callback);
      },
      getMethodById: function(methodId, callback) {
        var req = {'methodId': Number(methodId)};
        $http.post('/api/getMethod', req).success(callback);
      },
      listTypeUsages: function(typeId, callback) {
        var req = {'typeId': Number(typeId)};
        $http.post('/api/listTypeUsages', req).success(callback);
      },
      listMethodUsages: function(methodId, callback) {
        var req = {'methodId': Number(methodId)};
        $http.post('/api/listMethodUsages', req).success(callback);
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
      }
    };
  })
  .factory('LinkBuilder', function() {
    return {
      'source': function(jumpTarget) {
        if (jumpTarget.span) {
          return '/source?file=' + jumpTarget.file.id + '&line=' + jumpTarget.span.from.line;
        } else {
          return '/source?file=' + jumpTarget.file.id;
        }
      },
      'sourceFromFileId': function(fileId) {
        return '/source?file=' + fileId;
      },
      'type': function(id) {
        return '/goto/type/' + id;
      },
      'method': function(id) {
        return '/goto/method/' + id;
      }
    };
  });
