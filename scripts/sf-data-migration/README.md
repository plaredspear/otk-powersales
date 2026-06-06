# SF 데이터 마이그레이션 프레임워크 (Spec #764)

Salesforce 운영 조직의 사용자 / 조직 / 권한 데이터를 신규 시스템의
PostgreSQL 테이블로 1회 cut-over 이전하기 위한 **재사용 가능한 ETL 프레임워크**.

- 언어: Kotlin Script (`.main.kts`) + bash
- 실행: 사용자 직접 명령어 실행 (자동 실행 없음)
- 적용: Stage 1 = Kotlin 이 **JDBC 로 직접 INSERT/COPY** (psql 단계 없음) 또는 backend `copy-from-s3` (S3 업로드분) · Stage 2 = backend admin REST 엔드포인트 (FK/picklist/password/hierarchy)
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
├── reset-dev.main.kts                        # dev DB 초기화 (운영 가드 포함)
│
├── input/                                    # CSV 입력 (gitignore)
└── output/                                   # 리포트 (gitignore)
    ├── migration_report_stage1.txt
    └── migration_report_stage2.txt
```

### Stage 책임 분리

| Stage | 책임 | 진입점 | 트랜잭션 |
|-------|-----|--------|---------|
| **Stage 1** | CSV → staging 적재 (매핑 SF 원본 그대로). 로컬 JDBC INSERT/COPY 또는 backend `copy-from-s3` (XML 메타 target — S3 업로드분) | `migrate-stage1.main.kts` / `POST /api/v1/admin/sf-migration/stage1/copy-from-s3` | target 별 |
| **Stage 2-A** | sfid → FK id (audit / 도메인 / polymorphic owner_group) | backend admin API `POST /api/v1/admin/sf-migration/stage2/fk` | substep 단위 |
| **Stage 2-A2** | Natural-key FK resolve — developer_name / name / sfid 컬럼 기반 join (spec #800, 9 mapping) | `POST /api/v1/admin/sf-migration/stage2/fk-natural-key` | substep 단위 |
| **Stage 2-A3** | UploadFile polymorphic parent — record_id (SF Id text) → parent_id (Long FK) | `POST /api/v1/admin/sf-migration/stage2/upload-file-polymorphic-parent` | substep 단위 |
| **Stage 2-B** | derived 캐시 동기화 — Employee.cost_center_code → User.cost_center_code (spec #807 이후 picklist→enum 변환 폐기. SF AppAuthority picklist value 가 곧 저장값) | `POST /api/v1/admin/sf-migration/stage2/picklist` | substep 단위 |
| **Stage 2-C** | BCrypt password hash (사번 평문 → hash) | `POST /api/v1/admin/sf-migration/stage2/password` | substep 단위 |
| **Stage 2-D** | UserRole hierarchy snapshot 재계산 (depth / all_subordinate_ids / ancestor_path) | `POST /api/v1/admin/sf-migration/stage2/user-role-hierarchy` | substep 단위 |
| **Reset**     | DB 초기화. ① 전체 TRUNCATE (`db-reset.sh` truncate, flyway 보존) — 진짜 처음부터 ② SF 산출물만 삭제 (`reset-dev.main.kts`, sfid IS NOT NULL row) | `db-reset.sh` / `reset-dev.main.kts` (운영 가드) | 단일 |

> **Stage 2 가 backend admin API 로 이전된 이유**: 운영 EB 서버에서 RDS 와의 latency 를 단축하고 (사용자 로컬 → SSM 터널 경로 회피), backend 의 enum/Converter/PasswordEncoder SoT 와 정합 일관성을 유지하기 위함. 권한은 `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` 로 SYSTEM_ADMIN profile 만 호출 가능. backend 구현 위치: `backend/src/main/kotlin/com/otoki/powersales/sfmigration/` (런칭 후 폐기 시 패키지 통째 삭제).

각 substep 은 독립 transaction → 일부 실패 시 다른 substep 의 적용 결과 유지. 실패한 substep 만 재실행 가능.

> **폐기된 Stage 2-D `permission` substep**: 과거 `PermissionSetAssignment → user_permission` INSERT (`stage2/permission` 엔드포인트) 는 spec #801 (SF 권한 모델 전면 적용) 으로 **`user_permission` 테이블 자체와 함께 폐기**. PermissionSetAssignment 는 이제 Stage 1 SOQL 정규 적재 (spec #798) + Stage 2 `fk` substep (AssigneeId → user_id, PermissionSetId → permission_set_id) 로 처리. `stage2/permission` 엔드포인트는 더 이상 존재하지 않는다.

> employee ↔ organization 관계는 신규 시스템(과 SF 레거시 모두)에서 FK 가 아닌 `cost_center_code` 기반 application-level join 으로 처리되므로 ETL 에 FK 업데이트 단계가 존재하지 않습니다.

### 지원 target 일람 (54종)

backend 의 `@SFObject` 어노테이션이 붙은 모든 entity + Permission staging table + spec #790 의 SF Sharing 메타 7 entity + spec #791 의 SF OWD 메타 2 entity (SObjectSetting / SObjectRelation) + spec #794 의 SF Record Type 메타 3 entity (RecordType / ProfileRecordType / PermissionSetRecordType) + spec #795 의 FLS 메타 2 entity (ProfileFieldPermission / PermissionSetFieldPermission) + spec #796 의 PermissionSet 정규 entity 1종.
전체 권위 출처는 `common.kts` 의 `TARGET_SPECS` 정의 (SOQL 출처) + `backend/.../sfmigration/stage1/Stage1Targets.kt` 의 `ALL` 맵 (전체 — XML 메타 출처 포함).

**Stage 2 변환 substep 보유 target** (그 외 target 은 Stage 1 raw 적재만):

| target | SF SObject | Stage 2 substep |
|--------|-----------|-----------------|
| 전 entity | (전체) | `POST /stage2/fk` — `*_sfid` 컬럼 자동 스캔 + FK id 채움 |
| `Employee` | `DKRetail__Employee__c` | Stage 1 raw 적재 (spec #807 이후 role / ppt picklist → enum 변환 폐기 — SF picklist value 가 곧 저장값) |
| `User` | `User` | `POST /stage2/picklist` — User.cost_center_code 동기화 (Employee.cost_center_code → User, employee_code 조인) / `POST /stage2/password` — BCrypt |
| `PermissionSetAssignment` | `PermissionSetAssignment` | Stage 1 SOQL 정규 적재 (spec #798) + `POST /stage2/fk` (assignee_user / permission_set FK). 과거 `stage2/permission` → user_permission 경로는 spec #801 로 폐기 |
| `SharingRule` | (XML 메타) | `POST /stage2/fk` (sharing_rule_condition / sharing_rule_target 의 sharing_rule FK + target polymorphic) — spec #790 |
| `SharingRuleCondition` | (XML 메타) | (위 동일) — spec #790 |
| `SharingRuleTarget` | (XML 메타) | (위 동일) — spec #790 |
| `UserRoleHierarchySnapshot` | (XML 메타) | `POST /stage2/user-role-hierarchy` (신규 — depth/all_subordinate_ids/ancestor_path 재계산) — spec #790 |
| `ProfileFlags` | (XML 메타 + ObjectPermissions SOQL) | `POST /stage2/fk` (profile FK) — spec #790. system 비트 5종은 profile XML, object_permissions 는 `ObjectPermissions` SOQL (`Parent.IsOwnedByProfile=TRUE`) 출처 — profile XML 은 objectPermissions 가 비어 내려오므로 SOQL 로 보강. 자연 키 profile_name 은 파일명 URL 디코딩 (`6%2E조장`→`6.조장`) 후 profile.name 평문 매칭 |
| `PermissionSet` | `PermissionSet` | Stage 1 raw 적재 (Id/Name/Label) — spec #796. `permission_set_flags.permission_set_id` 자연 키 lookup ref. |
| `PermissionSetFlags` | (XML 메타) | `POST /stage2/fk` (permission_set FK) — spec #790 |
| `GroupMember` | `GroupMember` | `POST /stage2/fk` (group / user_or_group polymorphic FK) — spec #790 |
| `SObjectSetting` | (XML 메타) | `POST /stage1/copy-from-s3` — XML 3 출처 정규화 (`<sharingModel>` + Sharing.settings) — spec #791 |
| `SObjectRelation` | (XML 메타) | `POST /stage1/copy-from-s3` — master-detail relationship 정규화 — spec #791 |
| `RecordType` | (XML 메타) | `POST /stage1/copy-from-s3` — `recordTypes/<DeveloperName>.recordType-meta.xml` — spec #794 |
| `ProfileRecordType` | (XML 메타) | `POST /stage2/fk` (profile FK) — spec #794. 운영 0건 (Profile 위임 패턴) |
| `PermissionSetRecordType` | (XML 메타) | `POST /stage2/fk` (permission_set FK) — spec #794. 운영 10건 |
| `ProfileFieldPermission` | (XML 메타) | `POST /stage2/fk` (profile FK) — spec #795. 운영 0건 (Profile 위임 패턴) |
| `PermissionSetFieldPermission` | (XML 메타) | `POST /stage2/fk` (permission_set FK) — spec #795. 운영 26 PermissionSet |

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

# 모든 target 추출 — sharing 메타 (XML 출처) 자동 포함 (spec #790 Q4 옵션 1 채택)
./extract-csv.sh --org otoki-prod

# 특정 target 만 (예: 조직만 먼저 확인)
./extract-csv.sh --org otoki-prod --target=Organization

# GroupMember 추출 생략
./extract-csv.sh --org otoki-prod --skip-group-members

# Sharing 메타 (XML 출처 — sharing_rule / profile_flags 등) 추출 생략
./extract-csv.sh --org otoki-prod --skip-sharing-meta

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

**SF describe 대조 (선택)**: `--describe-dir=<path>` 인자를 주면 거울 검증(entity ↔ common.kts)에
더해 SF describe 산출물 (`<describe-dir>/<SObject>.md` — `sf sobject describe` 의 markdown 표,
컬럼 `API Name / Type / … / Calc`)와도 대조합니다. 거울 검증이 못 잡는 사각지대를 보고합니다:
- **STALE** — common.kts/entity 가 매핑한 SF field 가 describe 에 없음 (오타 / SF 측 rename·삭제)
- **CALC** — calculated(✓) 필드를 매핑 (SOQL 적재 불가)
- **MISSING** — SF 에만 있는 비-calculated·비-시스템 필드 수 (INFO 카운트)

```bash
# describe 산출물 경로는 호출자가 전달 (소스에 docs 경로 비고정).
# 예: sf-object-meta 분석 산출물의 prod 디렉토리 (<SObject>.md 모음).
kotlin verify-metadata.main.kts --describe-dir=<sf-object-meta>/prod
```

STALE/CALC 는 종료 코드를 바꾸지 않습니다 (describe dump 자체가 stale 일 수 있어 hard fail 회피 —
리포트로만 노출하여 수기 확인 유도).

산출: `input/` 디렉토리에 CSV 파일들.

### 2단계 — Stage 1 적용 (JDBC INSERT)

```bash
# 전체 target — 시작 시 "전체 TRUNCATE 하시겠습니까?" 프롬프트 (stdin)
kotlin migrate-stage1.main.kts

# 처음부터 다시 — reset 후 적재를 한 번에 (db-reset.sh truncate 호출)
kotlin migrate-stage1.main.kts --reset

# 기존 데이터 위에 누적 적재 (이미 별도로 reset 했거나 누적 의도)
kotlin migrate-stage1.main.kts --no-reset

# 특정 target 만
kotlin migrate-stage1.main.kts --target=Organization,Employee --no-reset
```

옵션:
- `--target=Organization,Account,Product,Promotion,Group,Employee,User,Notice,...` (default: 전체. 전체 target 권위 출처는 `common.kts` `TARGET_SPECS`)
- `--reset` — 적재 전 `db-reset.sh --mode truncate` 호출 (powersales 전체 TRUNCATE RESTART IDENTITY CASCADE, `flyway_schema_history` 보존). 확인 프롬프트 생략.
- `--no-reset` — reset 건너뜀 (확인 프롬프트 생략). 기존 데이터 위에 `ON CONFLICT DO NOTHING` 누적.
- (둘 다 미지정) — stdin 으로 reset 여부 대화형 질의. stdin 미가용이면 reset 건너뜀.
- `--input-dir=<path>` (default: `./input`)
- `--output-dir=<path>` (default: `./output`) — 리포트 출력 경로

산출:
- DB: 각 target 의 staging row 적재 (ON CONFLICT DO NOTHING — 재실행 안전)
- 파일: `output/migration_report_stage1.txt`

진행 상황은 ASCII progress bar 로 라이브 표시:
```
[##############----------------]  47% (470/1000) Stage 1 Employee (470/1000)
```

### 3단계 — Stage 2 적용 (backend admin API)

Stage 2 는 backend admin REST 엔드포인트로 이전됨. 운영 backend (EB / Docker) 에 배포된 상태에서 SYSTEM_ADMIN 계정의 JWT 로 호출. 권한 가드: `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.

```bash
# 사전: SYSTEM_ADMIN 계정으로 로그인하여 JWT 확보
JWT="<access-token>"
BASE="https://<backend-host>"

# 권장 순서 — fk (FK id 채움) → fk-natural-key (developer_name/name 기반) →
#            upload-file-polymorphic-parent → picklist (cost_center 동기화) →
#            password (BCrypt) → user-role-hierarchy (snapshot 재계산)
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/fk"                              -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/fk-natural-key"                  -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/upload-file-polymorphic-parent"  -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/picklist"                        -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/password"                        -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/user-role-hierarchy"             -H "Authorization: Bearer $JWT"
```

> `stage2/fk` 는 비동기 실행 — `202 Accepted` 즉시 반환 후 백그라운드 진행. 진행률은 `GET /api/v1/admin/sf-migration/stage2/fk/progress` 로 폴링 (status RUNNING/DONE). 다음 substep 은 fk 완료 (DONE) 확인 후 호출.

각 응답 JSON:
```json
{
  "substep": "picklist",
  "results": [
    { "label": "user.cost_center_code", "rowsAffected": 1234 }
  ],
  "totalRowsAffected": 1234
}
```

특이사항:
- **fk substep** 은 `powersales` schema 의 모든 `*_sfid` 컬럼을 information_schema 로 자동 발견 후 짝의 `*_id` FK 컬럼을 sfid lookup 으로 채운다. `created_by` / `last_modified_by` / `owner` audit prefix 와 `manager` / `parent` / `team_leader` 등 alias prefix 는 `backend/.../sfmigration/service/SfFkResolveTables.kt` 의 `FK_PREFIX_MAPPING` 화이트리스트에 명시. 완료 직후 admin permission / data scope 캐시를 자동 invalidate (마이그레이션 직후 권한 어긋남 방지).
- **fk substep 처리 방식** — 테이블 단위로 모든 `*_sfid` 컬럼을 한 번에 LEFT JOIN 단일 UPDATE 로 처리 (테이블 fullscan 1회 / FK 수 무관). PK 컬럼 기준 100,000 row chunk 페이징 + chunk 마다 별도 TX commit + chunk 단위 INFO 로그. polymorphic owner_sfid (`005` = User / `00G` = Group) 도 같은 UPDATE 의 SET CASE 분기로 흡수. 1천만 row 짜리 `erp_order_product` 같은 대형 테이블에 적합. 진행 상황은 backend 로그 (CloudWatch / docker logs) 에서 `[fk] <table> chunk N/M ...` 패턴으로 tail 가능.
- **fk-natural-key substep** (spec #800) — sfid prefix 가 아니라 developer_name / name / 외부 sfid 컬럼 기반 join 으로 id 를 채우는 9개 매핑 (`NATURAL_KEY_FK_MAPPINGS`) 일괄 적용. fk substep 직후 호출.
- **upload-file-polymorphic-parent substep** — UploadFile.record_id (SF Id text) → parent_id (Long FK). 매핑 표는 `com.otoki.powersales.common.storage.UPLOAD_FILE_POLYMORPHIC_PARENTS`. fk substep 직후 호출.
- **picklist substep** — 현재는 `user.cost_center_code` 동기화만 수행 (Employee.cost_center_code 를 employee_code 조인으로 User 에 복사). spec #807 이후 Employee.role / professional_promotion_team / User.profile_type 의 한글 picklist → enum 변환은 폐기 (SF AppAuthority picklist value 가 곧 저장값). 개별 컬럼 실행은 `POST /stage2/picklist/user_cost_center_code`.
- **password substep** 은 `sfid IS NOT NULL AND (password IS NULL OR password = '')` 인 user row 의 password 를 `employee_code` (사번) 평문으로 BCrypt hash (backend 의 PasswordEncoder 빈 재사용, strength=10) + `password_change_required=TRUE` 설정.
- **user-role-hierarchy substep** (spec #790) — `user_role.parent_user_role_id` 트리 기반으로 `all_subordinate_ids` (jsonb) + `depth` + `ancestor_path` + `snapshot_at` 재계산. user_role 적재 + fk substep 으로 user_role_id 가 모두 채워진 후 1회 호출.

#### DB reset (처음부터 다시 / 리허설 반복용)

두 가지 방식이 있고 초기화 범위가 다르다.

**① 전체 TRUNCATE — 진짜 "처음부터" (권장)**

```bash
# powersales 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE (sfid 유무 무관, IDENTITY PK 리셋).
# flyway_schema_history 는 보존 → backend 가 마이그레이션을 재실행하지 않음.
# stage 분기로 dev (localhost:15432) / prod (localhost:25432) 구분, 비밀번호는 환경변수.
scripts/db-reset.sh -s dev                          # 확인 프롬프트 있음
scripts/db-reset.sh -s dev --yes                    # 프롬프트 생략 (자동화)
scripts/db-reset.sh --db-properties scripts/sf-data-migration/db.properties --mode truncate --yes

# recreate 모드 (스키마 자체 DROP + flyway_schema_history 까지 삭제 → backend 가 전 마이그레이션 재실행)
scripts/db-reset.sh -s dev --mode recreate
```

> `migrate-stage1.main.kts --reset` 이 내부적으로 `db-reset.sh --mode truncate --yes` 를 호출하므로, Stage 1 을 `--reset` 으로 돌리면 본 스크립트를 별도 실행할 필요 없다.

**② SF 산출물만 삭제 — 앱 데이터 보존 (`reset-dev.main.kts`)**

```bash
# ⚠️ 운영 DB 금지 — db.properties 가 dev 환경인지 사전 확인
# 본 스크립트는 운영 RDS endpoint 감지 시 자동 거부 (localhost 만 허용)
kotlin reset-dev.main.kts
```

reset 동작:
- TRUNCATE `sf_permission_set_assignment_raw`
- DELETE `user_permission WHERE user_id IN (SELECT user_id FROM "user" WHERE sfid IS NOT NULL)`
- DELETE 전체 entity `WHERE sfid IS NOT NULL` (TARGET_SPECS 순회, dependency 역순)

> ⚠️ `user_permission` 테이블은 spec #801 로 폐기 대상이다. 해당 환경에서 이미 drop 된 경우 위 DELETE 가 실패하므로, 전체 초기화가 목적이면 ① 방식 (`db-reset.sh`) 을 사용한다.

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
  SELECT COUNT(*) FROM powersales.permission_set_assignment WHERE sfid IS NOT NULL;
"

# SYSTEM_ADMIN 사번 로그인 + 핵심 페이지 접근 검증 (수동)
```

### 5단계 — cut-over (런칭 시점)

```bash
# 1. db.properties 를 운영 환경으로 전환
vim db.properties   # host=<prod-host>, password=$PROD_OTK_PWRS_DB_PASSWORD 등

# 2. 최종 CSV 추출 (cut-over 작업 윈도우 직전)
./extract-csv.sh --org otoki-prod

# 3. Stage 1 적용 (처음부터면 --reset 으로 전체 TRUNCATE 후 적재)
kotlin migrate-stage1.main.kts --reset

# 4. Stage 2 적용 (backend admin API — JWT 필요). fk 는 비동기 → progress DONE 확인 후 다음 호출
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/fk"                              -H "Authorization: Bearer $JWT"
#   완료 대기: GET .../stage2/fk/progress 가 status=DONE 될 때까지 폴링
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/fk-natural-key"                  -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/upload-file-polymorphic-parent"  -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/picklist"                        -H "Authorization: Bearer $JWT"
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/password"                        -H "Authorization: Bearer $JWT"

# 5. UserRole hierarchy snapshot 재계산 (spec #790) — Stage 2 fk 완료 후 1회 호출
curl -X POST "$BASE/api/v1/admin/sf-migration/stage2/user-role-hierarchy"             -H "Authorization: Bearer $JWT"

# 6. Sharing recalc (spec #792) — 메타 일괄 재적재 후 cache evict
#    OWD / Record Type / FLS / Sharing Rule 등 모든 sharing 관련 cache 일괄 무효화
curl -X POST "$BASE/api/v1/admin/sharing/recalc/all"                                 -H "Authorization: Bearer $JWT"

# 7. SYSTEM_ADMIN 사번 로그인 + 핵심 페이지 접근 검증
```

⚠️ **운영 적용 결과는 DB 에 영구 보존**되며, CSV/리포트 등 `input/` + `output/` 산출물은 작업 윈도우 종료 후 폐기.

## 매핑 표

Stage 2 매핑 표는 backend `sfmigration` 도메인에 보관 (런칭 후 패키지 통째 폐기 용이):

| 매핑 | 상수 | 위치 |
|---|---|---|
| sfid prefix → ref table/id (FK) | `FK_PREFIX_MAPPING` | `backend/.../sfmigration/service/SfFkResolveTables.kt` |
| Polymorphic owner entity (owner_sfid → owner_user_id / owner_group_id) | `POLYMORPHIC_OWNER_TABLES` | 동일 |
| Polymorphic related / user_or_group entity | `POLYMORPHIC_RELATED_TABLES` / `POLYMORPHIC_USER_OR_GROUP_TABLES` | 동일 |
| Natural-key FK (developer_name / name 기반) | `NATURAL_KEY_FK_MAPPINGS` | 동일 |
| 테이블 한정 FK override | `TABLE_SCOPED_FK_OVERRIDE` | 동일 |
| FK 처리 제외 prefix | `SKIP_FK_PREFIXES` | 동일 |
| UploadFile polymorphic parent (record_id → parent_id) | `UPLOAD_FILE_POLYMORPHIC_PARENTS` | `backend/.../common/storage/` |

> 과거 한글 picklist → enum 매핑 표 (`APP_AUTHORITY_TO_USER_ROLE` / `PPT_KOREAN_TO_ENUM` / `PROFILE_NAME_TO_PROFILE_TYPE`) 와 권한 매핑 (`PERMISSION_SET_TO_PERMISSIONS`) 을 담던 `SfMappingTables.kt` 는 spec #807 (picklist value 직접 저장) + spec #801 (SF 권한 모델 전면 적용) 으로 **파일째 폐기**되었다.

### 매핑 보정 → 재배포

backend 매핑 표 변경은 코드 PR → backend 재배포 → admin API 재호출. backend 정합 테스트 (`SfFkResolveTablesTest` / `SfFkResolveInventoryTest` 등) 가 FK 매핑 정합을 자동 검증.

## 클레임 이미지 마이그레이션 (ContentVersion → S3 → upload_file)

클레임(`DKRetail__Claim__c`) 첨부 이미지는 레거시에서 **SF Files(ContentVersion)에만** 저장되었고 (`IF_REST_MOBILE_ClaimRegist`), `UploadFile__c` / S3 에는 적재된 적이 없다. 그래서 일반 Stage1 (UploadFile CSV 적재) 만으로는 클레임 이미지가 0건이다 — `upload_file` 의 `record_sfid` prefix `a01`(claim) 부재. (현장활동 SiteActivity 는 `S3ImageUniqueKey__c` SObject 필드로 S3 key 를 직접 보유해 일반 경로로 적재됨 — 클레임에는 그 필드가 없다.)

신규 시스템은 클레임 이미지를 `upload_file (parent_type='Claim', unique_key=S3 key)` 로 조회하므로, ContentVersion 을 추출 → S3 재업로드 → `upload_file` 적재해야 한다. 이를 **반복 실행 가능**하도록 전 단계를 스크립트화했다.

### 도구

| 파일 | 책임 |
|---|---|
| `migrate-claim-images.sh` | 단일 진입점 오케스트레이션 (query → download → s3-images → build-csv → upload-csv → trigger). 각 단계 멱등 / `--skip-*` 부분 재시도 |
| `build-claim-upload-files.main.kts` | ContentVersion 메타 CSV → `upload_files.csv` 변환 (오프라인, AWS 미사용). 단독 실행 가능 |

### 파이프라인

```
1) query      sf data query (ContentVersion 메타) → <out>/contentversion-claim.csv
              SOQL: Type__c IN ('클레임','일부인','영수증') AND RecordId__c != null
2) download   메타 CSV 행별 sf api request (VersionData) → <out>/images/{CV.Id}.{ext} (증분)
3) build-csv  build-claim-upload-files.main.kts → <out>/upload_files.csv
--- 여기까지가 기본. S3 업로드는 AWS 콘솔에서 직접 (스크립트가 경로 안내) ---
(opt --aws-upload) aws s3 cp <out>/images/ + upload_files.csv 자동 업로드
(opt --trigger)    Stage1 copy-from-s3(UploadFile) → polling → Stage2 upload-file-polymorphic-parent
```

**S3 업로드는 기본적으로 AWS 콘솔에서 사용자가 직접 수행** (aws CLI 미사용). 스크립트는 로컬 산출물을 준비하고 업로드 대상 두 곳을 안내한다:
- `<out>/images/` 의 파일들 → `s3://<bucket>/uploads/claim/migrated/`
- `<out>/upload_files.csv` → `s3://<bucket>/<stage1-prefix>/upload_files.csv`

(aws CLI 로 자동 업로드까지 원하면 `--aws-upload`.) **이미지 파일명(`{ContentVersion.Id}.{ext}`)을 바꾸지 말 것** — `upload_files.csv` 의 `UniqueKey__c` 와 1:1 로 맞물린다.

핵심 매핑 (한 행/ContentVersion → `upload_files.csv`):

| upload_files.csv (SF 필드명) | 값 | upload_file 컬럼 |
|---|---|---|
| `Id` | `ContentVersion.Id` (068...) | `sfid` (unique) |
| `UniqueKey__c` | `uploads/claim/migrated/{CV.Id}.{ext}` | `unique_key` (조회 key) |
| `RecordId__c` | `ContentVersion.RecordId__c` (=claim.Id, a01...) | `record_sfid` → Stage2 가 `claim.sfid` 조인으로 `parent_id` 해소 |
| `Object__c` | `DKRetail__Claim__c` (보존) | `object_type` |
| `UploadKbn__c` | `claim`/`part`/`receipt` (Type__c 매핑) | `upload_kbn` |

> 적재·연결은 **기존 Stage1/Stage2 재사용** — backend / `Stage1Targets` / Stage2 변경 없음. `record_sfid` 조인 Stage2 (`upload-file-polymorphic-parent`) 가 `parent_type='Claim'` + `parent_id` 를 자동 설정한다.

### 사용법

```bash
# 사전: sf org login web --alias <alias>

# 기본 — 로컬 준비(query+download+build-csv) 후 콘솔 업로드 경로 안내. S3 는 콘솔에서 직접.
./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix>
#   → 출력 안내대로 AWS 콘솔에서:
#       ① <out>/images/* → s3://<bucket>/uploads/claim/migrated/
#       ② <out>/upload_files.csv → s3://<bucket>/<prefix>/upload_files.csv
#   → 그 후 web SF Migration 에서 Stage1(UploadFile) → Stage2(UploadFile Parent Resolve)

# (옵션) aws CLI 로 S3 업로드까지 자동 (AWS 자격증명 필요)
./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix> --aws-upload

# (옵션) Stage1/Stage2 까지 자동 — S3 업로드 끝난 뒤 (backend 토큰 필요)
./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix> \
    --skip-query --skip-download --skip-build-csv --trigger --api-base https://<host> --token <JWT>

# 변환만 재시도 (이미 다운로드 끝났을 때)
./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix> \
    --skip-query --skip-download

# 추출 대상 건수만 미리 확인 (다운로드 안 함, bucket/prefix 불요)
./migrate-claim-images.sh --org <alias> --count-only

# 샘플 100건만 추출 (검증용)
./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix> --limit 100
```

> 매 실행 시작 시 `SELECT COUNT()` 로 **추출 대상 건수**를 항상 출력한다. `--count-only` 는 그 건수만 보고 종료(추출 X), `--limit N` 은 SOQL `LIMIT N` 으로 샘플만 추출한다.

재실행 안전: download 는 이미 받은 파일 skip, Stage1 은 `ON CONFLICT(sfid) DO NOTHING`, Stage2 는 `parent_id IS NULL` 한정. 데이터가 갱신되면 그대로 다시 실행하면 증분 적재된다.

## 마이그레이션 제외 항목 (의도적 미구현)

검증 결과 또는 정책 결정으로 본 프레임워크가 다루지 않는 SF 권한 기능:

| SF 기능 | 제외 사유 | 검증 근거 |
|---|---|---|
| ~~**Field-Level Security (FLS)**~~ | ~~SF org 차단 행 0건 — 실효 미사용~~ → spec #795 인프라 도입 (점진 적용) | 인벤토리 §2.8 결과 PermissionSet 26건 fieldPermissions 활용 발견 (2026-05-22) |
| **마케팅 채널 PermissionSet (X1/X2/X4)** | 활성 사용자 할당 0명 — 실효 미사용 | `PermissionSetAssignment LIKE 'X%'` 0건 (2026-05-17) |
| **Login IP Range** | 운영 정책 결정 신규 미구현 | Admin/CEO 4개 Profile 한정, 영업사원 무관 |
| **Role Hierarchy 250개** | 신규 UserRole 9종 + cost_center_code 로 축약 대체 (UserRole entity 적재 + hierarchy 재계산은 spec #790 으로 별도 처리) | AdminDataScopeService 동등 가시성 제공 |
| **Apex `Profile.Name` 문자열 분기** | 신규 코드에 UserRole enum 분기 존재 | 3건만 사용, 신규 동등 분기 확인 |
| **페이지/탭 접근 제어** | SF Profile pageAccesses 0건, Hidden tabSettings 0건 | retrieve 산출물 |

> spec #790 (SF Sharing 메타 데이터 적재) 가 SharingRule / PublicGroup / ProfileFlags / PermissionSetFlags 등 7 entity 의 메타 적재를 추가했으므로
> "**SharingRule / PublicGroup**" 행은 제거됨. 본 spec 이전에는 실효 미미 판정으로 제외했으나, #782 Sharing Policy framework 도입 후 정책 적용 대상이 됨.

## JDBC 동작 상세

- **Driver**: `org.postgresql:postgresql:42.7.4` (Kotlin script @file:DependsOn 자동 다운로드)
- **URL 옵션**: `stringtype=unspecified` (boolean/date/timestamp 컬럼에 String 값 자동 추론) + `reWriteBatchedInserts=true` (batch INSERT 를 multi-row 로 재작성)
- **Batch 크기**: 500 (`BATCH_SIZE` 상수, `migrate-stage1.main.kts`)
- **트랜잭션**: autoCommit OFF, 각 target/substep 단위로 commit/rollback
- **멱등성**: Stage 1 = `ON CONFLICT DO NOTHING`, Stage 2 = `WHERE` 조건 (NULL 체크 / 한글 매칭 / password IS NULL 등)
- **연결**: 각 target/substep 단위로 새 Connection — 장시간 idle 회피

## 폐기 절차 (런칭 + 안정화 후)

> 참고 — 1세대 ETL 도구 (`backend/scripts/sf-migrate/` : `sf-export.sh` / `db-import.sh` / `entity-meta.py` / `post-load/`) 는 본 프레임워크 (Spec #764) 가 완전 대체하여 이미 제거 완료 (commit `08408a13`, 2026-05-19). 본 절차는 본 프레임워크 자체의 런칭 후 폐기를 다룬다.

```bash
# 1. scripts 디렉토리 폐기 (Stage 1 + reset 도구)
git rm -rf scripts/sf-data-migration/

# 2. backend Stage 2 모듈 폐기 (admin API + 매핑 표)
git rm -rf backend/src/main/kotlin/com/otoki/powersales/sfmigration/
git rm -rf backend/src/test/kotlin/com/otoki/powersales/sfmigration/
```

매핑 결과는 운영 DB 의 `employee` / `user` / `organization` / `permission_set_assignment` 테이블에
적재된 row 로 영구 보존되므로 위 두 위치는 모두 안전하게 폐기 가능.

폐기 시 함께 삭제 가능 (선택):
- `backend/src/main/resources/db/migration/V152__create_sf_permission_set_assignment_raw.sql` —
  staging 테이블 자체 폐기는 별도 `V{next}__drop_sf_permission_set_assignment_raw.sql`
  마이그레이션 추가로 수행 (Flyway 정합).
