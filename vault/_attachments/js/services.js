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

        this.couchResourceFactory = function(that) {
            return function(defaultValues, dbName) {
                if (!dbName) {
                    dbName = that.currentDb();
                }

                var dbUrl = "/" + dbName + "/";

                var Document = function(data) {
                    angular.extend(this, data);
                };

                if (defaultValues) {
                    angular.extend(Document.prototype, defaultValues);
                }

                Document.get = function(id, callback) {
                    var doc = new Document();
                    $http.get(dbUrl + id).success(function(data) {
                        angular.extend(doc, data);
                        if (callback) { callback(doc); }
                    });
                    return doc;
                };

                Document.prototype.$save = function(callback) {
                    var config = { data: this, method: "POST", url: dbUrl };
                    if ("_id" in this) {
                        config.method = "PUT";
                        config.url += this._id;
                    }
                    $http(config).error(function(data, status, headers, config) {
                        console.log("Error saving", data, status, headers, config);
                        window.alert("Error saving: " + data.reason);
                    }).success(function(original_object){ return function(data, status, headers, config) {
                        original_object._id = data.id;
                        original_object._rev = data.rev;
                        if (callback) { callback(); }
                    };}(this));
                };

                Document.prototype.$delete = function(callback) {
                    if (!callback) { callback = function(){}; }
                    $http({
                        method: "DELETE",
                        url: dbUrl + this._id + "?rev=" + this._rev
                    }).error(function(data, status, headers, config) {
                        console.log("Error deleting", data, status, headers, config);
                        window.alert("Error deleting: " + data.reason);
                    }).success(callback);
                };

                return Document;
            };
        }(this);
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
