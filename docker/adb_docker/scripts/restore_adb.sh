#!/bin/bash

psql -c "DROP DATABASE IF EXISTS adb";
psql -c "CREATE DATABASE adb";
psql -c "CREATE USER adb with ENCRYPTED PASSWORD 'adb'";
psql -c "GRANT ALL PRIVILEGES ON DATABASE adb TO adb";
bash -c "pg_restore -d adb -U adb --verbose /adb_dump/adb-v0.1.1.dump";

