# Vault [![Build Status](https://travis-ci.org/stevearm/vault.png?branch=master)](https://travis-ci.org/stevearm/vault)

Vault is a distributed information keeping system that runs on top of CouchDB. It is a Java daemon that syncs between multiple vaults.

## Install

### Installing Vault

Vault can run standalone as a pure CouchApp

1. Install [CouchDB][couchdb]
2. Go to Futon's [replicator page](http://localhost:5984/_utils/replicator.html)
3. Replicate from `http://stevearm.iriscouch.org/vault-release` to local `vault`
4. Replicate from `http://stevearm.iriscouch.org/vaultdb-release` to local `vaultdb`
5. You've now got an unconfigured local vault. Go to the [install page](http://localhost:5984/vault/_design/ui/install.html) to set it up

### Installing Sentinel

Sentinel is a java daemon that syncs vaults automatically in the background for you.

1. Create a folder somewhere (c:/vault)
1. Download the newest `vault.jar` from the [releases page](https://github.com/stevearm/vault/releases) into that folder
1. Create a CouchDB user with admin rights for Vault to use
2. Edit CouchDB's local.ini file:
    * Ensure the `[couchdb]` section has a `uuid` property (this should already exist on any modern version of CouchDB. If it doesn't, set it to the `vaultId` value in [http://localhost:5984/vault/id](http://localhost:5984/vault/id))
    * Add a `[vault]` section at the bottom and add the following keys
        * `username` (required)
        * `password` (required)
    * Add a line to the `os_daemons` section of local.ini
        * Windows: `vault = java -cp c:/vault/vault.jar com.horsefire.vault.CouchMain c:/vault`
        * Linux: `vault = java -cp /opt/vault/vault.jar com.horsefire.vault.CouchMain /opt/vault`

Once you've added that line, CouchDB will keep Sentinel running, and Sentinel will read everything it needs from CouchDB's config using the [api][couchdb-externals], and keep syncing.

## Licences
Vault is licenced under [Apache Licence 2.0][apache20]. It contains libraries licenced under:

* [Apache Licence 2.0][apache20] (Gson, Guava, Joda-Time, JCommander, LightCouch)
* [Gnu Lesser General Public Licence LGPL][lgpl] (Logback)
* MIT Licences ([AngularJS][mit-angular], [AngularUI][mit-angularui], [Bootstrap][mit-bootstrap], SLF4J, [QR Code Generator][qrcode-generator], cornercouch)

As the LGPL libraries were not modified in any way, they can be released under non-GPL licences.

## Default port
Apparently port 5995 is [unassigned][unassignedport]. If/when I get discovery protocols working and/or need a port for the sentinel to listen on, use this.

## User Workflow
A user adds new vaults, installs/adds new apps, and updates connection information from a CouchApp directly against the vault db.

## Responsibilities
The sentinel needs to perform the following responsibilities, and needs to do it using only the data in the Data Structure section below.

### Ensures local data is correct
Vault should make sure the following are always true:

* There is publicly readable database called `vault`
* The `vault` db has an entry `id` with this vault's id
* There is non-publicly readable database called `vaultdb`
* The `vaultdb` db has an entry for this CouchDB's uuid
* The `vaultdb` entry for this CouchDB should have an accurate username, password, signature, and sentinel version

### Syncs with remote vaults (unimplemented)
* Periodically sync with other vaults
    1. Iterate through all vaults with an addressable block, in priority order
    1. Check the signature. If it does not match, skip vault
    1. Sync each database my vault has that the remote vault should have

### Trigger workers for installed vault apps (unimplemented)
* Trigger workers of installed apps
    1. Listen to the _changes feed for each installed app, watching for a changed to the worker entry
    1. If the "triggered" time is before the "started", do "run the worker" (see below)
    1. Update the "started" time to now
    1. Start the worker
    1. When the worker ends, update the "worker_finished" to now

### Data Structure
Vault db entry for each vault

* id: vault_id
* type: vault
* name: string
* signature: object
* dbs: array of database_name
* addressable: object (this only exists if vault is externally accessable)
** host: string
** port: int
** priority: int
** enabled: boolean

Vault db entry for each app

* id: random
* type: app
* name: string
* db: database_name
* ui: entry point (if _design/ui/index.html then: "ui/index.html")
* worker: id for tracking and triggering worker times
** optional, but if specified, look for worker.jar in ui, and run with: --db --host --port --username --password

Worker trigger db entry for each app
* triggered: timestamp (when someone requested the worker to run)
* started: timestamp (last time started)
* finished: timestamp (last time finished)

[couchdb]: http://couchdb.apache.org/
[apache20]: http://www.apache.org/licenses/LICENSE-2.0.html
[lgpl]: http://www.gnu.org/copyleft/lesser.html
[mit-angular]: https://github.com/angular/angular.js/blob/master/LICENSE
[mit-angularui]: https://github.com/angular-ui/bootstrap/blob/master/LICENSE
[mit-bootstrap]: https://github.com/twbs/bootstrap/blob/master/LICENSE
[qrcode-generator]: http://www.d-project.com/
[couchdb-externals]: http://davispj.com/2010/09/26/new-couchdb-externals-api.html
[unassignedport]: http://www.speedguide.net/port.php?port=5995
