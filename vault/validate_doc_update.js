function(newDoc, oldDoc, userCtx) {
	if (newDoc._id == "id") {
		if (!("vaultId" in newDoc)) {
			throw ({"forbidden":"id document must specify a vaultId"});
		}
		return;
	}
	throw ({"forbidden":"only a single db entry (_id == 'id') belongs in the vault database"});
}
