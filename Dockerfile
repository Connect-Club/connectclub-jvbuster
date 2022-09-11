FROM openjdk:11-jdk-slim as builder
WORKDIR /build
ADD . .
RUN ./gradlew clean assemble

FROM openjdk:11-jdk-slim as app
COPY --from=builder /build/build/libs/connectclub-jvbuster-*.jar /connectclub-jvbuster.jar
COPY --from=docker:dind /usr/local/bin/docker /usr/local/bin/

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/connectclub-jvbuster.jar"]
