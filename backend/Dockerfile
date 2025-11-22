# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk-jammy AS build

# Work from /app in the container
WORKDIR /app

# Copy Gradle wrapper + top-level build files
COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Copy the backend module
COPY backend ./backend

# Make gradlew executable and build ONLY the backend module
# We override JVM args here so we don't need to touch your local gradle.properties
RUN chmod +x ./gradlew && \
    ./gradlew -p backend clean build -x test -x check \
      -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/backend/build/libs/*.jar app.jar

# Port your Ktor/Spring app listens on (change if you use something else)
EXPOSE 8080

# Small heap + metaspace for the running app
ENV JAVA_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"

# Start the server
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
