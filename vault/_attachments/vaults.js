var cleanVault = function(vault) {
    vault.type = vault.type || "vault";
    vault.dbs = vault.dbs || [];
    vault.signature = vault.signature || null;
    vault.priority = vault.priority || 0;
    return vault;
};

angular.module("vaults", ["ngResource"])

.factory("Vault", function($resource, $http) {
    var db = "/vaultdb/"
    var Vault = $resource(db + ":vaultId", {vaultId:"@_id", vaultRev:"@_rev"}, {
        query: {
            method: "GET",
            isArray: true,
            url: '/vaultdb/_design/indexes/_view/type?include_docs=true&key="vault"',
            transformResponse: function(data, headers) {
                var vaults = [];
                data = JSON.parse(data);
                for (var i = 0; i < data.rows.length; i++) {
                    vaults.push(cleanVault(data.rows[i].doc));
                }
                return vaults;
            }
        },
        delete: {
            method: "DELETE",
            url: "/vaultdb/:vaultId?rev=:vaultRev"
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

.controller("VaultCtrl", ["$scope", "$http", "Vault", function($scope, $http, Vault) {
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

    $scope.vaultNotReachable = function() {
        delete $scope.currentVault["addressable"];
    };

    $scope.newVault = function() {
        $scope.currentVault = cleanVault(new Vault());
    }
    $scope.newVault();

    $scope.editVault = function(vault) {
        $scope.currentVault = angular.copy(vault);
    }

    $scope.saveVault = function() {
        if ("_id" in $scope.currentVault) {
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

    $scope.toggleDb = function(vault, db) {
        var index = vault.dbs.indexOf(db);
        if (index == -1) {
            vault.dbs.push(db);
        } else {
            vault.dbs.splice(index, 1);
        }
        vault.$save();
    }
}])

.directive('json', function() {
    return {
        restrict: 'A', // only activate on element attribute
        require: 'ngModel',
        link: function(scope, element, attrs, ngModelCtrl) {
            ngModelCtrl.$parsers.push(function(text) {
                try {
                    var obj = angular.fromJson(text);
                    ngModelCtrl.$setValidity('json', true);
                    return obj;
                } catch (e) {
                    ngModelCtrl.$setValidity('json', false);
                    return null;
                }
            });

            var toUser = function(object) {
                if (!object) { return ""; }
                return angular.toJson(object, true);
            }
            ngModelCtrl.$formatters.push(toUser);

            // $watch(attrs.ngModel) wouldn't work if this directive created a new scope;
            // see http://stackoverflow.com/questions/14693052/watch-ngmodel-from-inside-directive-using-isolate-scope how to do it then
            scope.$watch(attrs.ngModel, function(newValue, oldValue) {
                if (newValue != oldValue) {
                    ngModelCtrl.$setViewValue(toUser(newValue));
                    // TODO avoid this causing the focus of the input to be lost..
                    ngModelCtrl.$render();
                }
            }, true); // MUST use objectEquality (true) here, for some reason..
        }
    };
});
