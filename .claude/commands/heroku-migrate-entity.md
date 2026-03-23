---
description: HerokuMigrationTool에 엔티티 마이그레이션 등록 스펙 생성
runOn: project
---

# Heroku DB 마이그레이션 엔티티 등록 스펙 생성

HerokuMigrationTool에 엔티티를 등록하여 Heroku DB → Dev DB 데이터 마이그레이션을 지원하는 스펙을 자동 생성합니다.

## 입력 파라미터

사용자 인자: $ARGUMENTS

인자로 **엔티티명** (클래스명, 예: `TeamMemberSchedule`, `SafetyCheckSubmission`)을 받습니다.
인자가 비어 있으면 사용자에게 엔티티명을 질문합니다.

## 워크플로우

### 1. 엔티티 파일 탐색

`backend/src/` 하위에서 인자와 매칭되는 Entity 클래스 파일(`.kt` 또는 `.java`)을 찾습니다.

- 클래스명, 파일명, `@Table(name = "...")` 어노테이션으로 매칭
- **Entity 파일이 없으면 작업을 중단**합니다.

해당 Entity 파일을 Read 하여 현재 필드 구조, `@HCTable`, `@HCColumn`, `@Column`, `@Id` 어노테이션을 파악합니다.

**필수 확인**:
- `@HCTable` 어노테이션이 있는지 확인. 없으면 작업 중단 (마이그레이션 불가)
- `@HCColumn` 어노테이션이 충분히 있는지 확인

### 2. HerokuMigrationTool 현재 상태 확인

`HerokuMigrationTool.kt` 파일을 Read 하여:
- 해당 엔티티가 이미 등록되어 있는지 확인. 등록되어 있으면 작업 중단
- KDoc 매핑 테이블에서 해당 엔티티의 정보를 확인 (참조키, 비고 등)
- 기존 등록된 엔티티들의 패턴(columnTransformProvider, 후속 UPDATE, dependentTables)을 파악

### 3. FK 참조 분석

Entity 필드를 분석하여 FK 참조를 식별합니다:

**sfid와 PK/FK의 관계**:
- Heroku DB는 Salesforce의 `sfid`(18자리 문자열)를 레코드 식별자로 사용
- Dev DB는 `IDENTITY` 자동 채번 PK(Long)를 사용하므로, sfid는 **데이터 마이그레이션 및 연결 참조용으로만 저장**
- 마이그레이션 시 sfid를 그대로 가져오되, PK는 새로 생성하고, FK는 sfid를 기준으로 참조 테이블을 검색하여 새 PK 값으로 다시 채움

**예시 (Notice → Employee FK)**:
```
1. INSERT 단계: Heroku employeeid__c 값("a03xx...") → Dev DB employee_sfid 컬럼에 저장 (employee_id는 NULL)
2. post-UPDATE 단계: employee_sfid로 employee 테이블을 검색하여 새로 생성된 employee_id(PK)를 채움
   → UPDATE notice n SET employee_id = e.employee_id FROM employee e WHERE n.employee_sfid = e.sfid
```

**FK 판별 기준**:
- `@HCColumn`이 있는 sfid 저장 필드 (예: `account_sfid`, `employee_sfid`)와 대응하는 PK 저장 필드 (예: `account_id`, `employee_id`)가 쌍으로 존재
- sfid 저장 필드: `@HCColumn`이 있으므로 INSERT 시 Heroku 값이 그대로 저장됨
- PK 저장 필드: `@HCColumn`이 없으므로 INSERT에서 제외, **후속 UPDATE로 sfid 기준 검색하여 새 PK 값을 채움**
- HerokuMigrationTool KDoc 매핑 테이블의 "참조키" 컬럼에 기록된 sfid FK 정보

**FK가 있는 경우**:
- 참조 대상 테이블의 PK 컬럼명을 확인 (Dev DB에서 `\d <테이블>` 또는 Entity 확인)
- 후속 UPDATE 패턴 적용 (Notice, DisplayWorkSchedule, ProductBarcode 사례 참조)
- columnTransformProvider는 사용하지 않음 (allJpaColumns에 포함되지 않아 동작 불가)

**FK가 없는 경우**:
- 단순 등록 (AgreementWord, PushMessage 사례 참조)

### 4. Heroku DB 데이터 건수 확인

MEMORY.md의 Heroku DB 접속 정보를 사용하여 해당 테이블의 데이터 건수를 조회합니다.

```bash
psql "<접속문자열>" -c "SELECT COUNT(*) FROM salesforce2.<heroku_table_name>"
```

`salesforce2` 스키마를 먼저 확인하고, 없으면 `salesforce` 스키마를 확인합니다.

### 5. 스펙 파일 작성

`docs/specs/backlog/` 에 스펙을 생성합니다.

- 스펙 번호: `backlog/` + `ready/` + `completed/` 전체에서 최대 번호 + 1
- 폴더명: `<번호>-heroku-migrate-<entity명(kebab-case)>`
- 파일: `spec-B.md` (Backend 단일 플랫폼)

**스펙 내용에 포함할 사항**:

#### 5-1. 배경/현재 상태
- Heroku DB 데이터 건수
- Entity의 @HCTable/@HCColumn 어노테이션 상태
- FK 참조 관계 테이블 (있는 경우)

#### 5-2. 완료 조건
- entities 리스트 등록
- PK 제외 (IDENTITY 자동 채번)
- FK 후속 UPDATE (있는 경우, 각 FK 별로 1항목)
- 데이터 이관 건수
- 기존 엔티티 회귀 없음

#### 5-3. 데이터 모델
- Heroku DB 스키마 (컬럼, 타입, 설명)
- 컬럼 매핑 (Heroku → Dev DB): 마이그레이션 여부, 변환 유형 (없음/후속 UPDATE)

#### 5-4. 변경 사항
- HerokuMigrationTool entities 리스트 등록 (패턴 명시)
- 후속 UPDATE 내용 (FK가 있는 경우)
- KDoc 매핑 테이블 업데이트

#### 5-5. 비즈니스 로직
- HerokuMigrationTool 실행 흐름 (SELECT → TRUNCATE → INSERT → UPDATE)

#### 5-6. 영향 범위 / 파일 목록
- 수정: HerokuMigrationTool.kt
- 변경 없음: Entity (이미 어노테이션 있음), Flyway, SFSchemaUtils

#### 5-7. 테스트 시나리오
- Happy Path: 마이그레이션 성공, PK 자동 채번, sfid 보존, FK 변환 검증, 타임스탬프 보존, 회귀 없음
- Error Path: 중복 실행, 참조 대상 없음

**기존 스펙 참조**: `docs/specs/completed/367-heroku-migrate-push-message/spec-B.md` (FK 없는 단순 사례), `docs/specs/completed/372-displayworkschedule-employee-owner-sfid-column/spec-B.md` (FK 후속 UPDATE 사례)

### 6. 자동 리뷰

스펙 작성 완료 후 `.claude/commands/spec-review.md`를 읽고 자동 리뷰를 수행합니다.

### 7. 완료 안내

```
스펙이 생성되었습니다.
스펙: docs/specs/backlog/<번호>-heroku-migrate-<entity>/spec-B.md
Heroku 데이터: <건수>건
FK 참조: <FK 수>개 (후속 UPDATE)
```
