FROM bigg_db_current:latest AS dumper
# NOTE: this image is curently not available anywhere, but it is nothing more than postgres:14
# with the current DB dump + restore script from mephenors image

RUN ["sed", "-i", "s/exec \"$@\"/echo \"skipping...\"/", "/usr/local/bin/docker-entrypoint.sh"]

RUN mkdir -p /var/lib/postgresql-static/data
ENV PGDATA=/var/lib/postgresql-static/data
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres

RUN ["/usr/local/bin/docker-entrypoint.sh", "postgres"]

FROM postgres:14

COPY --from=dumper /var/lib/postgresql-static/data $PGDATA

# ENTRYPOINT ["/usr/bin/env"]

# USER postgres

# CMD ["postgres"]
