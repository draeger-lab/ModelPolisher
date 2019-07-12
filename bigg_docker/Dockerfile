FROM postgres:11.4
MAINTAINER ktrivedi@cs.iitr.ac.in

RUN apt update &&\
apt install wget -y &&\
# Create directory '/bigg_database_dump/' and download bigg_database dump as 'database.dump'
wget -P /bigg_database_dump/ https://modelpolisher.s3.ap-south-1.amazonaws.com/database.dump &&\
rm -rf /var/lib/apt/lists/*

COPY ./scripts/restore_biggdb.sh /docker-entrypoint-initdb.d/restore_biggdb.sh

EXPOSE 5432:5432