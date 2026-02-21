# 구현 모드 가이드

이 가이드는 승인된 스펙을 기반으로 코드를 구현할 때 적용됩니다.

---

## 작업 컨텍스트

### 플랫폼 판단 규칙

| 작업 경로 | 컨텍스트 | 적용 가이드라인 |
|----------|----------|----------------|
| `apps/mobile/**` | **Flutter** | Flutter 개발 규칙 + Dart 테스트 가이드라인 |
| `apps/backend/**` | **Backend** | Spring Boot 개발 규칙 + JUnit 테스트 가이드라인 |
| `apps/web/**` | **Web** | React 개발 규칙 + Vitest 테스트 가이드라인 |
| `docs/**`, `packages/**`, 루트 | **공통** | 공통 가이드라인만 적용 (테스트 불필요) |

### 컨텍스트별 필독 가이드

**IMPORTANT**: 컨텍스트별 가이드를 읽지 않고 작업을 시작하지 마세요.

| 컨텍스트 | 필독 가이드 |
|----------|------------|
| **Flutter** | [`apps/mobile/MOBILE_DEV_GUIDE.md`](apps/mobile/MOBILE_DEV_GUIDE.md) |
| **Backend** | [`apps/backend/BACKEND_DEV_GUIDE.md`](apps/backend/BACKEND_DEV_GUIDE.md) |

---

## 스펙 선택적 로드 전략

**IMPORTANT**: 스펙 파일 전체를 읽지 않습니다. Part별로 필요한 섹션만 선택적으로 로드하여 컨텍스트 윈도우를 절약합니다.

### 배경

스펙 문서는 18~60KB(8~25K 토큰)로, 전체를 로드하면 구현에 사용할 컨텍스트가 부족해집니다. 구현 시에는 현재 Part에 해당하는 섹션만 필요합니다.

### 로드 절차

**Step 1: 헤더 로드** — 스펙 파일의 처음 ~50줄만 읽기
- `## 개요` 확인 (Feature 전체 맥락 파악)
- `## 스펙 승인` 상태 확인 (✅ 승인됨 여부)

**Step 2: Part 계획 로드** — `## Part 분할 계획` 섹션만 읽기
- Grep으로 `## Part 분할 계획` 위치를 찾아 해당 섹션만 읽기
- 현재 구현할 Part의 범위 (포함 Task, 대상 API/화면) 확인

**Step 3: Part 관련 섹션만 선택적 로드**

| 플랫폼 | 로드할 섹션 | 로드 방법 |
|--------|-----------|----------|
| **Backend** | Part에 해당하는 API 상세 (API 1~N 중 해당 것만) | Grep으로 API 제목 위치 찾아 해당 구간만 읽기 |
| **Backend** | 데이터 모델 (Entity, DTO) | `## 데이터 모델` 섹션 전체 (보통 소규모) |
| **Backend** | 비즈니스 로직 (해당 Part 관련만) | `## 비즈니스 로직` 섹션 |
| **Mobile** | Part에 해당하는 화면 설계 | Grep으로 화면명 위치 찾아 해당 구간만 읽기 |
| **Mobile** | API 연동 (해당 API만) | `## API 연동` 중 관련 API만 |
| **Mobile** | 데이터 모델 | `## 데이터 모델` 섹션 전체 |

**Step 4: 테스트 시나리오는 지연 로드**
- 구현 코드 작성 완료 후, 테스트 작성 시점에 `## 테스트 시나리오` 섹션을 로드
- 구현과 테스트를 같은 Task에서 진행하되, 테스트 시나리오는 필요한 시점에 읽기

### 로드하지 않는 섹션

| 섹션 | 이유 |
|------|------|
| `## 스펙 승인` (승인 확인 후) | 승인 상태만 확인하면 충분 |
| `## 의존성` | 작업 시작 전 확인 후 불필요 |
| `## 변경 이력` | 구현에 불필요 |
| `## 구현 결과` | 구현 완료 후 작성 영역 |
| 다른 Part의 API 상세/화면 설계 | 현재 Part와 무관 |

### 예시: F22-주문서작성-B (4 Parts, 1,519줄)

Part 1 구현 시 로드하는 내용:
```
1. 줄 1~50: 개요 + 승인 상태 확인
2. "## Part 분할 계획" 섹션: Part 1 범위 확인 → API 1~3 담당
3. "### API 1", "### API 2", "### API 3" 섹션만 읽기
4. "## 데이터 모델" 섹션 읽기
5. "## 비즈니스 로직" 중 관련 부분만 읽기
```

전체 1,519줄 중 **~400줄만 로드** → 토큰 약 70% 절약

---

## 작업 규칙

### 1. 작업 시작 전

**a. 스펙 승인 확인 (Feature/System만 해당)**
- 스펙이 "✅ 승인됨" 상태여야 구현 시작 가능 (SDD 원칙)
- Bug/Refactor/Hotfix: 스펙 불필요, 즉시 처리

**b. Feature 단위 작업**
- 하나의 Feature (플랫폼별)를 완료한 후 다음 작업으로 이동
- Feature 중간에 다른 작업으로 이동하지 않음

### 2. 작업 진행 중

**a. 개발 로그 작성**
- `docs/execution/06-개발 로그/<주차>/` 디렉토리에 작업 로그 파일 생성
- 주차별 폴더: `YYYY-W<주차번호>` (ISO 8601, 월요일 시작)
- 중요한 결정사항, 발생한 문제, 해결 방법 기록

**b. 개발 로그 파일명 규칙**

| 작업 유형 | 파일명 형식 | 예시 |
|----------|-----------|------|
| Feature | `YYYY-MM-DD-F<번호>-<플랫폼>-작업명.md` | `2026-02-05-F59-M-알림-UI.md` |
| 버그 수정 | `YYYY-MM-DD-B<순번>-버그명.md` | `2026-02-05-B001-로그인-세션-만료-버그.md` |
| 리팩토링 | `YYYY-MM-DD-R<순번>-리팩토링명.md` | `2026-02-08-R001-Repository-구조-개선.md` |
| 긴급 패치 | `YYYY-MM-DD-H<순번>-패치명.md` | `2026-02-12-H001-프로덕션-크래시-수정.md` |
| 시스템 | `YYYY-MM-DD-S<순번>-작업명.md` | `2026-02-04-S001-Docker-Compose-설정.md` |
| 문서 | `YYYY-MM-DD-D<순번>-작업명.md` | `2026-02-04-D001-API-스펙-문서화.md` |

**c. Task 완료 시 자동 커밋**
- 각 Task 완료 시: 대응 테스트 실행 → 통과 시 전체 테스트 실행 → 통과하면 자동 커밋 (Claude가 자동 수행)
- 커밋 메시지: Conventional Commits 형식
- 히스토리 추적을 위해 Task 단위로 세밀하게 커밋

**d. 기술 논의 문서화**
- 패키지 선택, 아키텍처 결정 등 중요한 기술적 선택 시
- `docs/execution/07-기술 결정/` 디렉토리에 문서 생성

### 3. Part 완료 시

Part의 모든 Task가 완료되면, **사용자가** `/complete-task` 명령어를 수동 실행합니다.
코드 커밋은 이미 Task 단위로 완료된 상태이므로, 이 명령어는 문서 작업에 집중합니다.

| Phase | 작업 | 실행 주체 |
|:-----:|------|----------|
| 1 | **정보 수집**: 대화 맥락에서 작업 정보 정리 | 메인 Claude |
| 2 | **테스트 확인**: 최종 전체 테스트 실행, 실패 시 즉시 중단 | 메인 Claude |
| 3 | **문서 작업**: 개발 로그 생성 + 00-INDEX.md 갱신 + 스펙 파일 처리 | Task subagent |

상세 워크플로우는 `.claude/commands/complete-task.md` 참조.

### Part 내 워크플로우

1. Task 단위로 구현 코드 작성
2. **같은 컨텍스트에서** 테스트 작성 (서브에이전트 위임 불필요)
3. 대응 테스트 실행 → 통과 시 전체 테스트 실행 → 자동 커밋 (Task 완료 시 Claude가 자동 수행)
4. Part의 모든 Task 완료 후 → 사용자가 `/complete-task` 수동 실행 (테스트 확인 + 문서 작업)

**참고**: `docs/` 디렉토리는 `.gitignore`에 포함되어 있어 git 추적 대상이 아니므로, 문서는 커밋되지 않고 파일 시스템에만 저장됩니다.

**예외**: 스펙 규모가 작아 소스 파일 10개 이하인 경우, Part를 분할하지 않고 한 번에 처리합니다.

---

## 테스트

### 공통 원칙

- 코드 작성과 동시에 단위 테스트 작성 (TDD 권장)
- 테스트 파일 구조: 소스 파일과 동일 구조, 파일명 `원본파일명_test.확장자`
- 비즈니스 로직 테스트 시 정상 처리(happy path)와 예외 처리(error/edge case) 테스트를 반드시 함께 작성
- 코드 커버리지 80% 이상 권장
- 테스트 실패 시 작업 완료로 처리하지 않음
- Flutter/Backend/Web 작업 시: 코드 작성과 동시에 단위 테스트 작성
- 문서/공통 작업 시: 테스트 불필요

### 플랫폼별 테스트 명령어

| 작업 경로 | 테스트 명령어 |
|----------|-------------|
| `apps/mobile/**` | `cd apps/mobile && flutter test` |
| `apps/backend/**` | `cd apps/backend && ./gradlew test` |
| `apps/web/**` | `cd apps/web && pnpm test` |
| `docs/**`, `packages/**`, 기타 | 테스트 불필요 |

### 테스트 실행 전략: 대응 테스트 우선

Task 완료 시 테스트는 2단계로 실행합니다.

#### Step 1: 대응 테스트 실행

수정/생성한 소스 파일에 대응하는 테스트 파일만 먼저 실행합니다.

**파일 매핑 규칙:**

| 플랫폼 | 소스 파일 | 테스트 파일 |
|--------|----------|------------|
| Flutter | `lib/<path>/<name>.dart` | `test/<path>/<name>_test.dart` |
| Backend | `src/main/kotlin/.../<Name>.kt` | `src/test/kotlin/.../<Name>Test.kt` |

**실행:**

```bash
# Flutter - 대응 테스트만 실행
flutter test test/domain/usecases/login_usecase_test.dart --reporter compact 2>&1 | tail -1

# Backend - 대응 테스트만 실행
./gradlew test --tests "com.otoki.internal.service.AuthServiceTest"
```

**판단 기준:**
- 대응 테스트가 존재하지 않는 경우 (새 파일에 테스트 미작성 등): Step 2로 바로 진행
- 대응 테스트 실패: 즉시 수정, Step 2 진행하지 않음
- 대응 테스트 통과: Step 2로 진행

#### Step 2: 전체 테스트 실행

대응 테스트가 통과한 후, 전체 테스트를 실행하여 regression을 확인합니다.

```bash
# Flutter
flutter test --reporter compact 2>&1 | tail -1

# Backend
./gradlew test
```

전체 테스트 통과 시 자동 커밋을 진행합니다.

### Flutter 테스트 결과 확인

**IMPORTANT**: 테스트 실행 시 출력을 최소화하는 옵션을 **기본**으로 사용합니다. 실패가 발생한 경우에만 상세 출력으로 전환합니다.

#### Step 1: 기본 실행 (최소 출력)

```bash
# 전체 테스트 - 최종 결과 1줄만 확인
flutter test --reporter compact 2>&1 | tail -1

# 특정 파일 테스트 - 최종 결과 1줄만 확인
flutter test test/path/to_test.dart --reporter compact 2>&1 | tail -1
```

출력 예시:
- 성공: `+1299: All tests passed!`
- 실패: `+1289 -1: Some tests failed.`

#### Step 2: 실패 시 원인 파악

```bash
# 실패한 테스트명과 에러 메시지만 필터링
flutter test --reporter expanded 2>&1 | grep -A 20 "FAIL"

# 또는 compact에서 실패 라인만 추출
flutter test --reporter compact 2>&1 | grep -E "\-[0-9]+:.*FAIL|Some tests failed"

# 특정 테스트 파일만 상세 실행 (개별 수정 후 검증용)
flutter test test/domain/usecases/login_usecase_test.dart
```

**이 2단계 접근법을 반드시 따르세요**: 처음부터 전체 출력을 보지 않습니다. `flutter test` 전체 출력은 1300+ 라인이 되어 터미널에서 잘리고 컨텍스트를 낭비합니다.

### 서브에이전트 테스트 위임 규칙

**IMPORTANT**: 테스트 작성을 서브에이전트(Task 도구)에 위임할 때, 반드시 대상 소스 코드의 정확한 인터페이스 정보를 프롬프트에 포함해야 합니다. 이 정보가 없으면 서브에이전트가 존재하지 않는 필드, 잘못된 import 경로, 틀린 constructor 파라미터를 사용하여 테스트 수정에 추가 시간이 소요됩니다.

**필수 포함 정보:**

```
1. import 경로 (정확한 package 경로)
2. Constructor signature (필수/선택 파라미터, 타입, 기본값)
3. Public methods/getters 목록 (반환 타입 포함)
4. 의존하는 다른 클래스의 위치 (같은 파일 내 정의 여부)
```

**프롬프트 예시:**

```
User entity에 대한 테스트를 작성해주세요.

파일: apps/mobile/lib/domain/entities/user.dart
import: package:mobile/domain/entities/user.dart

Constructor:
  const User({
    required int id,
    required String employeeId,
    required String name,
    required String department,
    required String role,
    bool passwordChangeRequired = false,
  })

Methods: copyWith({...}), toJson() -> Map, fromJson(Map) -> User (factory)
Overrides: ==, hashCode, toString

참고: LoginResult는 별도 파일이 아니라 auth_repository.dart에 정의되어 있음
```

**금지 사항:**
- 소스 코드를 읽지 않고 테스트 작성 위임 금지
- "이 파일에 대한 테스트를 작성해주세요" 같은 모호한 지시 금지
- Mock 클래스 작성 시, 실제 인터페이스의 모든 메서드 시그니처를 전달할 것

---

## 개발 명령어

```bash
# 실행
cd apps/backend && ./gradlew bootRun      # Backend
cd apps/mobile && flutter run              # Mobile
cd apps/web && pnpm dev                    # Web

# API 코드 생성
cd apps/mobile && flutter pub run build_runner build
```
