FROM maven:3.8-jdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:11
COPY --from=build /usr/src/app/target/*.jar /usr/app/vcfimport.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/app/vcfimport.jar"]