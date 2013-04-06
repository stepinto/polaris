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

function SourceCtrl($scope, $routeParams, CodeSearch, Utils) {
  $scope.leftPanelVisible = true;

  var showFile = function (file, line) {
    console.log('Show file', file);
    if (file.kind == 'DIRECTORY') {
      $scope.view = 'DIR';
    } else if (file.kind == 'NORMAL_FILE') {
      $scope.view = 'CODE';
    } else {
      console.warn('Ignore bad FileHandle.Kind:', file.kind);
    }
    $scope.file = file;
    $scope.fileId = file.id;
    $scope.project = file.project;
    $scope.path = file.path;
    if (!$scope.pathsToExpand) {
      $scope.pathsToExpand = [$scope.path];
    } else if ($scope.pathsToExpand.indexOf($scope.path) == -1) {
      $scope.pathsToExpand.push($scope.path);
    }
    $scope.highlightedLine = line;
  };

  if (!$routeParams.project || !$routeParams.path) {
    console.error('Expect parameter project and path');
    return;
  }
  CodeSearch.getFileHandle($routeParams.project, $routeParams.path, function (resp) {
    var line = 0;
    if ($routeParams.line) {
      line = $routeParams.line;
    }
    showFile(resp.fileHandle, line);
  });

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

  $scope.onSelectFile = function(file) {
    showFile(file, 0);
  };

  $scope.onSelectJumpTarget = function(jumpTarget) {
    showFile(jumpTarget.file, jumpTarget.span.from.line);
  }
}

