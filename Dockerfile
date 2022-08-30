FROM maven:3.8-jdk-11
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
WORKDIR /usr/src/app
RUN mvn install
RUN mvn compile
RUN mvn exec:java -Dexec.mainClass="Main" -Dexec.args="../tiny.vcf jdbc:postgresql://localhost:5432/sample postgres password" -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2"