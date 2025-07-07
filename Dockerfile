FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/backend-1.0.0-jar-with-dependencies.jar marlin-backend.jar

ENTRYPOINT ["java", "-jar", "marlin-backend.jar"]