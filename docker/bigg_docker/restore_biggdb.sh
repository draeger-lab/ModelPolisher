#!/bin/bash

psql -c 'DROP DATABASE IF EXISTS bigg'
psql -c 'CREATE DATABASE bigg'
bash -c 'pg_restore -O -d bigg --verbose /bigg_database.dump'
