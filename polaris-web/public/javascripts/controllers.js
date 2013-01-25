'use strict';

/* Controllers */

function IndexCtrl($scope, $location) {
    $scope.search = function () {
        $location.url("search?query=" + $scope.query);
    };
}

function SearchCtrl($scope, $routeParams, CodeSearch) {
    $scope.query = ($routeParams.query ? $routeParams.query : "");
    $scope.loading = false;
    $scope.search = function () {
        console.log("search");
        if ($scope.query == "") {
            return;
        }
        $scope.loading = true;
        CodeSearch.search($scope.query, 0, 20, function (resp) {
            console.log("resp = ", resp);
            $scope.loading = false;
            $scope.results = resp.hits;
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
    };
    if ($routeParams.project && $routeParams.path) {
        CodeSearch.readSourceByPath($routeParams.project, $routeParams.path, callback);
    } else if ($routeParams.file) {
        CodeSearch.readSourceById($routeParams.file, callback);
    }
    
    $scope.findUsages = function(typeId) {
      console.log("findUsage", typeId);
    }
    $scope.goToDefinition = function(typeId) {
      console.log("goToDefinition", typeId);
    }
}

function GoToTypeCtrl($routeParams, CodeSearch, LinkBuilder, Utils, $location) {
    CodeSearch.getTypeById($routeParams.typeId, function(resp) {
        var target = resp.classType.jumpTarget;
        var url = LinkBuilder.source(target.fileId, target.span.from.line);
        $location.url(Utils.removeStart(url, '#'));
    });
}

