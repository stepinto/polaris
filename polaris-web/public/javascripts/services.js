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
                var req = {'fileId': id};
                $http.post('/api/source', req).success(callback);
            },
            listFiles: function(project, path, callback) {
                var req = {'projectName': project, 'directoryName': path};
                $http.post('/api/layout', req).success(callback);
            }
        };
    })
    .factory('Utils', function() {
        return {
            'startsWith': function(s, t) {
                return s.indexOf(t) == 0;
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
            }
        };
    })
    .factory('LinkBuilder', function() {
        return {
            'source': function(id, line) {
              if (!line) {
                  line = 0;
              }
              return '#/source?file=' + id + '&line=' + line;
            }
        };
    });
