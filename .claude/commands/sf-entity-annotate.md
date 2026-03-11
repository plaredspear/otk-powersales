---
description: Salesforce Object 정의를 기반으로 JPA 엔티티에 @SFObject/@SFField + @HCTable/@HCColumn 어노테이션 적용 스펙 생성
runOn: project
---

# SF/HC Entity 어노테이션 스펙 생성

JPA 엔티티에 Salesforce Object 및 Heroku Connect 테이블 메타데이터 어노테이션을 추가하는 스펙을 자동 생성합니다.

## 입력 파라미터

사용자 인자: $ARGUMENTS

인자로 다음 중 하나 이상을 받습니다 (순서 무관, 유추 가능):
- **테이블명** (DB 테이블명, 예: `account`)
- **Entity 명** (클래스명, 예: `Account`)
- **Object 명** (Salesforce Object명, 예: `Account`, 한글 가능: `거래처`)

인자가 비어 있으면 사용자에게 위 정보를 질문합니다.

## 워크플로우

### 1. Salesforce Object 파일 탐색

`docs/plan/old_source/salesforce_object/` 폴더에서 인자와 매칭되는 `.md` 파일을 찾습니다.

- 한글명으로 검색 (파일명이 한글): 예) `거래처.md`
- 영문 Object명으로도 파일 내용을 grep 하여 탐색
- **매칭 파일이 없으면 작업을 중단**하고, 사용 가능한 파일 목록을 안내합니다.

해당 파일을 Read 하여 Salesforce 필드 목록(필드 레이블, 필드 이름, 데이터 유형)을 파악합니다.

### 2. Entity 파일 탐색

`backend/src/` 하위에서 인자와 매칭되는 Entity 클래스 파일(`.kt` 또는 `.java`)을 찾습니다.

- 클래스명, 파일명, `@Table(name = "...")` 어노테이션으로 매칭
- **Entity 파일이 없으면 작업을 중단**합니다.

해당 Entity 파일을 Read 하여 현재 필드 구조(@Column, 기존 어노테이션 등)를 파악합니다.

### 3. Heroku DB 테이블 확인

Heroku DB에 접속하여 해당 테이블의 실제 컬럼 정보를 확인합니다.

**접속 정보**: MEMORY.md의 Heroku DB 접속 정보를 사용합니다.

```bash
# salesforce 스키마에서 테이블 컬럼 조회
psql "<접속문자열>" -c "\d salesforce.<테이블명>"
```

- `salesforce` 스키마를 먼저 확인하고, 없으면 `salesforce2` 스키마를 확인합니다.
- 테이블이 없으면 사용자에게 알리고 HC 어노테이션 없이 SF 어노테이션만 포함하여 진행합니다.

### 4. 매핑 분석

세 소스(SF Object 정의, Entity 필드, HC 테이블 컬럼)를 교차 대조하여 매핑 테이블을 작성합니다.

| Entity 필드 | @Column name | SF 필드명 | HC 컬럼명 | 상태 |
|-------------|-------------|----------|----------|------|
| name | name | Name | name | 매핑 완료 |
| phone | phone | Phone | phone | 매핑 완료 |
| ... | ... | ... | ... | ... |

**매핑 규칙** (Account Entity를 참고):
- `@SFField` 값 = Salesforce Object 정의의 `필드 이름` 컬럼 (예: `Name`, `ABCType__c`)
- `@HCColumn` 값 = Heroku DB의 실제 컬럼명 (보통 SF 필드명의 lowercase, 예: `abctype__c`)
- `id`, `sfid`, `isdeleted` 등 시스템 컬럼은 `@HCColumn`만 부여 (`@SFField` 불필요)
- Entity에 없는 SF 필드나 HC 컬럼은 "미매핑"으로 표시

### 5. 스펙 파일 작성

`docs/specs/backlog/` 에 스펙을 생성합니다.

- 스펙 번호: `backlog/` + `ready/` + `completed/` 전체에서 최대 번호 + 1
- 폴더명: `<번호>-sf-hc-annotate-<entity명(소문자)>`
- 파일: `spec-B.md` (Backend 단일 플랫폼)

스펙 내용에 포함할 사항:

#### 5-1. @SFObject / @SFField 어노테이션 적용

```
클래스 레벨:
  @SFObject("<SalesforceObjectName>")

필드 레벨 (각 매핑된 필드):
  @SFField("<SF필드명>")
```

- Salesforce Object 파일의 필드 정의를 기반으로 어떤 Entity 필드에 어떤 `@SFField` 값을 부여할지 명시
- 매핑 근거(SF 필드 레이블 ↔ Entity 필드명 대응)를 테이블로 제시

#### 5-2. @HCTable / @HCColumn 어노테이션 적용

```
클래스 레벨:
  @HCTable("<heroku_connect_table_name>")

필드 레벨 (각 매핑된 필드):
  @HCColumn("<hc_column_name>")
```

- Heroku DB 테이블의 실제 컬럼명을 기반으로 `@HCColumn` 값을 명시
- `id`, `sfid`, `isdeleted` 등 시스템 컬럼도 포함

#### 5-3. 체크리스트

스펙에 구현 체크리스트를 포함합니다:

- [ ] `@SFObject`, `@HCTable` 클래스 어노테이션 추가
- [ ] 각 필드에 `@SFField`, `@HCColumn` 어노테이션 추가
- [ ] 기존 `@Column` 어노테이션과 충돌 없는지 확인
- [ ] 컴파일 확인

### 6. 자동 리뷰

스펙 작성 완료 후 `.claude/commands/spec-review.md`를 읽고 자동 리뷰를 수행합니다.

### 7. 완료 안내

```
✅ SF/HC 어노테이션 스펙이 생성되었습니다.
📄 스펙: docs/specs/backlog/<번호>-sf-hc-annotate-<entity명>/spec-B.md
📊 매핑: <매핑 완료 필드 수> / <전체 Entity 필드 수> 필드
```
