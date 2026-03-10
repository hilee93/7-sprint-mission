FROM amazoncorretto:17 AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon clean bootJar -x test


FROM amazoncorretto:17

WORKDIR /app

ENV TZ=Asia/Seoul \
    PROJECT_NAME=discodeit \
    JVM_OPTS="" \
    SERVER_PORT=8080

EXPOSE 8080

COPY --from=build /app/build/libs/*.jar /app/app.jar

CMD ["sh", "-c", "exec java $JVM_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]
