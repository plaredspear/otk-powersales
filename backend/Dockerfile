# Stage 1: Build
FROM public.ecr.aws/docker/library/eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Gradle wrapper & build files first (layer caching)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

# Copy source and build.
# BuildKit 캐시 마운트로 /root/.gradle 를 빌드 간 보존한다.
# (의존성 jar + Kotlin incremental compile 출력 재사용)
# CodeBuild 에서는 Local Cache > DOCKER_LAYER_CACHE 가 켜져 있어야
# 호스트 재사용 시 이 마운트가 유효하다.
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew test bootJar --no-daemon

# Stage 2: Runtime
FROM public.ecr.aws/docker/library/eclipse-temurin:24-jre AS runtime

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 시각 처리 컨벤션 (스펙 #564): 컨테이너 timezone 을 UTC 로 명시.
# JVM `user.timezone` 도 명시하여 베이스 이미지 / 호스트 변동에 무관하게 UTC 보장.
ENV TZ=UTC

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Duser.timezone=UTC \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
