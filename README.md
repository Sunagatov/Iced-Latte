# Online-Store
* Rest API which simulates the work of an online store. <br />
* Based on AWS Services and Spring Boot ecosystem technologies. <br />
* Created for learning purposes. 

## Prerequisites
* JDK 17
* Apache Maven 3.6.3

## Tech stack
* Spring Boot 3
* Spring Security
* JWT
* AWS Dynamo Db
* AWS SNS
* AWS SQS
* Docker

## Docker
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
