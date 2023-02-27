#!/bin/sh
PG_IMAGE_NAME='docker.io/library/postgres'
PG_IMAGE_TAG='15.2-alpine'
PG_PUBLISHED_PORT='5432'

POSTGRES_USER='test'
POSTGRES_PASSWORD='test'
POSTGRES_DB='test'

CONTAINER_RUNTIME=''
CONTAINER_NAME='ocms-db'

if which podman >> /dev/null 2>&1; then
    CONTAINER_RUNTIME='podman'
elif which docker >> /dev/null 2>&1; then
    CONTAINER_RUNTIME='docker'
fi

if test -n "${CONTAINER_RUNTIME}"; then
    echo "Using container runtime               '${CONTAINER_RUNTIME}'"
    echo "Using postgres image                  '${PG_IMAGE_NAME}:${PG_IMAGE_TAG}'"
    echo "Published postgres port on 127.0.0.1: '${PG_PUBLISHED_PORT}'"
    echo "Container name:                       '${CONTAINER_NAME}"
    echo ''
    echo "Postgres user:                        '${POSTGRES_USER}'"
    echo "Postgres password:                    '${POSTGRES_PASSWORD}'"
    echo "Postgres database:                    '${POSTGRES_DB}'"
    echo ''

    ${CONTAINER_RUNTIME} run --publish "127.0.0.1:${PG_PUBLISHED_PORT}:5432/tcp" \
        --detach \
        --env POSTGRES_USER="${POSTGRES_USER}" \
        --env POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
        --env POSTGRES_DB="${POSTGRES_DB}" \
        --name "${CONTAINER_NAME}" \
        "${PG_IMAGE_NAME}:${PG_IMAGE_TAG}"
else
    echo 'No container runtime found'
fi
