#!/bin/bash

createuser -s zaking
createdb bigg
psql -c "ALTER USER postgres WITH PASSWORD 'postgres'"
bash -c 'pg_restore -d bigg /bigg_database_dump/database.dump' || true