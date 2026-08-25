ARG VERSION="0.0.1-SNAPSHOT" 

# Using maven base image in builder stage to build Java code.
FROM maven:3-eclipse-temurin-11-focal as builder

WORKDIR /usr/share/app
COPY pom.xml .
# Downloads all packages defined in pom.xml
RUN mvn clean package
COPY src src
# Build the source code to generate the fatjar
RUN mvn clean package -Dmaven.test.skip=true

# Building GDAL 3.13.2 from source, targeting Ubuntu 20.04 (matches the final
# image below) instead of pulling OSGeo's prebuilt ghcr.io/osgeo/gdal image.
# Why: the app only ever shells out to the ogr2ogr/ogrinfo CLI binaries (see
# CollectionOnboardingProcess/CollectionAppendingProcess/
# TilesOnboardingFromExistingFeatureProcess) -- it never touches GDAL's Python
# bindings. The old approach copied OSGeo's entire /usr tree wholesale
# (COPY --from=gdal-latest /usr /usr), which dragged in Python, pip, and a
# stale system Pillow install nobody used, just to get the GDAL binaries --
# and OSGeo's prebuilt tags for GDAL >=3.13.1 (needed to fix CVE-2026-49014)
# are now built on Ubuntu 26.04, which isn't safely copyable onto this Ubuntu
# 20.04 base without a real glibc ABI risk.
# Building from source here avoids all of that: it's compiled directly
# against this base's own glibc, so there's no cross-OS binary copy at all,
# and it never touches Python, so there's nothing Pillow-related to carry
# over or patch. Verified locally: `ogr2ogr --formats` lists exactly the
# drivers the app uses (PostgreSQL, MVT, GeoJSON, ESRI Shapefile), and a
# Trivy scan of the resulting image shows 0 HIGH/CRITICAL findings.
FROM ubuntu:20.04 as gdal-builder
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential pkg-config ca-certificates curl \
    libproj-dev libgeos-dev libsqlite3-dev libcurl4-openssl-dev \
    libpq-dev libexpat1-dev zlib1g-dev && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Ubuntu 20.04's apt cmake (3.16.3) is too old for GDAL 3.13's build system
# (needs CMake >=3.18 for the check_linker_flag module). Use an official
# Kitware prebuilt binary instead -- this is build-time only and never ships
# in the final image, so it doesn't touch the base OS.
WORKDIR /opt
RUN curl -sSL https://github.com/Kitware/CMake/releases/download/v3.28.3/cmake-3.28.3-linux-x86_64.tar.gz -o cmake.tar.gz && \
    tar xzf cmake.tar.gz && rm cmake.tar.gz
ENV PATH="/opt/cmake-3.28.3-linux-x86_64/bin:${PATH}"

WORKDIR /build
RUN curl -sSL https://github.com/OSGeo/gdal/releases/download/v3.13.2/gdal-3.13.2.tar.gz -o gdal.tar.gz && \
    tar xzf gdal.tar.gz && rm gdal.tar.gz

WORKDIR /build/gdal-3.13.2
RUN cmake -S . -B build \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX=/opt/gdal \
    -DBUILD_APPS=ON \
    -DBUILD_TESTING=OFF \
    -DGDAL_USE_TIFF_INTERNAL=ON \
    -DGDAL_USE_PNG_INTERNAL=ON \
    -DGDAL_USE_JPEG_INTERNAL=ON && \
    cmake --build build -j"$(nproc)" && \
    cmake --install build

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

# Runtime shared libraries GDAL's native binaries link against (verified via
# ldd against the built ogr2ogr). All plain runtime packages, no -dev/build
# tooling, no Python.
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      libproj15 libgeos-c1v5 libgeos-3.8.0 libpq5 libcurl4 libsqlite3-0 \
      libexpat1 libgomp1 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copying only the GDAL binaries/libs/data actually needed (ogr2ogr, ogrinfo,
# etc.) from the from-source build above -- no Python, no Pillow.
COPY --from=gdal-builder /opt/gdal /opt/gdal
ENV PATH="/opt/gdal/bin:${PATH}"
RUN echo "/opt/gdal/lib" > /etc/ld.so.conf.d/gdal.conf && ldconfig

# ---- Download Elastic APM Java Agent ----
RUN curl -sSL -o /usr/share/app/elastic-apm-agent.jar \
    https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.45.0/elastic-apm-agent-1.45.0.jar

EXPOSE 8080 8443
# Creating a non-root user
RUN useradd -r -u 1001 -g root ogc-rs-user
# Create storage directory and make ogc-rs-user as owner
RUN mkdir -p /usr/share/app/storage/temp-dir && chown ogc-rs-user /usr/share/app/storage/temp-dir
# hint for volume mount
VOLUME /usr/share/app/storage/temp-dir
# Setting non-root user to use when container starts
USER ogc-rs-user
