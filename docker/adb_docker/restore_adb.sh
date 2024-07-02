#!/bin/bash

psql -c "DROP DATABASE IF EXISTS adb";
psql -c "CREATE DATABASE adb";
bash -c "pg_restore -O -d adb --verbose /adb.dump";

