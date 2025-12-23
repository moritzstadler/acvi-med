FROM eclipse-temurin:11-jdk
COPY application.properties /target/resources
COPY src/main/resources /src/main/resources
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
