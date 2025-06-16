ARG BASE_IMAGE=confluentinc/cp-kafka-connect-base:7.8.1
FROM --platform=$BUILDPLATFORM gradle:8.9-jdk17 AS builder

RUN mkdir /code
WORKDIR /code
ENV GRADLE_USER_HOME=/code/.gradlecache \
    GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork -Dorg.gradle.vfs.watch=false"

COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY buildSrc /code/buildSrc
COPY kafka-connect-upload-source/build.gradle.kts  /code/kafka-connect-upload-source/

RUN gradle :kafka-connect-upload-source:downloadDependencies kafka-connect-upload-source:copyDependencies

COPY ./kafka-connect-upload-source/src/main/java /code/kafka-connect-upload-source/src/main/java

RUN gradle jar

FROM ${BASE_IMAGE}

LABEL org.opencontainers.image.authors="@pvannierop"
LABEL description="Kafka Data Upload Source connector"

ENV CONNECT_PLUGIN_PATH=/usr/share/java/kafka-connect/plugins

# To isolate the classpath from the plugin path as recommended
COPY --from=builder /code/kafka-connect-upload-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-upload-source/
COPY --from=builder /code/kafka-connect-upload-source/build/libs/kafka-connect-upload-source-*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-upload-source/

# Load topics validator
COPY ./kafka-connect-upload-source/src/main/docker/ensure /etc/confluent/docker/ensure

# Load modified launcher
COPY ./kafka-connect-upload-source/src/main/docker/launch /etc/confluent/docker/launch

# Overwrite the log4j configuration to include Sentry monitoring.
COPY ./kafka-connect-upload-source/src/main/docker/log4j.properties.template /etc/confluent/docker/log4j.properties.template
# Copy Sentry monitoring jars.
COPY --from=builder /code/kafka-connect-upload-source/build/third-party/sentry-* /etc/kafka-connect/jars

USER "root"
# create parent directory for storing offsets in standalone mode
RUN mkdir -p /var/lib/kafka-connect-upload-source/logs \
  && chown -R appuser:appuser /var/lib/kafka-connect-upload-source/logs

USER "appuser"
