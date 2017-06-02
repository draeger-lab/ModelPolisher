### Configuring SQLite DB

Before running ModelPolisher for the first time from source or building with maven, please run configurSQLiteDB.sh, which is located in the scripts directory. \
This will extract the provided db and create the Indices. Packing of the db into the jar is then done by Maven. \
Zip and Python are required for this to work.

### Converting DB

If you want to convert your running instance of a PostgreSQL BiGGDB into a SQLite db usable with Modelpolisher, \
run convertToSQLite.sh from the scripts folder. The finished bigg.sqlite db will be placed in resources/edu/ucsd/sbrg/bigg, \
where ModelPolisher can find and use it. This will overwrite the db present at this location (from configureSQLiteDB.sh). \
Python and sqlite3 are required for this to work.

##### Caveats

As the paths in those scripts are relative, they only work correctly if run from the scripts folder.
