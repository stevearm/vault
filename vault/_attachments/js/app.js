"use strict";

angular.module("vault", [
    "ngRoute",
    "ui.bootstrap",
    "monospaced.qrcode",
    "vault.controllers",
    "vault.directives"
])

.factory("UnauthenticatedInterceptor", [
    "$q", "$location",
    function($q, $location) {
        return {
            responseError: function(rejection) {
                if (rejection.status === 401) {
                    $location.path("/login");
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
                CurrentVault:   ["CurrentVault", function(CurrentVault) { return CurrentVault; }],
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/apps", {
            templateUrl:    "partials/appsList.html",
            controller:     "AppListCtrl",
            resolve: {
                CurrentVault:   ["CurrentVault", function(CurrentVault) { return CurrentVault; }]
            }
        })
        .when("/vaults", {
            templateUrl:    "partials/vaultList.html",
            controller:     "VaultListCtrl",
            resolve: {
                CurrentVault:   ["CurrentVault", function(CurrentVault) { return CurrentVault; }],
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/databases", {
            templateUrl:    "partials/dbList.html",
            controller:     "DatabaseListCtrl",
            resolve: {
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/barcodes", {
            templateUrl:    "partials/barcodes.html",
            controller:     "BarcodeListCtrl",
            resolve: {
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/sync", {
            templateUrl:    "partials/sync.html",
            controller:     "SyncCtrl",
            resolve: {
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/fixer", {
            templateUrl:    "partials/fixer.html",
            controller:     "FixerCtrl"
        })
        .when("/conflicts", {
            templateUrl:    "partials/conflicts.html",
            controller:     "ConflictsCtrl"
        })
        .when("/vault/:id?", {
            templateUrl:    "partials/vault.html",
            controller:     "VaultCtrl",
            resolve: {
                CurrentVault:   ["CurrentVault", function(CurrentVault) { return CurrentVault; }],
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .when("/login", {
            templateUrl:    "partials/login.html",
            controller:     "LoginCtrl"
        })
        .when("/about", {
            templateUrl:    "partials/about.html",
            controller:     "AboutCtrl",
            resolve: {
                CurrentVault:   ["CurrentVault", function(CurrentVault) { return CurrentVault; }],
                Vault:          ["Vault", function(Vault) { return Vault; }]
            }
        })
        .otherwise({redirectTo: "/"});

        $httpProvider.interceptors.push("UnauthenticatedInterceptor");
    }
]);
