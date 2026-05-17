# SF 데이터 마이그레이션 프레임워크 (Spec #764)

Salesforce 운영 조직의 사용자 / 조직 / 권한 데이터를 신규 시스템의
PostgreSQL 테이블로 1회 cut-over 이전하기 위한 **재사용 가능한 ETL 프레임워크**.

- 언어: Kotlin Script (`.main.kts`) + bash
- 실행: 사용자 직접 명령어 실행 (자동 실행 없음)
- 적용: Kotlin 이 **JDBC 로 직접 INSERT/UPDATE** (psql 단계 없음)
- 런칭 전까지 다회성 사용 / 런칭 후 미사용 (디렉토리 폐기 검토)
- 스키마 변경 (staging 테이블 등) 만 Flyway 활용 (예: `V152__create_sf_permission_set_assignment_raw.sql`)

## 디렉토리 구조

```
scripts/sf-data-migration/
├── README.md                                 # 본 파일
├── extract-csv.sh                            # SF CLI 기반 CSV 추출 (사전 verify 자동 호출)
├── verify-metadata.main.kts                  # EntityMetadata ↔ backend @SFField 정합 검증
├── generate-metadata.main.kts                # backend Entity → EntityMetadata + SOQL 자동 생성기
├── db.properties.template                    # DB 연결 정보 템플릿 (커밋)
├── db.properties                             # 실제 DB 연결 정보 (gitignore)
├── common.kts                                # 공통 모듈 (EntityMetadata, TARGET_SPECS, helper, ProgressBar, JDBC 로더)
├── migrate-stage1.main.kts                   # Stage 1 진입점 — CSV → JDBC INSERT
├── migrate-stage2.main.kts                   # Stage 2 진입점 — 매핑/Transform/Reset (JDBC)
│
├── input/                                    # CSV 입력 (gitignore)
└── output/                                   # 리포트 (gitignore)
    ├── migration_report_stage1.txt
    └── migration_report_stage2.txt
```

### Stage 책임 분리

| Stage | 책임 | 진입점 | 트랜잭션 |
|-------|-----|--------|---------|
| **Stage 1** | CSV → staging JDBC INSERT (매핑 SF 원본 그대로) | `migrate-stage1.main.kts` | target 별 |
| **Stage 2-B** | 한글 picklist → enum (role / ppt / profile_type) | `migrate-stage2.main.kts --substep=role / --substep=ppt / --substep=profile` | substep 별 |
| **Stage 2-C** | BCrypt password hash | `migrate-stage2.main.kts --substep=password` | substep 별 |
| **Stage 2-D** | PermissionSet → AdminPermission | `migrate-stage2.main.kts --substep=permission` | substep 별 |
| **Reset**     | dev DB 초기화 | `migrate-stage2.main.kts --reset` | 단일 |

각 substep 은 독립 transaction → 일부 실패 시 다른 substep 의 적용 결과 유지. 실패한 substep 만 재실행 가능.

> employee ↔ organization 관계는 신규 시스템(과 SF 레거시 모두)에서 FK 가 아닌 `cost_center_code` 기반 application-level join 으로 처리되므로 ETL 에 FK 업데이트 단계가 존재하지 않습니다.

### 지원 target 일람 (39종)

backend 의 `@SFObject` 어노테이션이 붙은 모든 entity + Permission staging table. 전체 권위 출처는
`common.kts` 의 `TARGET_SPECS` 정의.

**Stage 2 변환 substep 보유 target** (그 외 target 은 Stage 1 raw 적재만):

| target | SF SObject | Stage 2 substep |
|--------|-----------|-----------------|
| `Employee` | `DKRetail__Employee__c` | `--substep=role` (한글 → UserRole) + `--substep=ppt` (한글 → PPT enum) |
| `User` | `User` | `--substep=profile` (한글 → ProfileType) + `--substep=password` (BCrypt) |
| `Permission` | `PermissionSetAssignment` | `--substep=permission` (PermSet → AdminPermission) |

**Stage 1 raw 적재만 수행하는 target** (36종):

`Organization`, `Account`, `Product`, `Promotion`, `Group`, `Notice`,
`AccountCategoryMaster`, `AgreementHistory`, `AgreementWord`, `AlternativeHoliday`,
`Appointment`, `AttendanceLog`, `AttendInfo`, `BranchReview`, `Claim`,
`DisplayWorkSchedule`, `EmployeeInputCriteriaMaster`, `ErpOrder`, `ErpOrderProduct`,
`HolidayMaster`, `HqReview`, `InspectionTheme`,
`MonthlyFemaleEmployeeIntegrationSchedule`, `MonthlySalesHistory`, `NewProduct`,
`OrderRequest`, `OrderRequestProduct`, `ProductBarcode`,
`ProfessionalPromotionTeamHistory`, `ProfessionalPromotionTeamMaster`,
`PromotionEmployee`, `PushMessage`, `PushMessageReceiver`, `StaffReview`,
`TeamMemberSchedule`, `UploadFile`

### entity 추가/변경 시 자동화 도구

backend 의 신규 `@SFObject` entity 추가 시:

```bash
# 자동 생성 — common.kts 추가 코드 + extract-csv.sh SOQL 모두 출력
kotlin scripts/sf-data-migration/generate-metadata.main.kts <entity-rel-path>

# 예: 신규 SomeNew entity 의 metadata + SOQL 생성
kotlin scripts/sf-data-migration/generate-metadata.main.kts some/entity/SomeNew

# 출력을 common.kts / extract-csv.sh 에 수동 통합 후 검증
kotlin scripts/sf-data-migration/verify-metadata.main.kts
```

## 사전 준비

### 1. Kotlin CLI 설치

```bash
brew install kotlin   # macOS
# 또는 SDKMAN: sdk install kotlin
```

설치 후 `kotlin -version` 출력 확인 (1.9+ 필요).

### 2. SF CLI 설치 + 인증

```bash
npm i -g @salesforce/cli
sf org login web --alias otoki-prod
sf org list   # 인증 확인
```

### 3. DB 연결 정보 설정 (db.properties)

```bash
cd scripts/sf-data-migration
cp db.properties.template db.properties

# db.properties 편집 — host / port / database / user / password 채우기
```

dev 환경 예시:
```properties
host=localhost
port=15432
database=otoki
schema=powersales
user=otkadmin
password=<scripts/db-tunnel.sh -s dev --password 결과>
```

**사전 조건**: `scripts/db-tunnel.sh -s dev` 로 SSM 터널이 열려 있어야 dev DB 접속 가능.

`db.properties` 는 `.gitignore` 처리됨 (커밋되지 않음).

### 4. Permission staging 테이블 사전 생성 (Flyway 자동)

Stage 1 의 Permission target 적재 시 `sf_permission_set_assignment_raw` staging 테이블 필요.
본 테이블은 Flyway 마이그레이션 `V152__create_sf_permission_set_assignment_raw.sql` 로 자동 생성 →
backend 정상 배포된 환경이면 별도 작업 불필요.

## 실행

### 1단계 — CSV 추출 (SF CLI)

```bash
cd scripts/sf-data-migration

# 모든 target 추출
./extract-csv.sh --org otoki-prod

# 특정 target 만 (예: 조직만 먼저 확인)
./extract-csv.sh --org otoki-prod --target=Organization

# GroupMember 추출 생략
./extract-csv.sh --org otoki-prod --skip-group-members

# 사전 정합 검증 우회 (불일치 알면서도 강제 추출)
./extract-csv.sh --org otoki-prod --skip-verify
```

**자동 사전 검증**: extract-csv.sh 는 시작 시 `verify-metadata.main.kts` 를 호출하여
`common.kts` 의 `EntityMetadata.fields` 와 backend Entity 의 `@SFField` 어노테이션 set 정합을
확인합니다. 불일치 시 SF 호출 전에 즉시 중단되어 누락된 필드를 정확히 보고합니다.
정합 보강 없이 강제 추출하려면 `--skip-verify` 사용.

수동 검증도 가능:
```bash
kotlin verify-metadata.main.kts
```

산출: `input/` 디렉토리에 CSV 파일들.

### 2단계 — Stage 1 적용 (JDBC INSERT)

```bash
# 전체 target
kotlin migrate-stage1.main.kts

# 특정 target 만
kotlin migrate-stage1.main.kts --target=Organization,Employee
```

옵션:
- `--target=Organization,Account,Product,Promotion,Group,Employee,User,Notice,Permission` (default: 전체)
- `--input-dir=<path>` (default: `./input`)
- `--output-dir=<path>` (default: `./output`) — 리포트 출력 경로

산출:
- DB: 각 target 의 staging row 적재 (ON CONFLICT DO NOTHING — 재실행 안전)
- 파일: `output/migration_report_stage1.txt`

진행 상황은 ASCII progress bar 로 라이브 표시:
```
[##############----------------]  47% (470/1000) Stage 1 Employee (470/1000)
```

### 3단계 — Stage 2 적용 (Logical 처리)

```bash
# 전체 substep (role + ppt + profile + password + permission)
kotlin migrate-stage2.main.kts

# substep 단독 실행 (매핑 보정 후 반복 실행 용이)
kotlin migrate-stage2.main.kts --substep=role --target=Employee
kotlin migrate-stage2.main.kts --substep=ppt --target=Employee
kotlin migrate-stage2.main.kts --substep=profile --target=User
kotlin migrate-stage2.main.kts --substep=password --target=User
kotlin migrate-stage2.main.kts --substep=permission --target=Permission
```

옵션:
- `--substep=all|role|ppt|profile|password|permission` (default: `all`)
- `--target=Organization,Account,Product,Promotion,Group,Employee,User,Notice,Permission` (default: 전체)
- `--input-dir=<path>` (default: `./input`) — password substep 이 users.csv 의 employee_code 읽음
- `--output-dir=<path>` (default: `./output`) — 리포트 출력 경로

산출:
- DB: 각 substep 의 UPDATE/INSERT 적용 (substep 별 transaction)
- 파일: `output/migration_report_stage2.txt`

#### dev DB reset (리허설 반복용)

```bash
# ⚠️ 운영 DB 금지 — db.properties 가 dev 환경인지 사전 확인
kotlin migrate-stage2.main.kts --reset
```

reset 동작:
- TRUNCATE `sf_permission_set_assignment_raw`
- DELETE `user_permission WHERE user_id IN (SELECT user_id FROM "user" WHERE sfid IS NOT NULL)`
- DELETE `user / employee / organization WHERE sfid IS NOT NULL`

### 4단계 — 적용 결과 검증 (사용자)

```bash
# 리포트 확인
cat output/migration_report_stage1.txt
cat output/migration_report_stage2.txt

# DB 직접 확인
PGPASSWORD="..." psql -h localhost -p 15432 -U otkadmin -d otoki -c "
  SELECT COUNT(*) FROM powersales.organization WHERE sfid IS NOT NULL;
  SELECT COUNT(*) FROM powersales.employee WHERE sfid IS NOT NULL;
  SELECT COUNT(*) FROM powersales.\"user\" WHERE sfid IS NOT NULL;
  SELECT permission, COUNT(*) FROM powersales.user_permission GROUP BY 1 ORDER BY 1;
"

# SYSTEM_ADMIN 사번 로그인 + 핵심 페이지 접근 검증 (수동)
```

### 5단계 — cut-over (런칭 시점)

```bash
# 1. db.properties 를 운영 환경으로 전환
vim db.properties   # host=<prod-host>, password=$PROD_OTK_PWRS_DB_PASSWORD 등

# 2. 최종 CSV 추출 (cut-over 작업 윈도우 직전)
./extract-csv.sh --org otoki-prod

# 3. Stage 1 적용
kotlin migrate-stage1.main.kts

# 4. Stage 2 적용
kotlin migrate-stage2.main.kts

# 5. SYSTEM_ADMIN 사번 로그인 + 핵심 페이지 접근 검증
```

⚠️ **운영 적용 결과는 DB 에 영구 보존**되며, CSV/리포트 등 `input/` + `output/` 산출물은 작업 윈도우 종료 후 폐기.

## 매핑 표

모든 매핑 표는 `common.kts` 에 정의됨:

| 매핑 | 상수 | 적용 substep |
|---|---|---|
| AppAuthority (한글) → UserRole | `APP_AUTHORITY_TO_USER_ROLE` | `--substep=role` |
| ProfessionalPromotionTeam (한글) → PPT enum | `PPT_KOREAN_TO_ENUM` | `--substep=ppt` |
| Profile.Name → ProfileType | `PROFILE_NAME_TO_PROFILE_TYPE` | `--substep=profile` |
| PermissionSet → AdminPermission | `PERMISSION_SET_TO_PERMISSIONS` | `--substep=permission` |
| 미사용 PermissionSet skip 목록 | `INTENTIONALLY_SKIPPED_PERMISSION_SETS` | (Stage 2-D 에서 사용) |

### 매핑 보정 → 재실행

```bash
# 1. common.kts 의 매핑 표 편집
vim common.kts

# 2. 해당 substep 만 재실행
kotlin migrate-stage2.main.kts --substep=permission
```

## 마이그레이션 제외 항목 (의도적 미구현)

검증 결과 또는 정책 결정으로 본 프레임워크가 다루지 않는 SF 권한 기능:

| SF 기능 | 제외 사유 | 검증 근거 |
|---|---|---|
| **Field-Level Security (FLS)** | SF org 차단 행 0건 — 실효 미사용 | `FieldPermissions WHERE PermissionsRead = FALSE` 0건 (2026-05-17 확인) |
| **마케팅 채널 PermissionSet (X1/X2/X4)** | 활성 사용자 할당 0명 — 실효 미사용 | `PermissionSetAssignment LIKE 'X%'` 0건 (2026-05-17) |
| **Login IP Range** | 운영 정책 결정 신규 미구현 | Admin/CEO 4개 Profile 한정, 영업사원 무관 |
| **Role Hierarchy 250개** | 신규 UserRole 9종 + cost_center_code 로 축약 대체 | AdminDataScopeService 동등 가시성 제공 |
| **SharingRule / PublicGroup** | SF 빈 태그 — 실효 미미 | retrieve 산출물 |
| **Apex `Profile.Name` 문자열 분기** | 신규 코드에 UserRole enum 분기 존재 | 3건만 사용, 신규 동등 분기 확인 |
| **페이지/탭 접근 제어** | SF Profile pageAccesses 0건, Hidden tabSettings 0건 | retrieve 산출물 |

## JDBC 동작 상세

- **Driver**: `org.postgresql:postgresql:42.7.4` (Kotlin script @file:DependsOn 자동 다운로드)
- **URL 옵션**: `stringtype=unspecified` (boolean/date/timestamp 컬럼에 String 값 자동 추론) + `reWriteBatchedInserts=true` (batch INSERT 를 multi-row 로 재작성)
- **Batch 크기**: 500 (`BATCH_SIZE` 상수, `migrate-stage1.main.kts`)
- **트랜잭션**: autoCommit OFF, 각 target/substep 단위로 commit/rollback
- **멱등성**: Stage 1 = `ON CONFLICT DO NOTHING`, Stage 2 = `WHERE` 조건 (NULL 체크 / 한글 매칭 / password IS NULL 등)
- **연결**: 각 target/substep 단위로 새 Connection — 장시간 idle 회피

## 폐기 절차 (런칭 + 안정화 후)

```bash
git rm -rf scripts/sf-data-migration/
```

매핑 결과는 운영 DB 의 `employee` / `user` / `organization` / `user_permission` 테이블에
적재된 row 로 영구 보존되므로 본 디렉토리는 안전하게 폐기 가능.

폐기 시 함께 삭제 가능 (선택):
- `backend/src/main/resources/db/migration/V152__create_sf_permission_set_assignment_raw.sql` —
  staging 테이블 자체 폐기는 별도 `V{next}__drop_sf_permission_set_assignment_raw.sql`
  마이그레이션 추가로 수행 (Flyway 정합).
