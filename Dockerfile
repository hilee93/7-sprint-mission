FROM amazoncorretto:17 AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon clean bootJar -x test
RUN set -e; \
    JAR_PATH="$(ls -1 /app/build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1)"; \
    test -n "$JAR_PATH"; \
    cp "$JAR_PATH" /app/app.jar


FROM amazoncorretto:17

WORKDIR /app

RUN yum install -y curl && yum clean all

ENV TZ=Asia/Seoul \
    PROJECT_NAME=discodeit \
    JVM_OPTS="" \
    SERVER_PORT=8080

EXPOSE 8080

COPY --from=build /app/app.jar /app/app.jar

CMD ["sh", "-c", "exec java $JVM_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]
