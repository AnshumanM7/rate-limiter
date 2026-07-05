# Stage 1: Build the Spring Boot application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and dependency definitions first to cache Maven downloads
COPY pom.xml .
# Copy source code
COPY src ./src

# Build and package the application as a JAR, skipping test execution for CI/CD speed
RUN mvn clean package -DskipTests

# Stage 2: Create a secure, lightweight runtime container
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=build /app/target/rate-limiter-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (the default server port configured in Spring Boot)
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
