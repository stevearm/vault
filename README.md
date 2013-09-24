# Vault

Vault is a distributed information keeping system that runs on top of CouchDB. It is a Java daemon that syncs between multiple vaults.

## Install

Installing Vault right now is fragile. You need to create a 'vault' db, create an entry with all the info you want in the proper schema, save that entry with _id equal to the uuid from CouchDB's local.ini file, then add the following line to the os_daemons section of local.ini:

    vault = "c:/Program Files/Java/jdk1.7.0_21/bin/java.exe" -jar c:/Users/steve/Cloud/src/vault/tmp/vault.jar --sentinel

Once you've added that line, however, CouchDB will keep Vault running, and Vault will read everything it needs from CouchDB's config using the [api][couchdb-externals], and keep syncing.

## Licences
Vault is licenced under [Apache Licence 2.0][apache20]. It contains libraries licenced under:

* [Apache Licence 2.0][apache20] (Jetty, Gson, Guava, Joda-Time, JCommander, LightCouch)
* [Gnu Lesser General Public Licence LGPL][lgpl] (org.swinglabs.pdf-renderer, Logback)
* MIT Licences ([jQuery & jQuery-UI][mit-jquery], SLF4J)

As the LGPL libraries were not modified in any way, they can be released under non-GPL licences.

[apache20]: http://www.apache.org/licenses/LICENSE-2.0.html
[lgpl]: http://www.gnu.org/copyleft/lesser.html
[mit-jquery]: https://github.com/jquery/jquery/blob/master/MIT-LICENSE.txt
[couchdb-externals]: http://davispj.com/2010/09/26/new-couchdb-externals-api.html
