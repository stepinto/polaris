'use strict';

/*global angular*/

angular.module('polaris', []).
    config(function ($routeProvider) {
        $routeProvider.
            when('/', {
                controller: SearchCtrl,
                templateUrl: 'partials/index'
            });
    });
