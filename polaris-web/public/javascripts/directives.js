'use strict';

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
        placeholder: '@placeholder'
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
                  if (hit.kind == 'TYPE') {
                    var n = hit.classType.useCount;
                    if (n > 1) {
                      hit.useCountText = n + " uses";
                    } else if (n == 1) {
                      hit.useCountText = n + " use";
                    } else {
                      hit.useCountText = "";
                    }
                  }
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
  //   <code-view code="..." on-find-usages="f(kind, id)" on-go-to-definition="g(kind, id)" />
  .directive('codeView', function($compile, Utils) {
    return {
      restrict: 'E',
      templateUrl: 'partials/code-view',
      scope: {
        onFindUsages: '&',
        onGoToDefinition: '&',
        code: '=',
        usages: '=',
        highlightedLine: '='
      },
      replace: true,
      link: function(scope, element, attrs) {
        scope.lines = [];
        scope.highlightContext = {};

        var comparePosition = function(left, right) {
          if (left.line == right.line) {
            return left.column - right.column;
          } else {
            return left.line - right.line;
          }
        }

        var processMatch = function(text, k) {
          return '<usage ' +
            'on-find-usages="onFindUsagesInternal(kind, id)" ' +
            'on-go-to-definition="onGoToDefinitionInternal(kind, id)" ' +
            'highlight-context="highlightContext" ' +
            'usage="usages[' + k + ']">' +
            Utils.escapeHTML(text) +
            '</usage>';
        }

        var processSource = function(code, usages) {
          var i = 0;
          var j = 0;
          var current = {'line': 0, 'column': 0};
          var matchFrom = -1;
          var result = '';
          while (i < code.length) {
            if (matchFrom == -1) {
              while (j < usages.length && comparePosition(usages[j].jumpTarget.span.from, current) < 0) {
                j++;
              }
            }
            if (j < usages.length && comparePosition(usages[j].jumpTarget.span.from, current) == 0) {
              matchFrom = i;
            }
            if (matchFrom != -1 && comparePosition(usages[j].jumpTarget.span.to, current) <= 0) {
              var matchText = code.substring(matchFrom, i);
              result += processMatch(matchText, j);
              usages[j].text = matchText;
              matchFrom = -1;
            }
            if (matchFrom == -1) {
              result += Utils.escapeHTML(code[i]);
            }
            if (code[i] == '\n') {
              current.line++;
              current.column = 0;
            } else {
              current.column++;
            }
            i++;
          }
          return result;
        }

        scope.$watch('code + usages', function() {
          if (scope.code && scope.usages) {
            // Bind nested directievs.
            var s = processSource(scope.code, scope.usages);
            // console.log('s', s);
            s = prettyPrintOne(s);
            var pre = angular.element(Utils.getFirst(element.find(".code-column")));
            pre.html(s);
            // console.log("compile begins");
            $compile(pre.contents())(scope);
            // console.log("compile ends");

            // Set up line numbers.
            scope.lines = [];
            var lineCount = Utils.countLines(scope.code);
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
        scope.onFindUsagesInternal = function(kind, id) {
          scope.onFindUsages({'kind': kind, 'id': id});
        }
        scope.onGoToDefinitionInternal = function(kind, id) {
          scope.onGoToDefinition({'kind': kind, 'id': id});
        }
      }
    };
  })

  // Renders a usage with context menu.
  //
  // Usage:
  //   <usage find-usages=... go-to-definition=... highlighted-context=... />
  .directive('usage', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/usage',
      scope: {
        onFindUsages: '&',
        onGoToDefinition: '&',
        highlightContext: '=',
        usage: '&'
      },
      replace: false,
      transclude: true,
      link: function(scope, element, attrs) {
        var usage = scope.usage();
        if (!usage) {
          return;
        }
        scope.kind = usage.kind;
        scope.resolved = false;
        scope.id = -1;
        scope.text = usage.text;
        if (usage.kind ==='TYPE') {
          if (usage.type.type.kind == 'CLASS'
            && usage.type.type.clazz.resolved) {
            scope.resolved = true;
            scope.id = usage.type.type.clazz.id;
            scope.url = LinkBuilder.type(scope.id);
          }
        } else if (usage.kind == 'METHOD') {
          scope.resolved = true;
          scope.id = usage.method.method.id;
          scope.url = LinkBuilder.method(scope.id);
        } else if (usage.kind == 'VARIABLE') {
          scope.resolved = true;
          scope.id = usage.variable.variable.id;
          scope.url = LinkBuilder.type(scope.id);
        } else {
          console.log('Unknown kind: ', data.kind);
        }
        scope.findUsagesInternal = function() {
          scope.onFindUsages({'kind': scope.kind, 'id': scope.id});
        };
        scope.goToDefinitionInternal = function() {
          scope.onGoToDefinition({'kind': scope.kind, 'id': scope.id});
        };
        angular.element(element, 'usage > a').hover(function() {
          scope.highlightContext = {'kind': scope.kind, 'id': scope.id};
          scope.$apply();
        });
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
              u.title = u.jumpTarget.file.project + u.jumpTarget.file.path;
            });
          }
        });
      }
    };
  })

  .directive('fileStructureTree', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/file-structure-tree',
      scope: {
        classes: '='
      },
      replace: true,
      link: function(scope) {
        scope.$watch('classes', function(value) {
          scope.classes = value;
          $.each(scope.classes, function(index, clazz) {
            clazz.simpleName = Utils.getSimpleName(clazz.handle.name);
            clazz.url = LinkBuilder.source(clazz.jumpTarget);
            if (clazz.fields) {
              $.each(clazz.fields, function(index, field) {
                field.simpleName = Utils.getDisplayNameOfFieldHandle(field.handle);
                field.url = LinkBuilder.source(field.jumpTarget);
                field.type.simpleName = Utils.getDisplayNameOfTypeHandle(field.type);
              });
            }
            if (clazz.methods) {
              $.each(clazz.methods, function(index, method) {
                method.simpleName = Utils.getDisplayNameOfMethodHandle(method.handle);
                method.url = LinkBuilder.source(method.jumpTarget);
                method.returnType.simpleName = Utils.getDisplayNameOfTypeHandle(method.returnType);
                if (method.parameters) {
                  $.each(method.parameters, function(index, parameter) {
                    parameter.type.simpleName = Utils.getDisplayNameOfTypeHandle(parameter.type);
                  });
                }
              });
            }
          });
        });
      }
    }
  })

  // Usage:
  //   <path-bar on-selected="f(project, path)" project="project" path="/path/to/file" />
  .directive('pathBar', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/path-bar',
      scope: {
        onSelected: '&',
        project: '=',
        path: '='
      },
      link: function(scope, element, attrs) {
        scope.$watch('project + path', function() {
          var project = scope.project;
          var path = scope.path;
          if (!project || !path) {
            return;
          }
          if (!Utils.startsWith(path, '/')) {
            console.log('Path must start with "/"', path);
            return;
          }
          path = '/' + project + path;
          scope.parts = [];
          for (var i = project.length + 1, j = 0; j != -1; j = i, i = path.indexOf('/', j + 1)) {
            if (i == -1) {
              // last
              scope.parts.push({'name': path.substring(j + 1), 'active': true});
            } else {
              scope.parts.push({
                'name': path.substring(j + 1, i),
                'url': LinkBuilder.file(project, path.substring(0, i + 1))
              });
              scope.parts.push({'name': '/', 'divider': true});
            }
          }
        });
      }
    };
  });

