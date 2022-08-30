FROM maven:3.8-jdk-11
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
WORKDIR /usr/src/app
RUN mvn install
RUN mvn compile
RUN mvn exec:java -Dexec.mainClass="Main" -Dexec.args="sampleiso.vcf jdbc:postgresql://34.91.121.155:5432/sample postgres 4MzdliD0IKFpPh8l" -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2"