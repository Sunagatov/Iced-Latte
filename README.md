# Online-Store
* Rest API which simulates the work of an online store. <br />
* Based on AWS Services and Spring Boot ecosystem technologies. <br />
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
  * Mongo DB
  * AWS Dynamo DB
* Queues
  * AWS SQS
* Topics
  * AWS SNS
* Containerisation
  * Docker
* Monitoring
  * ElasticSearch
  * Logstash
  * Kibana
  * Filebeat
* Logging
  * Slf4j
  * Logback
  
## Getting the project up and running
Get project running by
```shell
docker-compose --profile dev up
```
If this doesn't work use
```shell
docker compose --profile dev up
```
### Working with REST API
For retrieving Open API schema in json use
```shell
http://localhost:8083/api/docs
```
For retrieving Open API schema in yaml use
```shell
http://localhost:8083/api/docs.yaml
```
Swagger UI page
```shell
http://localhost:8083/api/docs/swagger-ui
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
http://localhost:8083/api/products
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


