describe("InstallService test suite", function() {

    var callbackCalled;

    beforeEach(module("vault.services.installer"));
    beforeEach(inject([ "$location", function($location) {
        spyOn($location, "absUrl").and.returnValue("http://localhost:5984/vault/_design/ui/index.html#/");
        callbackCalled = false;
    }]));
    afterEach(inject([ "$httpBackend", function ($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
        expect(callbackCalled).toBe(true);
    }]));

    var getPart = function(parts, key) {
        for (var i = 0; i < parts.length; i++) {
            if (parts[i].key == key) {
                return parts[i];
            }
        }
        return {};
    };

    it("serverSec with admin-party", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend
            .expect("GET", "/_session")
            .respond({ userCtx: { name: null, roles: ["_admin"] }});

        $httpBackend.when("GET", /[a-zA-Z].*/).respond(404, { message: "Nothing here" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "serverSec").pass).toBe(false);
        });

        $httpBackend.flush();
    }]));

    it("serverSec logged out", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend
            .expect("GET", "/_session")
            .respond({ userCtx: { name: null, roles: [] }});

        $httpBackend.when("GET", /[a-zA-Z].*/).respond(404, { message: "Nothing here" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "serverSec").pass).toBe(true);
        });

        $httpBackend.flush();
    }]));

    it("serverSec logged in", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend
            .expect("GET", "/_session")
            .respond({ userCtx: { name: "me", roles: ["_admin"] }});

        $httpBackend.when("GET", /[a-zA-Z].*/).respond(404, { message: "Nothing here" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "serverSec").pass).toBe(true);
        });

        $httpBackend.flush();
    }]));

    it("idDoc missing", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});

        $httpBackend.expectGET("/vault/id").respond(404, { message: "Nothing here" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "idDoc").pass).toBe(false);
        });

        $httpBackend.flush();
    }]));

    it("idDoc with no vaultId", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});

        $httpBackend.expectGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "idDoc").pass).toBe(false);
            expect(getPart(result.parts, "idDoc").message).toMatch(/vaultId/);
        });

        $httpBackend.flush();
    }]));

    it("idDoc with no vaultDbName", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});

        $httpBackend.expectGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "idDoc").pass).toBe(false);
            expect(getPart(result.parts, "idDoc").message).toMatch(/vaultDbName/);
        });

        $httpBackend.flush();
    }]));

    it("idDoc with vaultDbName for non-existant db", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });

        $httpBackend.expectGET("/vaultdb/_security").respond(404, { error: "not_found", reason: "no_db_file" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "idDoc").pass).toBe(false);
            expect(getPart(result.parts, "idDoc").message).toMatch(/does not exist/);
        });

        $httpBackend.flush();
    }]));

    it("vaultDbSec not logged in", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });

        $httpBackend.expectGET("/vaultdb/_security").respond(401, { error: "not_found", reason: "no_db_file" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "vaultDbSec").pass).toBe(false);
            expect(getPart(result.parts, "vaultDbSec").message).toMatch(/logged in/);
        });

        $httpBackend.flush();
    }]));

    it("vaultDbSec public db", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });

        $httpBackend.expectGET("/vaultdb/_security").respond({});

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "vaultDbSec").pass).toBe(false);
            expect(getPart(result.parts, "vaultDbSec").message).toMatch(/public/);
        });

        $httpBackend.flush();
    }]));

    it("vaultDbSec publicly readable db with admin security", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });

        $httpBackend.expectGET("/vaultdb/_security")
            .respond({
                admins: { names: [], roles:[ "vaulters" ]},
                members: { names: [], roles:[]}
            });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "vaultDbSec").pass).toBe(false);
            expect(getPart(result.parts, "vaultDbSec").message).toMatch(/public/);
        });

        $httpBackend.flush();
    }]));

    it("vaultDbVault db has no entry", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });
        $httpBackend.whenGET("/vaultdb/_security").respond({admins: { names: [], roles:[]}, members: { names: [], roles:[ "vaulters" ]}});

        $httpBackend.expectGET("/vaultdb/234298jf982j").respond(404, { error: "not_found", reason: "missing" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(getPart(result.parts, "vaultDbVault").pass).toBe(false);
            expect(getPart(result.parts, "vaultDbVault").message).toMatch(/[Nn]o entry/);
        });

        $httpBackend.flush();
    }]));

    it("Valid install (using names)", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });
        $httpBackend.whenGET("/vaultdb/_security").respond({admins: { names: [], roles:[]}, members: { names: [ "sentinel" ], roles:[]}});
        $httpBackend.whenGET("/vaultdb/234298jf982j").respond({ _id: "234298jf982j", _rev: "2-985j2938j298j" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(result.pass).toBe(true);
            result.parts.forEach(function(e) {
                expect(e.pass).toBe(true);
            });
        });

        $httpBackend.flush();
    }]));

    it("Valid install (using roles)", inject([ "$httpBackend", "InstallService", function ($httpBackend, InstallService) {
        $httpBackend.whenGET("/_session").respond({ userCtx: { name: "me", roles: ["_admin"] }});
        $httpBackend.whenGET("/vault/id").respond({ _id: "id", _rev: "3-isjfoijdoijw", vaultId: "234298jf982j", vaultDbName: "vaultdb" });
        $httpBackend.whenGET("/vaultdb/_security").respond({admins: { names: [], roles:[]}, members: { names: [], roles:[ "vaulters" ]}});
        $httpBackend.whenGET("/vaultdb/234298jf982j").respond({ _id: "234298jf982j", _rev: "2-985j2938j298j" });

        InstallService.getReportCard(function(result) {
            callbackCalled = true;
            expect(result.pass).toBe(true);
            result.parts.forEach(function(e) {
                expect(e.pass).toBe(true);
            });
        });

        $httpBackend.flush();
    }]));
});
