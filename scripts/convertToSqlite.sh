#!/bin/bash

echo "Started conversion of local PostgreSQL BiGGDB to SQLite..." \
&& pg_dump --data-only --column-inserts bigg > bigg.sql \
&& echo "Finished dumping PostgreSQL DB." \
&& python cleanup_dump.py \
&& echo "Finished cleanup for migration to SQLite." \
&& sqlite3 "" ".read create_db.sql" \
&& echo "Finished creating SQLite DB." \
&& mv bigg.sqlite ../resources/edu/ucsd/sbrg/bigg \
&& echo "Creating indices." \
&& python createIndices.py \
&& rm bigg.sql converted.sql \
&& echo "Removing temporary files..." \
&& echo "Finished."
