"use strict";

angular.module("vault.directives", [])

.directive('json', function() {
    return {
        restrict: 'A', // only activate on element attribute
        require: 'ngModel',
        link: function(scope, element, attrs, ngModelCtrl) {
            ngModelCtrl.$parsers.push(function(text) {
                try {
                    var obj = angular.fromJson(text);
                    ngModelCtrl.$setValidity('json', true);
                    return obj;
                } catch (e) {
                    ngModelCtrl.$setValidity('json', false);
                    return null;
                }
            });

            var toUser = function(object) {
                if (!object) { return ""; }
                return angular.toJson(object, true);
            }
            ngModelCtrl.$formatters.push(toUser);

            // $watch(attrs.ngModel) wouldn't work if this directive created a new scope;
            // see http://stackoverflow.com/questions/14693052/watch-ngmodel-from-inside-directive-using-isolate-scope how to do it then
            scope.$watch(attrs.ngModel, function(newValue, oldValue) {
                if (newValue != oldValue) {
                    ngModelCtrl.$setViewValue(toUser(newValue));
                    // TODO avoid this causing the focus of the input to be lost..
                    ngModelCtrl.$render();
                }
            }, true); // MUST use objectEquality (true) here, for some reason..
        }
    };
})

.directive('activeTab', function ($location) {
    /* from http://stackoverflow.com/a/17496112/28038 */
    return {
        link: function postLink(scope, element, attrs) {
            scope.$on("$routeChangeSuccess", function (event, current, previous) {
                // designed for full re-usability at any path, any level, by using data from attrs
                // declare like this: <li class="nav_tab"><a href="#/home" active-tab="1">HOME</a></li>

                // this var grabs the tab-level off the attribute, or defaults to 1
                var pathLevel = attrs.activeTab || 1,
                // this var finds what the path is at the level specified
                pathToCheck = $location.path().split('/')[pathLevel],
                // this var finds grabs the same level of the href attribute
                tabLink = attrs.href.split('/')[pathLevel];
                // now compare the two:
                if (pathToCheck === tabLink) {
                  element.parent().addClass("active");
                }
                else {
                  element.parent().removeClass("active");
                }
            });
        }
    };
});
