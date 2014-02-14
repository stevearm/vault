"use strict";

angular.module("vault", [
    "ngRoute",
    "ngResource",
    "ui.bootstrap",
    "vault.controllers",
    "vault.directives",
    "vault.factories",
    "vault.services"
])

.config([
    "$routeProvider", "$httpProvider",
    function($routeProvider, $httpProvider) {
        $routeProvider
        .when("/", {
            templateUrl:    "partials/home.html",
            controller:     "HomeCtrl"
        })
        .when("/login", {
            templateUrl:    "partials/login.html",
            controller:     "LoginCtrl"
        })
        .otherwise({redirectTo: "/"});

        $httpProvider.interceptors.push("UnauthenticatedInterceptor");
    }
]);
