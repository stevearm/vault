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

    // Don't validate a deletion request
    if (newDoc._deleted) { return; }

    if (!("type" in newDoc)) { fail("Must have a type"); }
    switch (newDoc.type) {
        case "vault":
            require(newDoc, "name");
            if (!Array.isArray(newDoc.dbs)) { fail("dbs must be an array"); }
            if (typeof(newDoc.signature) === "undefined") { fail("signatures field must exist, even if null"); }
            break;
        case "app":
            require(newDoc, "name");
            require(newDoc, "db");
            require(newDoc, "ui");
            break;
        default:
            fail("Unsupported document type: "+newDoc.type);
    }
}
