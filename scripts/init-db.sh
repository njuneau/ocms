#!/bin/sh

export PGHOST='127.0.0.1'
export PGPORT='5432'
export PGDATABASE='test'
export PGUSER='test'
export PGPASSWORD='test'

TEST_ROLE="$(psql -Atc "SELECT rolname FROM pg_roles WHERE rolname = 'test';")"
if test -z "${TEST_ROLE}"; then
  psql "CREATE ROLE test WITH LOGIN PASSWORD 'test';"
fi

TEST_DATABASE="$(psql -Atc "SELECT datname FROM pg_database WHERE datname = 'test';")"
if test -z "${TEST_DATABASE}"; then
  psql -c "CREATE DATABASE test OWNER test LOCALE 'en_US.UTF-8' ENCODING 'UTF8';"
  psql -c "GRANT CONNECT ON DATABASE test TO test;"
  psql -f - <<EOF
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO test;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES to test;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON ROUTINES to test;
EOF
fi

TEST_TABLE="$(psql -Atc "SELECT tablename FROM pg_tables WHERE tablename = 'fridge';")"
if test -z "${TEST_TABLE}"; then
  psql -f - <<EOF
CREATE TABLE fridge(
  id            uuid                      PRIMARY KEY,
  name          VARCHAR(255)              NOT NULL,
  date_entered  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
  date_expiry   TIMESTAMP WITH TIME ZONE  NOT NULL
);

CREATE INDEX fridge_name_idx ON fridge (name);
CREATE INDEX fridge_date_entered_idx ON fridge (date_entered);
CREATE INDEX fridge_date_expiry_idx ON fridge (date_expiry);
EOF
fi
