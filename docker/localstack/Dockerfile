
#
# LOCALSTACK
#
FROM localstack/localstack:latest
MAINTAINER Zufar Sunagatov <zufar.sunagatov@gmail.com>

# Create a user group 'xyzgroup'
RUN addgroup -S xyzgroup

# Create a user 'appuser' under 'xyzgroup'
RUN adduser -S -D -h /usr/app/src appuser xyzgroup

# Chown all the files to the app user.
RUN chown -R appuser:xyzgroup /usr/app

# Switch to 'appuser'
USER appuser

