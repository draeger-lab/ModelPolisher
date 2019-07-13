#!/bin/bash

psql -c 'DROP DATABASE IF EXISTS adb'
psql -c 'CREATE DATABASE adb'
bash -c 'pg_restore -O -d adb /adb_dump/adb-v0.1.1.dump' || true