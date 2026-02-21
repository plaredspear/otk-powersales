# 스펙 작성 모드 가이드

이 가이드는 모든 작업(Feature, Refactor, System, Bug)의 스펙을 작성할 때 적용됩니다.

**IMPORTANT**: 스펙은 GitHub Issue 본문으로 작성합니다. `docs/specs/` 파일을 생성하지 않습니다. Issue 번호가 스펙 식별자입니다.

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
2. **플랫폼별 독립 스펙**: Mobile/Backend/Web 스펙을 독립적으로 관리하되, Feature 스펙은 B+M을 한 세션에서 함께 작성합니다
3. **스펙 출력 형식**: GitHub Issue 본문으로 사용할 수 있는 Markdown 형식으로 작성합니다

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

- System 작업: Docker 세팅, 배포 파이프라인, 모니터링 등
- Refactor 작업: 기존 코드 구조 개선
- Bug 작업: 버그 수정
- 기존 소스에 없는 신규 Feature

### 요구사항 수집 절차

1. 작업의 목적과 배경을 사용자에게 질문
2. 완료 조건 (Definition of Done)을 명확히 합의
3. 영향 범위 확인 (어떤 플랫폼, 어떤 디렉토리에 해당하는지)
4. 기술적 제약사항 확인
5. 관련 기존 코드가 있으면 참조

---

## Issue 작성 가이드

### Issue 제목

작업 유형을 Label 또는 제목 prefix로 표기합니다:

| 유형 | Issue 제목 예시 | Label |
|------|---------------|-------|
| Feature | `[Feature] 매출현황 목록 조회` | `feature` |
| Bug | `[Bug] 로그인 시 세션 만료 오류` | `bug` |
| Refactor | `[Refactor] Repository 구조 개선` | `refactor` |
| System | `[System] CI 파이프라인 구성` | `system` |

### Issue 본문 구조 (스펙)

출력은 GitHub Issue 본문으로 사용할 수 있는 Markdown 형식입니다:

```markdown
## 개요
- 기능 설명
- 대상 플랫폼: Backend / Mobile / Web

## API 설계 (Backend)
### API 1: <엔드포인트>
- Method: POST/GET/...
- Path: /api/v1/...
- Request/Response JSON 예시

## 화면 설계 (Mobile)
### 화면 1: <화면명>
- 텍스트 와이어프레임
- 컴포넌트 목록

## 데이터 모델
- 필드/타입/제약조건 테이블

## 비즈니스 규칙
- 자연어 규칙 목록

## 에러 처리
- HTTP 상태 + 에러 코드 + 메시지 테이블

## 테스트 시나리오
- 시나리오/조건/예상결과 테이블

## 파일 구조
- 생성할 파일 목록과 역할

## Part 분할 (선택)
- Part 1: ...
- Part 2: ...
```

---

## Part 분할 전략

스펙 작성 시 Part 분할 계획을 함께 수립합니다.

- **목표 크기**: 소스 파일 10개, 1,500줄 이내
- **예외**: 소스 파일 10개 이하인 경우 Part 분할 없이 한 번에 처리

| 분할 기준 | 적합한 경우 | 예시 |
|----------|-----------|------|
| **아키텍처 레이어** (기본) | 레이어별 복잡도가 균등 | Domain → Data → Presentation |
| **화면별** | Presentation 비중이 큰 경우 | 로그인 화면 → 비밀번호 변경 화면 |
| **엔드포인트 그룹별** | Backend API가 많은 경우 | 인증 API → 사용자 API → 권한 API |
