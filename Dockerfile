FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN mkdir -p /app/logs && chmod 755 /app/logs

COPY target/backend-1.0.0-jar-with-dependencies.jar marlin-backend.jar

ENV ENABLE_FILE_LOGGING=true
ENV LOG_DIR=/app/logs

ENTRYPOINT ["java", "-jar", "marlin-backend.jar"]