angular.module('vaults', ['ngResource'])

.factory('Vault', function($resource) {
    return $resource('/vaultdb/:vaultId', {vaultId:'@_id'}, {
        query: {
            method: 'GET',
            isArray: true,
            url: '/vaultdb/_design/indexes/_view/type?include_docs=true&key="vault"',
            transformResponse: function(data, headers) {
                var vaults = [];
                data = JSON.parse(data);
                for (var i = 0; i < data.rows.length; i++) {
                    vaults.push(data.rows[i].doc);
                }
                return vaults;
            }
        }
    });
})

.controller('VaultCtrl', ["$scope", "$http", "Vault", function($scope, $http, Vault) {
    $scope.cappedStringify = function(object, maxLength) {
        var string = JSON.stringify(object);
        if (string.length > maxLength) { return string.substr(0, maxLength - 3) + "..."; }
        return string;
    };

    $scope.vaults = Vault.query();

    $scope.dbs = ["None"];
    $http.get('/_all_dbs').success(function(data) {
        $scope.dbs = data.filter(function(name) {
            return name != 'vault' && name != 'vaultdb'
                && name != '_replicator' && name != '_users';
        });
    });
}]);