# CI/CD 파이프라인 구축 가이드: GitHub → CodeBuild → ECR

## 개요
Spring Boot 4.0.5 Kotlin 백엔드 애플리케이션을 AWS ECS로 배포하기 위한 사전 단계로,
GitHub 연동 → CodeBuild → ECR 이미지 푸시 파이프라인을 구축한다.

AWS 리소스는 **콘솔**을 통해 생성하고, 코드 파일만 직접 구현한다.

### 파이프라인 흐름
```
main 브랜치 push → GitHub webhook → CodeBuild 시작
  → ECR 로그인 → Docker 멀티스테이지 빌드 → ECR push
    → (향후) ECS에서 이미지 pull & 배포
```

---

## Phase 1: Git 초기화 & GitHub 연동

### Step 1.1 — `.gitignore` 업데이트
기존 `.gitignore` 하단에 추가:
```
### Docker ###
Dockerfile.local
docker-compose*.yml

### AWS ###
.aws/

### Environment ###
.env
.env.*
```

### Step 1.2 — Git 초기화 & 첫 커밋
```bash
cd /Users/youngsoolim/dev/codapt/otoki-code-build/backend
git init
git add .
git commit -m "Initial commit: Spring Boot 4.0.5 Kotlin project"
```

### Step 1.3 — GitHub 저장소 생성 & 푸시
**방법 A — GitHub CLI:**
```bash
gh repo create codapt/otoki-code-build-backend --private --source=. --remote=origin
git push -u origin main
```

**방법 B — GitHub 콘솔:**
1. github.com에서 새 Private 저장소 생성
2. 로컬에서 연결:
```bash
git remote add origin git@github.com:codapt/otoki-code-build-backend.git
git branch -M main
git push -u origin main
```

---

## Phase 2: Dockerfile 작성

### Step 2.1 — `Dockerfile` 생성 (멀티 스테이지 빌드)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Gradle wrapper & 빌드 파일 먼저 복사 (레이어 캐싱)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# 의존성 다운로드 (캐시 레이어)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 & 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:24-jre AS runtime

# 보안을 위한 non-root 유저
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 컨테이너 환경 JVM 최적화
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**설계 포인트:**
- `eclipse-temurin:24` — 프로젝트의 Java 24에 맞춤
- 레이어 캐싱 — Gradle 파일을 소스보다 먼저 복사하여 의존성 캐시 활용
- non-root 유저 — 컨테이너 보안 강화
- `-XX:+UseContainerSupport` — JVM이 컨테이너 메모리/CPU 제한을 인식 (ECS 필수)
- `-XX:MaxRAMPercentage=75.0` — 힙에 75% 할당, 나머지는 metaspace/스레드용

### Step 2.2 — `.dockerignore` 생성

```
.git
.gradle
.idea
.vscode
.kotlin
build/
*.iml
*.iws
*.ipr
HELP.md
```

### Step 2.3 — 로컬 검증
```bash
docker build -t demo:local .
docker run --rm -p 8080:8080 demo:local
# 확인: curl http://localhost:8080
```

### Step 2.4 — 커밋 & 푸시
```bash
git add Dockerfile .dockerignore
git commit -m "Add multi-stage Dockerfile for containerized deployment"
git push origin main
```

---

## Phase 3: AWS ECR 저장소 생성 (콘솔)

### Step 3.1 — ECR 리포지토리 생성
1. AWS 콘솔 > **ECR > Repositories > Create repository**
2. 설정:
   - **Repository name:** `otoki/demo-backend`
   - **Image tag mutability:** Mutable
   - **Scan on push:** Enabled (취약점 자동 스캔)
   - **Encryption:** AES-256

### Step 3.2 — Lifecycle Policy 설정
1. 생성된 리포지토리 선택 > **Lifecycle policy > Create rule**
2. 규칙: 최근 **10개** 이미지만 유지 (비용 관리)
   - Match criteria: Image count more than 10
   - Action: expire

### Step 3.3 — ECR URI 기록
생성 후 표시되는 URI를 메모:
```
<ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/otoki/demo-backend
```
이 URI는 Phase 4의 buildspec.yml에서 사용된다.

---

## Phase 4: AWS CodeBuild 설정

### Step 4.1 — `buildspec.yml` 생성

```yaml
version: 0.2

env:
  variables:
    AWS_DEFAULT_REGION: "ap-northeast-2"
    IMAGE_REPO_NAME: "otoki/demo-backend"
  exported-variables:
    - IMAGE_TAG
    - IMAGE_URI

phases:
  pre_build:
    commands:
      - echo "Logging in to Amazon ECR..."
      - ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
      - ECR_URI="${ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $ECR_URI
      - IMAGE_TAG="${CODEBUILD_RESOLVED_SOURCE_VERSION:0:8}-$(date +%Y%m%d%H%M%S)"
      - IMAGE_URI="${ECR_URI}/${IMAGE_REPO_NAME}:${IMAGE_TAG}"
      - echo "Image will be tagged as ${IMAGE_URI}"

  build:
    commands:
      - echo "Building Docker image..."
      - docker build -t $IMAGE_REPO_NAME:$IMAGE_TAG .
      - docker tag $IMAGE_REPO_NAME:$IMAGE_TAG $IMAGE_URI
      - docker tag $IMAGE_REPO_NAME:$IMAGE_TAG "${ECR_URI}/${IMAGE_REPO_NAME}:latest"

  post_build:
    commands:
      - echo "Pushing Docker image to ECR..."
      - docker push $IMAGE_URI
      - docker push "${ECR_URI}/${IMAGE_REPO_NAME}:latest"
      - echo "Image pushed successfully - ${IMAGE_URI}"
```

**설계 포인트:**
- **이미지 태그:** commit SHA 앞 8자 + timestamp (예: `a1b2c3d4-20260409143022`) — 추적 가능하고 유니크
- **듀얼 태깅:** 고유 태그 + `latest` 동시 푸시
- **exported-variables:** 향후 CodePipeline에서 ECS 배포 시 활용 가능

### Step 4.2 — IAM 역할 생성 (콘솔)
1. **IAM > Roles > Create role**
   - Trusted entity type: **AWS service**
   - Use case: **CodeBuild**
   - Role name: `codebuild-otoki-demo-backend-role`

2. **인라인 정책 추가** (Create inline policy > JSON):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "arn:aws:ecr:<REGION>:<ACCOUNT_ID>:repository/otoki/demo-backend"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:<REGION>:<ACCOUNT_ID>:log-group:/aws/codebuild/*"
    }
  ]
}
```
> `<REGION>`과 `<ACCOUNT_ID>`를 실제 값으로 교체

### Step 4.3 — CodeBuild 프로젝트 생성 (콘솔)
1. **CodeBuild > Create build project**
2. 설정:

| 항목 | 값 |
|---|---|
| Project name | `otoki-demo-backend` |
| Source provider | GitHub |
| Repository | `codapt/otoki-code-build-backend` (OAuth 연결) |
| Environment image | Managed image |
| Operating system | Amazon Linux |
| Runtime | Standard |
| Image | `aws/codebuild/amazonlinux-x86_64-standard:5.0` |
| Compute | `BUILD_GENERAL1_MEDIUM` (7GB RAM, 4 vCPU) |
| **Privileged** | **체크 (Docker 빌드 필수)** |
| Service role | `codebuild-otoki-demo-backend-role` |
| Buildspec | Use a buildspec file (`buildspec.yml`) |
| CloudWatch Logs | `/aws/codebuild/otoki-demo-backend` |

> **주의:** Privileged mode를 반드시 활성화해야 Docker 빌드가 가능합니다.
> `BUILD_GENERAL1_MEDIUM`을 권장합니다 — Gradle + Kotlin 빌드는 메모리를 많이 사용합니다.

### Step 4.4 — Webhook 설정 (콘솔)
CodeBuild 프로젝트 편집 > Primary source webhook events:
- Event type: **PUSH**
- Branch filter: `^refs/heads/main$`

→ main 브랜치 push 시 자동 빌드 트리거

### Step 4.5 — 커밋 & 푸시
```bash
git add buildspec.yml
git commit -m "Add AWS CodeBuild buildspec for ECR image pipeline"
git push origin main
```

---

## Phase 5: 검증

### Step 5.1 — 수동 빌드 트리거
- CodeBuild 콘솔 > 프로젝트 선택 > **Start build**
- 빌드 로그에서 각 phase 성공 여부 확인

### Step 5.2 — ECR 이미지 확인
- ECR 콘솔 > `otoki/demo-backend` > Images
- 이미지 태그와 push 시간 확인

### Step 5.3 — ECR 이미지 로컬 실행
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com

# Pull & 실행
docker pull <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/otoki/demo-backend:latest
docker run --rm -p 8080:8080 <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/otoki/demo-backend:latest
```

### Step 5.4 — Webhook 자동 트리거 검증
1. 코드에 작은 변경 후 `git push origin main`
2. CodeBuild 콘솔에서 빌드가 자동으로 시작되는지 확인
3. 완료 후 ECR에 새 이미지가 push 되었는지 확인

---

## 생성/수정 파일 요약

| 파일 | 작업 | Phase |
|---|---|---|
| `.gitignore` | 수정 (Docker/AWS 항목 추가) | 1 |
| `Dockerfile` | 신규 생성 | 2 |
| `.dockerignore` | 신규 생성 | 2 |
| `buildspec.yml` | 신규 생성 | 4 |

## AWS 콘솔 작업 요약

| 리소스 | 서비스 | Phase |
|---|---|---|
| ECR 리포지토리 `otoki/demo-backend` | ECR | 3 |
| Lifecycle policy | ECR | 3 |
| IAM 역할 `codebuild-otoki-demo-backend-role` | IAM | 4 |
| CodeBuild 프로젝트 `otoki-demo-backend` | CodeBuild | 4 |
| GitHub webhook | CodeBuild | 4 |
