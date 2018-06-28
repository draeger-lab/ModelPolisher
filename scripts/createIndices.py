#!/bin/python3

import sqlite3

def main():
    connection = sqlite3.connect("../resources/edu/ucsd/sbrg/bigg/bigg.sqlite")
    cursor = connection.cursor()
    queries = []
    queries.append("CREATE INDEX IF NOT EXISTS synonym_idx on synonym(ome_id)")
    queries.append("CREATE INDEX IF NOT EXISTS mcc_idx on model_compartmentalized_component(compartmentalized_component_id)")
    queries.append("CREATE INDEX IF NOT EXISTS cc_idx on compartmentalized_component(component_id)")
    queries.append("CREATE INDEX IF NOT EXISTS mr_idx on model_reaction(model_id)")
    queries.append("CREATE INDEX IF NOT EXISTS mr_r_idx on model_reaction(reaction_id)")
    for query in queries:
        cursor.execute(query)


main()

