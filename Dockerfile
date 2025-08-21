# --------- Build stage ---------
FROM maven:3.9.6-amazoncorretto-17 AS build

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the Spring Boot JAR
RUN mvn clean package -DskipTests

# --------- Run stage ---------
FROM amazoncorretto:17-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Create data and logs directories
RUN mkdir -p /app/data /app/logs

# Copy the exact built jar by name
COPY --from=build /app/target/Standup-App-0.0.1-SNAPSHOT.jar app.jar

# Copy .env file (IMPORTANT: This file must exist when building!)
COPY .env .env

# Set default environment variables (will be overridden by .env file)
ENV STANDUPS_FILE=/app/data/Standups.txt
ENV WTF_CSRF_TIME_LIMIT=3600

# Expose port 5555
EXPOSE 5555

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:5555/health || exit 1

CMD ["java", "-jar", "app.jar"]
