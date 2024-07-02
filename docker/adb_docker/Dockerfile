FROM postgres:latest AS builder

LABEL maintainer="dario.eltzner@student.uni-tuebingen.de"

RUN apt-get update && apt-get upgrade -y
COPY ./adb-v0.1.1.dump /adb.dump
COPY ./restore_adb.sh /docker-entrypoint-initdb.d/restore_adb.sh

RUN mkdir -p /var/lib/postgresql-static/data
ENV PGDATA=/var/lib/postgresql-static/data
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres

RUN ["sed", "-i", "s/exec \"$@\"/echo \"skipping...\"/", "/usr/local/bin/docker-entrypoint.sh"]

RUN ["/usr/local/bin/docker-entrypoint.sh", "postgres"]

FROM postgres:latest

COPY --from=builder /var/lib/postgresql-static/data $PGDATA

EXPOSE 5432

