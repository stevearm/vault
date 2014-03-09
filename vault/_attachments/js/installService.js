"use strict";

angular.module("vault.services.installer", ["couchapp.service", "CornerCouch"])

.service("InstallService", [
    "$http", "$location", "CouchAppService", "cornercouch",
    function($http, $location, CouchAppService, cornercouch) {
        var createResponse = function(m, b) {
            var createPart = function(key, title, message, baggage) {
                if (typeof(message) === "undefined") { message = "Unknown result"; }
                var result = { key: key, title: title, pass: true };
                if (message != null) {
                    result.pass = false;
                    result.message = message;
                }
                result.baggage = baggage || {};
                return result;
            }

            var parts = [
                createPart("serverSec",     "Security enabled on server",               m.serverSec,    b.serverSec),
                createPart("idDoc",         "Current db has valid id document",         m.idDoc,        b.idDoc),
                createPart("vaultDbSec",    "VaultDb is only viewable by admins",       m.vaultDbSec,   b.vaultDbSec),
                createPart("vaultDbVault",  "VaultDb has document for current vault",   m.vaultDbVault, b.vaultDbVault)
            ];

            return {
                pass: parts.reduce(function(acc, curr){ return acc && curr.pass; }, true),
                parts: parts
            };
        }

        var couchServer = cornercouch();

        this.getReportCard = function(callback) {

            var messages = { serverSec: "Untested", idDoc: "Untested", vaultDbSec: "Untested", vaultDbVault: "Untested" };
            var baggage = {};

            $http.get("/_session").error(function(data, status) {
                messages.serverSec = "Error getting current session information";
                callback(createResponse(messages, baggage));
                return;
            }).success(function(data) {
                if (data.userCtx.name == null && data.userCtx.roles.indexOf("_admin") != -1) {
                    messages.serverSec = "Server still admin-party";
                } else if (data.userCtx.name == null) {
                    messages.serverSec = "Must log in to check install status";
                } else {
                    messages.serverSec = null;
                }

                var currentDb = couchServer.getDB(CouchAppService.currentDb());
                var idDoc = new currentDb.docClass();
                idDoc.load("id").error(function() {
                    messages.idDoc = "Error loading doc";
                    callback(createResponse(messages, baggage));
                    return;
                }).success(function() {
                    baggage.idDoc = idDoc;
                    if (!("vaultId" in idDoc)) {
                        messages.idDoc = "'vaultId' not specified";
                        callback(createResponse(messages, baggage));
                        return;
                    }
                    if (!("vaultDbName" in idDoc) || idDoc.vaultDbName == "") {
                        messages.idDoc = "'vaultDbName' not specified";
                        callback(createResponse(messages, baggage));
                        return;
                    }
                    messages.idDoc = null;

                    baggage.vaultDbSec = { vaultDbName: idDoc.vaultDbName };
                    $http.get("/" + idDoc.vaultDbName + "/_security").error(function(data, status) {
                        if (status == 404) {
                            messages.idDoc = "'vaultDbName' " + idDoc.vaultDbName + " does not exist";
                            callback(createResponse(messages, baggage));
                            return;
                        }
                        messages.vaultDbSec = "Cannot read security info (are you logged in?)";
                        callback(createResponse(messages, baggage));
                        return;
                    }).success(function(data) {
                        if (!data.members || (data.members.names.length == 0 && data.members.roles.length == 0)) {
                            messages.vaultDbSec = "'vaultDbName' " + idDoc.vaultDbName + " is public";
                            callback(createResponse(messages, baggage));
                            return;
                        }
                        messages.vaultDbSec = null;

                        baggage.vaultDbVault = { vaultDbName: idDoc.vaultDbName, vaultId: idDoc.vaultId };
                        var vaultDb = couchServer.getDB(idDoc.vaultDbName);
                        var vaultDoc = new vaultDb.docClass();
                        vaultDoc.load(idDoc.vaultId).error(function() {
                            messages.vaultDbVault = "No entry in " + idDoc.vaultDbName + " for current vault id";
                            callback(createResponse(messages, baggage));
                            return;
                        }).success(function() {
                            messages.vaultDbVault = null;
                            callback(createResponse(messages, baggage));
                            return;
                        });
                    });
                });
            });
        };

        this.fixIdDoc = function(vaultId, vaultDbName, callback) {
            var currentDb = couchServer.getDB(CouchAppService.currentDb());
            var idDoc = new currentDb.docClass({ _id: "id" });
            idDoc.load().error(function() {
                save();
            }).success(function() {
                save();
            });

            var save = function() {
                idDoc.vaultId = vaultId;
                idDoc.vaultDbName = vaultDbName;
                idDoc.save().error(function() {
                    callback(false);
                }).success(function() {
                    callback(true);
                });
            };
        };

        this.fixDbMembers = function(vaultDbName, callback) {
            var url = "/" + vaultDbName + "/_security";
            $http.get(url).error(function() {
                callback(false);
            }).success(function(data) {
                if (!("members" in data)) {
                    data.members = { names: [], roles:[]};
                }
                if (!("roles" in data.members)) {
                    data.members.roles = [];
                }
                if (data.members.roles.length == 0) {
                    data.members.roles.push("vaulters");
                }
                $http.put(url, data).error(function() {
                    callback(false);
                }).success(function() {
                    callback(true);
                });
            });
        };

        this.fixVaultDbEntry = function(vaultDbName, vaultId, signature, callback) {
            var vaultDB = couchServer.getDB(vaultDbName);
            var vaultDoc = new vaultDB.docClass({
                _id: vaultId,
                type: "vault",
                name: "New Vault",
                dbs: [],
                signature: signature
            });
            vaultDoc.save().error(function() {
                callback(false);
            }).success(function() {
                callback(true);
            });
        };
    }
]);
