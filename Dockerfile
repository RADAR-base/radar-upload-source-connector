# Copyright 2025 The Hyve
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
ARG BASE_IMAGE=radarbase/kafka-connect-transform-keyvalue:8.0.0
FROM --platform=$BUILDPLATFORM gradle:8.9-jdk17 AS builder

RUN mkdir /code
WORKDIR /code

ENV GRADLE_USER_HOME=/code/.gradlecache \
    GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork -Dorg.gradle.vfs.watch=false"

COPY ../../build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY buildSrc /code/buildSrc
COPY kafka-connect-upload-source/build.gradle.kts  /code/kafka-connect-upload-source/

RUN gradle :kafka-connect-upload-source:downloadDependencies kafka-connect-upload-source:copyDependencies

COPY ./kafka-connect-upload-source/src/main/java /code/kafka-connect-upload-source/src/main/java

RUN gradle jar

FROM ${BASE_IMAGE}

USER root

LABEL org.opencontainers.image.authors="@pvannierop"
LABEL description="Kafka Data Upload Source connector"

ENV CONNECT_PLUGIN_PATH=/opt/kafka/plugins

# To isolate the classpath from the plugin path as recommended
COPY --from=builder /code/kafka-connect-upload-source/build/third-party/*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-upload-source/
COPY --from=builder /code/kafka-connect-upload-source/build/libs/kafka-connect-upload-source-*.jar ${CONNECT_PLUGIN_PATH}/kafka-connect-upload-source/
# Copy Sentry monitoring jars.
COPY --from=builder /code/kafka-connect-upload-source/build/third-party/sentry-* /opt/kafka/libs/

USER 1001

