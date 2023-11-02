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
