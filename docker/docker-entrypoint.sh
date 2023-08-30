#!/bin/sh
set -e

APP_ENV=$(grep APP_ENV /opt/app/.env | cut -d '=' -f2)
APP_VERSION=$(grep APP_VERSION /opt/app/.env | cut -d '=' -f2)
DATASOURCE_URL=$(grep DATASOURCE_URL /opt/app/.env | cut -d '=' -f2)

export APP_ENV
export APP_VERSION
export DATASOURCE_URL

while ! nc -z postgresdb 5432; do
  echo "Waiting for PostgresDB..."
  sleep 3
done

exec java -jar /opt/app/app.jar
