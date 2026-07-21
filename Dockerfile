# ─────────────────────────────────────────────
# Stage 1: Build (Maven + JDK 21)
# ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom first to cache dependency resolution
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Copy source and build (skip tests in this layer; tests run separately)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ─────────────────────────────────────────────
# Stage 2: Runtime (JRE 21)
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S ledgerly && adduser -S ledgerly -G ledgerly

# Copy jar from builder
COPY --from=builder /app/target/ledgerly-backend-*.jar app.jar

# Security: run as non-root
USER ledgerly

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health >/dev/null || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]