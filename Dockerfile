# First stage: complete build environment
FROM maven:3.9.7-eclipse-temurin-21 AS builder

ARG PROJECT_PATH
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV PROJECT_PATH=${PROJECT_PATH}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

# Add internal-ca cert
COPY internal-ca.crt /tmp/internal-ca.crt

# Import internal-ca.crt into java truststore if it's not empty
RUN if [ -s /tmp/internal-ca.crt ]; then \
      keytool -importcert -trustcacerts -file /tmp/internal-ca.crt \
      -alias corp-ca -keystore $JAVA_HOME/lib/security/cacerts \
      -storepass changeit -noprompt; \
    fi

# add pom.xml and source code
ADD ./pom.xml pom.xml
ADD ./src src/

# add maven config
ENV MAVEN_CONFIG=/maven/.m2
RUN mkdir -p ${MAVEN_CONFIG}
COPY settings.xml ${MAVEN_CONFIG}/settings.xml

RUN mvn clean package \
  -s ${MAVEN_CONFIG}/settings.xml \
  -Dmaven.test.skip=true

FROM gcr.io/distroless/java21:nonroot
WORKDIR /app
COPY --from=builder target/*.jar /app/app.jar

CMD ["-jar", "/app/app.jar"]
EXPOSE 8080
