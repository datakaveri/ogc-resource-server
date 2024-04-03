# Run from project root directory

ARG VERSION="0.0.1-SNAPSHOT"

FROM maven:3-eclipse-temurin-11

WORKDIR /usr/share/app
COPY pom.xml .

# Downloads all packages defined in pom.xml
RUN mvn clean package

COPY src src
RUN mvn clean package -Dmaven.test.skip=true

ARG VERSION
ENV JAR="ogc-resource-server-dev-${VERSION}-fat.jar"

# Copying openapi docs
COPY docs docs
# replace all instances of the server URL in OGC OpenAPI spec to the jenkins URL, since the OGC compliance
# tests actually parses the server URL in the OpenAPI spec
RUN sed -i 's|https://ogc.iudx.io|http://jenkins-slave1:8443|g' docs/openapiv3_0.json

# Move jar to workdir and copy classes for JaCoCo
RUN mv target/${JAR} ./fatjar.jar
RUN cp -r target/classes/ ./built-classes

# downloading JaCoCo JAR to run with the server JAR in javaagent mode
RUN wget https://repo1.maven.org/maven2/org/jacoco/jacoco/0.8.11/jacoco-0.8.11.zip -O /tmp/jacoco.zip
RUN apt update && apt install -y unzip && rm -rf /var/lib/apt/lists/*
RUN unzip /tmp/jacoco.zip -d /tmp/jacoco
RUN apt remove -y unzip && rm /tmp/jacoco.zip

EXPOSE 8080 8443

# not creating a non-root user since we want to use `exec` and run maven commands. It's complicated to
# do it using a non-root user - https://github.com/carlossg/docker-maven?tab=readme-ov-file#running-as-non-root-not-supported-on-windows
