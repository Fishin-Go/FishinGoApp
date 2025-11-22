# ---------- 1) BUILD STAGE ----------
FROM eclipse-temurin:21-jdk-alpine AS build

# Limit Gradle memory so it fits in small containers
ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"

WORKDIR /app

# Copy the entire repo into the image
COPY . .

# Make sure the Gradle wrapper is executable
RUN chmod +x ./gradlew

# Build ONLY the backend module
RUN ./gradlew :backend:build --no-daemon -x test

# ---------- 2) RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the backend JAR that you showed in your screenshot
COPY --from=build /app/backend/build/libs/backend.jar app.jar

# Limit JVM memory at runtime too (this one affects your Ktor app)
ENV JAVA_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"

# Render routes HTTP traffic to this port
EXPOSE 8080

# Start the Ktor application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
