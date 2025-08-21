# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Create data directory for volume mount
RUN mkdir -p /app/data

# Create logs directory
RUN mkdir -p /app/logs

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Expose port 5555
EXPOSE 5555

# Set environment variables
ENV STANDUPS_FILE=/app/data/Standups.txt
ENV WTF_CSRF_TIME_LIMIT=3600

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:5555/health || exit 1

# Run the application
CMD ["java", "-jar", "target/Standup-App-0.0.1-SNAPSHOT.jar"]
