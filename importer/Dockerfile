FROM maven:3.8-jdk-11
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
COPY import /usr/src/app
COPY application.properties /usr/src/app/src/main/resources
WORKDIR /usr/src/app
RUN chmod +x import
RUN mv install
EXPOSE 5432
