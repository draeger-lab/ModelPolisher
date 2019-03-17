#!/bin/python3

import sqlite3

from os import chdir, path
from sys import argv


def main():
    connection = sqlite3.connect("../resources/edu/ucsd/sbrg/bigg/bigg.sqlite")
    cursor = connection.cursor()
    queries = ["CREATE INDEX IF NOT EXISTS synonym_idx on synonym(ome_id)",
               "CREATE INDEX IF NOT EXISTS mcc_idx on model_compartmentalized_component(compartmentalized_component_id)",
               "CREATE INDEX IF NOT EXISTS cc_idx on compartmentalized_component(component_id)",
               "CREATE INDEX IF NOT EXISTS mr_idx on model_reaction(model_id)",
               "CREATE INDEX IF NOT EXISTS mr_r_idx on model_reaction(reaction_id)"]
    for query in queries:
        cursor.execute(query)


scripts_dir = path.dirname(path.abspath(argv[0]))
chdir(scripts_dir)
main()
