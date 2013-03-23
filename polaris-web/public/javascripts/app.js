'use strict';

/*global angular*/

angular.module('polaris', ['polarisServices', 'polarisDirectives']).
  config(function ($routeProvider, $locationProvider) {
    $routeProvider
      .when('/', {
        controller: IndexCtrl,
        templateUrl: 'partials/index',
        reloadOnSearch: false
      })
      .when('/search', {
        controller: SearchCtrl,
        templateUrl: 'partials/search',
        reloadOnSearch: false
      })
      .when('/source', {
        controller: SourceCtrl,
        templateUrl: 'partials/source',
        reloadOnSearch: false
      });
    $locationProvider
      .html5Mode(true)
      .hashPrefix('!');
  });
