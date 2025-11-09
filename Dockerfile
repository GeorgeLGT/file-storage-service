FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN mkdir -p storage

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]