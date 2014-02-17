"use strict";

angular.module("vault.factories", [ "ngResource", "vault.services" ])

.provider("CurrentVault", function() {
    var deferred = null;
    this.getDeferred = function($http, CouchService) {
        if (deferred == null) { deferred = $http.get("/" + CouchService.currentDb() + "/id"); }
        return deferred;
    };
    this.$get = [ "$http", "CouchService", function(that) { return function($http, CouchService) {
        return that.getDeferred($http, CouchService).then(function(data) {
            return data.data;
        });
    }}(this)];
})

.provider("Vault", [ "CurrentVaultProvider", function(CurrentVaultProvider) { this.$get = [
    "$http", "CouchService", "$resource",
    function($http, CouchService, $resource) {
        return CurrentVaultProvider.getDeferred($http, CouchService).then(function(data) {
            var CurrentVault = data.data

            // From here on, act like normal factory that had CurrentVault injected

            var db = "/" + CurrentVault.vaultDbName + "/"
            var Vault = $resource(db + ":vaultId", {vaultId:"@_id", vaultRev:"@_rev"}, {
                query: {
                    method: "GET",
                    isArray: true,
                    url: db + '_design/indexes/_view/type?include_docs=true&key="vault"',
                    transformResponse: function(data, headers) {
                        var vaults = [];
                        data = JSON.parse(data);
                        if (data.rows) {
                            for (var i = 0; i < data.rows.length; i++) {
                                vaults.push(data.rows[i].doc);
                            }
                        }
                        return vaults;
                    }
                },
                delete: {
                    method: "DELETE",
                    url: db + ":vaultId?rev=:vaultRev"
                }
            });

            Vault.prototype.$save = function() {
                var config = { data: this, method: "POST", url: db };
                if ("_id" in this) {
                    config.method = "PUT";
                    config.url += this._id;
                }
                $http(config).error(function(data, status, headers, config) {
                    console.log("Error saving", data, status, headers, config);
                    window.alert("Error saving: " + data.reason);
                }).success(function(original_object){ return function(data, status, headers, config) {
                    original_object._rev = data.rev;
                };}(this));
            };

            Vault.prototype.priority = function() {
                if ("addressable" in this) {
                    return this.addressable.priority || 0;
                }
                return 0;
            };

            return Vault;
        });
    }];
}]);
