FROM openjdk:12 as builder

RUN mkdir /code
WORKDIR /code

ENV GRADLE_OPTS -Dorg.gradle.daemon=false
COPY gradle/wrapper /code/gradle/wrapper
COPY ./gradlew ./build.gradle.kts ./settings.gradle /code/
COPY radar-upload-backend/build.gradle.kts  /code/radar-upload-backend/
RUN ./gradlew :radar-upload-backend:downloadDependencies

COPY radar-upload-backend/src /code/radar-upload-backend/src

RUN ./gradlew -Dkotlin.compiler.execution.strategy="in-process" -Dorg.gradle.parallel=false -Pkotlin.incremental=false :radar-upload-backend:distTar \
    && cd radar-upload-backend/build/distributions \
    && tar xf *.tar \
    && rm *.tar radar-upload-backend-*/lib/radar-upload-backend-*.jar

FROM openjdk:12

MAINTAINER @nivemaham @blootsvoets

LABEL description="RADAR-base data upload connector backend container"

COPY --from=builder /code/radar-upload-backend/build/distributions/radar-upload-backend-*/bin/* /usr/bin/
COPY --from=builder /code/radar-upload-backend/build/distributions/radar-upload-backend-*/lib/* /usr/lib/
COPY --from=builder /code/radar-upload-backend/build/libs/radar-upload-backend-*.jar /usr/lib/

EXPOSE 8090

CMD ["radar-upload-backend"]
