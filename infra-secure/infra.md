# otoki-code-build AWS 인프라 가이드 — 통합 개요

> 이 문서는 Backend와 Web 인프라의 **전체 조감도**를 제공한다.
> 각 서비스의 상세 구축 절차는 아래 문서를 참고한다.
>
> - Backend: [`infra-backend.md`](infra-backend.md)
> - Web (Frontend): [`infra-web.md`](infra-web.md)

---

## 프로젝트 개요

| 항목     | 내용                                        |
| -------- | ------------------------------------------- |
| 저장소   | GitHub `codapt/otoki-code-build` (모노레포) |
| Backend  | Kotlin + Spring Boot 4.0.5 (Java 24)        |
| Frontend | React 19 + TypeScript 6 + Vite 8 (CSR)      |
| Mobile   | 향후 추가 예정 (Backend API 공유)           |
| 리전     | `ap-northeast-2` (서울)                     |
| Stage    | dev, prod                                   |

---

## 아키텍처 다이어그램

```
                       GitHub (codapt/otoki-code-build)
                         브랜치 push
                      ┌──────────┴──────────┐
                      ▼                     ▼
               CodePipeline (backend)  CodePipeline (web)
               ┌─────┼─────┐               │
            Source  Build  Deploy         Source
                    │                    Build+Deploy
                    ▼                       │
                  ECR                       ▼
             Docker image                  S3
                    │                 Static files
                    ▼                       │
            Elastic Beanstalk               ▼
             (Docker, ALB)            CloudFront
                    │                    (CDN)
                    ▼                       │
                  RDS                  Route 53
             (PostgreSQL)               (DNS)
```

```
┌─── VPC (3-Tier Subnet) ─────────────────────────────────┐
│                                                          │
│  Public Subnets (2a, 2c)          ← 외부 트래픽 진입점   │
│  ┌─────────────────┐  ┌───────────────┐                 │
│  │  ALB (EB 연동)   │  │  NAT Gateway  │                 │
│  └────────┬────────┘  └───────────────┘                 │
│           │                                              │
│  App Private Subnets (2a, 2c)     ← 애플리케이션 계층    │
│  ┌────────▼────────┐                                    │
│  │  EB EC2 인스턴스  │                                    │
│  └────────┬────────┘                                    │
│           │                                              │
│  Data Private Subnets (2a, 2c)    ← 데이터 계층         │
│  ┌────────▼────────┐                                    │
│  │  RDS (PG 17)    │                                    │
│  └─────────────────┘                                    │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 셋업 순서

> 의존성에 따라 반드시 아래 순서대로 진행한다. **Backend를 먼저 완료한 후 Web을 진행한다.**

```
[Backend — infra-backend.md]
1. VPC & 네트워킹
2. IAM (Backend)             ← EB EC2 Role, CodeBuild Role, CodePipeline Role
3. RDS (PostgreSQL)          ← VPC, Security Group 필요
4. ECR
5. Elastic Beanstalk         ← VPC, IAM, RDS endpoint, ECR 이미지 필요
6. CI/CD (Backend)           ← CodePipeline + CodeBuild (EB 앱/환경 이름 필요)

[Web — infra-web.md]
7. S3 + CloudFront
8. IAM (Web)                 ← Web CodeBuild Role + Pipeline Role
9. CI/CD (Web)               ← CodePipeline + CodeBuild (S3, CloudFront ID 필요)

[공통]
10. Route 53 (DNS)            ← EB URL, CloudFront 도메인 필요
11. CloudWatch                ← 모든 리소스 생성 후
```

---

## 네이밍 컨벤션

### 패턴

```
{stage}-otk-pwrs-{service}
```

### 리소스 이름 전체 목록

| 리소스                             | Dev                                 | Prod                                               |
| ---------------------------------- | ----------------------------------- | -------------------------------------------------- |
| **VPC**                            | `dev-otk-pwrs-vpc`                  | `prod-otk-pwrs-vpc`                                |
| Public Subnet AZ-a                 | `dev-otk-pwrs-public-a`             | `prod-otk-pwrs-public-a`                           |
| Public Subnet AZ-c                 | `dev-otk-pwrs-public-c`             | `prod-otk-pwrs-public-c`                           |
| App Subnet AZ-a                    | `dev-otk-pwrs-app-a`                | `prod-otk-pwrs-app-a`                              |
| App Subnet AZ-c                    | `dev-otk-pwrs-app-c`                | `prod-otk-pwrs-app-c`                              |
| Data Subnet AZ-a                   | `dev-otk-pwrs-data-a`               | `prod-otk-pwrs-data-a`                             |
| Data Subnet AZ-c                   | `dev-otk-pwrs-data-c`               | `prod-otk-pwrs-data-c`                             |
| Internet Gateway                   | `dev-otk-pwrs-igw`                  | `prod-otk-pwrs-igw`                                |
| NAT Gateway                        | `dev-otk-pwrs-nat`                  | `prod-otk-pwrs-nat`                                |
| Route Table (Public)               | `dev-otk-pwrs-public-rt`            | `prod-otk-pwrs-public-rt`                          |
| Route Table (App)                  | `dev-otk-pwrs-app-rt`               | `prod-otk-pwrs-app-rt`                             |
| Route Table (Data)                 | `dev-otk-pwrs-data-rt`              | `prod-otk-pwrs-data-rt`                            |
| NACL (App)                         | `dev-otk-pwrs-app-nacl`             | `prod-otk-pwrs-app-nacl`                           |
| NACL (Data)                        | `dev-otk-pwrs-data-nacl`            | `prod-otk-pwrs-data-nacl`                          |
| **SG** - ALB                       | `dev-otk-pwrs-alb-sg`               | `prod-otk-pwrs-alb-sg`                             |
| **SG** - EB 인스턴스               | `dev-otk-pwrs-eb-sg`                | `prod-otk-pwrs-eb-sg`                              |
| **SG** - RDS                       | `dev-otk-pwrs-rds-sg`               | `prod-otk-pwrs-rds-sg`                             |
| **RDS** 인스턴스                   | `dev-otk-pwrs-db`                   | `prod-otk-pwrs-db`                                 |
| RDS 서브넷 그룹                    | `dev-otk-pwrs-db-subnet-group`      | `prod-otk-pwrs-db-subnet-group`                    |
| **ECR**                            | `dev-otk-pwrs`                      | `prod-otk-pwrs`                                    |
| **EB** Application                 | `dev-otk-pwrs-backend`              | `prod-otk-pwrs-backend`                            |
| **EB** Environment                 | `dev-otk-pwrs-backend-env`          | `prod-otk-pwrs-backend-env`                        |
| **S3** (Frontend)                  | `dev-otk-pwrs-web`                  | `prod-otk-pwrs-web`                                |
| **S3** (Pipeline Artifacts)        | `dev-otk-pwrs-pipeline-artifacts`   | `prod-otk-pwrs-pipeline-artifacts`                 |
| **CloudFront**                     | `dev-otk-pwrs-web-cf`               | `prod-otk-pwrs-web-cf`                             |
| **CodeBuild** (Backend)            | `dev-otk-pwrs-backend-build`        | `prod-otk-pwrs-backend-build`                      |
| **CodeBuild** (Web)                | `dev-otk-pwrs-web-build`            | `prod-otk-pwrs-web-build`                          |
| **CodePipeline** (Backend)             | `dev-otk-pwrs-backend-pipeline`     | `prod-otk-pwrs-backend-pipeline`                   |
| **CodePipeline** (Web)                 | `dev-otk-pwrs-web-pipeline`         | `prod-otk-pwrs-web-pipeline`                       |
| **GitHub 연결** (Connection)           | `otk-pwrs-github`                   | (동일, 공유)                                       |
| **IAM** - EB Service Role              | `aws-elasticbeanstalk-service-role` | (동일, AWS managed)                                |
| **IAM** - EB Instance Profile          | `dev-otk-pwrs-eb-ec2-role`          | `prod-otk-pwrs-eb-ec2-role`                        |
| **IAM** - CodeBuild Role (Backend)     | `dev-otk-pwrs-backend-build-role`   | `prod-otk-pwrs-backend-build-role`                 |
| **IAM** - CodeBuild Role (Web)         | `dev-otk-pwrs-web-build-role`       | `prod-otk-pwrs-web-build-role`                     |
| **IAM** - CodePipeline Role (Backend)  | `dev-otk-pwrs-pipeline-role`        | `prod-otk-pwrs-pipeline-role`                      |
| **IAM** - CodePipeline Role (Web)      | `dev-otk-pwrs-web-pipeline-role`    | `prod-otk-pwrs-web-pipeline-role`                  |
| **SNS** 알림 토픽                  | `dev-otk-pwrs-alerts`               | `prod-otk-pwrs-alerts`                             |

### 태그 정책

모든 리소스에 아래 태그를 부여한다:

| 태그 키   | 값                |
| --------- | ----------------- |
| `Stage`   | `dev` 또는 `prod` |
| `Project` | `otk-pwrs`        |

---

## Route 53 (DNS) — 통합 레코드

> 도메인이 있는 경우에만 해당한다. 호스팅 영역 생성 및 ACM 인증서 상세는 각 문서 참고.

| Record              | Type      | Alias Target                | 문서                  |
| ------------------- | --------- | --------------------------- | --------------------- |
| `dev-powersalesapi.otoki.com` | A (Alias) | EB 환경 URL (Dev)           | `infra-backend.md`    |
| `powersalesapi.otoki.com`     | A (Alias) | EB 환경 URL (Prod)          | `infra-backend.md`    |
| `dev-powersales.otoki.com`    | A (Alias) | CloudFront 배포 도메인 (Dev)  | `infra-web.md`        |
| `powersales.otoki.com`        | A (Alias) | CloudFront 배포 도메인 (Prod) | `infra-web.md`        |

> **웹 브라우저 → API 경로**: `powersales.*` (CloudFront) 의 `/api/*` behavior 가 ALB 로 프록시한다 → same-origin, CORS 불필요.
> **서버→서버 (SAP 등) → API 경로**: `powersalesapi.*` 로 직접 호출 (CloudFront 우회, CORS 무관).

### ACM 인증서 요약

| 용도              | 리전              | 도메인                                                          |
| ----------------- | ----------------- | --------------------------------------------------------------- |
| CloudFront (Web)  | **us-east-1**     | `powersales.otoki.com`, `dev-powersales.otoki.com`              |
| ALB (Backend)     | ap-northeast-2    | `powersalesapi.otoki.com`, `dev-powersalesapi.otoki.com`        |

---

## Dev vs Prod 차이점 요약

| 항목              | Dev                                | Prod                                 |
| ----------------- | ---------------------------------- | ------------------------------------ |
| VPC               | `dev-otk-pwrs-vpc` (`10.0.0.0/16`) | `prod-otk-pwrs-vpc` (`10.1.0.0/16`) |
| 서브넷 구조       | 3-Tier (Public 2 + App 2 + Data 2) | 동일                                 |
| NAT Gateway       | 1개 (단일 AZ)                      | 1개 (단일 AZ, 장애 시 수동 대응)     |
| RDS 인스턴스      | `db.t4g.micro`, Single-AZ          | `db.t4g.small`, Single-AZ            |
| RDS 삭제 방지     | Off                                | **On**                               |
| RDS 백업 보존     | 7일                                | 14일                                 |
| EB 인스턴스       | `t3.small`, Load balanced (1-1)    | `t3.small`, Auto Scaling (2-4)       |
| EB 배포 방식      | All at once                        | Rolling with additional batch        |
| EB 로드밸런서     | ALB + HTTPS                        | ALB + HTTPS                          |
| CloudFront TTL    | 짧게 (또는 항상 invalidation)      | 86400초 (1일)                        |
| CI/CD (Backend)   | CodePipeline (dev push 자동)       | CodePipeline (main push + 수동 승인) |
| CI/CD (Web)       | CodePipeline (dev push 자동)       | CodePipeline (main push + 수동 승인) |
| CloudWatch 알람   | 최소                               | 전체 구성                            |
| 커스텀 도메인     | 선택                               | 필수                                 |
| 예상 월 비용      | ~$50-80                            | ~$200-400                            |

---

## 향후 고려사항

- **WAF**: Prod CloudFront/ALB 앞단에 WAF 적용 (DDoS, SQL Injection 방어)
- **VPC Endpoint**: S3, ECR용 VPC Endpoint로 NAT Gateway 비용 절감
- **Lambda@Edge**: SEO 개선을 위한 서버사이드 렌더링 또는 리다이렉트 처리
- **Mobile**: API Gateway + Lambda 또는 기존 EB Backend API 공유
