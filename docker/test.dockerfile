# Run from project root directory

ARG VERSION="0.0.1-SNAPSHOT"
# Getting GDAL latest image
FROM ghcr.io/osgeo/gdal:ubuntu-small-3.8.5 as gdal-latest

FROM maven:3-eclipse-temurin-21-jammy

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

# Move jar to workdir and copy classes for JaCoCo
RUN mv target/${JAR} ./fatjar.jar
RUN cp -r target/classes/ ./built-classes

# downloading JaCoCo JAR to run with the server JAR in javaagent mode
RUN wget https://repo1.maven.org/maven2/org/jacoco/jacoco/0.8.12/jacoco-0.8.12.zip -O /tmp/jacoco.zip
RUN apt update && apt install -y unzip && rm -rf /var/lib/apt/lists/*
RUN unzip /tmp/jacoco.zip -d /tmp/jacoco
RUN apt remove -y unzip && rm /tmp/jacoco.zip

# Copying binaries from gdal into the eclipse-image
COPY --from=gdal-latest /usr /usr
# ldconfig creates the necessary links and cache to the most recent shared libraries found in the directories specified on the command line
RUN ldconfig

EXPOSE 8080 8443

# not creating a non-root user since we want to use `exec` and run maven commands. It's complicated to
# do it using a non-root user - https://github.com/carlossg/docker-maven?tab=readme-ov-file#running-as-non-root-not-supported-on-windows
