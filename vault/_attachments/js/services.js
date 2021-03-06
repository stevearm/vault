"use strict";

var externalVaultVars = function() {
    var hasLocalVault = false;

    return {
        hasLocalVault: function(hasVault) {
            if (typeof(hasVault) === "undefined") {
                return hasLocalVault;
            }
            hasLocalVault = hasVault;
        }
    };
}();

angular.module("vault.services", [ "CornerCouch" ])

.service("ExternalVaultVarsService", [
    "$q",
    function($q) {
        this.findLocalVault = function() {
            var deferred = $q.defer();

            // For now this is just hardcoded to dupliate the url used in index.html
            // It should be fixed to dynamically add <script> tags, cycling through localhost
            // and 127.0.0.1, along with trying any non-standard ports listed in the vault db
            if (externalVaultVars.hasLocalVault()) {
                deferred.resolve("http://localhost:5984/vault/_design/ui/index.html");
            } else {
                deferred.resolve(null);
            }
            return deferred.promise;
        }
    }
])

.service("CouchService", [
    "$http", "$q", "cornercouch",
    function($http, $q, cornercouch) {
        this.couchServer = cornercouch();

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

        /**
         * This expectes remoteCouch to start with http, end with a slash, and include (if needed):
         * - username
         * - password
         * - host
         * - port
         *
         * Examples:
         * - http://192.168.1.100:5984/
         * - http://username:password@example.iriscouch.org/
         */
        this.sync = function(db, remoteCouch, push, pull, callback) {
            var replicate = function(from, to, callback) {
                $http.post("/_replicate", {
                    source: from, target: to, create_target: true
                }).then(function() {
                    callback(true);
                }, function() {
                    callback(false);
                });
            };

            var deferred = [];
            if (push) {
                deferred.push($http.post("/_replicate", {
                    source: db, target: remoteCouch + db, create_target: true
                }));
            }
            if (pull) {
                deferred.push($http.post("/_replicate", {
                    source: remoteCouch + db, target: db, create_target: true
                }));
            }
            $q.all(deferred).then(function(results) {
                var passed = results.reduce(function(acc, curr) {
                    if (curr.data.history.length == 0) {
                        return false;
                    }
                    return acc && (curr.data.history[0].doc_write_failures == 0);
                }, true);
                callback(passed);
            }, function(failures) {
                callback(false);
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
