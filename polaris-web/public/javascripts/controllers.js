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
  var readSourceCallback = function (resp) {
    $scope.fileId = resp.source.handle.id;
    $scope.sourceCode = resp.source.source;
    $scope.usages = resp.usages ? resp.usages : [];
    $scope.project = resp.source.handle.project;
    $scope.path = resp.source.handle.path;
    if (!$scope.pathsToExpand) {
      $scope.pathsToExpand = [$scope.path];
    } else if ($scope.pathsToExpand.indexOf($scope.path) == -1) {
      $scope.pathsToExpand.push($scope.path);
    }
    $scope.highlightedLine = $routeParams.line;
  };
  if ($routeParams.project && $routeParams.path) {
    CodeSearch.readSourceByPath($routeParams.project, $routeParams.path, readSourceCallback);
  } else if ($routeParams.file) {
    CodeSearch.readSourceById($routeParams.file, readSourceCallback);
  }

  var loadXrefs = function(kind, id) {
    $scope.loadingXrefs = true;
    $scope.xrefs = undefined;
    CodeSearch.listUsages(kind, id, function(resp) {
      $scope.loadingXrefs = false;
      $scope.xrefs = resp.usages;
    });
  };

  $scope.onFindUsages = function(kind, id) {
    console.log('Find usages:', kind, id);
    loadXrefs(kind, id);
  };

  $scope.onGoToDefinition = function(kind, id) {
    console.log("Go to def:", kind, id);
  };

  $scope.onSelectFile = function(fileId) {
    // TODO: update URL
    CodeSearch.readSourceById(fileId, readSourceCallback);
  };

  $scope.onSelectJumpTarget = function(jumpTarget) {
    $routeParams.line = jumpTarget.span.from.line;
    CodeSearch.readSourceById(jumpTarget.file.id, readSourceCallback);
  }
}

