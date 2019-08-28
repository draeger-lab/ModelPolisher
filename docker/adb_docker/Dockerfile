FROM postgres:11.4
MAINTAINER ktrivedi@cs.iitr.ac.in

RUN adduser --disabled-password --gecos '' adb

RUN apt-get update &&\
    apt-get install wget -y &&\
    # Create directory '/adb_dump/' and download adb-v0.1.1 dump as 'adb-v0.1.1.dump'
    wget -P /adb_dump/ https://modelpolisher.s3.ap-south-1.amazonaws.com/adb-v0.1.1.dump &&\
    rm -rf /var/lib/apt/lists/*

COPY ./scripts/restore_adb.sh /docker-entrypoint-initdb.d/restore_adb.sh

EXPOSE 5432