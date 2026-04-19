# Stage 1: Build
FROM public.ecr.aws/docker/library/eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Gradle wrapper & build files first (layer caching)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code & build
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM public.ecr.aws/docker/library/eclipse-temurin:24-jre AS runtime

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
