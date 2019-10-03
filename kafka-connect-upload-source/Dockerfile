FROM azul/zulu-openjdk-debian:8 as builder

RUN mkdir /code
WORKDIR /code

ENV GRADLE_OPTS -Dorg.gradle.daemon=false
COPY gradle/wrapper /code/gradle/wrapper
COPY ./gradlew  /code/
RUN ./gradlew --version

COPY ./build.gradle.kts ./settings.gradle /code/
COPY kafka-connect-upload-source/build.gradle.kts  /code/kafka-connect-upload-source/

RUN ./gradlew :kafka-connect-upload-source:downloadDependencies kafka-connect-upload-source:copyDependencies

COPY ./kafka-connect-upload-source/src/ /code/kafka-connect-upload-source/src

RUN ./gradlew jar

FROM confluentinc/cp-kafka-connect-base:5.3.0

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

# create parent directory for storing offsets in standalone mode
RUN mkdir -p /var/lib/kafka-connect-upload-source/logs
