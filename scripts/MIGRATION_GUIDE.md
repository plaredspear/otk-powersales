# 데이터 마이그레이션 가이드 (SF + Heroku → 신규 PostgreSQL)

레거시 두 시스템(**Salesforce 운영 조직** + **Heroku PowerSales**)의 데이터를 신규 시스템
(Spring Boot + PostgreSQL `powersales` 스키마)으로 **1회 cut-over 이전**하기 위한 실행 절차서.

- **적재 본체**: backend 의 web 관리자 화면(S3 → PostgreSQL COPY) + backend admin REST(FK/picklist/password 등 변환)
- **실행 주체**: SF CLI / DB / AWS 명령은 **정책상 사용자가 직접 실행**한다(자동 실행 없음). export(SF CLI · TablePlus) + S3 업로드도 사용자 수동.
- **선행 관계**: **SF 마이그레이션 → Heroku 마이그레이션** 순서 고정. Heroku 의 패턴 A/C 가 SF 로 적재된 `employee` / `account` / `product` / `display_work_schedule` / `team_member_schedule` 을 자연키·sfid 로 참조한다.
- **환경 주의**: 아래 예시는 **prod 리허설(port 25432)** 기준. 실제 대상 환경(dev/prod)을 매 실행 전 확인한다. 되돌릴 수 없는 작업(TRUNCATE/DELETE)은 대상 테이블·환경을 반드시 재확인.

> 상세 프레임워크 문서: SF = `scripts/sf-data-migration/README.md`, Heroku = `scripts/heroku-data-migration/README.md`. 본 가이드는 두 문서를 실행 순서로 엮은 상위 절차서다.

---

## 0. 사전 조건

1. **backend/web 이 최신 코드로 재배포된 상태** — Stage1 파서(RFC4180, continue-on-error), profile_flags 처리, fcm_token/device_uuid 제외 등 마이그레이션 관련 수정은 **재배포되어야 반영**된다. 구버전 backend 에 대고 실행하면 이전에 겪은 오류가 재현된다.
2. **DB 터널** — dev/prod RDS 접속은 `scripts/db-tunnel.sh -s <env>` 로 SSM 터널을 먼저 연다(prod=25432, dev=15432).
3. **S3 버킷** — 운영 `S3_BUCKET` 환경 속성. 입력 prefix: SF = `s3://<bucket>/sf-migration/input/`, Heroku = `s3://<bucket>/heroku-migration/input/`.
4. **web 관리자 화면** — 로그인만 요구하고 사이드 메뉴 미노출(URL 직접 진입). cut-over 완료 후 가드 복원 권장.

| 화면 | URL |
|------|-----|
| SF Stage 1 | `/admin/tools/sf-migration-1` |
| SF Stage 2 | `/admin/tools/sf-migration-2` |
| Heroku Stage 1 | `/admin/tools/heroku-migration-1` |
| Heroku Stage 2 | `/admin/tools/heroku-migration-2` |

---

## 1. SF 데이터 마이그레이션

### 1-1. CSV 추출 (사용자 실행)

```bash
cd scripts/sf-data-migration
./extract-csv.sh              # 사전 verify-metadata 자동 호출 + SF CLI 로 entity 별 CSV 추출
```

- 추출 전 `verify-metadata.main.kts` 가 `EntityMetadata` ↔ backend `@SFField` 정합을 검증한다. **entity 경로 mismatch("Entity 파일 없음")** 가 나오면 `common.kts` 의 `entityRelPath` 가 backend 패키지 리팩토링과 어긋난 것 → 경로 갱신 후 재실행. verify 가 편집 후에도 옛 결과를 보이면 `rm -rf ~/Library/Caches/main.kts.compiled.cache/` 로 stale 컴파일 캐시 제거.
- 산출 CSV 는 `input/` (gitignore). SF export CSV 의 data row 는 PII 라 열람 금지.

### 1-2. S3 업로드 (사용자 실행)

추출 CSV 를 `s3://<bucket>/sf-migration/input/<파일명>.csv` 로 업로드.

### 1-3. Stage 1 적재 — web `/admin/tools/sf-migration-1`

- **"일괄 실행(전체 entity)"** 실행. 1개 entity 실패해도 **중단하지 않고 다음 entity 를 계속 적재**한다(continue-on-error). 최종 상태는 실패 건수로 판정.
- 멱등: `ON CONFLICT DO UPDATE / DO NOTHING` 이라 **재실행 안전**. db-reset recreate 는 불필요(스키마+flyway 이력까지 날림). 깨끗한 시작을 원하면 `db-reset.sh -s <env>` **truncate** 만 선택.
- **화면이 "실행중(RUNNING)" 으로 잠겨 있으면** — 인스턴스 재시작 등으로 Redis 진행 스냅샷이 남은 것. "상태 초기화" 버튼(Popconfirm) 으로 해제하거나, ElastiCache 에서 `DEL migration:progress:sf-stage1-copy`.

### 1-4. Stage 2 변환 — web `/admin/tools/sf-migration-2`

각 substep 은 독립 트랜잭션 → 일부 실패해도 나머지 유지, 실패분만 재실행 가능. **화면 카드 순서대로** 실행:

| 순서 | substep | 내용 |
|------|---------|------|
| 1 | **FK Resolve** | sfid → FK id (audit / 도메인 / polymorphic owner_group) |
| 2 | **Natural Key FK 해소** | developer_name / name / 외부 sfid 컬럼 기반 join (profile_flags / permission_set_* / sharing_rule_* 등). **FK Resolve 완료 후 실행** |
| 3 | **UploadFile Parent Resolve** | record_id(SF Id text) → parent_id(Long FK) |
| 4 | **공지 본문 이미지 placeholder 치환** | notice RTA 이미지 URL placeholder |
| 5 | **UserRole Hierarchy 재계산** | depth / all_subordinate_ids / ancestor_path. **Natural Key FK 해소 완료 후** |
| 6 | **Derived 캐시 동기화 (Stage 2-B)** | Employee.cost_center_code → User / ProfessionalPromotionTeamMaster 백필 |
| — | **비밀번호 해시 (Stage 2-C)** | 사번 평문 → BCrypt hash (`POST .../stage2/password`) |

### 1-5. 클레임/공지 이미지 (별도, Stage 2 이후 가능)

S3/ContentVersion 기반 첨부 이전은 Stage 2 이후 독립 실행 가능:

```bash
cd scripts/sf-data-migration
./migrate-claim-images.sh          # ContentDocumentLink → ContentVersion → S3 → upload_file
./migrate-notice-rta-images.sh     # 공지 본문 이미지
```

> 클레임 이미지 출처는 `ContentDocumentLink.LinkedEntityId`(운영 `RecordId__c`/`Type__c` 는 전부 null). `UploadFile__c` 는 적재 안 됨.

### 1-6. 이미지 저장소 S3 sync (레거시 이미지 버킷 → 신규 storage)

레거시 이미지 저장 버킷의 파일을 신규 시스템 storage 버킷의 `private/` 아래로 복사한다.

| 항목 | 값 |
|------|-----|
| **소스** | `s3://ottogi-nonsap-prd-imagerepository-s3/` |
| **대상** | `s3://{dev\|prod}-otk-pwrs-storage/private/` |

정책: **`--delete` 미사용**(소스 파일만 추가/갱신, 대상 기존 `private/` 파일 보존). **dev 검증 후 prod 적용**. `aws` 명령은 사용자가 직접 실행(자발 호출 금지).

> 경로 주의: 소스가 접두어 없이 버킷 루트라 소스 최상위 구조가 대상 `private/` 바로 아래로 들어간다. 특정 prefix 만 옮기려면 소스 경로에 붙인다(예: `.../ottogi-nonsap-prd-imagerepository-s3/claim/`).

**① dev 검증 (dry-run → 실제)**

```bash
# dry-run — 무엇이 복사될지만 확인
aws s3 sync s3://ottogi-nonsap-prd-imagerepository-s3/ s3://dev-otk-pwrs-storage/private/ --dryrun
# 확인 후 실제 실행
aws s3 sync s3://ottogi-nonsap-prd-imagerepository-s3/ s3://dev-otk-pwrs-storage/private/
```

**② prod 적용 (dev 검증 완료 후)**

```bash
aws s3 sync s3://ottogi-nonsap-prd-imagerepository-s3/ s3://prod-otk-pwrs-storage/private/ --dryrun
aws s3 sync s3://ottogi-nonsap-prd-imagerepository-s3/ s3://prod-otk-pwrs-storage/private/
```

**실행 경로 A — 로컬/관리 단말**: 두 버킷 접근 가능한 AWS 자격증명(SSO/프로파일)으로 위 명령 실행.

**실행 경로 B — AWS 콘솔 → SSM Session Manager 경유** (인스턴스 IAM 역할로 실행, 로컬 자격증명 불요):

1. AWS 콘솔 → **Systems Manager** → **Session Manager** → **Start session**
   (대안: **EC2** → 인스턴스 선택 → **Connect** → **Session Manager** 탭 → **Connect**)
2. 두 버킷 접근 IAM 역할을 가진 인스턴스 선택 → **Start session** → 브라우저 터미널
3. 위 sync 명령 실행 (dev → prod)

파일이 많아 오래 걸리면 세션이 끊겨도 계속 돌도록 백그라운드 실행:

```bash
nohup aws s3 sync s3://ottogi-nonsap-prd-imagerepository-s3/ s3://prod-otk-pwrs-storage/private/ \
  > /tmp/s3sync-prod.log 2>&1 &
tail -f /tmp/s3sync-prod.log   # 진행 확인
```

> **SSM 사전 조건**: 인스턴스에 ① SSM Agent 실행 중(Amazon Linux 2/2023·최신 Ubuntu 기본 포함) ② IAM 역할에 `AmazonSSMManagedInstanceCore` + 소스 버킷 `s3:GetObject`/`s3:ListBucket` + 대상 버킷 `s3:PutObject`/`s3:ListBucket` ③ `aws` CLI 설치(`aws --version`).

**sync 후 검증** — 객체 수 대조:

```bash
aws s3 ls s3://ottogi-nonsap-prd-imagerepository-s3/ --recursive | wc -l
aws s3 ls s3://dev-otk-pwrs-storage/private/ --recursive | wc -l
```

---

## 2. Heroku 데이터 마이그레이션

**SF 마이그레이션 완료 후** 진행. 적재 메타는 backend `HerokuStage1Targets` 가 `@HerokuOnly` + `@HCColumn` 리플렉션으로 자동 생성(SoT). 대상은 `@HerokuOnly` **19개 테이블**.

### 2-1. TablePlus Export (사용자 실행)

`salesforce2.<table>` 을 `SELECT * FROM salesforce2.<table>` 로 export (alias / 순서변경 / 컬럼누락 금지 — 헤더 = Heroku 원본 컬럼명 = `@HCColumn` value).

| # | 엔티티 | CSV (Heroku 원본) | # | 엔티티 | CSV |
|---|--------|-------------------|---|--------|-----|
| 1 | EducationCode | education_code_mng.csv | 11 | TmpOrderProduct | tmp_order_product.csv |
| 2 | TmpClaimCode | tmp_claimcode.csv | 12 | TmpClaim | tmp_claim.csv |
| 3 | DeviceVersion | device_version_mng.csv | 13 | TmpSuggest | tmp_suggest.csv |
| 4 | SafetyCheckItem | safetycheck_list.csv | 14 | TmpOnsite | tmp_onsite.csv |
| 5 | EmployeeAdmin | employee_admin_mng.csv | 15 | TmpPromotion | tmp_promotion.csv |
| 6 | **EmployeeInfo** ⚠️PII | employee_mng.csv | 16 | FavoriteProduct | product_favorites.csv |
| 7 | EducationPost | education_mng.csv | 17 | LoginHistory | employee_his.csv |
| 8 | EducationPostAttachment | education_file_mng.csv | 18 | ProductExpiration | expirationdate__mng.csv |
| 9 | EducationViewHistory | education_member_history.csv | 19 | SafetyCheckSubmission | safetycheck__workschedule__member.csv |
| 10 | TmpOrder | tmp_order.csv | | | |

**export 규칙**: 헤더 포함 · NULL → `\N`/빈문자 · UTF-8 고정(Excel 경유 금지) · TEXT 개행/콤마 RFC4180 quoting · 패턴 B 부모키(`edu_id`) 누락 금지.

**제외 2개**: `if_product__c`(ProductSyncBuffer, PLM 미재현), `commute_distance`(대응 엔티티 없음).

**PII 컬럼**:
- `employee_mng` = 사번 / `emp_pwd`(BCrypt) / **`emp_uuid`(기기 UUID)** / **`emp_token`(FCM 토큰)**.
- `emp_token`(→ fcm_token) / `emp_uuid`(→ device_uuid) 은 **적재 파서가 매핑 제외**라 CSV 에 있어도 적재되지 않는다(신규 앱 로그인 시 재등록). export 시 컬럼 유지/제거 무관.
- cut-over 완료 후 S3 의 `employee_mng.csv` 등 PII 객체 삭제.

### 2-2. S3 업로드 (사용자 실행)

`s3://<bucket>/heroku-migration/input/<원본테이블명>.csv`.

### 2-3. Stage 1 적재 — web `/admin/tools/heroku-migration-1`

- **"일괄 적재 (19개 전체)"**, Reset 모드 체크 권장(적재 전 TRUNCATE).
- S3 에 CSV 없는 entity(404) → **SKIPPED** 로 건너뛰고 계속(일부만 export 해도 있는 것만 적재). FAILED(적재 오류)는 batch 중단.
- `EmployeeInfo` 는 `employee` 미적재 고아 row 가 `unmatched` 로 집계(INSERT 제외 — 공유 PK resolve).

### 2-4. Stage 2 FK Resolve — web `/admin/tools/heroku-migration-2`

- **"FK Resolve 실행"** — 패턴 A(자연키 → serial id) + 패턴 B(부모 FK) 일괄.

### 2-5. Stage 2-C sfid FK — web `/admin/tools/sf-migration-2`

- SF 화면의 **FK Resolve 재실행** — `ProductExpiration` / `SafetyCheckSubmission` 의 `*_sfid → *_id`(sfid 자동 스캔).

### 2-6. PII 정리 (cut-over 완료 후)

S3 의 `employee_mng.csv` 등 객체 삭제. 이미 적재분에 PII 가 남았으면(구버전 파서로 적재한 경우 등) DB 에서 정리:

```sql
UPDATE powersales.employee_info
SET fcm_token = NULL,
    device_uuid = NULL
WHERE fcm_token IS NOT NULL
   OR device_uuid IS NOT NULL;
```

---

## 3. 트러블슈팅 (실제 관측 사례)

| 증상 | 원인 | 대응 |
|------|------|------|
| verify-metadata "Entity 파일 없음" 다수 | backend 패키지 리팩토링이 `common.kts` `entityRelPath` 에 미반영 | 경로 갱신 + `rm -rf ~/Library/Caches/main.kts.compiled.cache/` |
| `CsvMalformedLineException: Unterminated quoted field` | SF Bulk CSV 의 CRLF/LF 혼재(값 내부 LF) | 파서가 RFC4180(`RFC4180Parser`)로 처리 — **재배포 필요**. CSV 수정 불요 |
| Stage1 적재 0 rows (COMPLETED, inserted=0) | `updateOnly=true` 가 빈 테이블에 INSERT 못 함 | upsert(ON CONFLICT DO UPDATE) 복원 — 재배포 후 재실행 |
| Stage1 과다 지연 | row 별 Redis persist | 5,000-row 스로틀 — 재배포 반영 |
| `ON CONFLICT ... cannot affect row a second time` (ErpOrder) | SAP→SF 재전송으로 CSV 내부 `sap_order_number` 중복 | `dedupKey` 로 가장 오래된(created_at 최소) 1행만 적재 — 재배포 |
| `duplicate key ... promotion_emp_id_ext` (TeamMemberSchedule) | CSV 내부 자연키 중복 | 동일 dedup 처리 — 재배포 |
| `syntax error at or near "UNION"` (dedup) | UNION leg 괄호 누락(ORDER BY 동반 시) | leg 괄호화 SQL — 재배포 |
| 화면이 "실행중" 으로 잠김 | 인스턴스 재시작 후 Redis 진행 스냅샷 잔존 | "상태 초기화" 버튼 또는 `DEL migration:progress:sf-stage1-copy` |
| `duplicate key ... profile_flags_profile_id_key` (Stage2 Natural Key FK) | 부팅 Runner 가 만든 (profile_name=NULL, profile_id) row 와 Stage1 SF row(profile_name, profile_id=NULL)가 별개 row 로 공존 | `LeaderProfileFlagsSyncRunner` 비활성화(재배포) + 기존 오염 row 삭제: `DELETE FROM powersales.profile_flags WHERE profile_name IS NULL;` 후 Natural Key FK 재실행 |

---

## 4. 재실행 / 초기화 정책

- **재실행**: Stage1/Stage2 모두 멱등(ON CONFLICT / IS NULL 가드). 실패분만 재실행 가능.
- **truncate 초기화**: `db-reset.sh -s <env>` truncate — flyway 이력 보존, 데이터만 비움(깨끗한 시작).
- **recreate 금지**: 스키마 + flyway 이력까지 삭제라 마이그레이션 리허설엔 과하다.
- **DB 직접 접근**: 진단 SELECT/UPDATE 는 정책상 **사용자가 직접 실행**(SQL 제시 → 사용자 실행 → 결과 회신). 검증용 스키마 CREATE/out-of-order 마이그레이션도 금지.
