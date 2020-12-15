# Dockerfile for sabproxy

FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY target/sabproxy*.jar sabproxy.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/sabproxy.jar"]
