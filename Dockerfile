FROM maven:3.8-jdk-11
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
COPY application.properties /usr/src/app/src/main/resources
COPY run.sh /usr/src/app
WORKDIR /usr/src/app
RUN chmod +x run.sh
RUN mvn install
EXPOSE 5432
ENTRYPOINT ["./run.sh"]