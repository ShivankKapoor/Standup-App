# --------- Build stage ---------
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build

# Install Maven
USER root
RUN microdnf install -y maven && microdnf clean all

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the Spring Boot JAR
RUN mvn clean package -DskipTests

# --------- Run stage ---------
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest

# curl-minimal is already installed, just install tzdata
USER root
RUN microdnf install -y tzdata && microdnf clean all

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

# Health check (using HTTP)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:5555/health || exit 1

CMD ["java", "-jar", "app.jar"]
