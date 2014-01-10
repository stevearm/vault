angular.module('vaults', [])

.controller('VaultCtrl', function($scope, $http) {
    $scope.vaults = [];
    $http.get('/vaultdb/_design/indexes/_view/type?include_docs=true&key="vault"').success(function(data) {
        var vaults = [];
        for (var i = 0; i < data.rows.length; i++) {
            var doc = data.rows[i].doc;
            var sigString = JSON.stringify(doc.signature);
            if (sigString.length > 20) {
                sigString = sigString.substr(0, 17) + "...";
            }
            doc["sig_string"] = sigString;
            vaults.push(doc);
        }
        $scope.vaults = vaults;
    });

    $scope.dbs = ["None"];
    $http.get('/_all_dbs').success(function(data) {
        $scope.dbs = data.filter(function(name) {
            return name != 'vault' && name != 'vaultdb'
                && name != '_replicator' && name != '_users';
        });
    });
});