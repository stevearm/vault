describe("CouchService test suite", function() {

    beforeEach(module("vault.services"));
    afterEach(inject([ "$httpBackend", function ($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    }]));

    it("should merge constructor and default parameters", inject([ "CouchService", function (CouchService) {
        var Doc = CouchService.couchResourceFactory({ key1: "valueDefault1", key2: "valueDefault2" }, "dbName");

        var doc = new Doc({ key1: "newValue1", key3: "newValue3" });
        expect(doc.key1).toBe("newValue1");
        expect(doc.key2).toBe("valueDefault2");
        expect(doc.key3).toBe("newValue3");
    }]));

    it("should request a doc by id", inject([ "$httpBackend", "CouchService", function ($httpBackend, CouchService) {
        var Doc = CouchService.couchResourceFactory({}, "dbName");

        $httpBackend
            .expect("GET", "/dbName/myId")
            .respond({ key1: "val1", key2: "val2" });

        var assertDoc = function(doc) {
            expect(doc.key1).toBe("val1");
            expect(doc.key2).toBe("val2");
        }
        var calledBack = false;

        var doc = Doc.get("myId", function(result) {
            assertDoc(result);
            calledBack = true;
        });

        $httpBackend.flush();

        expect(calledBack).toBe(true);
        assertDoc(doc);
    }]));

    it("should do a post for a new doc", inject([ "$httpBackend", "CouchService", function ($httpBackend, CouchService) {
        var Doc = CouchService.couchResourceFactory({ defaultKey: "defaultValue" }, "dbName");

        $httpBackend
            .expect("POST", "/dbName/", { key1: "val1", key2: "val2" })
            .respond({ ok:true, id: "myId", rev: "myRev1" });

        var doc = new Doc({ key1: "val1", key2: "val2"});

        var calledBack = false;
        doc.$save(function() { calledBack = true; });

        $httpBackend.flush();

        expect(calledBack).toBe(true);
        expect(doc._id).toBe("myId");
        expect(doc._rev).toBe("myRev1");
    }]));

    it("should do a put for existing doc", inject([ "$httpBackend", "CouchService", function ($httpBackend, CouchService) {
        var Doc = CouchService.couchResourceFactory({ defaultKey: "defaultValue" }, "dbName");

        $httpBackend
            .expect("PUT", "/dbName/myId", { _id: "myId", _rev: "myRev1", key1: "val1", key2: "val2" })
            .respond({ ok:true, id: "myId", rev: "myRev2" });

        var doc = new Doc({_id: "myId", _rev: "myRev1", key1: "val1", key2: "val2"});

        var calledBack = false;
        doc.$save(function() { calledBack = true; });

        $httpBackend.flush();

        expect(calledBack).toBe(true);
        expect(doc._id).toBe("myId");
        expect(doc._rev).toBe("myRev2");
    }]));

    it("should delete a doc", inject([ "$httpBackend", "CouchService", function ($httpBackend, CouchService) {
        var Doc = CouchService.couchResourceFactory({ defaultKey: "defaultValue" }, "dbName");

        $httpBackend
            .expect("DELETE", "/dbName/myId?rev=myRev1")
            .respond({ ok:true, id: "myId", rev: "myRev2" });

        var doc = new Doc({_id: "myId", _rev: "myRev1", key1: "val1", key2: "val2"});

        var calledBack = false;
        doc.$delete(function() { calledBack = true; });

        $httpBackend.flush();

        expect(calledBack).toBe(true);
    }]));
});
