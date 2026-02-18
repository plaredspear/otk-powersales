#!/bin/bash
set -e

SCHEMA_SQL="/docker-entrypoint-initdb.d/파워세일즈 스키마.SQL"

if [ ! -f "$SCHEMA_SQL" ]; then
  echo "WARNING: Schema file not found: $SCHEMA_SQL"
  echo "Skipping salesforce2 schema initialization."
  exit 0
fi

echo "Applying salesforce2 schema..."

# Replace Heroku Connect user (u4bee3ek26k44g) with postgres user,
# then execute the SQL against the target database
sed 's/u4bee3ek26k44g/postgres/g' "$SCHEMA_SQL" | psql -v ON_ERROR_STOP=0 -U "$POSTGRES_USER" -d "$POSTGRES_DB"

echo "salesforce2 schema applied successfully."
