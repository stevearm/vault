function(newDoc, oldDoc, userCtx) {
    // Helper functions
    var fail = function(message) { throw {"forbidden":message}; };
    var require = function(doc, fieldName) {
        if (!(fieldName in doc)) {
            fail("Must specify a " + fieldName + " field");
        }
    };

    if (userCtx.roles.indexOf("_admin") == -1 && userCtx.roles.indexOf("vaulter") == -1) {
        fail("Must be an admin or a vaulter");
    }

    if (newDoc._id == "id") {
        require(newDoc, "vaultId");
        require(newDoc, "vaultDbName");
    } else {
        fail("only a single db entry (_id == 'id') belongs in the vault database");
    }
}
