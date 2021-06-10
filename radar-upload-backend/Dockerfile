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

FROM gradle:7.0-jdk11 as builder

RUN mkdir /code
WORKDIR /code
ENV GRADLE_USER_HOME=/code/.gradlecache

COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY radar-upload-backend/build.gradle.kts  /code/radar-upload-backend/
RUN gradle :radar-upload-backend:downloadDependencies

COPY radar-upload-backend/src /code/radar-upload-backend/src

RUN gradle -Dkotlin.compiler.execution.strategy="in-process" -Dorg.gradle.parallel=false -Pkotlin.incremental=false :radar-upload-backend:distTar \
    && cd radar-upload-backend/build/distributions \
    && tar xf *.tar \
    && rm *.tar radar-upload-backend-*/lib/radar-upload-backend-*.jar

FROM openjdk:11-jre-slim

MAINTAINER @nivemaham @blootsvoets

LABEL description="RADAR-base data upload connector backend container"

RUN apt-get update && apt-get install -y \
  curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=builder /code/radar-upload-backend/build/distributions/radar-upload-backend-*/bin/* /usr/bin/
COPY --from=builder /code/radar-upload-backend/build/distributions/radar-upload-backend-*/lib/* /usr/lib/
COPY --from=builder /code/radar-upload-backend/build/libs/radar-upload-backend-*.jar /usr/lib/

EXPOSE 8090

CMD ["radar-upload-backend"]
