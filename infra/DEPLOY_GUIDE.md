# Terraform 인프라 배포 가이드

이 문서는 `infra/` 디렉토리의 Terraform 코드를 사용하여 AWS Dev Stage를 구축하는 절차를 설명합니다.

---

## 사전 요구사항

### 도구 설치

| 도구 | 최소 버전 | 확인 명령어 |
|------|----------|------------|
| **Terraform** | >= 1.11.0 | `terraform version` |
| **AWS CLI** | v2 | `aws --version` |

Terraform 설치:
```bash
# macOS (Homebrew)
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

### AWS 인증 설정

AWS CLI에 적절한 권한이 설정되어 있어야 합니다.

```bash
# 방법 1: AWS CLI Profile 설정
aws configure --profile otoki-dev
export AWS_PROFILE=otoki-dev

# 방법 2: SSO 사용
aws configure sso
aws sso login --profile otoki-dev
export AWS_PROFILE=otoki-dev

# 인증 확인
aws sts get-caller-identity
```

필요 권한: VPC, ECS, RDS, ALB, ECR, Route53, ACM, Secrets Manager, IAM, S3, CodeBuild, CloudWatch 리소스 생성/관리 권한

---

## Step 1: 사전 준비 사항 확인

Terraform을 적용하기 전에 아래 항목을 준비합니다.

### 1-1. AWS Account ID

```bash
aws sts get-caller-identity --query Account --output text
```

### 1-2. Route53 Hosted Zone

API에 사용할 도메인의 Hosted Zone이 Route53에 존재해야 합니다.

```bash
# 기존 Hosted Zone 확인
aws route53 list-hosted-zones --query 'HostedZones[*].[Name,Id]' --output table
```

Hosted Zone이 없다면 도메인을 Route53에 등록하거나, 기존 도메인의 서브도메인을 위한 Hosted Zone을 생성합니다.

### 1-3. GitLab 프로젝트 정보

GitLab.com 프로젝트에서 다음 정보를 확인합니다:

- **프로젝트 경로**: Settings > General (예: `your-group/otoki`)
- **리포지토리 HTTPS URL**: Clone 버튼에서 확인 (예: `https://gitlab.com/your-group/otoki.git`)

---

## Step 2: Bootstrap — Terraform State 버킷 생성

Terraform state를 저장할 S3 버킷을 먼저 생성합니다. 이 단계는 **최초 1회만** 실행합니다.

```bash
cd infra/bootstrap

terraform init
terraform plan
terraform apply
```

생성되는 리소스:
- S3 버킷: `otoki-terraform-state` (버전 관리, 암호화, 퍼블릭 액세스 차단)

> **참고**: Terraform >= 1.11.0에서는 S3 native locking (`use_lockfile = true`)을 사용하므로 DynamoDB 테이블이 필요 없습니다.

---

## Step 3: 변수 파일 설정

### 3-1. dev.tfvars — 실제 값으로 교체

```bash
cd infra
```

`envs/dev.tfvars` 파일의 placeholder 값을 실제 값으로 교체합니다:

```hcl
# General
project        = "otoki"
environment    = "dev"
region         = "ap-northeast-2"
aws_account_id = "123456789012"              # ← 실제 AWS Account ID

# Networking (그대로 사용 가능)
vpc_cidr             = "10.0.0.0/16"
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]
availability_zones   = ["ap-northeast-2a", "ap-northeast-2c"]

# RDS (그대로 사용 가능)
rds_instance_class    = "db.t3.micro"
rds_engine_version    = "16.6"
rds_allocated_storage = 20
rds_multi_az          = false
db_name               = "otoki"
db_username           = "otoki_admin"

# ECS (그대로 사용 가능)
ecs_task_cpu      = "512"
ecs_task_memory   = "1024"
ecs_desired_count = 1

# DNS — 실제 값으로 교체
domain_name    = "dev-api.yourdomain.com"    # ← 실제 도메인
hosted_zone_id = "Z0123456789ABCDEFGHIJ"     # ← 실제 Hosted Zone ID

# CI/CD — 실제 값으로 교체
gitlab_project_path   = "your-group/otoki"                       # ← 실제 GitLab 프로젝트 경로
gitlab_repository_url = "https://gitlab.com/your-group/otoki.git" # ← 실제 HTTPS clone URL
gitlab_deploy_branch  = "main"
```

### 3-2. secrets.tfvars — 시크릿 생성

```bash
cp envs/secrets.tfvars.example envs/secrets.tfvars
```

`envs/secrets.tfvars` 를 편집합니다:

```hcl
db_password = "YourSecureDbPassword123!"     # ← DB 비밀번호 (영문+숫자+특수문자)
jwt_secret  = "YourJwtSecretKeyHere"         # ← JWT 서명 키
```

> **주의**: `secrets.tfvars` 파일은 `.gitignore`에 포함되어 있어 git에 커밋되지 않습니다. 절대 커밋하지 마세요.

---

## Step 4: Terraform Init — 초기화

```bash
cd infra
make init-dev
```

이 명령은 다음을 수행합니다:
```bash
terraform init -backend-config=envs/dev.backend.hcl -reconfigure
```

- AWS provider 플러그인 다운로드
- S3 remote state backend 연결 (`otoki-terraform-state` 버킷, `dev/terraform.tfstate` 키)
- 모듈 초기화

---

## Step 5: Terraform Plan — 변경사항 사전 확인

```bash
make plan-dev
```

이 명령은 다음을 수행합니다:
```bash
terraform plan -var-file=envs/dev.tfvars -var-file=envs/secrets.tfvars
```

출력에서 확인할 사항:
- `Plan: XX to add, 0 to change, 0 to destroy` — 최초 적용 시 모든 리소스가 `add`
- 에러가 없는지 확인
- 생성될 리소스 목록이 예상과 일치하는지 확인

### 예상 생성 리소스 (~40개)

| 카테고리 | 주요 리소스 |
|---------|-----------|
| **Networking** | VPC, 4 Subnets, IGW, NAT GW, 3 Route Tables, 3 Security Groups |
| **ECR** | Repository, Lifecycle Policy |
| **RDS** | DB Subnet Group, DB Instance |
| **Secrets** | 2 Secrets Manager Secrets + Values |
| **DNS/ACM** | ACM Certificate, DNS Validation Record, Route53 A Record |
| **ALB** | ALB, Target Group, HTTPS Listener, HTTP Listener (redirect) |
| **ECS** | Cluster, Log Group, 2 IAM Roles + Policies, Task Definition, Service |
| **CI/CD** | OIDC Provider, CodeConnection, Source Credential, 2 IAM Roles + Policies, CodeBuild Project, Log Group |

---

## Step 6: Terraform Apply — 리소스 생성

```bash
make apply-dev
```

이 명령은 다음을 수행합니다:
```bash
terraform apply -var-file=envs/dev.tfvars -var-file=envs/secrets.tfvars
```

- 변경사항 요약이 표시됩니다
- `yes` 를 입력하여 승인합니다
- 완료까지 약 **10~15분** 소요 (RDS, NAT Gateway, ACM 검증이 오래 걸림)

### ACM 인증서 DNS 검증 관련 주의사항

ACM 인증서는 DNS 검증을 사용합니다. Terraform이 Route53에 CNAME 레코드를 자동으로 생성하지만, DNS 전파에 시간이 걸릴 수 있습니다. 보통 수 분 이내에 완료되나, 경우에 따라 최대 30분 소요될 수 있습니다.

### Step 6-1: CodeConnections — GitLab 연동 승인 (최초 1회)

Terraform apply가 완료되면, CodeBuild가 GitLab 리포지토리에 접근하기 위한 연동을 수동 승인해야 합니다.

1. **AWS Console** > **Developer Tools** > **Settings** > **Connections**
2. `otoki-dev-gitlab` 연결이 **Pending** 상태로 표시됩니다
3. **Update pending connection** 클릭
4. GitLab 계정으로 로그인하여 **승인(Authorize)** 클릭
5. 연결 상태가 **Available**로 변경되면 완료

> **참고**: 이 승인은 최초 1회만 필요합니다. 승인 후에는 CodeBuild가 GitLab 리포지토리 (private 포함)에서 자동으로 소스코드를 clone할 수 있습니다.

---

## Step 7: Apply 완료 후 출력값 확인

apply가 완료되면 아래 output 값들이 표시됩니다:

```bash
terraform output
```

| Output | 설명 | 용도 |
|--------|------|------|
| `vpc_id` | VPC ID | 참조용 |
| `ecr_repository_url` | ECR 리포지토리 URL | Docker 이미지 푸시 대상 |
| `rds_endpoint` | RDS 접속 주소 | 백엔드 DB 연결 |
| `alb_dns_name` | ALB DNS 이름 | 참조용 |
| `api_url` | API URL (https://...) | API 접속 주소 |
| `nat_gateway_public_ip` | NAT Gateway EIP | **외부 시스템 방화벽 등록용 Outbound IP** |
| `ecs_cluster_name` | ECS 클러스터 이름 | CI/CD 설정 |
| `ecs_service_name` | ECS 서비스 이름 | CI/CD 설정 |
| `gitlab_ci_role_arn` | GitLab CI IAM Role ARN | **GitLab CI/CD 변수로 등록** |
| `codebuild_project_name` | CodeBuild 프로젝트 이름 | **GitLab CI/CD 변수로 등록** |

---

## Step 8: GitLab CI/CD 변수 등록

GitLab 프로젝트의 **Settings > CI/CD > Variables** 에서 다음 변수를 등록합니다:

| 변수명 | 값 | 비고 |
|-------|---|------|
| `DEV_AWS_ROLE_ARN` | `terraform output -raw gitlab_ci_role_arn` 의 출력값 | Protected, Masked |
| `DEV_CODEBUILD_PROJECT` | `terraform output -raw codebuild_project_name` 의 출력값 | Protected |

```bash
# 각 값 확인
terraform output -raw gitlab_ci_role_arn
terraform output -raw codebuild_project_name
```

---

## Step 9: 최초 Docker 이미지 수동 배포

ECS 서비스가 시작되려면 ECR에 Docker 이미지가 존재해야 합니다. 최초 1회는 수동으로 이미지를 푸시합니다.

```bash
# 1. ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url | cut -d/ -f1)

# 2. 프로젝트 루트에서 Docker 이미지 빌드
cd /path/to/otoki/main/backend
docker build -t otoki-backend .

# 3. 태깅
ECR_URL=$(cd /path/to/otoki/main/infra && terraform output -raw ecr_repository_url)
docker tag otoki-backend:latest $ECR_URL:latest

# 4. 푸시
docker push $ECR_URL:latest

# 5. ECS 서비스 강제 재배포 (새 이미지 사용)
aws ecs update-service \
  --cluster $(cd /path/to/otoki/main/infra && terraform output -raw ecs_cluster_name) \
  --service $(cd /path/to/otoki/main/infra && terraform output -raw ecs_service_name) \
  --force-new-deployment
```

---

## Step 10: 배포 검증

### Health Check

```bash
# API URL 확인
API_URL=$(terraform output -raw api_url)

# Health check
curl -s $API_URL/actuator/health | jq .
```

정상 응답:
```json
{
  "status": "UP"
}
```

### AWS Console 확인

- **ECS > Clusters**: 태스크가 `RUNNING` 상태인지 확인
- **RDS > Databases**: 인스턴스가 `Available` 상태인지 확인
- **ALB > Target Groups**: 타겟이 `healthy` 상태인지 확인

---

## 일상 운영

### GitLab CI/CD 자동 배포

위 설정이 완료되면, `main` 브랜치에 push할 때마다 자동으로:
1. GitLab CI가 AWS OIDC 인증
2. CodeBuild 트리거 (해당 커밋 SHA 전달)
3. CodeBuild가 GitLab에서 소스코드 clone (CodeConnections 사용, private repo 지원)
4. Docker 이미지 빌드 → ECR 푸시
5. ECS 서비스 업데이트 (Rolling Deploy)

### 인프라 변경

인프라 변경이 필요한 경우:

```bash
cd infra

# 1. 코드 수정 후 변경사항 확인
make plan-dev

# 2. 적용
make apply-dev
```

### 리소스 전체 삭제

dev 환경 전체를 삭제할 때:

```bash
cd infra
make destroy-dev
```

`yes`를 입력하여 승인합니다.

> **주의**: RDS 데이터가 모두 삭제됩니다. dev 환경에서는 `skip_final_snapshot = true`로 설정되어 있어 스냅샷 없이 바로 삭제됩니다.

> **참고**: Bootstrap에서 생성한 S3 state 버킷은 `prevent_destroy = true`로 보호됩니다. 이 버킷까지 삭제하려면 `infra/bootstrap` 디렉토리에서 별도로 `terraform destroy`를 실행합니다.

---

## 트러블슈팅

### Terraform init 실패: "S3 bucket does not exist"

Bootstrap 단계(Step 2)가 완료되지 않았습니다. `infra/bootstrap`에서 먼저 `terraform apply`를 실행하세요.

### ACM 인증서 검증 대기 (Timeout)

- Route53 Hosted Zone의 네임서버가 도메인에 올바르게 설정되어 있는지 확인
- 서브도메인의 경우, 상위 도메인에 NS 레코드가 올바르게 위임되어 있는지 확인

### ECS 태스크 시작 실패

```bash
# 태스크 실패 사유 확인
aws ecs describe-services \
  --cluster $(terraform output -raw ecs_cluster_name) \
  --services $(terraform output -raw ecs_service_name) \
  --query 'services[0].events[:5]'
```

일반적인 원인:
- ECR에 이미지가 없음 → Step 9 수동 배포 수행
- Secrets Manager 접근 권한 문제 → IAM Role 확인
- 컨테이너 메모리 부족 → `ecs_task_memory` 값 증가

### RDS 연결 실패

- Security Group이 ECS Security Group으로부터의 5432 포트 접근을 허용하는지 확인
- RDS가 Private Subnet에 있으므로 직접 접속 불가 — ECS 컨테이너 로그에서 확인

### CodeBuild 소스 clone 실패

- AWS Console > Developer Tools > Connections에서 연결 상태가 **Available**인지 확인
- 연결이 **Pending** 상태이면 Step 6-1의 수동 승인을 수행
- `gitlab_repository_url`이 올바른 HTTPS clone URL인지 확인 (예: `https://gitlab.com/group/project.git`)
- GitLab 계정의 리포지토리 접근 권한이 유효한지 확인

---

## 파일 구조 요약

```
infra/
├── bootstrap/            # Step 2: 최초 1회 — S3 state 버킷 생성
│   ├── main.tf
│   ├── variables.tf
│   └── outputs.tf
├── modules/              # 재사용 가능한 Terraform 모듈
│   ├── networking/       # VPC, Subnets, NAT, Security Groups
│   ├── ecr/              # Docker 이미지 저장소
│   ├── rds/              # PostgreSQL
│   ├── secrets/          # Secrets Manager
│   ├── alb/              # Application Load Balancer
│   ├── dns/              # ACM 인증서 + DNS 검증
│   ├── ecs/              # ECS Fargate 서비스
│   └── cicd/             # GitLab OIDC + CodeBuild
├── envs/
│   ├── dev.tfvars        # Step 3-1: dev 환경 변수값
│   ├── dev.backend.hcl   # S3 backend 설정
│   └── secrets.tfvars.example  # Step 3-2: 시크릿 템플릿
├── main.tf               # 모듈 호출 + Route53 A 레코드
├── variables.tf          # 변수 선언
├── outputs.tf            # 출력값
├── versions.tf           # Terraform/Provider 버전 제약
├── Makefile              # make init-dev / plan-dev / apply-dev / destroy-dev
└── .gitignore            # secrets.tfvars 등 제외
```
