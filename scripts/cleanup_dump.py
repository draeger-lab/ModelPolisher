#!/bin/python3

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


main()
