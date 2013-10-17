function(newDoc, oldDoc, userCtx) {
	// Helper functions
	var fail = function(message) { throw({"forbidden":message}); };
	
	// Don't validate a deletion request
	if (newDoc._deleted) { return; }
	
	if (!("type" in newDoc)) { fail("Must have a type"); }
	switch (newDoc.type) {
		case "vault":
			break;
		case "app":
			break;
		default:
			fail("Unsupported document type: "+newDoc.type);
	}
}