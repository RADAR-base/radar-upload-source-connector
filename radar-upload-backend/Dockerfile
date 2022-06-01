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

FROM --platform=$BUILDPLATFORM gradle:7.4-jdk17 as builder

RUN mkdir /code
WORKDIR /code
ENV GRADLE_USER_HOME=/code/.gradlecache \
    GRADLE_OPTS=-Djdk.lang.Process.launchMechanism=vfork

COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY radar-upload-backend/build.gradle.kts  /code/radar-upload-backend/
RUN gradle downloadDependencies copyDependencies startScripts --no-watch-fs

COPY radar-upload-backend/src /code/radar-upload-backend/src

RUN gradle jar --no-watch-fs

FROM eclipse-temurin:17-jre

MAINTAINER @nivemaham @blootsvoets

LABEL description="RADAR-base data upload connector backend container"

COPY --from=builder /code/radar-upload-backend/build/scripts/* /usr/bin/
COPY --from=builder /code/radar-upload-backend/build/third-party/*.jar /usr/lib/
COPY --from=builder /code/radar-upload-backend/build/libs/radar-upload-backend-*.jar /usr/lib/

EXPOSE 8090
USER 101:101

CMD ["radar-upload-backend"]
