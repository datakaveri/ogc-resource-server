ARG VERSION="0.0.1-SNAPSHOT"

# Using maven base image in builder stage to build Java code.
FROM maven:3-eclipse-temurin-11 as builder

WORKDIR /usr/share/app
COPY pom.xml .
# Downloads all packages defined in pom.xml
RUN mvn clean package
COPY src src
# Build the source code to generate the fatjar
RUN mvn clean package -Dmaven.test.skip=true

# Java Runtime as the base for final image
FROM eclipse-temurin:11-jre-focal

ARG VERSION
ENV JAR="ogc-resource-server-dev-${VERSION}-fat.jar"

WORKDIR /usr/share/app
# Copying openapi docs 
COPY docs docs
COPY iudx-pmd-ruleset.xml iudx-pmd-ruleset.xml
COPY google_checks.xml google_checks.xml

# Copying dev fatjar from builder stage to final image
COPY --from=builder /usr/share/app/target/${JAR} ./fatjar.jar

EXPOSE 8080 8443
# Creating a non-root user
RUN useradd -r -u 1001 -g root ogc-rs-user
# Create storage directory and make ogc-rs-user as owner
RUN mkdir -p /usr/share/app/storage/temp-dir && chown ogc-rs-user /usr/share/app/storage/temp-dir
# hint for volume mount 
VOLUME /usr/share/app/storage/temp-dir
# Setting non-root user to use when container starts
USER ogc-rs-user