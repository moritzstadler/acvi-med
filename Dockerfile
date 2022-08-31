FROM maven:3.8-jdk-11
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
COPY run.sh /usr/src/app
RUN sudo chmod +x run.sh
WORKDIR /usr/src/app
RUN mvn install
EXPOSE 5432
ENTRYPOINT ["/run.sh"]