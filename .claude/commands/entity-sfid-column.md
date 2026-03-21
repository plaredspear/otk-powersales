---
description: 엔티티 sfid 컬럼 추가 스펙 생성 (@HCColumn/@SFField가 sfid를 참조하는 FK 필드에 sfid String 컬럼 추가)
runOn: project
---

# 엔티티 sfid 컬럼 추가 스펙 생성

엔티티의 `@HCColumn` 또는 `@SFField` 어노테이션이 달린 필드 중, Heroku DB 원본이 sfid(varchar(18))를 참조하는 FK 컬럼을 찾아 sfid String 컬럼을 추가하는 스펙을 자동 생성합니다.

## 입력 파라미터

사용자 인자: $ARGUMENTS

인자로 다음 중 하나를 받습니다:
- **Entity 명** (클래스명, 예: `SafetyCheckSubmission`)
- **테이블명** (DB 테이블명, 예: `safety_check_submission`)

인자가 비어 있으면 사용자에게 엔티티명을 질문합니다.

## 워크플로우

### 1. Entity 파일 탐색

`backend/src/main/` 하위에서 인자와 매칭되는 Entity 클래스 파일(`.kt`)을 찾습니다.

- 클래스명, 파일명, `@Table(name = "...")` 어노테이션으로 매칭
- **Entity 파일이 없으면 작업을 중단**합니다.

해당 Entity 파일을 Read 하여 현재 필드 구조를 파악합니다:
- `@HCColumn`, `@SFField` 어노테이션이 붙은 필드 목록
- `@HCTable`, `@SFObject` 클래스 어노테이션의 값
- 각 필드의 타입 (`Long`, `Long?`, `String?` 등)

### 2. sfid 참조 후보 필드 식별

다음 조건을 **모두** 만족하는 필드를 sfid 참조 후보로 식별합니다:

1. `@HCColumn` 또는 `@SFField` 어노테이션이 있다
2. 현재 타입이 `Long` 또는 `Long?` 이다 (이미 `String?`이면 sfid 컬럼이 이미 존재할 수 있으므로 제외)
3. 필드명이 `*Id` 패턴이다 (FK 성격)

**제외 대상:**
- PK 필드 (`@Id` 어노테이션)
- 이미 대응하는 `*Sfid: String?` 필드가 같은 엔티티에 존재하는 필드
- 데이터 값 컬럼 (count, date, flag 등 명백히 FK가 아닌 필드)

### 3. Heroku DB 검증

`@HCTable` 값으로 Heroku DB에 접속하여 후보 필드의 원본 컬럼 타입을 확인합니다.

**접속 정보**: MEMORY.md의 Heroku DB 접속 정보를 사용합니다.

```bash
# salesforce 스키마에서 테이블 컬럼 조회
psql "<접속문자열>" -c "\d salesforce.<HCTable값>"
```

- `salesforce` 스키마를 먼저 확인하고, 없으면 `salesforce2` 스키마를 확인합니다.
- 후보 필드의 HC 컬럼이 `varchar(18)` 타입이면 → **sfid 참조 확정**
- `varchar(18)`이 아니면 → 후보에서 제외

**Heroku DB 테이블이 없는 경우**: Salesforce 소스(`docs/plan/old_source/`)에서 `salesforce-analyzer` 에이전트를 활용하여 필드의 데이터 유형(Lookup/MasterDetail 관계)을 확인합니다.

### 4. sfid 참조 대상 엔티티 확인

확정된 각 sfid 필드에 대해, 참조 대상 엔티티를 파악합니다:

1. `heroku-analyzer` 에이전트를 활용하여 Heroku 소스에서 해당 컬럼이 어떤 테이블의 sfid와 조인되는지 확인
2. 또는 `salesforce-analyzer` 에이전트를 활용하여 Salesforce 필드 정의에서 Lookup/MasterDetail 대상 오브젝트를 확인
3. HC 컬럼 DB 주석(COMMENT)이 있으면 참고

확인 결과를 매핑 테이블로 정리:

| 필드 | HC 원본 컬럼 | DDL 타입 | 참조 대상 테이블 | 참조 대상 엔티티 |
|------|------------|---------|---------------|---------------|

### 5. 기존 서비스/리포지토리 영향 분석

엔티티를 사용하는 파일을 탐색합니다:
- Repository: 쿼리 메서드에서 sfid 추가 대상 필드를 사용하는지
- Service/Controller: 해당 필드를 직접 참조하는지

기존 Long 필드의 `@Column`은 변경하지 않으므로 대부분 영향 없음을 확인합니다.

### 6. HerokuMigrationTool 현황 확인

`backend/src/main/kotlin/com/otoki/internal/migration/HerokuMigrationTool.kt`에서 해당 엔티티의 현재 기재 내용을 확인합니다:
- 참조키(sfid FK) 컬럼 정보
- Migrate 여부
- 비고

### 7. SFSchemaUtilsTest 확인

`backend/src/test/kotlin/com/otoki/internal/common/salesforce/SFSchemaUtilsTest.kt`에서 해당 엔티티의 SF/HC 필드 매핑 검증이 있는지 확인합니다. 있으면 스펙의 영향 범위에 포함합니다.

### 8. 스펙 파일 작성

`docs/specs/backlog/` 에 스펙을 생성합니다.

- 스펙 번호: `backlog/` + `ready/` + `completed/` 전체에서 최대 번호 + 1
- 폴더명: `<번호>-<entity명(소문자-kebab)>-sfid-columns`
- 파일: `spec-B.md` (Backend 단일 플랫폼)

스펙 내용은 `docs/specs/completed/375-teammemberschedule-sfid-columns/spec-B.md`의 구조를 참고하여 작성합니다. 필수 포함 사항:

#### 8-1. 공통 섹션

1. **개요 테이블**: 스펙 번호, 기능명 (`<Entity명> 엔티티 sfid 컬럼 추가`), 플랫폼(B), 상태(DRAFT), Part 수(1)
2. **승인 이력**: AI 리뷰 PENDING, 사람 리뷰 PENDING
3. **변경 이력**: v1.0 초안 작성

#### 8-2. 배경 / 현재 상태

- 문제: 엔티티의 N개 필드에 레거시 어노테이션이 있으나 HC 원본 컬럼이 sfid(varchar(18))를 저장. 현재 Long 타입으로 매핑되어 타입 불일치
- Heroku DB DDL 확인 결과 테이블 (각 컬럼의 DDL 타입, DB 주석, 참조 대상)
- 이미 올바르게 매핑된 필드 목록 (변경 불필요한 필드)
- 해결 방향: sfid String 컬럼 추가 → 레거시 어노테이션 이동 → 기존 Long 필드 유지

#### 8-3. 완료 조건

각 sfid 필드에 대해:
- [ ] 엔티티에 `<필드명>Sfid: String?` 필드가 추가되어 있다 (어노테이션 명시)
- [ ] 기존 Long 필드에서 `@HCColumn`/`@SFField` 어노테이션이 제거되어 있다

공통:
- [ ] Flyway 마이그레이션 파일이 추가되어 N개 sfid 컬럼이 추가된다
- [ ] HerokuMigrationTool 문서 테이블 반영
- [ ] SFSchemaUtilsTest 반영 (해당 시)
- [ ] `./gradlew test` 기존 테스트 전체 통과

#### 8-4. 데이터 모델

변경 사항 테이블 (필드명 | 타입 | 필수 | 변경 내용):
- 기존 Long 필드: 레거시 어노테이션 제거, `@Column` 유지
- 신규 sfid 필드: `@HCColumn`/`@SFField` + `@Column(name = "..._sfid", length = 18)`

Flyway 마이그레이션 테이블 (컬럼명 | 타입 | 설명):
- 각 `*_sfid` 컬럼: `VARCHAR(18)`, NULL 허용

#### 8-5. 비즈니스 로직

- 기존 서비스 영향: 각 서비스/리포지토리 메서드가 Long 필드를 사용하며 변경 불필요임을 명시
- HerokuMigrationTool 문서 테이블 수정: 참조키 컬럼 추가, 비고에 `V{N}: sfid 컬럼 추가` 기재
- SFSchemaUtilsTest 수정 (해당 시): 기존 Long 필드 → 새 sfid 필드 반영

#### 8-6. 영향 범위 / 파일 목록

수정 파일과 신규 파일을 구분하여 테이블로 작성

#### 8-7. 테스트 시나리오

| 시나리오 | 조건 | 예상 결과 |
|---------|------|----------|

Happy Path: 컴파일 성공, 기존 테스트 통과, 마이그레이션 성공, 기존 동작 유지 (각 Long 필드별)

### 9. 자동 리뷰

스펙 작성 완료 후 `.claude/commands/spec-review.md`를 읽고 자동 리뷰를 수행합니다.

### 10. 완료 안내

```
스펙이 생성되었습니다.
스펙: docs/specs/backlog/<번호>-<entity명>-sfid-columns/spec-B.md
대상 필드: N개 (각 필드명 나열)
```

## sfid 후보가 없는 경우

Entity에 sfid 참조 후보 필드가 하나도 없으면 작업을 중단하고 다음을 안내합니다:

```
<Entity명> 엔티티에 sfid 참조 후보 필드가 없습니다.

확인 결과:
- @HCColumn/@SFField 어노테이션이 있는 Long 타입 FK 필드: 0개
- 이미 sfid 컬럼이 존재하는 필드: N개 (목록)
- 데이터 값 컬럼 (비FK): N개 (목록)
```
