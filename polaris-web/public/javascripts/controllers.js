'use strict';

/* Controllers */

function IndexCtrl($scope, $location) {
    $scope.search = function () {
        $location.url("search?query=" + $scope.query);
    };
}

function SearchCtrl($scope, $routeParams, CodeSearch, LinkBuilder) {
    $scope.query = ($routeParams.query ? $routeParams.query : "");
    $scope.loading = false;
    $scope.search = function () {
        if ($scope.query == "") {
            return;
        }
        $scope.loading = true;
        CodeSearch.search($scope.query, 0, 20, function (resp) {
            $scope.loading = false;
            $scope.results = resp.hits;
            $.each($scope.results, function(i, hit) {
              hit.url = LinkBuilder.source(hit.jumpTarget.file.id, hit.jumpTarget.span.from.line);
            });
            $scope.latency = resp.latency;
            $scope.count = resp.count;
        });
    }
    $scope.search();
}

function SourceCtrl($scope, $routeParams, CodeSearch) {
    $scope.loading = true;
    var callback = function (resp) {
        $scope.loading = false;
        $scope.sourceCode = resp.source.source;
        $scope.sourceCodeAnnotation = resp.source.annotatedSource;
        $scope.project = resp.source.handle.project;
        $scope.path = resp.source.handle.path;
        $scope.highlightedLine = $routeParams.line;
    };
    if ($routeParams.project && $routeParams.path) {
        CodeSearch.readSourceByPath($routeParams.project, $routeParams.path, callback);
    } else if ($routeParams.file) {
        CodeSearch.readSourceById($routeParams.file, callback);
    }

    var loadXrefs = function(kind, id) {
      $scope.loadingXrefs = true;
      $scope.xrefs = undefined;
      CodeSearch.listUsages(kind, id, function(resp) {
        $scope.loadingXrefs = false;
        $scope.xrefs = resp.usages;
      });
    }

    $scope.findTypeUsages = function(typeId) {
      console.log('Find type usages:', typeId);
      loadXrefs('TYPE', typeId);
    }

    $scope.goToTypeDefinition = function(typeId) {
      console.log("Go to def:", typeId);
    }

    $scope.findMethodUsages = function(methodId) {
      console.log('Find method usages:', methodId);
      loadXrefs('METHOD', methodId);
    }
}

function GoToTypeCtrl($routeParams, CodeSearch, LinkBuilder, Utils, $location) {
    CodeSearch.getTypeById($routeParams.typeId, function(resp) {
        var target = resp.classType.jumpTarget;
        var url = LinkBuilder.source(target);
        $location.url(Utils.removeStart(url, '#'));
        $location.replace();
    });
}

function GoToMethodCtrl($routeParams, CodeSearch, LinkBuilder, Utils, $location) {
    CodeSearch.getMethodById($routeParams.methodId, function(resp) {
        var target = resp.method.jumpTarget;
        var url = LinkBuilder.source(target);
        $location.url(Utils.removeStart(url, '#'));
        $location.replace();
    });
}

