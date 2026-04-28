FROM eclipse-temurin:21-jre-alpine AS dev

WORKDIR /app

RUN apk add --no-cache curl

ARG SERVICE_NAME

COPY ${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

ARG SERVICE_NAME

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl ${SERVICE_NAME} -am clean package -DskipTests \
    && JAR_FILE=$(find ${SERVICE_NAME}/target -maxdepth 1 -type f -name "*.jar" ! -name "*.original" ! -name "*sources.jar" ! -name "*javadoc.jar" | head -n 1) \
    && echo "Using JAR: $JAR_FILE" \
    && cp "$JAR_FILE" /tmp/app.jar


FROM eclipse-temurin:21-jre-alpine AS prod

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /tmp/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]