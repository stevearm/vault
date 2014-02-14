"use strict";

angular.module("vault.services", [])

.service("CouchService", [
    "$http",
    function($http) {
        this.currentDb = function() {
            return document.location.pathname.split("/")[1];
        };
        this.currentDesignDoc = function() {
            return document.location.pathname.split("/")[3];
        };
        this.viewUrl = function(that) {
            return function(viewName, designDocId) {
                if (!viewName) {
                    throw "Must specify a view name";
                }
                if (!designDocId) {
                    designDocId = that.currentDesignDoc();
                }
                return "/" + that.currentDb() + "/_design/" + designDocId + "/_view/" + viewName;
            };
        }(this);
        this.listUrl = function(that) {
            return function(listName, viewName, designDocId) {
                if (!listName || !viewName) {
                    throw "Must specify list and view names";
                }
                if (!designDocId) {
                    designDocId = that.currentDesignDoc();
                }
                return "/" + that.currentDb() + "/_design/" + designDocId + "/_list/" + listName + "/" + viewName;
            };
        }(this);

        this.attachmentUrl = function(that) {
            return function(docId, attachmentName) {
                if (!docId || !attachmentName) {
                    throw "Must specify a doc id and attachment name";
                }
                return "/" + that.currentDb() + "/" + docId + "/" + attachmentName;
            };
        }(this);

        this.login = function(usr, pwd, callback) {
            if (!callback) { callback = function(){}; }
            $http({
                method:     "POST",
                url:        "/_session",
                headers:    { "Content-Type": "application/x-www-form-urlencoded" },
                data:       "name=" + encodeURIComponent(usr) + "&password=" + encodeURIComponent(pwd).replace(/%20/g, "+")
            })
            .success(function(data) {
                callback(data.roles);
            });
        };

        this.logout = function(callback) {
            if (!callback) { callback = function(){}; }
            return $http ({
                method:     "DELETE",
                url:        "/_session"
            })
            .success(function() {
                callback();
            });
        };
    }
])

.service("DateUtils", [
    function() {
        this.toLocalIso8601 = function(date) {
            function pad(num) {
                var norm = Math.abs(Math.floor(num));
                return (norm < 10 ? "0" : "") + norm;
            }

            var tzo = -date.getTimezoneOffset();
            var sign = tzo >= 0 ? "+" : "-";
            return date.getFullYear()
                + "-" + pad(date.getMonth()+1)
                + "-" + pad(date.getDate())
                + "T" + pad(date.getHours())
                + ":" + pad(date.getMinutes())
                + ":" + pad(date.getSeconds())
                + sign + pad(tzo / 60)
                + ":" + pad(tzo % 60);
        };
    }
]);
