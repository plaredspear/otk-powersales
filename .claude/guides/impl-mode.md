# 구현 모드 가이드

이 가이드는 GitHub Issue에 작성된 스펙을 기반으로 코드를 구현할 때 적용됩니다.

---

## 플랫폼 판단

| 작업 경로 | 컨텍스트 | 필독 컨벤션 |
|----------|----------|------------|
| `mobile/**` | Flutter | `.claude/guides/mobile-conventions.md` |
| `backend/**` | Backend | `.claude/guides/backend-conventions.md` |
| `web/**` | Web | (추후 생성) |
| `infra/**` | Terraform | (이 파일의 "Terraform 구현 체크리스트" 참조) |
| `docs/**`, 기타 | 공통 | 테스트 불필요 |

**IMPORTANT**: 컨텍스트별 컨벤션 문서를 반드시 읽고 패턴을 따르세요.

---

## Issue 기반 스펙 읽기

1. `gh issue view <번호>`로 Issue 본문 확인
2. Issue 본문에서 구현할 기능, API 설계, 비즈니스 규칙 파악
3. Part 분할이 명시된 경우 해당 Part만 구현

---

## 작업 워크플로우

### 1. 브랜치 생성

```
feature/#<Issue번호>-<간단한설명>   (예: feature/#42-매출현황)
fix/#<Issue번호>-<간단한설명>       (예: fix/#55-로그인오류)
```

### 2. Task 단위 구현 + 커밋

각 Task 완료 시:
1. 대응 테스트 실행 → 통과 확인
2. 전체 테스트 실행 → regression 확인
3. 자동 커밋

**커밋 메시지**: Conventional Commits + Issue 번호
```
feat(mobile): 매출현황 목록 조회 (#42)
fix: 로그인 세션 만료 버그 수정 (#55)
```

### 3. PR 생성

모든 Task 완료 후:
- 제목: `feat(mobile): 매출현황 목록 조회 (Closes #42)`
- 본문: 구현 내용 요약 + 테스트 결과
- `Closes #<Issue번호>`로 자동 close 연결

**예외**: 스펙 규모가 작아 소스 파일 10개 이하인 경우, Part를 분할하지 않고 한 번에 처리합니다.

---

## 테스트

### 원칙
- 코드 작성과 동시에 단위 테스트 작성
- 정상(happy path) + 예외(error/edge case) 테스트 필수
- 테스트 실패 시 작업 완료 처리 금지

### 테스트 명령어

| 플랫폼 | 대응 테스트 | 전체 테스트 |
|--------|-----------|-----------|
| Flutter | `cd mobile && flutter test test/<path>/<name>_test.dart --reporter compact 2>&1 \| tail -1` | `cd mobile && flutter test --reporter compact 2>&1 \| tail -1` |
| Backend | `cd backend && ./gradlew test --tests "com.otoki.internal.<패키지>.<Name>Test"` | `cd backend && ./gradlew test` |
| Terraform | `cd infra && terraform validate` | `cd infra && terraform fmt -check -recursive && terraform validate` |

### 테스트 실행 전략

**Step 1: 대응 테스트** — 수정/생성한 소스에 매핑되는 테스트만 실행

| 플랫폼 | 소스 파일 | 테스트 파일 |
|--------|----------|------------|
| Flutter | `lib/<path>/<name>.dart` | `test/<path>/<name>_test.dart` |
| Backend | `src/main/kotlin/.../<Name>.kt` | `src/test/kotlin/.../<Name>Test.kt` |

- 대응 테스트 실패 → 즉시 수정, Step 2 진행 안 함
- 대응 테스트 통과 → Step 2로

**Step 2: 전체 테스트** — regression 확인 후 커밋

### Flutter 테스트 결과 확인

**IMPORTANT**: 테스트 실행 시 출력을 최소화하는 옵션을 **기본**으로 사용합니다. 실패가 발생한 경우에만 상세 출력으로 전환합니다.

- 기본: `flutter test --reporter compact 2>&1 | tail -1` → 결과 1줄만 확인
- 실패 시: `flutter test --reporter expanded 2>&1 | grep -A 20 "FAIL"` → 원인 파악

`flutter test` 전체 출력은 1300+ 라인이 되어 컨텍스트를 낭비합니다. 처음부터 전체 출력을 보지 마세요.

---

## Terraform 구현 체크리스트

### 작업 절차
1. `infra/` 디렉토리에서 작업
2. 모듈 수정 시: 기존 모듈 구조(variables.tf / main.tf / outputs.tf) 유지
3. 신규 모듈 시: 기존 모듈 패턴 참조 (변수명 규칙: `project`, `environment` 필수)

### 검증 단계
1. **포맷팅**: `terraform fmt -recursive` 실행
2. **검증**: `terraform validate` 실행
3. **Plan**: `make plan-dev` 실행 → 예상 변경 수와 스펙 일치 확인
4. **Apply**: `make apply-dev` (사용자 승인 후에만)

### 커밋 규칙
- prefix: `infra:` (예: `infra: CloudWatch Alarm 모듈 추가 (#60)`)
- `terraform.tfstate`, `secrets.tfvars`는 커밋 금지 (.gitignore 확인)

### 변수 추가 시
1. `infra/variables.tf`에 변수 선언
2. `infra/envs/dev.tfvars`에 값 설정
3. 시크릿이면 `secrets.tfvars`에 추가 (`.gitignore` 대상)
4. `secrets.tfvars.example` 업데이트

### 출력 추가 시
1. 모듈 `outputs.tf`에 output 추가
2. `infra/main.tf`에서 모듈 output 참조
3. `infra/outputs.tf`에 root output 추가
4. 필요 시 `ssm-outputs` 모듈에 파라미터 추가

---

## 서브에이전트 테스트 위임 규칙

**IMPORTANT**: 테스트 작성을 서브에이전트(Task 도구)에 위임할 때, 반드시 대상 소스 코드의 정확한 인터페이스 정보를 프롬프트에 포함해야 합니다.

**필수 포함 정보:**
1. import 경로 (정확한 package 경로)
2. Constructor signature (필수/선택 파라미터, 타입, 기본값)
3. Public methods/getters 목록 (반환 타입 포함)
4. 의존하는 다른 클래스의 위치 (같은 파일 내 정의 여부)

**금지 사항:**
- 소스 코드를 읽지 않고 테스트 작성 위임 금지
- "이 파일에 대한 테스트를 작성해주세요" 같은 모호한 지시 금지
- Mock 클래스 작성 시, 실제 인터페이스의 모든 메서드 시그니처를 전달할 것

---

## 개발 빌드 명령어

```bash
cd backend && ./gradlew bootRun      # Backend 실행
cd mobile && flutter run              # Mobile 실행
cd mobile && flutter pub run build_runner build  # API 코드 생성
```
