"use strict";

angular.module("couchapp.service", [])

.service("CouchAppService", [
    "$location",
    function($location) {
        this.currentDb = function() {
            return $location.absUrl().split("://")[1].split("/")[1];
        };
        this.currentDesignDoc = function() {
            return $location.absUrl().split("://")[1].split("/")[3];
        };
    }
]);
