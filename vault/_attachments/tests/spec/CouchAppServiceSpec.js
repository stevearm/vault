describe("CouchAppService test suite", function() {

    beforeEach(module("couchapp.service"));
    beforeEach(inject([ "$location", function($location) {
        spyOn($location, "absUrl").and.returnValue("http://localhost:5984/vault/_design/ui/index.html#/");
    }]));

    it("parses a standard path", inject(["CouchAppService", function (CouchAppService) {
        expect(CouchAppService.currentDb()).toBe("vault");
        expect(CouchAppService.currentDesignDoc()).toBe("ui");
    }]));
});
