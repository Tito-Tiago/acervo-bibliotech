# dockerfile
FROM gradle:9.1-jdk21 AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle* ./
COPY src src

# Remover CRLF se existirem, tornar executável e gerar o JAR
RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew clean bootJar -x test --no-daemon

RUN mkdir -p /app/dist \
    && cp build/libs/*.jar /app/dist/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/dist/app.jar ./app.jar

EXPOSE 8090
ENTRYPOINT ["java","-jar","app.jar"]
