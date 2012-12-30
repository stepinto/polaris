/*global angular*/

function SearchCtrl($scope) {
    $scope.submit = function (a, b, c) {
        console.log("$scope:", $scope);
    };
}

angular.module('Polaris', []).
    config(function ($routeProvider) {
        $routeProvider.
            when('/', {
                controller: SearchCtrl,
                templateUrl: 'Bonjour.html'
            });
    });
