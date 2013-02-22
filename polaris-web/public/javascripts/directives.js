angular.module('polarisDirectives', ['polarisServices'])
  // Search box with auto completion
  //
  // Usage:
  //   <search-box placeholder="..." />
  .directive('searchBox', function ($location, CodeSearch, LinkBuilder) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        placeholder: '@placeholder',
      },
      templateUrl: 'partials/search-box',
      link: function(scope, iElement, iAttrs, controller) {
        scope.loading = false;
        scope.visible = true;
        scope.click = function (choice) {
          console.log("click", choice);
        };
        scope.selected = -1;
        scope.update = function () {
          if (scope.loading) {
            return; // Allow at most one outstanding query.
          }
          if (!scope.query || scope.query == "") {
            return;
          }
          scope.loading = true;
          CodeSearch.complete(scope.query, 8, function (resp) {
              if (resp.hits) {
                $.each(resp.hits, function(index, hit) {
                  hit["index"] = index;
                  hit["url"] = LinkBuilder.source(hit.jumpTarget);
                  }
                  );
                scope.choices = resp.hits;
                if (scope.selected >= scope.choices.length) {
                  scope.selected = 0;
                }
              } else {
                scope.choices = [];
                scope.selected = 0;
              }
            scope.loading = false;
          });
        }
        scope.search = function() {
          if (scope.visible && 0 <= scope.selected && scope.selected < scope.choices.length) {
            var choice = scope.choices[scope.selected];
            $location.url(choice.url);
          } else {
            $location.url('search?query=' + scope.query);
          }
        }
        scope.moveUp = function () {
          if (scope.selected >= 0) { // Allow -1
            scope.selected--;
          }
        }
        scope.moveDown = function () {
          if (scope.selected + 1 < scope.choices.length) {
            scope.selected++;
          }
        }
        scope.blur = function () {
          scope.visible = false;
        }
        scope.focus = function() {
          scope.visible = true;
        }

        var input = angular.element(iElement.children()[0]);
        input.keydown(function (e) {
          if (e.keyCode == 38) {
            scope.moveUp();
          } else if (e.keyCode == 40) {
            scope.moveDown();
          } else if (e.keyCode == 13) {
            scope.search();
            e.preventDefault();
          }
          scope.$apply();
        });
        input.blur(function (e) { scope.blur(); scope.$apply(); });
        input.focus(function (e) { scope.focus(); scope.$apply(); });
        var choiceBox = angular.element(iElement.children()[1]);
        // choiceBox.width(input.width());
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
  })

  // Asynchounous project tree
  //
  // Usage:
  //   <project-tree path="..." />
  .directive('projectTree', function ($location, CodeSearch, Utils, LinkBuilder) {
    return {
      restrict: 'E',
      scope: true,
      replace: true,
      transclude: true,
      templateUrl: 'partials/project-tree',
      link: function(scope, element, attrs) {
        scope.loading = true;
        var project;
        var path;
        var update = function() {
          if (!project || !path) {
            return;
          }
          populate(0, angular.element(element.children()[1]), function() {
            scope.loading = false;
          });
        }
        scope.$watch(attrs.project, function(value) {
          project = value;
          update();
        });
        scope.$watch(attrs.path, function(value) {
          path = value;
          update();
        });
        var populate = function(start, element, callback) {
          // console.log("populate", "start=", start, "path=", path);
          var slash = path.indexOf('/', start);
          if (slash == -1) {
            callback();
            return;
          }
          var count = 0;
          CodeSearch.listFiles(project, path.substring(0, slash + 1), function(resp) {
            element.append("<ul></ul>");
            var ul = angular.element(Utils.getLast(element.children()));
            if (resp.directories) {
              for (var i = 0; i < resp.directories.length; i++){
                var dir = resp.directories[i];
                ul.append("<li class='dir'>" + Utils.getBaseName(dir) + "</li>");
                if (Utils.startsWith(path, dir)) {
                  count++;
                  populate(slash + 1, angular.element(Utils.getLast(ul.children())), function(){
                    count--;
                    if (count == 0) {
                      callback();
                    }
                  });
                }
              }
            }
            if (resp.files) {
              for (var i = 0; i < resp.files.length; i++) {
                var file = resp.files[i];
                ul.append("<li class='java-file'><a href=#" + LinkBuilder.sourceFromFileId(file.id) + ">" +
                    Utils.getBaseName(resp.files[i].path) + "</a></li>");
              }
            }
            if (count == 0) {
              callback();
            }
          });
        }
      }
    };
  })

  // Renders source code with cross-reference support.
  //
  // Usage:
  //   <code-view code="..." find-usages="f(typeId)" go-to-definition="g(typeId)" />
  .directive('codeView', function($compile, Utils) {
    return {
      restrict: 'E',
      templateUrl: 'partials/code-view',
      scope: {
        findTypeUsages: '&',
        goToTypeDefinition: '&',
        findMethodUsages: '&',
        goToMethodDefinition: '&',
        code: '=',
        highlightedLine: '=',
      },
      replace: true,
      link: function(scope, element, attrs) {
        scope.lines = [];
        scope.$watch('code', function(value) {
          if (value) {
            // Bind nested directievs.
            value = value
              .replace(
                /<type-usage /g,
                '<type-usage find-usages="findTypeUsagesInternal(typeId)" ' +
                'go-to-definition="goToTypeDefinitionInternal(typeId)" ')
              .replace(
                /<method-usage /g,
                '<method-usage find-usages="findMethodUsagesInternal(methodId)" ' +
                'go-to-definition="goToMethodDefinitionInternal(methodId)" ')
              .replace("<source>", "")
              .replace("</source>", "");
            value = prettyPrintOne(value);
            var pre = angular.element(Utils.getFirst(element.find(".code-column")));
            pre.html(value);
            $compile(pre.contents())(scope);

            // Set up line numbers.
            scope.lines = [];
            var lineCount = Utils.countLines(value);
            for (var i = 0; i < lineCount; i++) {
              scope.lines.push(i);
            }

            // Scroll to highlighted line.
            setTimeout(function() {
              var l = scope.highlightedLine;
              if (l > 3) { l -= 3; }
              element.scrollTop(l * 20);
            });
          }
        });
        scope.findTypeUsagesInternal = function(typeId) {
          scope.findTypeUsages({'typeId': typeId});
        }
        scope.goToTypeDefinitionInternal = function(typeId) {
          scope.goToTypeDefinition({'typeId': typeId});
        }
        scope.findMethodUsagesInternal = function(typeId) {
          scope.findMethodUsages({'methodId': typeId});
        }
        scope.goToMethodDefinitionInternal = function(methodId) {
          scope.goToMethodDefinition({'methodId': methodId});
        }
      }
    };
  })

  // Renders a type usage with context menu.
  .directive('typeUsage', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/type-usage',
      scope: {
        findUsages: '&',
        goToDefinition: '&',
      },
      replace: false,
      transclude: true,
      link: function(scope, element, attrs) {
        var typeId = parseInt(attrs.typeId);
        scope.resolved = Utils.str2bool(attrs.resolved);
        scope.classUrl = LinkBuilder.type(typeId);
        scope.findUsagesInternal = function() {
          scope.findUsages({'typeId': typeId});
        };
        scope.goToDefinitionInternal = function() {
          scope.goToDefinition({'typeId': typeId});
        };
      }
    };
  })

  // Renders a method usage with context menu.
  .directive('methodUsage', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/method-usage',
      scope: {
        findUsages: '&',
        goToDefinition: '&',
      },
      replace: false,
      transclude: true,
      link: function(scope, element, attrs) {
        var methodId = parseInt(attrs.methodId);
        scope.methodUrl = LinkBuilder.method(methodId);
        scope.findUsagesInternal = function() {
          scope.findUsages({'methodId': methodId});
        };
        scope.goToDefinitionInternal = function() {
          scope.goToDefinition({'methodId': methodId});
        };
      }
    }
  })

  // Shows type usages
  // 
  // Usage:
  //   <xref-box xrefs="..." />
  .directive('xrefBox', function(CodeSearch, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/xref-box',
      scope: {
        xrefs: '='
      },
      replace: true,
      link: function(scope) {
        scope.$watch('xrefs', function(value) {
          if (value) {
            scope.usages = value;
            $.each(scope.usages, function(i, u) {
              u.url = LinkBuilder.source(u.jumpTarget);
              u.text = u.jumpTarget.file.project + u.jumpTarget.file.path;
            });
          }
        });
      }
    };
  });

