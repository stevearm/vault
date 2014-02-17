"use strict";

angular.module("vault", [
    "ngRoute",
    "ui.bootstrap",
    "vault.controllers",
    "vault.directives"
])

.factory("UnauthenticatedInterceptor", [
    "$q", "$location",
    function($q, $location) {
        return {
            responseError: function(rejection) {
                if (rejection.status === 401) {
                    $location.path( "/login" );
                    return;
                }
                return $q.reject(rejection);
            }
        };
    }
])

.config([
    "$routeProvider", "$httpProvider",
    function($routeProvider, $httpProvider) {
        $routeProvider
        .when("/", {
            templateUrl:    "partials/home.html",
            controller:     "HomeCtrl",
            resolve: {
                Vault: ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/login", {
            templateUrl:    "partials/login.html",
            controller:     "LoginCtrl"
        })
        .otherwise({redirectTo: "/"});

        $httpProvider.interceptors.push("UnauthenticatedInterceptor");
    }
]);
