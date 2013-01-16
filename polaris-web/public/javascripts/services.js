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
            }
        };
    });
