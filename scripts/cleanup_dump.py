#!/bin/python3

import re

def main():

    with open("bigg.sql") as file:
        content = file.readlines()
    output = []
    for line in content:
        if line.startswith("--") or line.startswith("SET") or line.startswith("COPY") or "SELECT pg_catalog.setval(" in line:
            line = ""
        line = line.replace("true", "1")
        line = line.replace("false", "0")
        output.append(line)
    with open("converted.sql", "w") as db:
        db.write("".join(output))

    parens = re.compile("\(.*\);")
    using = re.compile("USING.*")

    with open("bigg_schema.sql") as file:
        content = file.readlines()
    output = []
    type_block = False
    function_block = False
    function_end = False
    sequence_block = False
    alter_table_block = False
    for line in content:
        if type_block:
            if ");" in line:
                type_block = False
        elif function_block:
            if function_end and line.strip() == '':
                function_block = False
                function_end = False
            if not line.startswith(" ") and ";" in line:
                function_end = True
        elif sequence_block:
            if ";" in line:
                sequence_block = False
        elif alter_table_block:
            if ";" in line:
                alter_table_block = False
        elif line.startswith("--") or line.startswith("SET") or line.startswith("COPY") or "SELECT pg_catalog.setval(" in line\
                or line.startswith("CREATE EXTENSION") or line.startswith("COMMENT") or "OWNER" in line:
            pass
        elif line.startswith("CREATE TYPE") or line.startswith("ALTER TYPE"):
            if not line.endswith(";"):
                type_block = True
        elif line.startswith("CREATE FUNCTION") or line.startswith("ALTER FUNCTION"):
            if not line.endswith(";"):
                function_block = True
        elif line.startswith("CREATE SEQUENCE") or line.startswith("ALTER SEQUENCE"):
            if not line.endswith(";"):
                sequence_block = True
        elif line.startswith("ALTER TABLE ONLY"):
            if not line.endswith(";"):
                alter_table_block = True
        elif line.startswith("CREATE INDEX"):
            line = line.replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS")
            matches = re.search(parens, line)
            match = matches.group(0)
            match = match.replace(" gin_trgm_ops", "")
            line = re.sub(using, match, line)
            output.append(line)
        else:
            line = line.replace("true", "1")
            line = line.replace("false", "0")
            if line.strip() != "":
                output.append(line)
    with open("schema.sql", "w") as db:
        db.write("".join(output))


main()
