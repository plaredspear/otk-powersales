# 구현 모드 가이드

이 가이드는 GitHub Issue에 작성된 스펙을 기반으로 코드를 구현할 때 적용됩니다.

---

## 작업 컨텍스트

### 플랫폼 판단 규칙

| 작업 경로 | 컨텍스트 | 적용 가이드라인 |
|----------|----------|----------------|
| `mobile/**` | **Flutter** | Flutter 개발 규칙 + Dart 테스트 가이드라인 |
| `backend/**` | **Backend** | Spring Boot 개발 규칙 + JUnit 테스트 가이드라인 |
| `web/**` | **Web** | React 개발 규칙 + Vitest 테스트 가이드라인 |
| `docs/**`, `packages/**`, 루트 | **공통** | 공통 가이드라인만 적용 (테스트 불필요) |

### 컨텍스트별 필독 가이드

**IMPORTANT**: 컨텍스트별 가이드를 읽지 않고 작업을 시작하지 마세요.

| 컨텍스트 | 필독 가이드 |
|----------|------------|
| **Flutter** | `MOBILE_DEV_GUIDE.md` (워크스페이스 루트) |
| **Backend** | `BACKEND_DEV_GUIDE.md` (워크스페이스 루트) |

---

## Issue 기반 스펙 읽기

**스펙은 GitHub Issue 본문입니다.** Issue 번호가 스펙 식별자입니다.

### 스펙 로드 절차

1. **Issue 본문 읽기**: `gh issue view <번호>` 명령으로 Issue 본문을 확인
2. **요구사항 파악**: Issue 본문에서 구현할 기능, API 설계, 화면 설계, 비즈니스 규칙 등을 파악
3. **Part 계획 확인**: Issue 본문에 Part 분할이 명시되어 있으면 해당 Part만 구현

### Issue 본문이 긴 경우

- Issue 본문 전체를 읽되, 현재 구현할 Part에 해당하는 섹션에 집중
- 다른 Part의 상세 내용은 구현 시점에 다시 참조

---

## 작업 규칙

### 1. 작업 시작 전

**a. Issue 확인**
- GitHub Issue 본문을 읽어 구현 요구사항 파악
- Bug/Hotfix: Issue 본문 또는 댓글의 설명만으로 즉시 처리

**b. 브랜치 생성**
- Feature 브랜치 명명: `feature/#<Issue번호>-<간단한설명>` (예: `feature/#42-매출현황`)
- Bug 브랜치: `fix/#<Issue번호>-<간단한설명>`

### 2. 작업 진행 중

**a. Task 완료 시 자동 커밋**
- 각 Task 완료 시: 대응 테스트 실행 → 통과 시 전체 테스트 실행 → 통과하면 자동 커밋 (Claude가 자동 수행)
- 커밋 메시지: Conventional Commits 형식 + Issue 번호 참조
  - 예: `feat(mobile): 매출현황 목록 조회 (#42)`
  - 예: `fix: 로그인 세션 만료 버그 수정 (#55)`
- 히스토리 추적을 위해 Task 단위로 세밀하게 커밋

### 3. 작업 완료 시

작업의 모든 Task가 완료되면 **PR을 생성**합니다.

- PR 제목: `feat(mobile): 매출현황 목록 조회 (Closes #42)` (Conventional Commits 형식)
- PR 본문: 구현 내용 요약, 테스트 결과
- Issue 참조: `Closes #<Issue번호>`로 PR merge 시 Issue 자동 close

### 워크플로우 요약

1. Issue 본문에서 스펙 읽기
2. Feature 브랜치 생성
3. Task 단위로 구현 코드 작성
4. **같은 컨텍스트에서** 테스트 작성
5. 대응 테스트 실행 → 통과 시 전체 테스트 실행 → 자동 커밋
6. 모든 Task 완료 후 → PR 생성 (`Closes #이슈번호`)

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
| `mobile/**` | `cd mobile && flutter test` |
| `backend/**` | `cd backend && ./gradlew test` |
| `web/**` | `cd web && pnpm test` |
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

파일: mobile/lib/domain/entities/user.dart
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
cd backend && ./gradlew bootRun      # Backend
cd mobile && flutter run              # Mobile
cd web && pnpm dev                    # Web

# API 코드 생성
cd mobile && flutter pub run build_runner build
```
