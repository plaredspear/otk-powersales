# Terraform IaC for `dev-otk-pwrs`

AWS Console 로 수동 구축된 dev 인프라를 Terraform 으로 관리합니다.

## 범위

- **관리 대상**: `dev-otk-pwrs-*` prefix 리소스 (VPC, RDS, ElastiCache, EB, ECR, S3, CloudFront, CodePipeline, IAM, Route 53 레코드, ACM 등)
- **관리 대상 아님**: Prod 환경, `dev-pwrs-admin`/`dev-pwrs-api` 관련 리소스 (이름에 `dev-otk-pwrs` 없음), `codapt.kr` 호스팅 영역, CodeConnections 자체, EB Service Role (모두 data source 로 참조)

## 사전 조건

- AWS CLI 프로필 `dev-codapt` (계정 `983241034734`) 로 접근 가능해야 함
- Terraform `>= 1.5.0` (import block 지원 필요)
- `terraform.tfvars` 파일 생성 (`terraform.tfvars.example` 복사)

## 최초 셋업

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# 필요 시 terraform.tfvars 값 수정

terraform init
terraform plan
```

## Import 절차 (콘솔 → Terraform state)

Terraform 1.5+ `import` 블록 방식을 사용합니다. 카테고리별로 진행:

1. `imports.tf` 에 `import { to = ... id = ... }` 블록 추가
2. `terraform plan -generate-config-out=_gen.tf` 로 HCL 자동 생성
3. `_gen.tf` 내용을 적절한 `.tf` 파일로 이동 + 변수/locals/참조 정리
4. `terraform plan` 결과가 "No changes." 될 때까지 반복
5. `import` 블록 제거

## State 관리

- **Local state** 사용 (`terraform.tfstate`)
- `.gitignore` 에 `*.tfstate*`, `*.tfvars`, `.terraform/` 등록됨 → **절대 커밋 금지**
- 팀 협업 시 S3+DynamoDB remote state 로 이전 필요

## 비밀값

- **RDS 마스터 비밀번호**: `manage_master_user_password = true` 로 AWS 가 자동 생성 → Secrets Manager 에 저장. 시크릿 ARN 은 `aws_db_instance.main.master_user_secret[0].secret_arn` 으로 참조. 애플리케이션은 IAM 권한을 통해 Secrets Manager 에서 읽어야 함
- **EB 환경변수 중 비밀값**: 해당 `setting` 만 `ignore_changes` 로 제외
- `terraform.tfvars` 는 비밀을 담을 수 있으므로 절대 커밋하지 않음

## Destroy → Apply 재현성

| 항목 | 재현됨 | 비고 |
|------|--------|------|
| VPC CIDR, 서브넷, SG 규칙, IAM 정책, RDS/ElastiCache 사양 | ✅ | 구성 완전 일치 |
| EB option_settings, CloudFront 동작, S3 설정 | ✅ | Terraform 코드가 SSoT |
| 자동 생성 ID (VPC ID, CloudFront distribution ID, RDS endpoint 등) | ❌ | 매번 신규 발급 |
| RDS 데이터, S3 파일, ECR 이미지 | ❌ | **데이터 손실** (스냅샷 없으면 복구 불가) |
| ACM 인증서 ARN | ❌ | 신규 발급 + DNS 재검증 대기 (5–30분) |

**권장**: 전체 `terraform destroy` 는 피하고, 재현성 검증은 별도 계정/리전에 apply 하거나 `-target` 으로 개별 리소스만 재생성 테스트.

**주의**: RDS `deletion_protection = false`, 모든 S3 버킷 `force_destroy = true`. `terraform destroy` 실행 시 **버킷 내 객체(런타임 데이터 포함)가 경고 없이 전부 삭제**되므로 실행 전 반드시 필요한 데이터를 백업하거나 별도 계정으로 복제할 것.

## 자주 쓰는 명령

```bash
terraform plan                                   # 변경사항 미리보기
terraform plan -generate-config-out=_gen.tf      # import 대상 HCL 자동 생성
terraform apply                                  # 변경 적용
terraform state list                             # import 된 리소스 목록
terraform state rm <addr>                        # state 에서만 제거 (AWS 리소스 유지)
terraform state show <addr>                      # 특정 리소스 state 세부 보기
```
