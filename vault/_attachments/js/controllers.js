"use strict";

angular.module("vault.controllers", [ "vault.factories", "vault.services" ])

.controller("HeaderCtrl", [
    "$scope", "$location", "CouchService",
    function($scope, $location, CouchService) {
        $scope.logout = function() {
            CouchService.logout();
        };
    }
])

.controller("LoginCtrl", [
    "$scope", "$location", "CouchService",
    function($scope, $location, CouchService) {
        $scope.login = function() {
            CouchService.login($scope.username, $scope.password, function() {
                $location.path("/");
            });
        };
    }
])

.controller("HomeCtrl", [
    "$scope", "$http", "Vault", "CurrentVault",
    function($scope, $http, Vault, CurrentVault) {
        $scope.me = Vault.get({ id: CurrentVault.vaultId}, function() {
            $scope.couchSig = $http.get("/").success(function(data) {
                $scope.isValidSig = angular.equals($scope.me.signature, data);
                $scope.me.signature = data;
            });
        });
        $scope.fixSig = function() {
            $scope.me.$save(function() { $scope.isValidSig = true; });
        };
    }
])

.controller("VaultListCtrl", [
    "$scope", "$http", "Vault",
    function($scope, $http, Vault) {
        $scope.cappedStringify = function(object, maxLength) {
            if (!object) {
                return "";
            }
            var string = JSON.stringify(object);
            if (string.length > maxLength) { return string.substr(0, maxLength - 3) + "..."; }
            return string;
        };

        $scope.vaults = Vault.query();

        $scope.localDbs = [];
        $http.get("/_all_dbs").success(function(data) {
            $scope.localDbs = data;
        });

        $scope.dbs = function() {
            var list = $scope.localDbs;
            for (var i = 0; i < $scope.vaults.length; i++) {
                list = list.concat($scope.vaults[i].dbs);
            }
            list = list.filter(function(name) {
                return name != "vault" && name != "vaultdb"
                    && name != "_replicator" && name != "_users";
            }).reduce(function(p, c) {
                if (p.indexOf(c) < 0) p.push(c);
                return p;
            }, []).sort();
            if (list.length == 0) {
                return ["None"];
            }
            return list;
        }

        var indexOf = function(vault) {
            for (var i = 0; i < $scope.vaults.length; i++) {
                if ($scope.vaults[i]._id == vault._id) {
                    return i;
                }
            }
            return -1;
        };

        $scope.toggleAddressable = function() {
            if ("addressable" in $scope.currentVault) {
                delete $scope.currentVault["addressable"];
            } else {
                $scope.currentVault["addressable"] = {};
            }
        };

        $scope.newVault = function() {
            $scope.currentVault = new Vault({
                type: "vault",
                dbs: [],
                signature: null
            });
        }
        $scope.newVault();

        $scope.editVault = function(vault) {
            $scope.currentVault = angular.copy(vault);
        }

        $scope.saveVault = function() {
            if ("_rev" in $scope.currentVault) {
                var index = indexOf($scope.currentVault);

                // Persist things not in the edit form
                $scope.currentVault._rev = $scope.vaults[index]._rev;
                $scope.currentVault.dbs = $scope.vaults[index].dbs;

                $scope.vaults[index] = $scope.currentVault;
            } else {
                $scope.vaults.push($scope.currentVault);
            }
            $scope.currentVault.$save();
            $scope.newVault();
        }

        $scope.deleteVault = function() {
            if ("_id" in $scope.currentVault) {
                $scope.vaults.splice(indexOf($scope.currentVault), 1);
                $scope.currentVault.$delete();
            }
            $scope.newVault();
        }

        $scope.toggleEnabled = function(vault) {
            if (!("addressable" in vault)) { return; }
            if ("enabled" in vault.addressable) {
                vault.addressable.enabled = !vault.addressable.enabled;
            } else {
                vault.addressable.enabled = true;
            }
            vault.$save();
        };

        $scope.toggleDb = function(vault, db) {
            var index = vault.dbs.indexOf(db);
            if (index == -1) {
                vault.dbs.push(db);
            } else {
                vault.dbs.splice(index, 1);
            }
            vault.$save();
        }
    }
])

.controller("VaultCtrl", [
    "$scope", "$routeParams", "$window", "Vault",
    function($scope, $routeParams, $window, Vault) {
        var id = $routeParams.id;
        if (id) {
            $scope.vault = Vault.get({ id: id });
        } else {
            $scope.vault = new Vault();
        }

        $scope.toggleAddressable = function() {
            if ("addressable" in $scope.vault) {
                delete $scope.vault.addressable;
            } else {
                $scope.vault.addressable = { enabled: false };
            }
        }

        $scope.save = function() {
            $scope.vault.$save(function() {
                $window.history.back();
            });
        };
        $scope.delete = function() {
            $scope.vault.$delete(function() {
                $window.history.back();
            });
        };
    }
]);
