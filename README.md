# Vault

Vault is a distributed information keeping system that runs on top of CouchDB. It is a Java daemon that syncs between multiple vaults.

## Install

Installing Vault right now is fragile. You need to create a 'vault' db, create an entry with all the info you want in the proper schema, save that entry with _id equal to the uuid from CouchDB's local.ini file, then add the following line to the os_daemons section of local.ini:

    vault = "c:/Program Files/Java/jdk1.7.0_21/bin/java.exe" -jar c:/Users/steve/Cloud/src/vault/tmp/vault.jar --sentinel

Once you've added that line, however, CouchDB will keep Vault running, and Vault will read everything it needs from CouchDB's config using the [api][couchdb-externals], and keep syncing.

If you've secured the vault database on your CouchDB (recommended), then make sure your local.ini has 'vault_user' and 'vault_pass' properties right after the 'uuid'

## Licences
Vault is licenced under [Apache Licence 2.0][apache20]. It contains libraries licenced under:

* [Apache Licence 2.0][apache20] (Jetty, Gson, Guava, Joda-Time, JCommander, LightCouch)
* [Gnu Lesser General Public Licence LGPL][lgpl] (org.swinglabs.pdf-renderer, Logback)
* MIT Licences ([jQuery & jQuery-UI][mit-jquery], SLF4J)

As the LGPL libraries were not modified in any way, they can be released under non-GPL licences.

## Default port
Apparently port 5995 is [unassigned][unassignedport]. If/when I get discovery protocols working and/or need a port for the sentinel to listen on, use this.

## User Workflow
A user adds new vaults, installs/adds new apps, and updates connection information from a CouchApp directly against the vault db.

## Responsibilities
The sentinel needs to perform the following responsibilities, and needs to do it using only the data in the Data Structure section below.
* Periodically check the signature of the current vault, and update the vault entry's stored signature if needed
* Periodically sync with other vaults
  1. Iterate through all vaults in priority order
  2. For each vault, if it has no host/port info, skip vault
  3. Check the signature. If it does not match, skip vault
  4. Sync each database my vault has that the remote vault should have
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
* id: app_id
* type: app
* database: database_name
* worker: id (db entry with worker, only if there's a worker)

Worker trigger db entry for each app
* _attachment/worker_jar (jar file for worker. Always run with: --port --db --username --password)
* triggered: timestamp (when someone requested the worker to run)
* started: timestamp (last time started)
* finished: timestamp (last time finished)

[apache20]: http://www.apache.org/licenses/LICENSE-2.0.html
[lgpl]: http://www.gnu.org/copyleft/lesser.html
[mit-jquery]: https://github.com/jquery/jquery/blob/master/MIT-LICENSE.txt
[couchdb-externals]: http://davispj.com/2010/09/26/new-couchdb-externals-api.html
[unassignedport]: http://www.speedguide.net/port.php?port=5995