# ===================================================================
# Stage 1: Build the application JAR using Maven and Java 21
# ===================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and package executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===================================================================
# Stage 2: Minimal Production Runtime Image (Java 21 JRE on Alpine)
# ===================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Upgrade base OS packages to pull official vendor security patches
RUN apk update && apk upgrade --no-cache

# Create dedicated non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Create upload directory and grant permissions to non-root user
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy packaged JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/shopsphere-backend-*.jar /app/app.jar

# Switch to non-root user
USER appuser:appgroup

# Expose backend service port
EXPOSE 8080

# Environment defaults for containerized runtime
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    APP_UPLOAD_DIR=/app/uploads \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Health check probing Actuator liveness/readiness endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Launch Spring Boot application with JVM container options
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
