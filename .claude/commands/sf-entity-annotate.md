---
description: Salesforce Object 정의를 기반으로 JPA 엔티티에 @Comment 어노테이션 추가
runOn: project
---

# Salesforce Object → JPA Entity @Comment 어노테이션 추가

Salesforce Object 정의 파일과 Heroku Connect DB를 참조하여 기존 JPA 엔티티에 `@Comment` 어노테이션을 추가합니다.

## 입력 파라미터

사용자 인자: $ARGUMENTS

인자가 비어 있으면 `docs/plan/old_source/salesforce_object/` 폴더의 파일 목록을 보여주고 선택을 요청합니다.

## 워크플로우

### 1. Salesforce Object 파일 읽기

`docs/plan/old_source/salesforce_object/` 에서 `$ARGUMENTS`에 해당하는 `.md` 파일을 읽습니다.

파일 형식은 Markdown 테이블이며, 주요 컬럼:
- **필드 레이블**: 한국어 필드명 (링크 텍스트에서 추출) → `@Comment` 값으로 사용
- **필드 이름**: Salesforce API Name (예: `CostCenterLevel2__c`) → 엔티티 필드 매칭에 사용
- **데이터 유형**: 타입 및 길이 정보 → `@Column` length 검증에 사용

### 2. Heroku Connect DB 테이블 존재 여부 확인

Heroku Connect DB(접속 정보: `docs/plan/old-accounts.json`)에서 해당 오브젝트에 대응하는 테이블이 존재하는지 확인합니다.

```bash
# salesforce2 스키마에서 테이블 검색
psql "<접속문자열>" -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'salesforce2' AND table_name ILIKE '%<오브젝트명>%';"
```

테이블이 존재하면 컬럼 정보도 조회합니다:
```bash
psql "<접속문자열>" -c "\d salesforce2.<테이블명>"
```

### 3. 대상 JPA 엔티티 찾기

`backend/src/main/kotlin/` 에서 해당 Salesforce Object에 대응하는 JPA 엔티티 파일을 찾습니다.

매칭 기준:
- `@Table(name = "...")` 의 테이블명
- 엔티티 클래스명
- `@Column(name = "...")` 의 컬럼명이 Salesforce API Name을 lowercase로 변환한 것과 일치하는지

### 4. 어노테이션 추가

각 필드에 대해 다음 작업을 수행합니다:

#### 4-1. `@Comment` 어노테이션 추가
- import: `org.hibernate.annotations.Comment`
- Salesforce Object의 **필드 레이블**(한국어)을 `@Comment("필드 레이블")` 형태로 추가
- `@Column` 어노테이션 바로 위에 배치

#### 4-2. `@Column` length 검증
- Salesforce Object의 데이터 유형에서 길이를 추출 (예: `텍스트(100)` → 100)
- 현재 엔티티의 `@Column(length = ...)` 값과 비교
- 불일치 시 Salesforce Object 기준으로 수정

#### 4-3. Heroku Connect DB 테이블이 존재하는 경우
- DB 컬럼 타입/길이와도 교차 검증
- DB에만 존재하고 엔티티에 없는 Heroku Connect 공통 컬럼 추가:
  - `sfid` (VARCHAR(18))
  - `isdeleted` (BOOLEAN)
  - `systemmodstamp` (TIMESTAMP)
  - `createddate` (TIMESTAMP)
  - `_hc_lastop` (VARCHAR(32))
  - `_hc_err` (TEXT)

#### 4-4. 제외 대상 필드
다음 Salesforce 시스템 필드는 엔티티에 추가하지 않습니다 (Heroku Connect 동기화 대상이 아닌 경우):
- `OwnerId` (소유자)
- `CreatedById` (작성자)
- `LastModifiedById` (최종 수정자)

### 5. 빌드 검증

```bash
cd backend && ./gradlew compileKotlin
```

빌드 실패 시 원인을 분석하고 수정합니다.

### 6. 완료 보고

변경 사항을 요약합니다:
- 추가/수정된 `@Comment` 어노테이션 수
- 수정된 `@Column` length 수
- Heroku Connect DB 동기화 여부
- 추가된 Heroku Connect 공통 컬럼 (있는 경우)
