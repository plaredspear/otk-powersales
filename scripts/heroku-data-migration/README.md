# Heroku 데이터 마이그레이션 (스펙 #853)

런칭 cut-over 시점에 Heroku PG `salesforce2.*` 의 **Heroku-only 테이블**(`@HerokuOnly` 19개)
데이터를 신규 시스템(Spring Boot + PostgreSQL `powersales`)으로 1회 이전한다.

SF 데이터 마이그레이션(`scripts/sf-data-migration/`)과 달리 **적재 본체는 backend 의 web 운영
도구**다(S3 COPY). 본 폴더는 export → S3 업로드 가이드 + dev 보조 스크립트만 둔다.

## 전체 흐름 (2-Stage)

```
[Heroku PG salesforce2.*] ─(TablePlus export, 수동)→ CSV ─(S3 업로드, 수동)→ s3://<bucket>/heroku-migration/input/*.csv
   ↓
Stage 1 (web 화면 /admin/tools/heroku-migration-1)
   - backend 가 S3 stream 을 PostgreSQL COPY 로 적재
   - 자연 키만 채우고 FK *_id 는 NULL (EmployeeInfo 만 employee JOIN 으로 PK 채움)
   - Reset 모드(적재 전 TRUNCATE) 기본
   ↓
Stage 2 (web 화면 /admin/tools/heroku-migration-2)
   - 패턴 A(자연키→serial id) + 패턴 B(부모 FK) LEFT JOIN UPDATE
   ↓
Stage 2-C (SF 마이그레이션 web 화면 /admin/tools/sf-migration-2 의 FK Resolve 재사용)
   - ProductExpiration / SafetyCheckSubmission 의 *_sfid → *_id (sfid 자동 스캔)
```

## 실행 순서 (cut-over)

1. **SF 데이터 마이그레이션 선행** — `employee` / `account` / `product` / `display_work_schedule`
   / `team_member_schedule` 가 신규 DB 에 적재되어 있어야 한다(패턴 A 자연 키 lookup + 패턴 C sfid lookup 전제).
2. **TablePlus export** — 아래 §export 가이드대로 19개 테이블을 CSV 로 export.
3. **S3 업로드** — `s3://<S3_BUCKET>/heroku-migration/input/<heroku원본테이블명>.csv` 로 업로드.
4. **Stage 1 적재** — web `/admin/tools/heroku-migration-1` 에서 "일괄 적재 (19개 전체)". Reset 모드 체크 권장.
   - EmployeeInfo 는 employee 미적재 고아 row 가 `unmatched` 로 집계된다(INSERT 제외).
5. **Stage 2 FK Resolve** — web `/admin/tools/heroku-migration-2` 에서 "FK Resolve 실행". 패턴 A+B 일괄.
6. **Stage 2-C sfid FK** — SF 마이그레이션 web `/admin/tools/sf-migration-2` 의 FK Resolve 재실행(ProductExpiration / SafetyCheckSubmission 의 *_sfid → *_id).
7. **PII 정리** — cut-over 완료 후 S3 의 `employee_mng.csv`(사번/비밀번호/디바이스 UUID/FCM 토큰) 등 객체 삭제.

> 권한: Stage 1·2 web 화면은 로그인만 요구하고 사이드 메뉴 미노출(URL 직접 진입). 권한 부트스트랩
> 닭-달걀 회피. cut-over 완료 후 가드 복원 권장.

## TablePlus Export 가이드

| # | 주의점 | 대응 |
|---|--------|------|
| 0 | **헤더 행 포함 (필수)** | 첫 행에 컬럼명 헤더 포함. 적재 파서가 헤더로 `@HCColumn` value ↔ 헤더명 매핑. **헤더 = Heroku 원본 컬럼명** → `SELECT * FROM salesforce2.<table>` 로 export(alias/순서변경/일부컬럼 누락 금지) |
| 1 | NULL 표현 | export NULL 을 `\N` 으로 지정(COPY `NULL '\N'` 규약) 또는 빈 문자열 |
| 2 | UTF-8 인코딩 | 한글 다수(education_mng / tmp_claim.description). UTF-8 고정, **Excel 경유 금지**(CP949 오염) |
| 3 | TEXT 개행/콤마 | tmp_claim.description / education_mng.content → RFC4180 quoting |
| 4 | timestamp 타임존 | Heroku inst_date UTC 여부 확인 → 신규 created_at KST 변환 정책 |
| 5 | boolean 표현 | isdeleted / gps_yn 등 `t/f` vs `true/false` → COPY 시 자동 cast |
| 6 | PII / 비밀번호 | employee_mng = 사번/emp_pwd/디바이스 UUID/FCM 토큰. **S3 업로드 후 cut-over 완료 시 객체 삭제**. emp_pwd 형식(BCrypt `$2a$` prefix) 확인 |
| 7 | 부모키 포함 | 패턴 B 자식(education_file_mng / education_member_history) export 시 부모 식별 키(`edu_id`) 누락 금지 |
| 8 | sfid 컬럼 검증 | 패턴 C 의 employeeid__c / masterId / eventmasterid 가 18자 sfid 인지 확인 |
| 9 | S3 업로드 | export CSV 를 `s3://<bucket>/heroku-migration/input/<table>.csv` 로 업로드. bucket = 운영 `S3_BUCKET` 환경 속성 |

> Heroku DB 직접 SELECT 는 자발 금지(external-system-policy). 본 export 는 사용자가 TablePlus 로 직접 수행 + S3 업로드도 사용자 수동 → 정책 위반 아님.

## CSV 파일명 ↔ 신규 엔티티 (19개)

CSV 파일명 = Heroku 원본 테이블명 + `.csv`. 적재 메타는 backend `HerokuStage1Targets` 가
`@HerokuOnly` + `@HCColumn` 리플렉션으로 자동 생성한다(단일 출처 SoT — 별도 매핑 표 불요).

`GET /api/v1/admin/heroku-migration/stage1/targets` 가 (targetName, csvFileName) 일람을 반환한다.

| 적재 순서 | 엔티티 | CSV (Heroku 원본) |
|------|--------|---------------------|
| 1 | EducationCode | education_code_mng.csv |
| 2 | TmpClaimCode | tmp_claimcode.csv |
| 3 | DeviceVersion | device_version_mng.csv |
| 4 | SafetyCheckItem | safetycheck_list.csv |
| 5 | EmployeeAdmin | employee_admin_mng.csv |
| 6 | EmployeeInfo | employee_mng.csv |
| 7 | EducationPost | education_mng.csv |
| 8 | EducationPostAttachment | education_file_mng.csv |
| 9 | EducationViewHistory | education_member_history.csv |
| 10 | TmpOrder | tmp_order.csv |
| 11 | TmpOrderProduct | tmp_order_product.csv |
| 12 | TmpClaim | tmp_claim.csv |
| 13 | TmpSuggest | tmp_suggest.csv |
| 14 | TmpOnsite | tmp_onsite.csv |
| 15 | TmpPromotion | tmp_promotion.csv |
| 16 | FavoriteProduct | product_favorites.csv |
| 17 | LoginHistory | employee_his.csv |
| 18 | ProductExpiration | expirationdate__mng.csv |
| 19 | SafetyCheckSubmission | safetycheck__workschedule__member.csv |

> `if_product__c`(ProductSyncBuffer) 는 마이그레이션 제외(PLM 연동 미재현). `commute_distance` 는 대응 엔티티 부재로 대상 외.

## 보조 스크립트

| 파일 | 책임 |
|------|------|
| `verify-metadata.main.kts` | `@HCColumn` ↔ `HerokuStage1Targets` 리플렉션 메타 정합 안내(실제 정합 검증은 `HerokuStage1TargetsTest` 가 담당 — backend 테스트로 자동화) |
| `reset-dev.main.kts` | dev DB 의 19개 Heroku-only 테이블 TRUNCATE(운영 RDS 거부, localhost 만 허용) |
| `db.properties.template` | dev DB 접속 정보 템플릿(복사 후 `db.properties` 작성 — gitignore) |

> Stage 1 적재용 JDBC 로더(`migrate-stage1.main.kts` / `common.kts`)는 **생성하지 않는다**.
> 적재 본체는 backend S3 COPY(web 화면)로 일원화(스펙 Q4 옵션 1).
