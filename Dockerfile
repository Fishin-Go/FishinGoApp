# ---------- 1) BUILD STAGE ----------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Limit Gradle memory so it fits into Render's container
ENV GRADLE_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"

# Copy the entire repo into the image
COPY . .

# Make sure the Gradle wrapper is executable
RUN chmod +x ./gradlew

# Build ONLY the backend fat JAR (shadowJar), NOT :backend:build
RUN ./gradlew :backend:shadowJar --no-daemon -x test

# ---------- 2) RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the fat JAR that shadowJar produced (backend-all.jar by default)
COPY --from=build /app/backend/build/libs/backend-all.jar app.jar

# Limit JVM memory at runtime too
ENV JAVA_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"

# Render routes HTTP traffic to this port
EXPOSE 8080

# Start the Ktor application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
