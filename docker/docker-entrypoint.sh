#!/bin/sh
set -e

# Set environment variables
export APP_ENV=$(grep APP_ENV /opt/app/.env | cut -d '=' -f2)
export APP_VERSION=$(grep APP_VERSION /opt/app/.env | cut -d '=' -f2)
export DATASOURCE_URL=$(grep DATASOURCE_URL /opt/app/.env | cut -d '=' -f2)
export DATASOURCE_PORT=$(grep DATASOURCE_PORT /opt/app/.env | cut -d '=' -f2)

setfacl -R -m u:"$(whoami)":rwX /opt/app
setfacl -dR -m u:"$(whoami)":rwX /opt/app

retries=0
max_retries=20

# Wait for PostgresSQL to become available
while ! nc -z postgresdb $DATASOURCE_PORT; do
  retries=$((retries + 1))

  if [ $retries -ge $max_retries ]; then
    echo "PostgresDB is not reachable. Exiting after $max_retries retries."
    exit 1
  fi

  echo "Waiting for PostgresDB... Retry $retries"
  sleep 3
done

# Execute the application
exec java -jar /opt/app/app.jar
