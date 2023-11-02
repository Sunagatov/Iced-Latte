#!/bin/sh
set -e

export APP_ENV=$(grep APP_ENV /usr/app/.env | cut -d '=' -f2)
export APP_VERSION=$(grep APP_VERSION /usr/app/.env | cut -d '=' -f2)
export DATASOURCE_URL=$(grep DATASOURCE_URL /usr/app/.env | cut -d '=' -f2)
export DATASOURCE_PORT=$(grep DATASOURCE_PORT /usr/app/.env | cut -d '=' -f2)

setfacl -R -m u:"$(whoami)":rwX /usr/app
setfacl -dR -m u:"$(whoami)":rwX /usr/app

retries=0
max_retries=20

while ! nc -z postgresdb $DATASOURCE_PORT; do
  retries=$((retries + 1))

  if [ $retries -ge $max_retries ]; then
    echo "PostgresDB is not reachable. Exiting after $max_retries retries."
    exit 1
  fi

  echo "Waiting for PostgresDB... Retry $retries"
  sleep 3
done

exec java -jar /usr/app/app.jar
