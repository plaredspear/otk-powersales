---
description: Salesforce Object 정의를 기반으로 JPA 엔티티 @Comment 어노테이션 스펙 생성
runOn: project
---

# Salesforce Object → JPA Entity @Comment 어노테이션 스펙 생성

Salesforce Object 정의 파일과 Heroku Connect DB를 분석하여, JPA 엔티티 변경 **스펙 문서**를 `docs/specs/backlog/` 에 생성합니다.
**소스 코드를 직접 수정하지 않습니다.** 분석 결과를 스펙으로 작성하고, 승인 후 `/impl`로 구현합니다.

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

### 4. 필드별 분석 (코드 수정 금지)

각 필드에 대해 **분석만 수행**하고 결과를 스펙에 기록합니다:

#### 4-1. `@Comment` 어노테이션 분석
- Salesforce Object의 **필드 레이블**(한국어)과 현재 엔티티의 `@Comment` 존재 여부 비교
- 누락된 `@Comment`, 값이 다른 `@Comment`를 식별

#### 4-2. `@Column` length 검증
- Salesforce Object의 데이터 유형에서 길이를 추출 (예: `텍스트(100)` → 100)
- 현재 엔티티의 `@Column(length = ...)` 값과 비교
- 불일치 항목을 식별

#### 4-3. Heroku Connect DB 교차 검증 (테이블 존재 시)
- DB 컬럼 타입/길이와 엔티티 비교
- 엔티티에 없는 Heroku Connect 공통 컬럼 식별:
  - `sfid` (VARCHAR(18))
  - `isdeleted` (BOOLEAN)
  - `systemmodstamp` (TIMESTAMP)
  - `createddate` (TIMESTAMP)
  - `_hc_lastop` (VARCHAR(32))
  - `_hc_err` (TEXT)

#### 4-4. 누락/초과 필드 식별
- SF Object에 있지만 엔티티에 없는 필드
- 엔티티에 있지만 SF Object에 없는 필드

#### 4-5. 제외 대상 필드
다음 Salesforce 시스템 필드는 분석에서 제외합니다:
- `OwnerId` (소유자)
- `CreatedById` (작성자)
- `LastModifiedById` (최종 수정자)

### 5. 스펙 문서 생성

**스펙 작성 모드에 따라** `.claude/guides/spec-writing-guide.md`를 읽고, 분석 결과를 `docs/specs/backlog/<번호>-<기능명>/spec-B.md` 로 생성합니다.

스펙 번호: `backlog/` + `ready/` + `completed/` 에서 최대 번호 + 1

#### 스펙에 포함할 내용

1. **개요 테이블** — 스펙 번호, 기능명, 플랫폼(B), 상태(DRAFT)
2. **승인 이력 테이블**
3. **배경** — SF Object 정의 기반 엔티티 정렬 목적
4. **SF Object ↔ JPA 엔티티 필드 매핑 테이블** — 전체 필드 대조 결과
   - SF 필드명 | SF 레이블 | SF 타입 | 엔티티 필드 | 현재 @Comment | 현재 @Column length | 변경 필요 여부 | 변경 내용
5. **Heroku Connect DB 검증 결과** (테이블 존재 시)
   - DB 컬럼 대조 테이블
   - 추가 필요한 HC 공통 컬럼 목록
6. **변경 작업 목록** — 구현 시 수행할 작업을 번호 매긴 자연어 목록으로 기술
7. **영향 범위 / 파일 목록** — 수정 대상 엔티티 파일 경로
8. **완료 조건** — 검증 가능한 체크리스트
9. **테스트 시나리오** — 빌드 성공, @Comment 값 확인 등

### 6. 자동 리뷰

스펙 작성 완료 후 `.claude/commands/spec-review.md`를 읽고 리뷰를 수행합니다.
