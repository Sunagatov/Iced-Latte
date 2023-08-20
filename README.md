# Online-Store
* Rest API which simulates the work of an online store. <br />
* Based on Spring Boot ecosystem technologies and PostgreSQL database. <br />
* Created for learning purposes. 

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
Get project running by
```shell
docker-compose --profile dev up
```
If this doesn't work use
```shell
docker compose --profile dev up
```

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

## Docker (Should be fixed)
Build the Docker image
```shell
docker build -t zufar_sunagatov/online-store:v1 .
```

Run the Docker image
```shell
docker run -it --rm -p 8081:8081 --name online-store zufar_sunagatov/online-store:v1
```

Push the image to Docker
```shell
docker login --username=zufar_sunagatov
docker tag zufar_sunagatov/online-store:v1 zufar_sunagatov/online-store:v1
docker push zufar_sunagatov/online-store:v1
```

Override our code style
```shell
1. Press ⌘ + , or Ctrl+Alt+S to open the IDE settings and select Editor | Code Style.
2. Check the box Enable EditorConfig support.
3. Apply the changes and close the dialog.
```
