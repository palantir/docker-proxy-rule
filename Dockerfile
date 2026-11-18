# Multi-stage build for optimized image size and enhanced security
# Stage 1: Build stage
FROM gradle:8-jdk11-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and dependency files first (for cache optimization)
COPY gradle/ gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle versions.props versions.lock ./

# Download dependencies (separate layer for cache optimization)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY . .

# Build application (excluding tests for faster build)
RUN ./gradlew build -x test -x integrationTest --no-daemon && \
    ./gradlew publishToMavenLocal --no-daemon

# Stage 2: Runtime stage (using Distroless for enhanced security)
FROM gcr.io/distroless/java11-debian11:nonroot

# Add metadata labels
LABEL maintainer="Palantir Technologies" \
      version="1.0" \
      description="Docker Proxy Rule for JUnit Testing" \
      org.opencontainers.image.source="https://github.com/palantir/docker-proxy-rule"

# Run as non-root user (security enhancement)
USER nonroot:nonroot

# Copy built JAR files
COPY --from=builder --chown=nonroot:nonroot /app/docker-proxy-rule-core/build/libs/*.jar /app/lib/
COPY --from=builder --chown=nonroot:nonroot /app/docker-proxy-rule-junit4/build/libs/*.jar /app/lib/
COPY --from=builder --chown=nonroot:nonroot /app/docker-proxy-junit-jupiter/build/libs/*.jar /app/lib/
COPY --from=builder --chown=nonroot:nonroot /app/docker-proxy-rule-core-jdk21/build/libs/*.jar /app/lib/

# Copy example test files for demonstration
COPY --from=builder --chown=nonroot:nonroot /app/docker-proxy-rule-core/src/test/java/com/palantir/docker/proxy/DockerProxySelectorTest.java /app/examples/

# Set working directory
WORKDIR /app

# Expose default port (proxy port)
EXPOSE 1080

# For library projects, provide a way to run example tests
# This demonstrates the library functionality
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-cp", "/app/lib/*"]

# Default command - shows available JARs and library information
CMD ["-cp", "/app/lib/*", "java.lang.Object"]
