"use strict";

angular.module("vault.controllers", [ "vault.factories", "vault.services" ])

.controller("HeaderCtrl", [
    "$scope", "$location", "CouchService", "ExternalVaultVarsService",
    function($scope, $location, CouchService, ExternalVaultVarsService) {
        $scope.logout = function() {
            CouchService.couchServer.logout();
        };
        if ($location.host() != "localhost" && $location.host() != "127.0.0.1") {
            ExternalVaultVarsService.findLocalVault().then(function(url) {
                if (url != null) {
                    $scope.localVaultUrl = url;
                }
            });
        }
    }
])

.controller("LoginCtrl", [
    "$scope", "$location", "CouchService",
    function($scope, $location, CouchService) {
        $scope.login = function() {
            $scope.message = "";
            CouchService.couchServer.login($scope.username, $scope.password).then(function() {
                if (CouchService.couchServer.userCtx && CouchService.couchServer.userCtx.name) {
                    $location.path("/");
                } else {
                    $scope.message = "Login failure, try again";
                }
            });
        };
    }
])

.controller("AboutCtrl", [
    "$scope", "$http", "Vault", "CurrentVault", "CouchService",
    function($scope, $http, Vault, CurrentVault, CouchService) {
        $scope.sentinelRun = new Date(CurrentVault.sentinelRun);
        Vault.get(CurrentVault.vaultId, function(result) {
            $scope.sentinelVersion = result.sentinel;
        });
        $http.get(
            "/" + CouchService.currentDb() + "/_design/" + CouchService.currentDesignDoc()
        ).success(function(data) {
            $scope.uiVersion = data._rev;
        });
        $http.get(
            "/" + CurrentVault.vaultDbName + "/_design/indexes"
        ).success(function(data) {
            $scope.indexVersion = data._rev;
        });
    }
])

.controller("HomeCtrl", [
    "$scope", "$http", "Vault", "CurrentVault",
    function($scope, $http, Vault, CurrentVault) {
        $scope.me = Vault.get(CurrentVault.vaultId, function(result) {
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

.controller("AppListCtrl", [
    "$scope", "$http", "CurrentVault",
    function($scope, $http, CurrentVault) {
        $scope.apps = [];
        $http.get("/" + CurrentVault.vaultDbName
            + "/_design/indexes/_view/type?include_docs=true&key=\"app\""
        ).success(function(data) {
            $scope.apps = data.rows.map(function(e) {
                return e.doc;
            });
        });
    }
])

.controller("VaultListCtrl", [
    "$scope", "$http", "Vault", "CurrentVault",
    function($scope, $http, Vault, CurrentVault) {
        $scope.currentVaultId = CurrentVault.vaultId;
        $scope.cappedStringify = function(object, maxLength) {
            if (!object) { return ""; }
            var string = JSON.stringify(object);
            if (string.length > maxLength) { return string.substr(0, maxLength - 3) + "..."; }
            return string;
        };

        $scope.vaults = Vault.query();
    }
])

.controller("DatabaseListCtrl", [
    "$scope", "$http", "Vault",
    function($scope, $http, Vault) {
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

.controller("BarcodeListCtrl", [
    "$scope", "$http", "Vault",
    function($scope, $http, Vault) {
        $scope.vaults = [];
        Vault.query(function(vaults) {
            $scope.vaults = vaults.filter(function(e) {
                return e.enabled();
            })
        });

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

        $scope.showCode = function(vault, db) {
            $scope.message = "";
            $scope.qrCode = "http://" + vault.username + ":" + vault.password
                + "@" + vault.addressable.host + ":" + vault.addressable.port
                + "/" + db;
            if ($scope.qrCode.length > 213) {
                $scope.qrCode = ""
                $scope.message = "Cannot currently encode over 213 characters";
            }
        }
    }
])

.controller("FixerCtrl", [
    "$scope", "$http", "CouchService",
    function($scope, $http, CouchService) {
        $scope.selectFunction = "function(doc) {\n  return !(\"type\" in doc);\n}";
        $scope.fixFunction = "function(doc) {\n  doc.type = \"event\";\n}";

        $scope.dbs = [];
        $http.get("/_all_dbs").success(function(data) {
            $scope.dbs = data;
            $scope.db = data[0];
        });

        $scope.selectedDocs = [];
        $scope.currentDoc = null;
        var currentDocOffset = 0;

        $scope.select = function() {
            $http.post("/" + $scope.db + "/_temp_view", {
                map: "function(doc){ var test=" + $scope.selectFunction + "; if (test(doc)) { emit(null,doc); }}"
            }).success(function(data) {
                $scope.selectedDocs = data.rows.map(function(e) {
                    return e.value;
                });
                currentDocOffset = 0;
                $scope.next();
            });
        };

        var currentFixFunction = function() {
            var globalFixFunction = null;
            eval("globalFixFunction=" + $scope.fixFunction);
            return globalFixFunction;
        };

        $scope.fix = function() {
            var fix = currentFixFunction();
            $scope.selectedDocs.forEach(function(e) {
                fix(e);
                $http.put("/" + $scope.db + "/" + e._id, e);
            });
            $scope.selectedDocs = [];
            $scope.next();
        }

        $scope.next = function() {
            if ($scope.selectedDocs.length > 0) {
                if (currentDocOffset == $scope.selectedDocs.length) {
                    currentDocOffset = 0;
                }
                $scope.currentDoc = $scope.selectedDocs[currentDocOffset++];
            } else {
                $scope.currentDoc = null;
            }
        }

        $scope.fixCurrent = function() {
            globalFixFunction()($scope.currentDoc);
        };
    }
])

.controller("VaultCtrl", [
    "$scope", "$routeParams", "$window", "$http", "Vault", "CouchService", "CurrentVault",
    function($scope, $routeParams, $window, $http, Vault, CouchService, CurrentVault) {
        var id = $routeParams.id;
        if (id) {
            $scope.vault = Vault.get(id);
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
        $scope.setAsCurrent = function() {
            CurrentVault.vaultId = $scope.vault._id;
            $http.put("/" + CouchService.currentDb() + "/id", CurrentVault).then(function() {
                $window.location.reload();
            })
        }
    }
])

.controller("ConflictsCtrl", [
    "$scope", "$http",
    function($scope, $http) {
        $scope.dbs = [];
        $http.get("/_all_dbs").success(function(data) {
            $scope.dbs = data;
            $scope.db = data[0];
        });

        $scope.conflicts = [];

        $scope.newDb = function() {
            $scope.docId = null;
            $scope.conflictedDoc = null;
            $scope.conflicts = [];
            $http.post("/" + $scope.db + "/_temp_view", {
                map: "function(doc){ if (doc._conflicts) { emit(doc._rev, doc._conflicts); } }"
            }).success(function(data) {
                $scope.conflicts = data.rows.map(function(e) {
                    return { id: e.id, current: e.key, revs: e.value };
                });
                $scope.newConflict();
            });
        };

        $scope.newConflict = function() {
            if (!$scope.docId) {
                $scope.conflictedDoc = null;
                return;
            }

            var getDocRev = function(db, id, rev) {
                var result = { rev: rev };
                $http.get("/" + db + "/" + id + "?rev=" + rev).success(function(data) {
                    result.doc = data;
                });
                return result;
            }

            $scope.conflictedDoc = {
                id:         $scope.docId.id,
                current:    getDocRev($scope.db, $scope.docId.id, $scope.docId.current),
                revs:       $scope.docId.revs.map(function(e) {
                    return getDocRev($scope.db, $scope.docId.id, e);
                })
            };
        };

        $scope.delete = function(id, rev) {
            $http({
                method: "DELETE",
                url:    "/" + $scope.db + "/" + id + "?rev=" + rev
            });
        };
    }
]);
