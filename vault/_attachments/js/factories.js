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

            var Vault = CouchService.couchResourceFactory({
                type: "vault",
                name: null,
                dbs: [],
                signature: null
            }, CurrentVault.vaultDbName);

            Vault.query = function(callback) {
                var vaults = [];
                $http.get("/" + CurrentVault.vaultDbName
                    + "/_design/indexes/_view/type?include_docs=true&key=\"vault\""
                ).success(function(data) {
                    if (data.rows) {
                        for (var i = 0; i < data.rows.length; i++) {
                            vaults.push(new Vault(data.rows[i].doc));
                        }
                    }
                    if (callback) { callback(vaults); }
                });
                return vaults;
            };

            Vault.prototype.priority = function() {
                if ("addressable" in this) { return this.addressable.priority || 0; }
                return 0;
            };

            Vault.prototype.enabled = function() {
                if ("addressable" in this) { return this.addressable.enabled || false; }
                return false;
            }

            return Vault;
        });
    }];
}]);
