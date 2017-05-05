import sqlite3


def main():
    connection = sqlite3.connect('../resources/edu/ucsd/sbrg/bigg/bigg.sqlite')
    cursor = connection.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()
    for table in tables:
        table = table[0]
        cursor.execute("SELECT * FROM " + table)
        for (pos, column) in enumerate(cursor.description):
            column = column[0]
            query = "CREATE INDEX IF NOT EXISTS " + table +"_idx" + str(pos) + " on " + table + "(" + column + ")"
            cursor.execute(query)


main()
