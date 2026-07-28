# 데이터 마이그레이션 가이드 (SF + Heroku → 신규 PostgreSQL)

레거시 두 시스템(**Salesforce 운영 조직** + **Heroku PowerSales**)의 데이터를 신규 시스템
(Spring Boot + PostgreSQL `powersales` 스키마)으로 **1회 cut-over 이전**하기 위한 실행 절차서.

- **적재 본체**: backend 의 web 관리자 화면(S3 → PostgreSQL COPY) + backend admin REST(FK/picklist/password 등 변환)
- **실행 주체**: SF CLI / DB / AWS 명령은 **정책상 사용자가 직접 실행**한다(자동 실행 없음). export(SF CLI · TablePlus) + S3 업로드도 사용자 수동.
- **선행 관계**: **SF 마이그레이션 → Heroku 마이그레이션** 순서 고정. Heroku 의 패턴 A/C 가 SF 로 적재된 `employee` / `account` / `product` / `display_work_schedule` / `team_member_schedule` 을 자연키·sfid 로 참조한다.
- **환경 주의**: 아래 예시는 **prod 리허설(port 25432)** 기준. 실제 대상 환경(dev/prod)을 매 실행 전 확인한다. 되돌릴 수 없는 작업(TRUNCATE/DELETE)은 대상 테이블·환경을 반드시 재확인.

> 상세 프레임워크 문서: SF = `scripts/sf-data-migration/README.md`, Heroku = `scripts/heroku-data-migration/README.md`. 본 가이드는 두 문서를 실행 순서로 엮은 상위 절차서다.

### 실행 순서 한눈에 (SF)

```
0.  backend/web 최신 코드 재배포                        ← 코드 수정 반영 (§0-1)
1.  scripts/db-tunnel.sh -s <env>                       ← SSM 터널 (§0-2)
2.  [선택] scripts/db-reset.sh -s <env>                 ← truncate 권장 (§1-0)
      └ recreate 를 썼다면 → backend 재기동으로 Flyway 재실행 (§1-0-1) ★
3.  scripts/sf-data-migration/extract-csv.sh            ← CSV 추출 (§1-1)
4.  S3 업로드 s3://<bucket>/sf-migration/input/         ← 사용자 수동 (§1-2)
5.  web /admin/tools/sf-migration-1  → 일괄 실행         ← Stage 1 적재 (§1-3)
6.  web /admin/tools/sf-migration-2  → 카드 순서대로     ← Stage 2 변환 (§1-4)
7.  이미지 (§1-5, §1-6)
      ├ migrate-claim-images.sh       → 콘솔 업로드 → web Stage1(ClaimImageUploadFile) → Stage2
      ├ migrate-notice-rta-images.sh  → 콘솔 업로드 → web Stage1(NoticeImageUploadFile) → Stage2
      │    └ replace-notice-rta-urls.main.kts --apply  (본문 placeholder 치환)
      └ aws s3 sync (레거시 이미지 버킷 → 신규 storage private/)
8.  검증 (리포트 + psql COUNT + SYSTEM_ADMIN 로그인)
9.  Heroku 마이그레이션 (§2) — SF 완료 후
```

---

## 0. 사전 조건

1. **backend/web 이 최신 코드로 재배포된 상태** — Stage1 파서(RFC4180, continue-on-error), profile_flags 처리, fcm_token/device_uuid 제외 등 마이그레이션 관련 수정은 **재배포되어야 반영**된다. 구버전 backend 에 대고 실행하면 이전에 겪은 오류가 재현된다. 트러블슈팅 표의 대응 상당수가 "재배포 필요" 인 이유가 이것이며, 이때 CSV 를 고치는 방향으로 접근하면 헛수고가 된다.
2. **DB 터널** — dev/prod RDS 접속은 `scripts/db-tunnel.sh -s <env>` 로 SSM 터널을 먼저 연다(prod=25432, dev=15432). 비밀번호는 환경변수 `DEV_OTK_PWRS_DB_PASSWORD` / `PROD_OTK_PWRS_DB_PASSWORD`(`db-tunnel.sh -s <env> --password` 로 조회).
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

### 1-0. DB reset (선택 — 리허설 반복 / 처음부터 다시)

기존 데이터 위에 누적 적재해도 무방하면(Stage1/2 모두 멱등) **건너뛴다**. 깨끗한 상태에서 다시 시작할 때만 실행.

`scripts/db-reset.sh` 는 `powersales` 스키마 **전체 테이블 일괄** 초기화다(부분 선택 불가). 사전에 **DB 터널이 떠 있어야** 하고, 실행 전 테이블별 현재 행 수를 출력한 뒤 확인 프롬프트를 띄운다.

| 모드 | 동작 | `flyway_schema_history` | backend 재기동 |
|------|------|------------------------|----------------|
| **truncate** (기본, 권장) | 전 테이블 `TRUNCATE RESTART IDENTITY CASCADE` | **보존** | **불필요** |
| **recreate** | `DROP SCHEMA powersales CASCADE` + `CREATE SCHEMA`(owner 보존) | **삭제** | **필수** (1-0-1) |

```bash
scripts/db-reset.sh -s dev                 # dev, truncate, 확인 프롬프트
scripts/db-reset.sh -s dev --yes           # 프롬프트 생략 (자동화)
scripts/db-reset.sh -s prod                # prod — 추가 경고 프롬프트
scripts/db-reset.sh -s dev --mode recreate
scripts/db-reset.sh --db-properties scripts/sf-data-migration/db.properties --mode truncate --yes
```

접속 정보: `--db-properties` 명시 시 그 파일 우선, 아니면 stage 분기(dev = `localhost:15432/otkadmin`, prod = `localhost:25432/postgres`).

- **마이그레이션 리허설에 recreate 는 과하다** — 스키마 + flyway 이력까지 날려 재기동 부담만 늘어난다. 깨끗한 시작은 truncate 로 충분하다.
- `migrate-stage1.main.kts --reset` 은 내부적으로 `db-reset.sh --mode truncate --yes` 를 호출하므로, Stage 1 을 `--reset` 으로 돌리면 본 스크립트를 별도 실행할 필요 없다.
- **SF 산출물만 삭제**(앱 데이터 보존)가 목적이면 `kotlin scripts/sf-data-migration/reset-dev.main.kts` — `WHERE sfid IS NOT NULL` 로 dependency 역순 DELETE(운영 endpoint 감지 시 자동 거부, localhost 전용). 단 `user_permission` 은 폐기 대상이라 이미 drop 된 환경에서는 실패하므로, **전체 초기화가 목적이면 `db-reset.sh` 를 쓴다**.

#### 1-0-1. backend 재기동 (recreate 를 실행한 경우에만)

recreate 는 `flyway_schema_history` 까지 지우므로, backend 가 부팅하며 **전 마이그레이션을 재실행해 스키마를 다시 만들어야** Stage 1 적재가 가능하다.

```bash
cd backend && ./gradlew flywayMigrate    # 또는 ./gradlew bootRun
```

배포 환경(EB)이면 애플리케이션 재시작으로 부팅 시 Flyway 가 재실행된다. truncate 모드에서는 이 단계가 없다.

> **재기동 직후 주의** — Redis 진행 스냅샷이 남아 web 화면이 "실행중(RUNNING)" 으로 잠길 수 있다. "상태 초기화" 버튼 또는 `DEL migration:progress:sf-stage1-copy`(1-3 참조).

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
- 멱등: `ON CONFLICT DO UPDATE / DO NOTHING` 이라 **재실행 안전** — reset 없이 실패분만 다시 돌려도 된다. 깨끗한 시작이 필요하면 1-0 의 **truncate** 를 쓴다(recreate 불필요).
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
| — | **비밀번호 해시 (Stage 2-C)** | 사번 기반 초기 평문 `{사번}@pwrs` → BCrypt hash. **`password IS NULL OR ''` 인 row 만** (`POST .../stage2/password`) |
| 7 | **조장 화면 권한 회수** | `6.조장` 에서 ERP주문(`erp_order`) / 조직마스터(`organization`) / 근무 등록현황(`attendance_log`) / 대체휴무(`alternative_holiday`) 권한 키 제거. **dirty row 에도 강제 적용**(대상 키만 제거라 다른 편집분 보존) (`POST .../stage2/leader-erp-org-revoke`) |
| 최종 | **조장/지정 사번 비밀번호 초기화** | profile `6.조장` + 지정 사번 8명. **기존 비밀번호도 덮어씀**(가드 없음) — 화면 **최하단 카드** (`POST .../stage2/leader-password-reset`) |

> **두 비밀번호 카드의 차이** — Stage 2-C 는 미설정 row 만 채우는 **적재**용(멱등), 최하단 카드는 이미 쓰던 비밀번호까지 되돌리는 **초기화**용(반복 적용). 후자는 `profile_id` 가 확정된 뒤(FK Resolve → Natural Key FK → User Profile 정합 이후) 실행해야 조장 대상이 정확히 잡히므로 Stage 2 최종 단계에 둔다. 대상 사번은 backend `SfMigrationStage2Service.MANUAL_PASSWORD_RESET_EMPLOYEE_CODES` 상수에서 관리한다.

### 1-5. 클레임/공지 이미지 (별도, Stage 2 이후 가능)

두 스크립트 모두 **로컬 산출물 준비 + 업로드 경로 안내까지만** 담당한다. **S3 업로드는 AWS 콘솔에서 사용자가 직접**, **Stage1/Stage2 는 web SF Migration 화면**에서 처리한다(bucket 은 web 이 backend `app.aws.s3.bucket` 로 프리필하므로 스크립트가 다루지 않는다). 각 단계 멱등 + `--skip-*` 로 부분 재시도 가능.

사전 준비: `sf org login web --alias <alias>` (SF CLI 는 스크립트가 래핑하나 **실행 주체는 사용자**).

#### 1-5-1. 클레임 이미지 — `migrate-claim-images.sh`

파이프라인: `query`(ContentVersion 메타 → `contentversion-claim.csv`) → `download`(VersionData → `images/{CV.Id}.{ext}`, 증분) → `build-csv`(→ `claim_upload_files.csv`). 산출물은 `input/claim-images/`.

```bash
cd scripts/sf-data-migration
./migrate-claim-images.sh --org <alias>                        # 전체 (로컬 준비 + 콘솔 안내)
./migrate-claim-images.sh --org <alias> --count-only           # 추출 대상 건수만 (다운로드 안 함)
./migrate-claim-images.sh --org <alias> --limit 100            # 샘플 100건만 (검증용)
./migrate-claim-images.sh --org <alias> --parallel 12          # 다운로드 12개 동시 (수십시간→수시간)
./migrate-claim-images.sh --org <alias> --skip-query --skip-download   # 변환(build-csv)만 재시도
```

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--org <alias>` | (SF CLI 기본 org) | 대상 SF org alias |
| `--api-version <v>` | `60.0` | SF API 버전 |
| `--image-prefix <p>` | `uploads/claim/migrated` | `upload_file.unique_key` prefix. **`private/`·`public/` 로 시작 금지**(가드로 거부) — backend 가 조회 시 `private/` 를 합성하므로 중복된다 |
| `--image-exts <csv>` | `jpg,jpeg,png,gif,bmp,webp,heic,heif` | 이미지 확장자 화이트리스트(PDF 등 비이미지 첨부 제외). 레거시 UI 허용분으로 좁힐 때 사용 |
| `--stage1-prefix <p>` | `sf-migration/claim-images` | Stage1 CSV 의 S3 prefix. 레거시 `sf-migration/input/upload_files.csv` 와 파일명이 겹쳐 **별도 prefix 로 격리** |
| `--out-dir <path>` | `input/claim-images` | 로컬 산출물 디렉토리 |
| `--skip-query` / `--skip-download` / `--skip-build-csv` | off | 해당 단계 건너뛰기(부분 재시도) |
| `--count-only` | off | 추출 대상 건수만 출력하고 종료 |
| `--limit <n>` | (전체) | 샘플 n 건만 추출 |
| `--parallel <n>` | `1` | 다운로드 동시 실행 수. SF API rate limit 내에서 **8~16 권장** |

**후속 (스크립트 출력 안내대로)**:
1. AWS 콘솔 업로드 — `<out>/images/*` → `s3://<bucket>/private/uploads/claim/migrated/`, `<out>/claim_upload_files.csv` → `s3://<bucket>/sf-migration/claim-images/`
2. web Stage 1 — **target=`ClaimImageUploadFile`**, `s3KeyPrefix=sf-migration/claim-images`
3. web Stage 2 — **UploadFile Parent Resolve**(`upload-file-polymorphic-parent`)

> 클레임 이미지 출처는 `ContentDocumentLink.LinkedEntityId`(운영 `RecordId__c`/`Type__c` 는 전부 null). `UploadFile__c` 는 적재된 적 없음.

#### 1-5-2. 공지 본문 이미지 — `migrate-notice-rta-images.sh`

파이프라인: `scan`(본문 파싱 → `notice-rta-scan.csv`, 한 행 = rtaImage `<img>` 1개) → `download`(rtaImage 서블릿 GET → `images/{refid}.{ext}`, 증분) → `build-csv`(→ `notice_image_upload_files.csv`). 산출물은 `input/notice-images/`.

```bash
./migrate-notice-rta-images.sh --org <alias>                   # 전체
./migrate-notice-rta-images.sh --org <alias> --count-only      # 본문 rtaImage 건수만
./migrate-notice-rta-images.sh --org <alias> --limit 50        # 샘플 50개 공지만 scan
./migrate-notice-rta-images.sh --org <alias> --parallel 8      # 다운로드 8개 동시
./migrate-notice-rta-images.sh --org <alias> --sid <쿠키값>     # Bearer 실패 시 sid 쿠키 fallback
./migrate-notice-rta-images.sh --org <alias> --skip-scan --skip-download   # 변환만 재시도
```

옵션은 클레임 스크립트와 동일하되 아래가 다르다:

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--skip-scan` | off | (클레임의 `--skip-query` 에 대응) 본문 scan 건너뛰기 |
| `--sid <쿠키값>` | (없음) | **공지 전용**. Bearer 인증이 file 도메인에서 실패할 때 브라우저에서 추출한 sid 쿠키로 fallback |
| `--image-prefix <p>` | `uploads/notice/migrated` | 동일 가드(`private/`·`public/` 시작 금지) |
| `--stage1-prefix <p>` | `sf-migration/notice-images` | 클레임/레거시 CSV 와 분리 |
| `--out-dir <path>` | `input/notice-images` | |

**후속**:
1. AWS 콘솔 업로드 — `<out>/images/*` → `s3://<bucket>/private/uploads/notice/migrated/`, CSV → `s3://<bucket>/sf-migration/notice-images/`
2. web Stage 1 — **target=`NoticeImageUploadFile`**
3. web Stage 2 — UploadFile Parent Resolve + **공지 본문 이미지 placeholder 치환**(§1-4 의 4번)
4. **본문 URL 치환** (적재 완료 후 사용자 실행, **기본 dry-run**):

```bash
kotlinc -script replace-notice-rta-urls.main.kts -- \
    --scan-csv input/notice-images/notice-rta-scan.csv          # dry-run — 변경 대상만 출력
kotlinc -script replace-notice-rta-urls.main.kts -- \
    --scan-csv input/notice-images/notice-rta-scan.csv --apply  # 실제 UPDATE
```

본문의 rtaImage `<img>` 를 `<img src="notice-image://{refid}" data-refid="{refid}" alt="{alt}">` placeholder 로 통째 치환한다. presigned URL 은 만료되므로 본문에 완성 URL 을 박지 않고, 조회 시점에 backend(`NoticeService.getNoticeDetail`)가 rewrite 한다. 멱등(이미 `data-refid` 가 있으면 skip) + DB 연결은 `db.properties` 재사용.

> 본문에 rtaImage 가 없고 첨부 위젯 이미지만 있는 공지는 이미 `upload_file` 에 적재되어 있어 **scan 에서 자동 제외**된다.

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

**SF 마이그레이션 완료 후** 진행. 적재 메타는 backend `HerokuStage1Targets` 가 `@HerokuOnly` + `@HCColumn` 리플렉션으로 자동 생성(SoT — 별도 매핑 표 불요). 대상은 `@HerokuOnly` **19개 테이블**.

**선행 조건 (필수)**: SF 마이그레이션으로 `employee` / `account` / `product` / `display_work_schedule` / `team_member_schedule` 이 적재되어 있어야 한다 — 패턴 A 자연키 lookup + 패턴 C sfid lookup 이 이들을 참조한다.

**FK 해소 패턴 3종** — Stage 2 가 무엇을 하는지의 축:

| 패턴 | 해소 방식 | 처리 위치 |
|------|----------|----------|
| **A** | 자연키 → 신규 serial id (SF 적재분 참조) | 2-4 |
| **B** | 부모 FK (`edu_id` 등 CSV 내 부모키) | 2-4 |
| **C** | `*_sfid` → `*_id` (18자 SF Id) | **2-5** (SF 화면 재사용) |

### 실행 순서 한눈에 (Heroku)

```
0.  SF 마이그레이션 완료 (§1) — employee/account/product/... 적재 전제
1.  TablePlus export — salesforce2.<table> 19개 → CSV        (§2-1)
2.  S3 업로드 s3://<bucket>/heroku-migration/input/          (§2-2)
3.  web /admin/tools/heroku-migration-1 → 일괄 적재(19개)     (§2-3)
4.  web /admin/tools/heroku-migration-2 → FK Resolve (패턴 A+B) (§2-4)
5.  web /admin/tools/sf-migration-2     → FK Resolve 재실행 (패턴 C) (§2-5)
6.  web /admin/tools/heroku-migration-2 → 초기 비밀번호 적재   (§2-6) ★ 누락 주의
7.  PII 정리 (cut-over 완료 후)                               (§2-7)
```

### 2-1. TablePlus Export (사용자 실행)

`salesforce2.<table>` 을 `SELECT * FROM salesforce2.<table>` 로 export (alias / 순서변경 / 컬럼누락 금지 — 헤더 = Heroku 원본 컬럼명 = `@HCColumn` value).

아래 표는 **TablePlus 좌측 테이블 목록에 보이는 순서**(`salesforce2` 스키마 표시 순)로 정렬했다 — 화면을 위에서 아래로 훑으며 하나씩 export 할 때 누락 확인이 쉽다. **적재 순서가 아니다**(적재 순서는 backend `HerokuStage1Targets` 가 결정하며 web 화면 "일괄 적재" 가 알아서 처리한다).

| # | CSV (Heroku 원본) | 엔티티 | 비고 |
|---|-------------------|--------|------|
| 1 | product_favorites.csv | FavoriteProduct | |
| 2 | safetycheck__workschedule__member.csv | SafetyCheckSubmission | 패턴 C (`*_sfid`) |
| 3 | safetycheck_list.csv | SafetyCheckItem | |
| 4 | tmp_claim.csv | TmpClaim | TEXT 개행/콤마 (`description`) |
| 5 | tmp_claimcode.csv | TmpClaimCode | |
| 6 | tmp_onsite.csv | TmpOnsite | |
| 7 | tmp_order.csv | TmpOrder | |
| 8 | tmp_order_product.csv | TmpOrderProduct | |
| 9 | tmp_promotion.csv | TmpPromotion | |
| 10 | tmp_suggest.csv | TmpSuggest | |
| 11 | device_version_mng.csv | DeviceVersion | |
| 12 | education_code_mng.csv | EducationCode | |
| 13 | education_file_mng.csv | EducationPostAttachment | 패턴 B — `edu_id` 누락 금지 |
| 14 | education_member_history.csv | EducationViewHistory | 패턴 B — `edu_id` 누락 금지 |
| 15 | education_mng.csv | EducationPost | 한글 다수 (UTF-8) |
| 16 | employee_admin_mng.csv | EmployeeAdmin | |
| 17 | employee_his.csv | LoginHistory | |
| 18 | employee_mng.csv | **EmployeeInfo** | ⚠️ **PII** |
| 19 | expirationdate__mng.csv | ProductExpiration | 패턴 C (`*_sfid`) |

**같은 스키마에 보이지만 대상이 아닌 테이블** (TablePlus 목록에서 건너뛴다): `pushmessagereceiver__c` · `staffreview__c` · `if_product__c` · `productbarcode__c` · `pushmessage__c` · `theme__c` · `monthlysaleshistory__c` · `uploadfile__c` · `_hcmeta` · `_sf_event_log` · `_trigger_log` · `_trigger_log_archive` · `agreementword__c` · `displayworkschedulemaster__c` · `commute_distance` · `dkretail__employee__c` · `dkretail__notice__c` · `account` · `dkretail__teammemberschedule__c` · `dkretail__product__c` · `hqreview__c`. `dkretail__*` / `account` 등 SF 동기화 테이블은 **SF 마이그레이션(§1)이 담당**하므로 여기서 export 하지 않는다.

**export 규칙**:

| 항목 | 규칙 |
|------|------|
| 헤더 | **첫 행에 컬럼명 포함(필수)** — 파서가 헤더로 `@HCColumn` value ↔ 헤더명 매핑 |
| NULL | `\N`(COPY 규약) 또는 빈 문자열 |
| 인코딩 | **UTF-8 고정, Excel 경유 금지**(CP949 오염 — `education_mng` / `tmp_claim.description` 한글 다수) |
| TEXT | 개행/콤마는 RFC4180 quoting |
| timestamp | Heroku `inst_date` UTC 여부 확인 → 신규 `created_at` KST 변환 정책 |
| boolean | `isdeleted` / `gps_yn` 등 `t/f` vs `true/false` — COPY 시 자동 cast |
| 패턴 B 부모키 | `education_file_mng` / `education_member_history` 의 `edu_id` **누락 금지** |
| 패턴 C sfid | `employeeid__c` / `masterId` / `eventmasterid` 가 **18자 sfid** 인지 확인 |

> Heroku DB 직접 SELECT 는 자발 금지(external-system-policy). 본 export 는 **사용자가 TablePlus 로 직접 수행** + S3 업로드도 사용자 수동이라 정책 위반이 아니다.

**제외 2개**: `if_product__c`(ProductSyncBuffer, PLM 미재현), `commute_distance`(대응 엔티티 없음).

**PII 컬럼**:
- `employee_mng` = 사번 / `emp_pwd`(BCrypt) / **`emp_uuid`(기기 UUID)** / **`emp_token`(FCM 토큰)**.
- `emp_token`(→ fcm_token) / `emp_uuid`(→ device_uuid) 은 **적재 파서가 매핑 제외**라 CSV 에 있어도 적재되지 않는다(신규 앱 로그인 시 재등록). export 시 컬럼 유지/제거 무관.
- cut-over 완료 후 S3 의 `employee_mng.csv` 등 PII 객체 삭제.

### 2-2. S3 업로드 (사용자 실행)

`s3://<bucket>/heroku-migration/input/<원본테이블명>.csv`.

### 2-3. Stage 1 적재 — web `/admin/tools/heroku-migration-1`

backend 가 S3 stream 을 PostgreSQL COPY 로 적재한다. **자연키만 채우고 FK `*_id` 는 NULL 로 둔다**(EmployeeInfo 만 `employee` JOIN 으로 PK 를 채움) — FK 는 Stage 2 에서 해소.

- **"일괄 적재 (19개 전체)"**, **Reset 모드 체크 권장**(적재 전 해당 19개 테이블 TRUNCATE).
- S3 에 CSV 없는 entity(404) → **SKIPPED** 로 건너뛰고 계속(일부만 export 해도 있는 것만 적재). 단건 적재(SINGLE)는 파일 없으면 그대로 실패. **FAILED(적재 오류)는 batch 중단**.
- `EmployeeInfo` 는 `employee` 미적재 고아 row 가 `unmatched` 로 집계(INSERT 제외 — 공유 PK resolve).
- **dev 초기화**: `kotlin scripts/heroku-data-migration/reset-dev.main.kts` — 19개 테이블만 TRUNCATE(운영 RDS 거부, localhost 전용). 전 스키마 초기화는 §1-0 의 `db-reset.sh`.

### 2-4. Stage 2 FK Resolve (패턴 A+B) — web `/admin/tools/heroku-migration-2`

- **"FK Resolve 실행"** — 패턴 A(자연키 → serial id) + 패턴 B(부모 FK) LEFT JOIN UPDATE 일괄.

### 2-5. Stage 2-C sfid FK (패턴 C) — web `/admin/tools/sf-migration-2`

- **SF 화면의 FK Resolve 재실행** — `ProductExpiration` / `SafetyCheckSubmission` 의 `*_sfid → *_id`(sfid 자동 스캔). Heroku 화면이 아니라 **SF 화면**이라는 점에 주의.

### 2-6. 초기 비밀번호 적재 (EmployeeInfo) — web `/admin/tools/heroku-migration-2`

화면 **최하단 "초기 비밀번호 적재 (BCrypt) — EmployeeInfo"** 카드의 "실행" 버튼.

- `employee_info.password` ← 사번 기반 초기 평문 **`{사번}@pwrs`**(`TemporaryPasswordPolicy.forEmployeeCode`, 사번 없으면 `pwrs1234!` fallback)의 BCrypt hash + `password_change_required=TRUE`.
- 대상은 `password IS NULL OR password = ''` — **멱등**.
- **레거시 `emp_pwd` 는 재사용하지 않는다** — 레거시 BCrypt hash 를 이관하지 않고 전원 동일 초기값으로 재발급.
- ⚠️ **SF 화면의 Stage 2-C 와 별개다** — SF 쪽은 `user`(web 로그인), 본 카드는 `employee_info`(mobile 로그인)로 **대상 테이블이 다르다**. 동일 초기 평문 상수를 공유하지만 **양쪽 화면에서 각각 1회씩 실행**해야 한다(모바일 로그인 불가 사고의 흔한 원인).
- curl 대안: `curl -X POST "$BASE/api/v1/admin/heroku-migration/stage2/password" -H "Authorization: Bearer $JWT"`

### 2-7. PII 정리 (cut-over 완료 후)

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
- **truncate 초기화**: `db-reset.sh -s <env>` truncate — flyway 이력 보존, 데이터만 비움(깨끗한 시작). **backend 재기동 불요**. 상세는 1-0.
- **recreate 금지**: 스키마 + flyway 이력까지 삭제라 마이그레이션 리허설엔 과하다. 부득이 실행했다면 **backend 재기동으로 Flyway 재실행**(1-0-1)이 반드시 뒤따라야 스키마가 복구된다.
- **backend 재기동이 필요한 두 경우** — ① recreate 후 스키마 재생성(1-0-1) ② 마이그레이션 관련 코드 수정 반영을 위한 재배포(0. 사전 조건 1). 원인이 다르므로 혼동하지 않는다.
- **DB 직접 접근**: 진단 SELECT/UPDATE 는 정책상 **사용자가 직접 실행**(SQL 제시 → 사용자 실행 → 결과 회신). 검증용 스키마 CREATE/out-of-order 마이그레이션도 금지.
