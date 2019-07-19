FROM postgres:9.6.2
MAINTAINER ktrivedi@cs.iitr.ac.in

RUN apt-get update &&\
apt-get install wget -y &&\
# Create directory '/bigg_database_dump/' and download bigg_database dump as 'database.dump'
wget -P /bigg_database_dump/ https://modelpolisher.s3.ap-south-1.amazonaws.com/bigg_database.dump &&\
rm -rf /var/lib/apt/lists/*

COPY ./scripts/restore_biggdb.sh /docker-entrypoint-initdb.d/restore_biggdb.sh

EXPOSE 1310:5432
