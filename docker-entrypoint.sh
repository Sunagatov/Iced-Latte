#!/bin/sh
set -e

host="${POSTGRES_HOST:-postgresdb}"
max_retries="${POSTGRES_MAX_RETRIES:-5}"
wait_seconds="${POSTGRES_INITIAL_WAIT:-2}"

retries=0
until nc -z "$host" "$DATASOURCE_PORT" || [ $retries -eq "$max_retries" ]
do
  retries=$((retries+1))
  echo "Waiting for postgres database at $host:$DATASOURCE_PORT... ($retries/$max_retries)"
  sleep $((wait_seconds))
  wait_seconds=$((wait_seconds*2))
done

if [ $retries -eq "$max_retries" ]
then
  echo "Failed to connect to postgres database at $host:$DATASOURCE_PORT after $max_retries retries."
  exit 1
fi

echo "PostgresDB at $host:$DATASOURCE_PORT is up - executing command"
exec java -jar /usr/app/app.jar
