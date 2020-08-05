FROM gradle:6.5.1-jdk11 as builder

RUN mkdir /code
WORKDIR /code

COPY ./build.gradle.kts ./settings.gradle /code/
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

COPY --from=builder /code/radar-upload-backend/build/distributions/radar-upload-backend-*/bin/* /usr/bin/
COPY --from=builder /code/radar-upload-backend/build/distributions/radar-upload-backend-*/lib/* /usr/lib/
COPY --from=builder /code/radar-upload-backend/build/libs/radar-upload-backend-*.jar /usr/lib/

EXPOSE 8090

CMD ["radar-upload-backend"]
