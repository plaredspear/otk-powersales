# 스펙 작성 모드 가이드

이 가이드는 모든 작업(Feature, Refactor, System, Bug)의 스펙 문서를 작성할 때 적용됩니다.

---

## 용어 정의

- **기존 소스**: `docs/plan/기존소스/otg_PowerSales-master/` 디렉토리의 레거시 앱 소스코드.
  Spring Boot 2.2.7 + JSP + MyBatis + jQuery 기반 모놀리스 웹앱.
- **모듈 인덱스**: `docs/specs/기존소스-모듈-인덱스.md` 파일.
  기존 소스의 모듈별 소스 파일 매핑과 스펙 작성 상태를 추적.
- **controller-guide**: `docs/plan/기존소스/otg_PowerSales-master/docs/controller-guide.md` 파일.
  기존 소스의 21개 컨트롤러별 엔드포인트/역할 문서.

---

## SDD 핵심 원칙

1. **구현 전 스펙 작성 필수**: 코드를 작성하기 전에 반드시 스펙을 작성하고 승인받습니다
2. **플랫폼별 독립 스펙**: Mobile/Backend/Web 스펙을 독립 파일로 관리하되, Feature 스펙은 B+M을 한 세션에서 함께 작성합니다
3. **대화형 승인 프로세스**: 사용자와 Claude가 대화를 통해 스펙을 검증하고 승인합니다
4. **Worktree 전략 연계**: 플랫폼별 독립 개발을 위해 Worktree를 활용합니다
5. **항상 새 스펙 생성**: 기존 Feature를 분석하여 누락/보완/확장 사항을 발견하면, 기존 스펙 파일을 수정하지 않고 항상 새 번호의 독립 스펙을 생성합니다. 기존 스펙은 참조(읽기)만 합니다.

---

## 스펙 작성 범위 (WHAT vs HOW)

**IMPORTANT**: 스펙 문서는 "무엇을(WHAT) 만들지"를 정의합니다. "어떻게(HOW) 만들지"는 구현 단계(impl-mode)에서 결정합니다. 스펙에 구현 코드를 작성하지 마세요.

### 스펙에 포함할 것 (계약과 규칙)

| 항목 | 작성 방식 | 예시 |
|------|----------|------|
| **API 엔드포인트** | HTTP 메서드, 경로, 요청/응답 JSON 예시 | `POST /api/v1/auth/login` + JSON 샘플 |
| **데이터 모델** | 필드/타입/제약조건을 **표**로 정리 | 필드명, 타입, 필수여부, 설명 테이블 |
| **비즈니스 규칙** | **자연어 목록**으로 서술 | "비밀번호는 4글자 이상, 동일 문자 반복 불가" |
| **에러 코드** | HTTP 상태 + 에러 코드 + 메시지 테이블 | 401 / INVALID_CREDENTIALS / "인증 실패" |
| **테스트 시나리오** | 시나리오/조건/예상결과 테이블 | "사번 없음 → 401 INVALID_CREDENTIALS" |
| **화면 설계** | 텍스트 와이어프레임, 컴포넌트 목록, 사용자 흐름 | 기존 JSP UI → 텍스트 와이어프레임 변환 |
| **파일 구조** | 생성할 파일 목록과 역할 | `lib/domain/entities/user.dart` - User 엔티티 |

### 스펙에 포함하지 않을 것 (구현 코드)

| 항목 | 이유 |
|------|------|
| **Service/UseCase 클래스 구현 코드** | 비즈니스 로직의 구현 방식은 구현 단계에서 결정 |
| **Controller 메서드 구현 코드** | API 계약은 정의하되, 코드 구현은 제외 |
| **JWT/Security Provider 구현 코드** | 보안 컴포넌트의 내부 동작은 구현 단계 영역 |
| **Repository 구현 코드** | 쿼리 메서드 시그니처 정도는 허용하나, 구현체는 제외 |
| **상태 관리 Provider/Notifier 구현 코드** | 상태 관리 구조는 구현 단계에서 결정 |
| **Widget/Component 구현 코드** | UI 구현은 화면 설계(와이어프레임)로 대체 |
| **DB 스키마 / DDL / 마이그레이션** | 테이블 생성, 인덱스, 초기 데이터 등은 구현 단계에서 결정 |

### 예시: 비즈니스 로직 작성 방식

구현 코드(클래스, 메서드 구현체) 대신 자연어 규칙으로 서술합니다:

> **로그인 비즈니스 규칙**:
> 1. 사번으로 사용자를 조회한다. 존재하지 않으면 `INVALID_CREDENTIALS` 에러를 반환한다.
> 2. 입력된 비밀번호를 BCrypt로 검증한다. 불일치 시 동일 에러를 반환한다 (사번/비밀번호 구분 불가하게).
> 3. 인증 성공 시 Access Token(1시간)과 Refresh Token(7일)을 생성하여 반환한다.
> 4. 응답에 `requiresPasswordChange`(초기 비밀번호 여부)와 `requiresGpsConsent`(GPS 동의 필요 여부)를 포함한다.

---

## 기존 소스 분석 전략

Feature 스펙을 작성할 때 기존 소스를 분석하여 비즈니스 로직과 화면 구성을 파악합니다.

### 서브에이전트 활용 (권장)

레거시 소스 분석은 `legacy-analyzer` 서브에이전트에 위임하여 메인 컨텍스트를 절약할 수 있습니다:

Task 도구로 `legacy-analyzer` 호출:
- module_name: 대상 모듈명
- platforms: M+B (B+M 함께 작성 시)
- analysis_depth: full (기본) 또는 quick (초안용)

에이전트가 반환하는 구조화된 분석 결과를 기반으로 스펙을 작성합니다.

에이전트 사용이 어려운 경우, 아래 수동 분석 절차를 따릅니다.

### 분석 순서 (모듈 단위)

| 단계 | 대상 | 경로 패턴 | 추출 정보 |
|------|------|----------|----------|
| 1 | **모듈 인덱스** | `docs/specs/기존소스-모듈-인덱스.md` | 대상 모듈의 소스 파일 목록 확인 |
| 2 | **Controller** | `src/main/java/com/ottogi/controller/<Module>Controller.java` | 엔드포인트, HTTP 메서드, 요청 파라미터, 응답 타입, 외부 API 호출 |
| 3 | **Service** | `src/main/java/com/ottogi/service/<module>/` | 비즈니스 로직, 유효성 검증, 트랜잭션 처리, 외부 시스템 연동 |
| 4 | **Mapper Interface** | `src/main/java/com/ottogi/mapper/<module>/` | 데이터 접근 메서드 시그니처 |
| 5 | **Mapper XML** | `src/main/resources/mybatis/mapper/<module>/` | SQL 쿼리 → 테이블/컬럼 구조, 조인 관계, 동적 SQL 조건 |
| 6 | **JSP Views** | `src/main/webapp/WEB-INF/views/<module>/` | UI 레이아웃, 폼 필드, 표시 항목 → 텍스트 와이어프레임으로 변환 |
| 7 | **JavaScript** | `src/main/webapp/files/js/main.js` (관련 함수 검색) | AJAX 호출 패턴, 클라이언트 유효성 검증, UI 인터랙션 |

### 토큰 효율화 규칙

- **main.js(14K lines)는 전체를 읽지 않음**. Grep으로 모듈 관련 함수/URL만 검색
- Mapper XML에서 SQL 구조를 파악하면 Entity/DTO 모델을 도출할 수 있음
- JSP는 화면 구성 파악용. 텍스트 와이어프레임으로 변환하여 스펙에 포함
- 모듈 인덱스에서 Service가 `-`인 모듈(home, mypage 등)은 controller-guide에서 사용하는 서비스를 확인

---

## 아키텍처 매핑 가이드

기존 소스의 패턴을 새 프로젝트 아키텍처로 변환하는 매핑 규칙:

| 기존 (레거시) | 새 프로젝트 (Mobile) | 새 프로젝트 (Backend) | 새 프로젝트 (Web) |
|-------------|-------------------|--------------------|--------------------|
| Controller endpoints (JSP view) | - | REST API endpoints (Spring Boot 3.x) | - |
| JSP form/view | Flutter Screen + Widgets | - | React Component + Ant Design |
| jQuery AJAX | Dio + Retrofit | - | TanStack Query + Axios |
| MyBatis HashMap | Entity (domain model) | JPA Entity or MyBatis typed DTO | TypeScript interface |
| Session-based auth | JWT + flutter_secure_storage | JWT + Spring Security | JWT + Zustand store |
| Salesforce API 연동 | Repository interface | 외부 API 클라이언트 서비스 | API 호출 hook |
| AWS S3 업로드 | 이미지 촬영/선택 + API 업로드 | S3 presigned URL 발급 | 파일 업로드 컴포넌트 |
| Server-side validation | UseCase 유효성 검증 | @Valid + Service 검증 | Form validation (Ant Design) |
| Tiles layout | GoRouter navigation | - | React Router |
| FullCalendar (jQuery) | fl_chart / table_calendar | - | Ant Design Calendar / ECharts |
| Map<String, Object> | Freezed data class | Record / DTO class | TypeScript type |

### 변환 시 주의사항

- 기존 소스의 비즈니스 로직은 스펙에 **자연어 규칙으로 서술** (코드 복사 금지)
- 기존 SQL에서 테이블/컬럼 구조를 파악하되, DDL/스키마는 스펙에 포함하지 않음
- 기존 소스의 외부 시스템 연동(Salesforce, AWS 등)은 새 프로젝트에서도 동일한 비즈니스 요건이므로 API 계약에 반영

---

## 스펙 작성 순서

**Backend(B) 스펙을 먼저 작성**하고, 이를 기반으로 Mobile(M) 스펙을 작성합니다.
Backend API가 실제로 동작하는 것을 전제로 설계합니다.

1. **Backend(B) 스펙 먼저**: 기존 소스의 Controller/Service/Mapper 분석 → REST API 설계, 데이터 모델, 비즈니스 로직 정의
2. **Mobile(M) 스펙 이후**: Backend API 계약을 기반으로 화면 설계, API 연동 정의
3. **한 세션에서 B + M 함께 작성**: API 계약 일관성 보장

### Mock 데이터 불필요

- Backend API가 실제로 동작하므로 Mock 기반 개발을 하지 않습니다
- Mobile은 실제 Backend API를 호출하여 개발합니다

### Web(W) 스펙

- Web 스펙은 해당 기능이 Web에서도 필요할 때만 작성합니다
- Web 스펙 작성 시에도 Backend API 계약을 기반으로 합니다

---

## 대화 기반 스펙 작성

기존 소스에 대응하는 모듈이 없는 작업 (Docker 세팅, CI/CD, SDK 업그레이드, 완전히 새로운 기능 등)은
사용자와의 대화를 통해 요구사항을 수집하고 스펙을 작성합니다.

### 적용 대상

- System 작업 (S): Docker 세팅, 배포 파이프라인, 모니터링 등
- Refactor 작업 (R): 기존 코드 구조 개선
- Bug 작업 (B): 버그 수정
- 기존 소스에 없는 신규 Feature (F)

### 요구사항 수집 절차

1. 작업의 목적과 배경을 사용자에게 질문
2. 완료 조건 (Definition of Done)을 명확히 합의
3. 영향 범위 확인 (어떤 플랫폼, 어떤 디렉토리에 해당하는지)
4. 기술적 제약사항 확인
5. 관련 기존 스펙/코드가 있으면 참조

### 템플릿 선택

- Feature → 플랫폼별 `Feature-B/M/W-Template.md`
- R/S/B → `Task-Template.md`

---

## 작업 유형

| 접두사 | 유형 | 설명 | 템플릿 |
|--------|------|------|--------|
| `F` | Feature | 기능 개발 | `Feature-M/B/W-Template.md` |
| `R` | Refactor | 리팩토링 | `Task-Template.md` |
| `S` | System | 인프라, 설정, 버전 업그레이드 | `Task-Template.md` |
| `B` | Bug | 버그 수정 | `Task-Template.md` |

- Feature는 플랫폼별 전용 템플릿, 일반작업(R/S/B)은 공통 `Task-Template.md` 사용
- 플랫폼 구분이 필요한 일반작업은 파일명에 `-M`, `-B`, `-W` 접미사 추가

---

## 세부 기능 스펙 (Fine-grained Specs)

**IMPORTANT**: 기존 Feature를 검토/분석하여 누락, 보완, 확장 사항을 발견하면, **기존 스펙 파일을 수정하지 않고 항상 새 번호의 독립 스펙을 생성**합니다. 기존 스펙은 참조(읽기)만 하며, 수정/추가는 새 스펙 파일에서 합니다.

### 적용 대상

- 기존 Feature(F3-로그인 등)에 누락된 세부 기능 추가 (예: 단말기 바인딩, 로그인 이력)
- 레거시 소스 분석 후 발견된 갭(gap) 항목
- 기존 Feature와 관련되지만 독립적으로 구현/테스트 가능한 기능
- 기존 Feature의 기능 확장 또는 변경

### 기존 스펙 수정 금지 원칙

기존 스펙을 수정해야 할 것 같은 상황에서도 새 스펙을 만듭니다:

| 상황 | 잘못된 접근 | 올바른 접근 |
|------|-----------|-----------|
| F3-로그인에 단말기 바인딩이 빠져있음 | F3-로그인-B.md에 단말기 바인딩 섹션 추가 | F59-단말기바인딩-B.md 새로 생성 |
| F3-로그인 API에 파라미터 추가 필요 | F3-로그인-B.md의 API 상세 수정 | 새 스펙에서 "기존 API 변경사항"으로 기술 |
| 기존 Entity에 필드 추가 필요 | 기존 스펙의 데이터 모델 수정 | 새 스펙에서 "Entity 변경" 섹션으로 기술 |

### 작성 규칙

1. **독립 번호 부여**: 통합 번호 시퀀스에서 새 번호를 부여한다 (부모 Feature의 하위 번호가 아님)
2. **부모 Feature 참조**: 개요에 `관련 Feature`를 명시하여 맥락을 연결한다
3. **플랫폼 선택적 작성**: 해당 기능이 특정 플랫폼에만 영향을 주면 해당 플랫폼 스펙만 작성한다
   - Backend만: `F60-로그인이력-B.md`
   - B+M: `F59-단말기바인딩-B.md` + `F59-단말기바인딩-M.md`
4. **동일 템플릿 사용**: Feature-B/M-Template.md를 사용하되, 해당 기능에 불필요한 섹션은 생략 가능
5. **Part 분할 불필요**: 세부 기능 스펙은 대부분 소규모이므로 Part 분할 없이 단일 Part로 처리

### 명명 규칙

| 패턴 | 예시 | 설명 |
|------|------|------|
| `F<번호>-<세부기능명>/` | `F59-단말기바인딩/` | 디렉토리 |
| `F<번호>-<세부기능명>-<플랫폼>.md` | `F59-단말기바인딩-B.md` | 스펙 파일 |

### 예시

F3-로그인 분석 후 발견된 갭 항목을 각각 독립 스펙으로 작성:
- F59: 단말기 바인딩 (B+M) — 한 사원 = 한 디바이스 정책
- F60: 로그인 이력 (B) — 매 로그인 시 이력 기록
- F61: FCM 토큰 저장 (B+M) — 로그인 시 푸시 토큰 등록
- F62: GPS 재동의 (B+M) — GPS 동의 6개월 만료 후 재동의 플로우

---

## 스펙 파일 명명 규칙

### 통합 번호 체계

모든 작업 유형(F, S, R, B)은 **단일 번호 시퀀스**를 공유합니다.
- 번호 1~58은 기존 완료/레거시 작업으로 사용 완료
- 새 스펙은 **59번부터** 순차 부여
- 작업 유형에 관계없이 전체에서 고유한 번호를 사용

| 유형 | 디렉토리 패턴 | 파일 패턴 | 예시 |
|------|-------------|----------|------|
| Feature | `F<번호>-<기능명>/` | `F<번호>-<기능명>-<플랫폼>.md` | `F59-알림-M.md` |
| Refactor | `R<번호>-<작업명>/` | `R<번호>-<작업명>[-<플랫폼>].md` | `R60-Provider-구조-최적화-M.md` |
| System | `S<번호>-<작업명>/` | `S<번호>-<작업명>.md` | `S61-CI-파이프라인.md` |
| Bug | `B<번호>-<작업명>/` | `B<번호>-<작업명>[-<플랫폼>].md` | `B62-차트-렌더링-오류-M.md` |

플랫폼 약어: **M** (Mobile/Flutter), **B** (Backend/Spring Boot), **W** (Web/React)

---

## 스펙 디렉토리 구조

```
docs/specs/
├── templates/                     # 스펙 템플릿
│   ├── Feature-M-Template.md     # Feature - Mobile
│   ├── Feature-B-Template.md     # Feature - Backend
│   ├── Feature-W-Template.md     # Feature - Web
│   └── Task-Template.md          # 일반작업 (R/S/B)
│
├── F59-알림/                       # Feature 작업 (번호 59~, 통합 시퀀스)
│   ├── F59-알림-M.md
│   ├── F59-알림-B.md
│   └── attachments/
├── R60-Provider-구조-최적화/       # Refactor 작업 (통합 번호)
│   └── R60-Provider-구조-최적화-M.md
├── S61-CI-파이프라인/              # System 작업 (통합 번호)
│   └── S61-CI-파이프라인.md
├── B62-차트-렌더링-오류/           # Bug 작업 (통합 번호)
│   └── B62-차트-렌더링-오류-M.md
│
├── completed/                     # 완료된 작업 (아카이빙)
└── _legacy/                       # 기존 Feature 파일 (참조용)
```

---

## 스펙 상태

| 상태 | 아이콘 | 설명 |
|------|--------|------|
| **초안** | 📝 | 스펙 작성 중 |
| **리뷰 중** | 🔍 | 사용자 검토 중 |
| **승인됨** | ✅ | 스펙 승인 완료, 구현 가능 |
| **구현 중** | 🚀 | 구현 작업 진행 중 |
| **완료** | ✅ | 구현 및 테스트 완료 |

---

## 대화형 승인 프로세스

1. **Claude가 스펙 초안 작성**
   - 템플릿을 기반으로 스펙 초안 작성
   - 스펙 상태: 📝 초안 → 🔍 리뷰 중

2. **사용자가 승인 체크리스트 검토**
   - UI/UX 스펙 (M/W): 화면 레이아웃, 컴포넌트, 사용자 흐름 명확성
   - API 스펙 (B): 엔드포인트, 요청/응답 형식, 에러 코드 정의 완성도
   - 데이터 모델 (M/B/W): Entity, DTO, 인터페이스 정의 완성도
   - 비즈니스 로직 (M/B): UseCase, Service 로직 구체성
   - 테스트 시나리오 (M/B/W): 정상/오류 케이스 충분성
   - 기술적 실현 가능성 (M/B/W): 구현 가능 여부

3. **피드백 반영 및 수정**
   - 사용자 피드백 → Claude가 스펙 수정
   - 변경 이력 기록 (버전, 날짜, 변경 내용)

4. **최종 승인**
   - 모든 체크리스트 항목 ✅ → 스펙 승인 완료
   - 스펙 상태: 🔍 리뷰 중 → ✅ 승인됨
   - 승인자 이름 및 승인 일자 기록

---

## Feature 분할 전략 (구현 세션 효율화)

**IMPORTANT**: Feature 번호는 모듈 인덱스의 기존 Feature 매핑을 기반으로 결정하되, **구현 세션의 효율성**을 고려하여 Feature 자체를 분할할지 판단합니다.

### 배경

구현 단계(`impl-mode`)에서는 Sonnet 4.5 모델을 사용합니다. 하나의 Feature 구현이 Sonnet 4.5의 컨텍스트 윈도우(200K 토큰)를 크게 소비하면 구현 품질이 저하됩니다. 따라서 스펙 작성 단계에서 구현 분량을 추정하고, 필요 시 Feature를 분할합니다.

### 판단 기준

스펙 작성 시 **한 플랫폼(M 또는 B) 기준으로** 다음을 추정합니다:

| 지표 | 분할 불필요 | 분할 권장 |
|------|-----------|---------|
| **예상 Part 수** (M 기준) | 1~2 Parts | 3 Parts 이상 |
| **예상 소스 파일 수** (M 기준) | ~20개 이하 | 25개 초과 |
| **화면 수** | 1~3개 화면 | 4개 이상 독립적 화면 |
| **API 엔드포인트 수** | 1~4개 | 5개 이상 |

### 분할 절차

1. **Feature 번호 결정**: 통합 번호 시퀀스에서 다음 번호를 부여
2. **구현 분량 추정**: 화면 수, 파일 수, Part 수를 개략 추정
3. **분할 여부 판단**: 위 기준에 따라 분할 필요성 판단
4. **분할 실행**: 기능적으로 독립 가능한 단위로 Feature를 나눔

### 분할 시 번호 부여

분할된 Feature는 통합 번호 시퀀스에서 각각 독립 번호를 순차 부여합니다.

**예시**: 매출현황 기능이 화면 6개로 분할이 필요한 경우:
- F59: 매출현황-행사매출 — 행사 매출 목록 + 상세
- F60: 매출현황-일매출등록 — 일 매출 등록

### 분할하지 않는 경우

다음의 경우 하나의 Feature로 유지합니다:
- 화면 간 데이터 의존성이 높아 분리하면 API 계약이 복잡해지는 경우
- 같은 탭/모달 내 구성요소인 경우 (예: 동일 화면의 탭 2개)
- 분할 시 스펙 문서 자체가 과도하게 늘어나는 경우 (스펙 작성 세션도 비효율)

---

## Part 분할 전략

스펙 초안 작성 시 Part 분할 계획을 함께 수립합니다.

- **목표 크기**: 소스 파일 10개, 1,500줄 이내
- **분할 대상**: 플랫폼별 스펙 (Feature 자체가 아님)
- **예외**: 소스 파일 10개 이하인 경우 Part 분할 없이 한 번에 처리

| 분할 기준 | 적합한 경우 | 예시 |
|----------|-----------|------|
| **아키텍처 레이어** (기본) | 레이어별 복잡도가 균등 | Domain → Data → Presentation |
| **화면별** | Presentation 비중이 큰 경우 | 로그인 화면 → 비밀번호 변경 화면 |
| **엔드포인트 그룹별** | Backend API가 많은 경우 | 인증 API → 사용자 API → 권한 API |

---

## Worktree 전략

플랫폼별 독립 개발을 위해 Worktree를 활용합니다:

```bash
# Mobile 개발
git worktree add ../mobile-worktree-f59 -b feature/f59-notification-mobile

# Backend 개발
git worktree add ../backend-worktree-f59 -b feature/f59-notification-backend

# Web 개발
git worktree add ../web-worktree-f61 -b feature/f61-orders-web
```

**장점**:
- 플랫폼별 독립 개발 가능 (Mobile, Backend, Web 동시 작업)
- 각 플랫폼 테스트 독립 실행 가능
- Git rebase 충돌 최소화

---

참고: `docs/execution/09-SDD-워크플로우.md`에 플랫폼별 작업 흐름, FAQ 등 추가 참고 자료가 있습니다.
