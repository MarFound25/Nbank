FROM maven:3.9.9-eclipse-temurin-22

ARG TEST_PROFILE=api
ARG BASEAOIURL=http://localhost:4111
ARG BASEUIURL=http://localhost:3000

ENV TEST_PROFILE=${TEST_PROFILE}
ENV BASEAOIURL=${BASEAOIURL}
ENV BASEUIURL=${BASEUIURL}

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY . .


USER root

CMD /bin/bash -c " \
    mkdir -p /app/logs ; \
    { \
    echo '>>> Running tests with profile: ${TEST_PROFILE}' ; \
    mvn test -q -P ${TEST_PROFILE} ; \
    \
    echo '>>> Running surefire-report:report' ; \
    mvn -DskipTests=true surefire-report:report ; \
   } > /app/logs/run.log 2>&1"
