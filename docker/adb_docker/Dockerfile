FROM postgres:11.4
MAINTAINER ktrivedi@cs.iitr.ac.in

RUN adduser --disabled-password --gecos '' adb

RUN apt-get update && \
    apt-get install curl -y && \
    # Create directory '/adb_dump/' and download adb-v0.1.1 dump as 'adb-v0.1.1.dump'
    mkdir /adb_dump && \
    curl -Lo /adb_dump/adb-v0.1.1.dump https://www.dropbox.com/s/qjiey8y88gt4h0l/adb-v0.1.1.dump?dl=0 && \
    rm -rf /var/lib/apt/lists/*

COPY ./scripts/restore_adb.sh /docker-entrypoint-initdb.d/restore_adb.sh

EXPOSE 5432
