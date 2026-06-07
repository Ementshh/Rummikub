# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy the gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Ensure the gradlew script is executable
RUN chmod +x ./gradlew

# Resolve dependencies (this layer will be cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon || return 0

# Copy the source code
COPY src src

# Build the application (creates the executable jar)
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built jar from the builder stage
# Spring boot 3 usually generates a jar with '-SNAPSHOT.jar' suffix
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
