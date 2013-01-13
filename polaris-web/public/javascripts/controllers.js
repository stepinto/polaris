'use strict';

/* Controllers */

function SearchCtrl($scope) {
    $scope.submit = function (a, b, c) {
        console.log("$scope:", $scope);
    };
}

