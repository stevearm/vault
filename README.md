# Vault

Vault is a distributed information keeping system that runs on top of CouchDB. It is a Java daemon that syncs between multiple vaults.

## Install

To install Vault, you need to prepare CouchDB:

1. Lock down your couch install (Vault won't work with admin party)
2. Create a user with admin rights for Vault to use
3. Edit CouchDB's local.ini file:
    * Ensure the `[couchdb]` section has a `uuid` property (this should already exist on any modern version of CouchDB)
    * Add a `[vault]` section at the bottom and add the following keys
        * `username` (required)
        * `password` (required)

Once that's done, create a folder somewhere (c:/vault), put `couch.jar` in it, and add the following line to the `os_daemons` section of local.ini for Windows:

    vault = java -cp c:/vault/vault.jar com.horsefire.vault.Sentinel c:/vault

or for linux:

    vault = java -cp /opt/vault/vault.jar com.horsefire.vault.Sentinel /opt/vault

Once you've added that line, CouchDB will keep Vault running, and Vault will read everything it needs from CouchDB's config using the [api][couchdb-externals], and keep syncing.

## Licences
Vault is licenced under [Apache Licence 2.0][apache20]. It contains libraries licenced under:

* [Apache Licence 2.0][apache20] (Gson, Guava, Joda-Time, JCommander, LightCouch)
* [Gnu Lesser General Public Licence LGPL][lgpl] (Logback)
* MIT Licences ([jQuery & jQuery-UI][mit-jquery], SLF4J)

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
* The entry for this CouchDB should have an accurate username, password and signature


### Syncs with remote vaults (unimplemented)
* Periodically sync with other vaults
    1. Iterate through all vaults in priority order
    2. For each vault, if it has no host/port info, skip vault
    3. Check the signature. If it does not match, skip vault
    4. Sync each database my vault has that the remote vault should have

### Trigger workers for installed vault apps (unimplemented)
* Trigger workers of installed apps
    1. Listen to the _changes feed for each installed app, watching for a changed to the worker entry
    2. If the "triggered" time is before the "started", do "run the worker" (see below)
    3. Update the "started" time to now
    4. Start the worker
    5. When the worker ends, update the "worker_finished" to now

### Data Structure
Vault db entry for each vault

* id: vault_id
* type: vault
* name: string
* priority: int (optional, default to 0)
* signature: object
* dbs: array of database_name
* (connection info only if pushable)
** host: string
** port: int

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

[apache20]: http://www.apache.org/licenses/LICENSE-2.0.html
[lgpl]: http://www.gnu.org/copyleft/lesser.html
[mit-jquery]: https://github.com/jquery/jquery/blob/master/MIT-LICENSE.txt
[couchdb-externals]: http://davispj.com/2010/09/26/new-couchdb-externals-api.html
[unassignedport]: http://www.speedguide.net/port.php?port=5995