FROM gradle:6.6.1-jdk11 as builder

RUN mkdir /code
WORKDIR /code
ENV GRADLE_USER_HOME=/code/.gradlecache

COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY kafka-connect-upload-source/build.gradle.kts  /code/kafka-connect-upload-source/

RUN gradle :kafka-connect-upload-source:downloadDependencies kafka-connect-upload-source:copyDependencies

COPY ./kafka-connect-upload-source/src/main/java /code/kafka-connect-upload-source/src/main/java

RUN gradle jar

FROM confluentinc/cp-kafka-connect-base:5.5.2-1-ubi8

MAINTAINER @nivemaham @blootsvoets

LABEL description="Kafka Data Upload Source connector"

ENV CONNECT_PLUGIN_PATH /usr/share/java/kafka-connect/plugins

# To isolate the classpath from the plugin path as recommended
COPY --from=builder /code/kafka-connect-upload-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-upload-source/
COPY --from=builder /code/kafka-connect-upload-source/build/libs/kafka-connect-upload-source-*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-upload-source/

# Load topics validator
COPY ./kafka-connect-upload-source/src/main/docker/kafka-wait /usr/bin/kafka-wait

# Load modified launcher
COPY ./kafka-connect-upload-source/src/main/docker/launch /etc/confluent/docker/launch

USER "root"
# create parent directory for storing offsets in standalone mode
RUN mkdir -p /var/lib/kafka-connect-upload-source/logs \
  && chown -R appuser:appuser /var/lib/kafka-connect-upload-source/logs

USER "appuser"
