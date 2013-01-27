'use strict';

/*global angular*/

angular.module('polaris', ['polarisServices', 'polarisDirectives']).
  config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        controller: IndexCtrl,
        templateUrl: 'partials/index'
      })
      .when('/search', {
        controller: SearchCtrl,
        templateUrl: 'partials/search'
      })
      .when('/source', {
        controller: SourceCtrl,
        templateUrl: 'partials/source'
      })
      .when('/goto/type/:typeId', {
        controller: GoToTypeCtrl,
        templateUrl: 'partials/null'
      });
  });
