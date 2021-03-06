'use strict';

angular.module('polarisDirectives', ['polarisServices'])

  .directive('eatClick', function() {
    return function(scope, element, attrs) {
      $(element).click(function(event) { event.preventDefault(); });
    };
  })

  // Search box with auto completion
  //
  // Usage:
  //   <search-box placeholder="..." />
  .directive('searchBox', function ($location, CodeSearch, LinkBuilder, Protos) {
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
              $.each(scope.choices, function(i, choice) {
                choice.icon = null;
                if (choice.kind == Protos.Hit.Kind.FILE) {
                  choice.icon = 'file';
                } else if (choice.kind == Protos.Hit.Kind.TYPE) {
                  if (choice.classType.kind == Protos.ClassType.Kind.CLASS) {
                    choice.icon = 'clazz';
                  } else if (choice.classType.kind = Protos.ClassType.Kind.INTERFACE) {
                    choice.icon = 'interface';
                  } else if (choice.classType.kind = Protos.ClassType.Kind.ENUM) {
                    choice.icon = 'enum';
                  } else if (choice.classType.kind = Protos.ClassType.Kind.ANNOTATION) {
                    choice.icon = 'annotation';
                  } else {
                    console.log('Ignore ClassType of unkonwn kind:', choice.classType);
                  }
                } else {
                  console.warn('Ignore Hit of unknown kind:', choice);
                }
              });
              console.log('Got ' + scope.choices.length + ' candidates for ' + scope.query);
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
  //   <project-tree project='...' pathsToExpand='["path1", "path2"]' onSelectFile='f(fileId)' />
  .directive('projectTree', function ($location, $timeout, CodeSearch, Utils, LinkBuilder, Protos) {
    return {
      restrict: 'E',
      scope: {
        project: '=',
        pathsToExpand: '=',
        onSelectFile: '&',
        currentFile: '&' // optional FileHandle
      },
      replace: true,
      templateUrl: 'partials/project-tree',
      link: function(scope, element, attrs) {
        scope.loading = false;
        var highlightFile = function(fileId) {
          element.find('.current').removeClass('current');
          element.find('li [file-id="' + fileId + '"]').addClass('current');
        }
        scope.$watch('project + pathsToExpand + currentFile()', function() {
          if (!scope.project || !scope.pathsToExpand) {
            return;
          }
          var root = element.find('.root');
          $.each(scope.pathsToExpand, function(i, e) {
            expand(e, root);
          });
          element.find('.current').removeClass('current');
          if (scope.currentFile()) {
            highlightFile(scope.currentFile().id);
          }
        });
        scope.$watch('currentFile()', function() {
          var currentFile = scope.currentFile();
          if (!currentFile) {
            return;
          }
          highlightFile(currentFile.id);
        });
        var expand = function(path, node) {
          doExpand(path, node, 0);
        };
        var doExpand = function(path, node, p) {
          expandSingleLevel(path.substring(0, p + 1), node, function() {
            var q = path.indexOf('/', p + 1);
            if (q == -1) {
              return;
            }
            var part = path.substring(p + 1, q);
            var matches = node.find('ul > *').filter(function(i, child) {
              var childName = $(child).find('> a').text();
              return childName == part;
            });
            if (matches.length > 1) {
              console.log('Multiple matches: ' + matches, path);
              return;
            }
            if (matches.length == 0) {
              console.log('No match: ' + path);
              return;
            }
            doExpand(path, $(matches[0]), q);
          });
        };
        var expandSingleLevel = function(path, node, callback) {
          if (node.hasClass('expanded')) {
            return;
          }
          node.removeClass('collapsed').addClass('expanded');
          if (!node.hasClass('unknown')) {
            node.find('> ul > *').show();
            return;
          }
          var ul = node.find('> ul');
          CodeSearch.listFiles(scope.project, path, function(resp) {
            node.removeClass('unknown');
            if (resp.children) {
              $.each(resp.children, function(i, child) {
                var li = null;
                if (child.kind == Protos.FileHandle.Kind.DIRECTORY) {
                  ul.append('<li><a href="#">' + Utils.getBaseName(child.path) + '</a><ul></ul></li>');
                  li = ul.find('> li:last');
                  li.addClass('dir');
                  li.addClass('collapsed');
                  li.addClass('unknown');
                  li.find('> a').click(function () {
                    if (li.hasClass('expanded')) {
                      collapseSingleLevel(child.path, li);
                    } else {
                      expandSingleLevel(child.path, li, function() {});
                    }
                    scope.$apply();
                    return false;
                  });
                } else if (child.kind == Protos.FileHandle.Kind.NORMAL_FILE) {
                  ul.append("<li><a href=" + LinkBuilder.sourceFromHandle(child) + ">" +
                    Utils.getBaseName(child.path) + "</a></li>");
                  li = ul.find('> li:last');
                  li.addClass('normal-file');
                  li.find('> a').click(function () {
                    scope.onSelectFile({'file': child});
                    scope.$apply();
                  })
                } else {
                  console.warn('Ignore unknown FileHandle.Kind:', child.kind);
                  return;
                }
                li.attr('file-id', child.id);
              });
            }
            callback();
          });
        }
        var collapseSingleLevel = function(path, node) {
          node.removeClass('expanded').addClass('collapsed');
          node.find('> ul > *').hide();
        }
      }
    };
  })

  .directive('codeViewLoader', function(CodeSearch, Protos) {
    return {
      restrict: 'E',
      templateUrl: 'partials/code-view-loader',
      scope: {
        onFindUsages: '&',
        onSelectJumpTarget: '&',
        file: '&',
        highlightedLine: '&'
      },
      replace: true,
      link: function(scope, element, attrs) {
        scope.loading = true;
        scope.$watch('file()', function() {
          var file = scope.file();
          if (!file) {
            return;
          }
          if (file.kind != Protos.FileHandle.Kind.NORMAL_FILE) {
            return;
          }
          console.log('fileIdLoaded', scope.fileIdLoaded);
          console.log('file.id', file.id);
          if (scope.fileIdLoaded && file.id.equals(scope.fileIdLoaded)) {
            console.log('File #' + file.id + ' is already loaded.');
            return;
          }
          scope.loading = true;
          CodeSearch.readSourceById(file.id, function(resp) {
            scope.sourceCode = resp.source.source;
            scope.usages = resp.usages;
            scope.loading = false;
            scope.fileIdLoaded = file.id;
          });
        });
        scope.onFindUsagesInternal = function(kind, id) {
          scope.onFindUsages({'kind': kind, 'id': id});
        }
        scope.onSelectJumpTargetInternal = function(jumpTarget) {
          scope.onSelectJumpTarget({'jumpTarget': jumpTarget});
        }
      }
    };
  })

  // Renders source code with cross-reference support.
  //
  // Usage:
  //   <code-view code="..."
  //     on-find-usages="f(kind, id)"
  //     on-go-to-definition="g(kind, id)"
  //     on-select-jump-target="h(jumpTarget)" />
  .directive('codeView', function($compile, Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/code-view',
      scope: {
        onFindUsages: '&',
        onSelectJumpTarget: '&',
        code: '&',
        usages: '&',
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

        var processMatch = function(text, k, usage) {
          return '<span class="usage dropdown" index="' + k + '" highlight-group="' + Utils.getEntityIdOfUsage(usage) + '">' +
              '<a href="' + LinkBuilder.source(usage.jumpTarget) + '">' +
                Utils.escapeHTML(text) +
              '</a>' + 
              '<span>' +
                '<a class="dropdown-toggle" data-toggle="dropdown" href="#"><b class="caret"></b></a>' +
                '<ul class="dropdown-menu" role="menu">' +
                  '<li><a class="go-to-definition-button">Definition</a></li>' +
                  '<li><a class="find-usages-button">Find usages</a></li>' +
                '</ul>' +
              '</span>' + 
              // '<a href="#"><b class="usage-dropdown-menu-button"></b></a>' +
            '</span>';
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
              result += processMatch(matchText, j, usages[j]);
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

        var scrollToLine = function (line) {
          console.log('Scroll to line ' + line);
          if (line > 3) { line -= 3; }
          $(element).scrollTop(line * 20);
        };

        scope.$watch('code() + usages()', function() {
          var code = scope.code();
          var usages = scope.usages();
          if (code && usages) {
            console.time('processSource');
            // Bind nested directievs.
            var s = processSource(code, usages);
            console.timeEnd('processSource');
            console.time('prettyPrintOne');
            s = prettyPrintOne(s);
            console.timeEnd('prettyPrintOne');
            console.time('pre.html');
            var pre = $(".code-column");
            pre.html(s);
            console.timeEnd('pre.html');

            // Set up line numbers.
            console.time('setUpLineNumbers');
            scope.lines = [];
            var lineCount = Utils.countLines(code);
            for (var i = 0; i < lineCount; i++) {
              scope.lines.push(i);
            }
            console.timeEnd('setUpLineNumbers');

            console.time('scrollToLine');
            if (scope.highlightedLine) {
              scrollToLine(scope.highlightedLine);
            }
            console.timeEnd('scrollToLine');

            // Set up menu click handlers
            console.time('setUpMenuHandlers');
            $(element).find('.usage').each(function(i, e) {
              var j = $(e).attr('index');
              var usage = usages[j];
              var goToDefinitionFn = function(e) {
                scope.onSelectJumpTarget({'jumpTarget': usage.definitionJumpTarget});
                scope.$apply();
              };
              var a = $(e).find('> a');
              $(a).click(goToDefinitionFn);
              $(a).hover(function() {
                $(element).find('.usage > a').removeClass('highlighted-word');
                $(element).find('.usage[highlight-group=' + Utils.getEntityIdOfUsage(usage) + '] > a').addClass('highlighted-word');
              });
              $(e).find('.go-to-definition-button').click(goToDefinitionFn);
              $(e).find('.find-usages-button').click(function(e) {
                scope.onFindUsages({'kind': usage.kind, 'id': Utils.getEntityIdOfUsage(usage)});
                scope.$apply();
              });
            });
            console.timeEnd('setUpMenuHandlers');
          }
        });
        scope.$watch('highlightedLine', function() {
          if (!scope.highlightedLine) {
            return;
          }
          scrollToLine(scope.highlightedLine);
        });
        scope.onFindUsagesInternal = function(kind, id) {
          scope.onFindUsages({'kind': kind, 'id': id});
        }
        scope.onSelectJumpTargetInternal = function(jumpTarget) {
          scope.onSelectJumpTarget({'jumpTarget': jumpTarget});
        }
      }
    };
  })

  .directive('dirViewLoader', function(CodeSearch) {
    return {
      restrict: 'E',
      templateUrl: 'partials/dir-view-loader',
      scope: {
        project: '&',
        path: '&',
        onSelectFile: '&'
      },
      link: function($scope, $element, $attrs) {
        $scope.$watch('project() + path()', function() {
          var project = $scope.project();
          var path = $scope.path();
          if (!project || !path) {
            return;
          }
          CodeSearch.listFiles(project, path, function(resp) {
            $scope.children = resp.children ? resp.children : [];
          });
        });
      }
    };
  })

  .directive('dirView', function(LinkBuilder, Utils) {
    return {
      restrict: 'E',
      templateUrl: 'partials/dir-view',
      scope: {
        project: '&',
        children: '&',
        onSelectFile: '&'
      },
      link: function($scope, $element, $attrs) {
        $scope.$watch('project() + children()', function() {
          var project = $scope.project();
          var children = $scope.children();
          if (!project || !children) {
            return;
          }
          $scope.children_ = [];
          $.each(children, function(i, child) {
            $scope.children_.push({
              'kind': child.kind,
              'name': Utils.getBaseName(child.path),
              'url': LinkBuilder.sourceFromHandle(child),
              'handle': child
            });
          });
        });
      }
    };
  })

  // Renders a usage with context menu.
  //
  // Usage:
  //   <usage find-usages=... go-to-definition=... highlighted-context=... />
  /*.directive('usage', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/usage',
      scope: {
        onFindUsages: '&',
        onGoToDefinition: '&',
        onSelectJumpTarget: '&',
        highlightContext: '=',
        usage: '&'
      },
      replace: false,
      link: function(scope, element, attrs) {
        var usage = scope.usage();
        if (!usage) {
          return;
        }
        scope.kind = usage.kind;
        scope.id = -1;
        scope.text = usage.text;
        if (usage.definitionJumpTarget) {
          scope.resolved = true;
          scope.url = LinkBuilder.source(usage.definitionJumpTarget);
        } else {
          scope.resolved = false;
        }
        if (usage.kind ==='TYPE') {
          if (usage.type.type.kind == 'CLASS'
            && usage.type.type.clazz.resolved) {
            scope.id = usage.type.type.clazz.id;
          }
        } else if (usage.kind == 'METHOD') {
          scope.id = usage.method.method.id;
        } else if (usage.kind == 'VARIABLE') {
          scope.id = usage.variable.variable.id;
        } else {
          console.log('Unknown kind: ', data.kind);
        }
        scope.onFindUsagesInternal = function() {
          scope.onFindUsages({'kind': scope.kind, 'id': scope.id});
        };
        scope.onGoToDefinitionInternal = function() {
          scope.onSelectJumpTarget({'jumpTarget': usage.definitionJumpTarget});
        };
        $(element).find('.usage > span > a').hover(function() {
          scope.highlightContext = {'kind': scope.kind, 'id': scope.id};
          scope.$apply();
        });
      }
    }
  })*/

  // Shows type usages
  // 
  // Usage:
  //   <xref-box xrefs="..." on-select-jump-target='...' />
  .directive('xrefBox', function(CodeSearch, LinkBuilder, Protos) {
    return {
      restrict: 'E',
      templateUrl: 'partials/xref-box',
      scope: {
        xrefs: '=',
        onSelectJumpTarget: '&'
      },
      replace: true,
      link: function(scope) {
        scope.usageCount = 0;
        scope.categories = [
          {name: 'Declaration', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.TYPE_DECLARATION},
          {name: 'Extends/implements', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.SUPER_CLASS},
          {name: 'Methods', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.METHOD_SIGNATURE},
          {name: 'Fields', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.FIELD},
          {name: 'Local variables', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.LOCAL_VARIABLE},
          {name: 'Generic types', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.GENERIC_TYPE_PARAMETER},
          {name: 'Imports', kind: Protos.Usage.Kind.TYPE, subkind: Protos.TypeUsage.Kind.IMPORT},
          {name: 'Declaration', kind: Protos.Usage.Kind.METHOD, subkind: Protos.MethodUsage.Kind.METHOD_DECLARATION},
          {name: 'New instance creation', kind: Protos.Usage.Kind.METHOD, subkind: Protos.MethodUsage.Kind.INSTANCE_CREATION},
          {name: 'Method calls', kind: Protos.Usage.Kind.METHOD, subkind: Protos.MethodUsage.Kind.METHOD_CALL},
          {name: 'Declaration', kind: Protos.Usage.Kind.VARIABLE, subkind: Protos.VariableUsage.Kind.DECLARATION},
          {name: 'Variable access', kind: Protos.Usage.Kind.VARIABLE, subkind: Protos.VariableUsage.Kind.ACCESS}
        ];
        var getUsageSubkind = function(u) {
          if (u.kind == Protos.Usage.Kind.TYPE) {
            return u.type.kind;
          } else if (u.kind == Protos.Usage.Kind.METHOD) {
            return u.method.kind;
          } else if (u.kind == Protos.Usage.Kind.VARIABLE) {
            return u.variable.kind;
          } else {
            console.log('Bad Usage.Kind:', u.kind);
            return null;
          }
        }
        scope.$watch('xrefs', function(value) {
          if (value) {
            var usages = value;
            scope.usages = usages;
            scope.usageCount = value.length;
            $.each(scope.categories, function(i, c) {
              c.usages = [];
            });
            $.each(usages, function(i, u) {
              u.title = u.jumpTarget.file.project + u.jumpTarget.file.path;
              u.url = LinkBuilder.source(u.jumpTarget);
              var found = false;
              $.each(scope.categories, function(j, c) {
                if (u.kind == c.kind && getUsageSubkind(u) == c.subkind) {
                  c.usages.push(u);
                  found = true;
                }
              });
              if (!found) {
                console.warn('Ignore usage of unknown kind:', u);
              }
            });
          }
        });
        scope.onSelectUsage = function(usage) {
          scope.onSelectJumpTarget({'jumpTarget': usage.jumpTarget});
        }
      }
    };
  })

  // Usage:
  //   <class-tree-loader fileId='100' />
  .directive('classTreeLoader', function(CodeSearch) {
    return {
      restrict: 'E',
      templateUrl: 'partials/class-tree-loader',
      scope: {
        fileId: '=',
        onSelectJumpTarget: '&'
      },
      replace: true,
      link: function(scope) {
        scope.loading = true;
        scope.classes = [];
        scope.$watch('fileId', function() {
          if (!scope.fileId) {
            return;
          }
          if (scope.fileIdLoaded == scope.fileId) {
            return;
          }
          scope.loading = true;
          CodeSearch.listTypesInFile(scope.fileId, function(resp) {
            scope.classes = resp.classTypes;
            scope.fileIdLoaded = scope.fileId;
            scope.loading = false;
          });
        });
        scope.onSelectJumpTargetInternal = function(jumpTarget) {
          scope.onSelectJumpTarget({'jumpTarget': jumpTarget});
        }
      }
    };
  })

  .directive('classTree', function(Utils, LinkBuilder) {
    return {
      restrict: 'E',
      templateUrl: 'partials/class-tree',
      scope: {
        classes: '=',
        onSelectJumpTarget: '&'
      },
      replace: true,
      link: function(scope) {
        scope.$watch('classes', function() {
          if (!scope.classes) { return; }
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
        scope.onSelectJumpTargetInternal = function(jumpTarget) {
          scope.onSelectJumpTarget({'jumpTarget': jumpTarget});
        }
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
        onSelectFile: '&',
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
              scope.parts.push({
                'name': path.substring(j + 1),
                'path': path,
                'active': true
              });
            } else {
              var prefix = path.substring(project.length + 1, i + 1);
              scope.parts.push({
                'name': path.substring(j + 1, i),
                'path': prefix,
                'url': LinkBuilder.sourceFromProjectAndPath(project, prefix)
              });
              scope.parts.push({'name': '/', 'divider': true});
            }
          }
        });
        scope.onSelectFileInternal = function(path) {
          var fileHandle = {
            'kind': 'DIRECTORY',
            'project': scope.project,
            'path': path
          };
          scope.onSelectFile({'file': fileHandle});
        }
      }
    };
  })

  // Usage:
  //   <collapsible-panel title=... attach-to-boundary='LEFT/RIGHT' visible=...>
  //     some content
  //   </colappsible-panel>
  .directive('collapsiblePanel', function() {
    return {
      restrict: 'E',
      templateUrl: 'partials/collapsible-panel',
      scope: {
        'title': '@',
        'attach-to-boundary': '@',
        'visible': '='
      },
      transclude: true,
      link: function(scope, element, attr) {
        scope.title = attr.title;
        scope.attachToLeft = (attr.attachToBoundary == 'LEFT');
        scope.attachToRight = (attr.attachToBoundary == 'RIGHT');
        scope.onShowOrHide = function() {
          scope.visible = !scope.visible;
        }
      }
    };
  });

