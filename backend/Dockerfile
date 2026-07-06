# Stage 1: Build
FROM public.ecr.aws/docker/library/eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Gradle wrapper & build files first (layer caching)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x gradlew

# Copy source and build.
# BuildKit 캐시 마운트로 /root/.gradle 를 빌드 간 보존한다.
# (의존성 jar + Kotlin incremental compile 출력 재사용)
# CodeBuild 에서는 Local Cache > DOCKER_LAYER_CACHE 가 켜져 있어야
# 호스트 재사용 시 이 마운트가 유효하다.
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM public.ecr.aws/docker/library/eclipse-temurin:24-jre AS runtime

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 시각 처리 컨벤션: 단일 KST 서비스 — JVM/컨테이너 TZ 를 Asia/Seoul 로 일관.
# 도메인 코드는 LocalDateTime / LocalDate / LocalTime 으로 KST wall-clock 작성,
# DB 컬럼 TIMESTAMPTZ 가 JDBC 매핑 시 JVM TZ(KST) 기준으로 자동 변환된다.
ENV TZ=Asia/Seoul

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=65.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Duser.timezone=Asia/Seoul \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
