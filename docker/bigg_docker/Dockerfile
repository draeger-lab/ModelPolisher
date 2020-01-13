FROM postgres:9.6.2
MAINTAINER zajac.thomas1992@gmail.com

RUN apt-get update && \
    apt-get install curl -y && \
    # Create directory '/bigg_database_dump/' and download bigg_database dump as 'database.dump'
    mkdir /bigg_database_dump && \
    curl -Lo /bigg_database_dump/database.dump https://www.dropbox.com/sh/ye05djxrpxy37da/AAB-rhgcEv9p8gcMpkYuowu8a/v1.6/database.dump?dl=0 && \    
    rm -rf /var/lib/apt/lists/*

COPY ./scripts/restore_biggdb.sh /docker-entrypoint-initdb.d/restore_biggdb.sh

EXPOSE 5432
