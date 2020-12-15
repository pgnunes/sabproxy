# Dockerfile for sabproxy

FROM openjdk:11-jdk-slim
VOLUME /tmp
ARG JAR_FILE
COPY target/sabproxy*.jar sabproxy.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/sabproxy.jar"]
