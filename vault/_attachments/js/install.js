"use strict";

angular.module("vault", [ "vault.services.installer", "CornerCouch" ])

.controller("InstallCtrl", [
    "$scope", "$http", "cornercouch", "InstallService", "CouchAppService",
    function($scope, $http, cornercouch, InstallService, CouchAppService) {

        var couchServer = cornercouch();

        $scope.refresh = function() {
            $scope.report = null;
            setFix();
            InstallService.getReportCard(function(report) {
                $scope.report = report;
                if (!report.pass) {
                    for (var i = 0; i < report.parts.length; i++) {
                        if (!report.parts[i].pass) {
                            setFix(report.parts[i]);
                            break;
                        }
                    }
                }
            });
        };

        var setFix = function(part) {
            if (typeof(part) === "undefined") {
                $scope.fix = null;
                return;
            }
            $scope.fix = {
                key: part.key,
                run: function() { alert("Not yet implemented"); }
            };

            switch($scope.fix.key) {
                case "idDoc":
                    $scope.fix.vaultDbName = part.baggage.vaultDbName;
                    if ("vaultId" in part.baggage) {
                        $scope.fix.vaultId = part.baggage.vaultId;
                    } else {
                        $http.get("/").success(function(data) {
                            if (data.uuid) {
                                $scope.fix.vaultId = data.uuid;
                            } else {
                                $http.get("/_uuids").success(function(data) {
                                    $scope.fix.vaultId = data.uuids[0];
                                });
                            }
                        });
                    }
                    $http.get("/_all_dbs").success(function(data) {
                        $scope.fix.dbNames = data.filter(function(e) {
                            return e.charAt(0) != "_";
                        });
                    });
                    $scope.fix.run = function() {
                        InstallService.fixIdDoc($scope.fix.vaultId, $scope.fix.vaultDbName, function() {
                            $scope.refresh();
                        });
                    };
                    break;
                case "vaultDbSec":
                    $scope.fix.run = function() {
                        InstallService.fixDbMembers(part.baggage.vaultDbName, function() {
                            $scope.refresh();
                        });
                    };
                    break;
                case "vaultDbVault":
                    $scope.fix.signature = null;
                    $http.get("/").success(function(data) {
                        $scope.fix.signature = data;
                    });
                    $scope.fix.run = function() {
                        InstallService.fixVaultDbEntry(part.baggage.vaultDbName, part.baggage.vaultId, $scope.fix.signature, function() {
                            $scope.refresh();
                        });
                    };
                    break;
            }
        };

        $scope.refresh();
    }
]);
