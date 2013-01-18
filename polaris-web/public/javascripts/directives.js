angular.module('polarisDirectives', ['polarisServices'])
    // Search box with auto completion
    //
    // Usage:
    //   <search-box placeholder="..." />
    .directive('searchBox', function ($location, CodeSearch) {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                placeholder: '@placeholder',
            },
            template:
                '<span class="search-box">\n' + 
                '  <input class="query-input" type="text" ng-model="query" ng-placeholder="placeholder"\n' + 
                '    tabindex="1" accesskey="s" class="input-medium search-query" focused="focused">\n' + 
                '  <ul class="choice-box" ng-show="choices.length > 0">\n' + 
                '    <li class="row" ng-repeat="choice in choices">\n' + 
                '      <a ng-click="select(choice)" ng-class="{active: choice.index == selected}">{{choice.display}}</a>\n' +
                '    </li>\n' + 
                '  </ul>\n' + 
                '</div>\n',
            link: function(scope, iElement, iAttrs, controller) {
                scope.loading = false;
                scope.click = function (choice) {
                    console.log("click", choice);
                };
                scope.selected = 0;
                scope.update = function () {
                    if (scope.loading) {
                        return; // Allow at most one outstanding query.
                    }
                    if (scope.query == "") {
                        return;
                    }
                    scope.loading = true;
                    CodeSearch.complete(scope.query, 10, function (resp) {
                        scope.choices = []
                        if (resp.entries) {
                            for (var i = 0; i < resp.entries.length; i++) {
                                scope.choices.push({"index": i, "display": resp.entries[i]});
                            }
                        }
                        if (scope.selected >= scope.choices.length) {
                            scope.selected = 0;
                        }
                        scope.loading = false;
                    });
                }
                scope.select = function(choice) {
                    $location.url('search?query=' + choice.display);
                }
                scope.moveUp = function () {
                    if (scope.selected > 0) {
                        scope.selected--;
                    }
                }
                scope.moveDown = function () {
                    if (scope.selected + 1 < scope.choices.length) {
                        scope.selected++;
                    }
                }

                var input = angular.element(iElement.children()[0]);
                input.keydown(function (e) {
                    if (e.keyCode == 38) {
                        scope.moveUp();
                    } else if (e.keyCode == 40) {
                        scope.moveDown();
                    } else if (e.keyCode == 13) {
                        scope.select(scope.choices[scope.selected]);
                        e.preventDefault();
                    }
                    scope.$apply();
                });
                var choiceBox = angular.element(iElement.children()[1]);
                choiceBox.width(input.width());
                scope.$watch('query', scope.update);
            }
        };
    })
    .directive('zippy', function () {
        return {
            restrict: 'C',
            replace: true,
            transclude: true,
            scope: {
                title:'@zippyTitle'
            },
            template: '<div>' +
                        '<div class="title">{{title}}</div>' +
                        '<div class="body" ng-transclude></div>' +
                      '</div>',
            link: function(scope, element, attrs) {
                var title = angular.element(element.children()[0]),
                opened = true;
                title.bind('click', toggle);
                function toggle() {
                    opened = !opened;
                    element.removeClass(opened ? 'closed' : 'opened');
                    element.addClass(opened ? 'opened' : 'closed');
                }
                toggle();
            }
        };
    });
