#!/bin/python3

import re
import os

parens = re.compile("\(.*\);")
using = re.compile("USING.*")


def main():
    clean_schema()
    clean_data()


def clean_schema():
    with open("bigg_schema.sql") as file:
        content = file.readlines()
    output = []
    block = False
    function_block = False
    function_end = False
    found_cobra = False
    found_start = False
    for line in content:

        # only use the public schema for now
        if not found_cobra:
            if line.startswith("SET search_path = cobradb, pg_catalog;"):
                found_cobra = True
            continue
        if not found_start:
            if line.startswith("SET search_path = public, pg_catalog;"):
                found_start = True
            continue

        if function_block:
            if function_end and line.strip() == '':
                function_block = False
                function_end = False
            if not line.startswith(" ") and ";" in line:
                function_end = True
            continue

        elif block:
            if ";" in line:
                block = False
            continue

        elif is_var(line, "TYPE") or is_var(line, "SEQUENCE") or line.startswith("ALTER TABLE ONLY"):
            block = is_block(line)
            continue

        elif is_var(line, "FUNCTION"):
            function_block = is_block(line)
            continue

        elif line.startswith("CREATE INDEX"):
            line = fix_index(line)
        elif line.startswith("--") \
                or line.strip() == "" \
                or line.startswith("SET") \
                or line.startswith("COPY") \
                or line.startswith("CREATE EXTENSION") \
                or line.startswith("COMMENT") \
                or "OWNER" in line \
                or "is_version" in line \
                or "SELECT pg_catalog.setval(" in line \
                or "SCHEMA" in line:
            line = ""

        line = line.replace("true", "1")
        line = line.replace("false", "0")
        if line.strip() != "":
            output.append(line)

    with open("schema.sql", "w") as db:
        db.write("".join(output))


def is_var(line, var):
    return line.startswith("CREATE {}".format(var)) or line.startswith("ALTER {}".format(var))


def is_block(line):
    return not line.endswith(";")


def fix_index(line):
    line = line.replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS")
    matches = re.search(parens, line)
    match = matches.group(0)
    match = match.replace(" gin_trgm_ops", "")
    return re.sub(using, match, line)


def clean_data():
    with open("bigg.sql") as file:
        content = file.readlines()
    output = ["BEGIN;"]
    found_start = False
    for line in content:
        if line.startswith("SET search_path = public, pg_catalog;"):
            line = ""
            found_start = True
        elif not found_start:
            continue
        elif line.startswith("--") \
                or line.startswith("SET") \
                or line.startswith("COPY") \
                or "SELECT pg_catalog.setval(" in line:
            line = ""
        elif "database_version" in line:
            line = line.replace("is_version, ", "")
            line = line.replace("'is_version', ", "")
        line = line.replace("true", "1")
        line = line.replace("false", "0")
        if line.strip() != "":
            output.append(line)
    output.append("COMMIT;")
    with open("converted.sql", "w") as db:
        db.write("".join(output))


main()
