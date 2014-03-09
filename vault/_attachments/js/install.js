"use strict";

angular.module("vault", [ "vault.services.installer", "CornerCouch" ])

.controller("InstallCtrl", [
    "$scope", "$http", "cornercouch", "InstallService",
    function($scope, $http, cornercouch, InstallService) {
        InstallService.getReportCard(function(report) {
            $scope.report = report;
        });
    }
]);
