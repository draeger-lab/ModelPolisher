#!/bin/bash

createuser -s adb
createdb adb
psql -c "ALTER USER postgres WITH PASSWORD 'postgres'"
bash -c 'pg_restore -d adb /adb_dump/adb-v0.1.1.dump' || true