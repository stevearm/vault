"use strict";

angular.module("vault.services.installer", ["CornerCouch"])

.service("InstallService", [
    "$http", "$location", "cornercouch",
    function($http, $location, cornercouch) {
        var createResponse = function(m) {
            var createPart = function(key, title, message) {
                if (typeof(message) === "undefined") { message = "Unknown result"; }
                var result = { key: key, title: title, pass: true };
                if (message != null) {
                    result.pass = false;
                    result.message = message;
                }
                return result;
            }

            var parts = [
                createPart("serverSec",     "Security enabled on server",               m.serverSec),
                createPart("idDoc",         "Current db has valid id document",         m.idDoc),
                createPart("vaultDbSec",    "VaultDb is only viewable by admins",       m.vaultDbSec),
                createPart("vaultDbVault",  "VaultDb has document for current vault",   m.vaultDbVault)
            ];

            return {
                pass: parts.reduce(function(acc, curr){ return acc && curr.pass; }, true),
                parts: parts
            };
        }

        var couchServer = cornercouch();

        this.getReportCard = function(callback) {

            var messages = { serverSec: "Untested", idDoc: "Untested", vaultDbSec: "Untested", vaultDbVault: "Untested" };

            $http.get("/_session").error(function(data, status) {
                messages.serverSec = "Error getting current session information";
                callback(createResponse(messages));
                return;
            }).success(function(data) {
                if (data.userCtx.name == null && data.userCtx.roles.indexOf("_admin") != -1) {
                    messages.serverSec = "Server still admin-party";
                } else {
                    messages.serverSec = null;
                }

                var currentDb = couchServer.getDB($location.absUrl().split("://")[1].split("/")[1]);
                var idDoc = new currentDb.docClass();
                idDoc.load("id").error(function() {
                    messages.idDoc = "Error loading doc";
                    callback(createResponse(messages));
                    return;
                }).success(function() {
                    if (!("vaultId" in idDoc)) {
                        messages.idDoc = "'vaultId' not specified";
                        callback(createResponse(messages));
                        return;
                    }
                    if (!("vaultDbName" in idDoc) || idDoc.vaultDbName == "") {
                        messages.idDoc = "'vaultDbName' not specified";
                        callback(createResponse(messages));
                        return;
                    }
                    messages.idDoc = null;

                    $http.get("/" + idDoc.vaultDbName + "/_security").error(function(data, status) {
                        if (status == 404) {
                            messages.idDoc = "'vaultDbName' " + idDoc.vaultDbName + " does not exist";
                            callback(createResponse(messages));
                            return;
                        }
                        messages.vaultDbSec = "Cannot read security info (are you logged in?)";
                        callback(createResponse(messages));
                        return;
                    }).success(function(data) {
                        if (!data.members || (data.members.names.length == 0 && data.members.roles.length == 0)) {
                            messages.vaultDbSec = "'vaultDbName' " + idDoc.vaultDbName + " is public";
                            callback(createResponse(messages));
                            return;
                        }
                        messages.vaultDbSec = null;

                        var vaultDb = couchServer.getDB(idDoc.vaultDbName);
                        var vaultDoc = new vaultDb.docClass();
                        vaultDoc.load(idDoc.vaultId).error(function() {
                            messages.vaultDbVault = "No entry in " + idDoc.vaultDbName + " for current vault id";
                            callback(createResponse(messages));
                            return;
                        }).success(function() {
                            messages.vaultDbVault = null;
                            callback(createResponse(messages));
                            return;
                        });
                    });
                });
            });
        };

        this.fixProblems = function(callback) {
            callback(createResponse({}));
        }
    }
]);
