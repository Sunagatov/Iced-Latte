## How to start up SonarQube locally
1. Run SonarQube in Docker Container
```shell
   docker compose up -d sonarqube
   ```
2. Connect to the **localhost:9000** and set up a new project manually
3. Run sonar-maven-plugin and provide necessary credentials. Preferably, try NOT to use a maven installed on your local machine
```shell
docker run --rm \
  -v /path/to/your/project:/usr/src/app \
  -w /usr/src/app \
  zufar_sunagatov/online-store:v1 \
  mvn clean verify sonar:sonar \
  -Dsonar.projectKey=YOUR_KEY \
  -Dsonar.projectName=YOUR_PROJECT_NAME \
  -Dsonar.host.url=YOUR_HOST_URL \
  -Dsonar.token=YOUR_TOKEN \
  -DskipTests=true
   ```