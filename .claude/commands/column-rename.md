---
description: 엔티티 테이블/컬럼명 가독성 개선 스펙 생성 (PK 변경 + 레거시 컬럼명 정리)
runOn: project
---

# 테이블/컬럼명 가독성 개선 스펙 생성

엔티티의 DB 컬럼명을 가독성 좋은 이름으로 변경하는 스펙을 자동 생성합니다.

## 입력 파라미터

사용자 인자: $ARGUMENTS

인자로 다음 중 하나 이상을 받습니다 (순서 무관, 유추 가능):
- **테이블명** (DB 테이블명, 예: `agreement_word`)
- **Entity 명** (클래스명, 예: `AgreementWord`)

인자가 비어 있으면 사용자에게 위 정보를 질문합니다.

## 워크플로우

### 1. Entity 파일 탐색

`backend/src/` 하위에서 인자와 매칭되는 Entity 클래스 파일(`.kt` 또는 `.java`)을 찾습니다.

- 클래스명, 파일명, `@Table(name = "...")` 어노테이션으로 매칭
- **Entity 파일이 없으면 작업을 중단**합니다.

해당 Entity 파일을 Read 하여 현재 필드 구조(`@Column`, `@Id` 등)를 파악합니다.

### 2. Dev DB 테이블 확인

Dev DB에 접속하여 해당 테이블의 실제 컬럼 정보를 확인합니다.

**접속 정보**: MEMORY.md의 Dev DB 접속 정보를 사용합니다.

```bash
psql -h dev-db.codapt.kr -U otoki_admin -d otoki -c "\d salesforce2.<테이블명>"
```

테이블이 존재하지 않으면 작업을 중단합니다.

### 3. FK 참조 현황 확인

Dev DB에서 해당 테이블의 PK를 참조하는 다른 테이블/컬럼이 있는지 확인합니다.

```bash
psql -h dev-db.codapt.kr -U otoki_admin -d otoki -c "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema='salesforce2' AND column_name LIKE '%<테이블명 키워드>%'"
```

FK 참조가 있는 경우 스펙에 참조 현황을 기록하되, 해당 테이블의 변경은 별도 스펙으로 분리한다.

### 4. 사용처 분석

Entity가 사용되는 Repository, Service, Controller 파일을 탐색합니다:
- Repository의 쿼리 메서드 (파생 쿼리, QueryDSL, JPQL)
- Service/Controller에서의 필드 참조

Kotlin 필드명 기반인지 DB 컬럼명 직접 참조인지 판별합니다.

### 5. 컬럼명 변경 매핑 분석

가독성 개선이 필요한 컬럼을 식별합니다:

**변경 대상 패턴:**
- Salesforce 커스텀 필드 접미사: `*__c` → 접미사 제거 (예: `contents__c` → `contents`)
- Heroku 접두사: `dkretail__*__c` → 접두사/접미사 제거
- 비표준 네이밍: `isdeleted` → `is_deleted` (snake_case 정규화)
- 연속 단어 미구분: `activedate__c` → `active_date`
- PK: `id` → `{table_name}_id`

**변경하지 않는 패턴:**
- 이미 가독성 있는 이름 (예: `name`, `sfid`)
- 다른 스펙에서 이미 변경 완료된 컬럼

**PK 규칙:**
- PK 컬럼은 `{table_name}_id` 형식으로 변경
- `@HCColumn("id")` 가 있으면 제거 (heroku DB의 `id`를 사용하지 않음)

### 6. 스펙 파일 작성

`docs/specs/backlog/` 에 스펙을 생성합니다.

- 스펙 번호: `backlog/` + `ready/` + `completed/` 전체에서 최대 번호 + 1
- 폴더명: `<번호>-column-rename-<entity명(소문자-kebab)>`
- 파일: `spec-B.md` (Backend 단일 플랫폼)

스펙 내용은 `docs/specs/completed/328-pk-column-rename-notice/spec-B.md`의 구조를 참고하여 작성합니다. 필수 포함 사항:

1. **개요 테이블** + **승인 이력** + **변경 이력**
2. **배경 / 현재 상태**: 변경 동기, 대상 엔티티 요약, FK 참조 현황, 선행 스펙
3. **데이터 모델**: 컬럼명 변경 매핑 테이블 (변경 전/후) + 변경하지 않는 필드 테이블
4. **비즈니스 로직**: DB 마이그레이션 (ALTER TABLE RENAME COLUMN 목록, 파일명은 `V{next}__<설명>.sql` 플레이스홀더 사용 — 구현 시 최신 버전 확인 후 결정) + 엔티티 변경 (번호 매긴 목록)
5. **기존 코드 영향 분석**: Repository, Service, Controller, Test 영향 분석
6. **영향 범위 / 파일 목록**: 수정/신규 파일 테이블
7. **완료 조건**: 검증 가능한 체크리스트
8. **테스트 시나리오**: Happy Path + Error Path

### 7. 자동 리뷰

스펙 작성 완료 후 `.claude/commands/spec-review.md`를 읽고 자동 리뷰를 수행합니다.

### 9. 완료 안내

```
✅ 컬럼명 가독성 개선 스펙이 생성되었습니다.
📄 스펙: docs/specs/backlog/<번호>-column-rename-<entity명>/spec-B.md
📊 변경: PK 1개 + 레거시 컬럼 N개
```
