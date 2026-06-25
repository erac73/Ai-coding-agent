FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/agent.jar /app/agent.jar
ENTRYPOINT ["java", "-jar", "/app/agent.jar"]
CMD ["--help"]
