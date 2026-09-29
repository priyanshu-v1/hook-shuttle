# 1. Use the official lightweight Eclipse Temurin Java 25 Alpine image
FROM eclipse-temurin:25-jdk-alpine

# 2. Set the working directory inside the container
WORKDIR /app

# 3. Copy your compiled JAR file from your local target folder into the container
COPY target/webhook-gateway-0.0.1-SNAPSHOT.jar app.jar

# 4. Expose port 8080 (matches your Spring Boot application.properties)
EXPOSE 8080

# 5. Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]