# Online-Store
* Rest API which simulates the work of an online store. <br />
* Based on Spring Boot ecosystem technologies and PostgreSQL database. <br />
* Created for learning purposes. 

docker run --name postgresdb -e POSTGRES_PASSWORD=5zxxA2wJ;A,11-8 -d postgres:latest

docker run --name postgresdb -e POSTGRES_PASSWORD=5zxxA2wJ;A,11-8 -v /opt/:/var/lib/postgresql/data -d postgres:latest

docker run --name postgresdb -e 'POSTGRES_PASSWORD=5zxxA2wJ;A,11-8' -v /root/opt/:/var/lib/postgresql/data -d postgres:latest

docker run --name postgresdb \
           -e 'POSTGRES_PASSWORD=5zxxA2wJ;A,11-8' \ 
           -p 5432:5432 \
           -v /root/opt/:/var/lib/postgresql/data \
           -d postgres:latest
134.209.25.111

docker run \
-d \
-p 8083:8083 \
-e 'APP_ENV=dev' \
-e 'APP_VERSION=0.0.1-SNAPSHOT' \
-e 'APP_SERVER_PORT=8083' \
-e 'APP_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970' \
-e 'APP_JWT_EXPIRATION_TIME=1800000' \
-e 'DATASOURCE_URL=jdbc:postgresql://134.209.25.111:5432/testdb?serverTimezone=UTC' \
-e 'DATASOURCE_PORT=5432' \
-e 'DATASOURCE_NAME=testdb' \
-e 'DATASOURCE_USERNAME=postgres' \
-e 'DATASOURCE_PASSWORD=postgres' \
zufarexplainedit/online-store:test


## Prerequisites
* JDK 17
* Apache Maven 3.6.3
* Docker Desktop

## Tech stack
* Spring 
  * Boot 3
  * Data
  * Security
* Security
  * JWT
  * TLS
* Databases
  * PostgreSQL
* Containerisation
  * Docker
* Monitoring
  * ElasticSearch
  * Logstash
  * Kibana
  * Filebeat
* Logging
  * Log4j2
  
## Getting the project up and running
See [START.MD](https://github.com/Sunagatov/Online-Store/blob/development/START.MD)

### Obtaining a token
Using Postman get Bearer token by running
```shell
http://localhost:8083/api/auth/register
```
Copy the token and insert into Auth tab
### Testing authorisation
Run this GET request to test authentication
```shell 
http://localhost:8083/api/v1/products/a3c4d3f7-1172-4fb2-90a9-59b13b35dfc6
```

Override our code style
```shell
1. Press ⌘ + , or Ctrl+Alt+S to open the IDE settings and select Editor | Code Style.
2. Check the box Enable EditorConfig support.
3. Apply the changes and close the dialog.
```
