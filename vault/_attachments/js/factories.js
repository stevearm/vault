"use strict";

angular.module("vault.factories", [])

.factory("Vault", function($resource, $http) {
    var cleanVault = function(vault) {
        vault.type = vault.type || "vault";
        vault.dbs = vault.dbs || [];
        vault.signature = vault.signature || null;
        vault.priority = function() {
            if ("addressable" in this) {
                return this.addressable.priority || 0;
            }
            return 0;
        };
        return vault;
    };

    var db = "/vaultdb-dev/"
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
                        vaults.push(cleanVault(data.rows[i].doc));
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
    return Vault;
})

.factory("UnauthenticatedInterceptor", [
    "$q", "$location",
    function($q, $location) {
        return {
            responseError: function(rejection) {
                if (rejection.status === 401) {
                    $location.path( "/login" );
                    return;
                }
                return $q.reject(rejection);
            }
        };
    }
]);
