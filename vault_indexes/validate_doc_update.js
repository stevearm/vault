function(newDoc, oldDoc, userCtx) {
	// Helper functions
	var fail = function(message) { throw({"forbidden":message}); };
	var require = function(doc, fieldName) {
		if (!(fieldName in doc)) {
			fail("Must specify a " + fieldName + " field");
		}
	};
	
	// Don't validate a deletion request
	if (newDoc._deleted) { return; }
	
	if (!("type" in newDoc)) { fail("Must have a type"); }
	switch (newDoc.type) {
		case "vault":
			require(newDoc, "name");
			require(newDoc, "dbs");
			if (!Array.isArray(newDoc.dbs)) { fail("dbs must be an array"); }
			break;
		case "app":
			require(newDoc, "name");
			require(newDoc, "db");
			break;
		default:
			fail("Unsupported document type: "+newDoc.type);
	}
}