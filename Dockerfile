# Build: compile and package with the Maven wrapper (tests run locally and in CI, not here).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Run: a JRE only, as an unprivileged user. Render supplies PORT; the app reads it.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home kairos
WORKDIR /app
COPY --from=build /app/target/kairos-backend.jar app.jar
USER kairos
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
