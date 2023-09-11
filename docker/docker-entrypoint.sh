#!/bin/sh
set -e

APP_ENV=$(grep APP_ENV /opt/app/.env | cut -d '=' -f2)
APP_VERSION=$(grep APP_VERSION /opt/app/.env | cut -d '=' -f2)
DATASOURCE_URL=$(grep DATASOURCE_URL /opt/app/.env | cut -d '=' -f2)

export APP_ENV
export APP_VERSION
export DATASOURCE_URL

retries=0
max_retries=20

while ! nc -z postgresdb 5432; do
  retries=$((retries + 1))
  if [ $retries -ge $max_retries ]; then
    echo "PostgresDB is not reachable. Exiting after $max_retries retries."
    exit 1
  fi

  echo "Waiting for PostgresDB... Retry $retries"
  sleep 3
done

exec java -jar /opt/app/app.jar
