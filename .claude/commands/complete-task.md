---
description: 작업 완료 시 테스트/커밋/로그/인덱스를 자동 처리하는 작업 완료 에이전트
runOn: project
---

# Task Completion Agent (작업 완료 에이전트)

**IMPORTANT**: 작업이 완료되었을 때 실행합니다. 5개 Phase를 순서대로 수행하며, 테스트 실패 시 즉시 중단합니다.

---

## 공통 매핑 테이블

이 명령어 전체에서 사용하는 변환 규칙입니다.

### work_type 매핑

| work_type | 커밋 prefix | 작업 유형 (한국어) | INDEX 주차 테이블 값 | INDEX 섹션 제목 |
|-----------|-----------|-----------------|--------------------|--------------------|
| Feature | `feat(<platform>):` | Feature 작업 (기능 구현) | Feature (<work_id>) | `### Feature 작업` |
| Bug | `fix:` | 버그 수정 | 버그 수정 | `### 버그 수정 (Bugs)` |
| Refactor | `refactor:` | 리팩토링 | 리팩토링 | `### 리팩토링 (Refactoring)` |
| Hotfix | `fix: [HOTFIX]` | 긴급 패치 | 긴급 패치 | `### 긴급 패치 (Hotfixes)` |
| System | `build:` 또는 `chore:` | System 작업 (인프라 구성) | 인프라 | `### 인프라 설정 (Infrastructure)` > `#### System 작업` |
| Docs | `docs:` | 문서 작업 | 문서 | `### 문서 작업 (Documentation)` (없으면 생성) |

### platform 매핑

| platform | 커밋 scope | 표기 |
|----------|-----------|------|
| M | `mobile` | Mobile (Flutter) |
| B | `backend` | Backend (Spring Boot) |
| W | `web` | Web (React) |

### 태그 규칙

| work_type | 태그 형식 | 예시 |
|-----------|---------|------|
| Feature | `#개발로그 #<work_id> #<platform> #<기술키워드들>` | `#개발로그 #1-P1 #Mobile #알림 #Riverpod` |
| System | `#개발로그 #<work_id> #<기술키워드들>` | `#개발로그 #1-P1 #Docker #Backend #인프라` |
| Bug | `#개발로그 #<work_id> #<기술키워드들>` | `#개발로그 #2-P1 #로그인 #버그수정` |
| 기타 | `#개발로그 #<work_id> #<기술키워드들>` | 동일 패턴 |

---

## Phase 1: 정보 수집

대화 맥락과 작업 결과를 기반으로 다음 정보를 정리합니다. 누락된 정보가 있으면 사용자에게 질문합니다.

### 필수 정보

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `work_type` | Feature, Bug, Refactor, Hotfix, System, Docs 중 하나 | Feature |
| `work_id` | 작업 식별자 (스펙번호-파트) | 1-P1, 2-P3, S001, D001 |
| `platform` | M (Mobile), B (Backend), W (Web) - **Feature인 경우만** | M |
| `title` | 작업 제목 (한국어, 간결하게) | 로그인 UI 구현 |
| `work_path` | 작업 경로 (테스트 컨텍스트 판단용) | apps/mobile/ |

### 대화에서 수집하는 정보

다음 정보는 대화 중 구현 과정에서 자연스럽게 수집된 내용을 정리합니다:

| 변수명 | 설명 | 필수 |
|--------|------|:---:|
| `implementation_steps` | 진행 과정 (구현 단계별 상세 내용) | Y |
| `files_created` | 생성/수정된 파일 목록 (경로 + 라인 수) | Y |
| `test_scenarios` | 테스트 시나리오 (실행한 테스트 목록) | Y |
| `issues` | 발생한 이슈와 해결 방법 | N |
| `decisions` | 기술적 결정사항 (옵션 비교, 선택 근거) | N |
| `next_work` | 다음 작업 안내 | N |

정보가 모두 수집되면 바로 Phase 2로 진행합니다.

---

## Phase 2: 테스트 실행

**참고**: Task 단위 커밋 시 대응 테스트 → 전체 테스트를 이미 통과한 상태입니다.
이 Phase는 Part 전체의 최종 검증으로, 전체 테스트만 1회 실행합니다.

### 컨텍스트 판단 규칙

| 작업 경로 | 테스트 명령어 |
|----------|-------------|
| `apps/mobile/**` | `cd apps/mobile && flutter test` |
| `apps/backend/**` | `cd apps/backend && ./gradlew test` |
| `apps/web/**` | `cd apps/web && pnpm test` |
| `docs/**`, `packages/**`, 기타 | 테스트 불필요 -> Phase 3으로 |

### 테스트 실패 시 (CRITICAL)

```
테스트 실패 - 작업 완료 프로세스를 중단합니다.

실패한 테스트:
[실패한 테스트 목록]

에러 내용:
[에러 메시지]

다음 단계:
1. 실패한 테스트를 수정하세요
2. 테스트 통과 후 다시 /complete-task 를 실행하세요
```

**IMPORTANT**: 테스트 실패 시 Phase 3 이후를 절대 실행하지 않습니다. 커밋, 로그 작성, 인덱스 업데이트 모두 하지 않습니다.

### 테스트 성공 시

테스트 결과를 기록합니다:
- 테스트 명령어
- 총 테스트 수
- 통과 수
- 소요 시간

---

## Phase 3: Git Commit

### 커밋 메시지 형식

커밋 메시지는 "공통 매핑 테이블"의 `커밋 prefix`를 사용합니다.

**형식**: `<prefix> <설명적 제목> (<work_id>)`

| work_type | 예시 |
|-----------|------|
| Feature | `feat(mobile): 알림 UI 구현 (1-P1)` |
| Bug | `fix: 로그인 세션 만료 버그 수정 (2-P1)` |
| Refactor | `refactor: Repository 구조 개선 (3-P1)` |
| Hotfix | `fix: [HOTFIX] 프로덕션 크래시 수정 (4-P1)` |
| System | `build: Backend Docker 환경 구성 (1-P1)` |
| Docs | `docs: API 스펙 문서화 (5-P1)` |

### 커밋 실행

```bash
git add <작업 관련 파일들>
git commit -m "$(cat <<'EOF'
<prefix> <설명적 제목> (<work_id>)

- <구현 내용 요약 bullet 1>
- <구현 내용 요약 bullet 2>
- <구현 내용 요약 bullet 3>

테스트: <테스트 명령어> (전체 통과, N개 테스트)

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

커밋 후 `git log -1 --format='%h'`로 commit hash를 기록합니다.

---

## Phase 3.5: Main 머지

Feature 브랜치의 작업을 main에 로컬 머지합니다.

```bash
git checkout main
git merge --no-ff <feature-branch> -m "<prefix> <설명적 제목> (<work_id>)"
```

예시:
```bash
git checkout main
git merge --no-ff feature/1-P1-redis-config -m "feat(backend): Redis 설정 (1-P1)"
```

머지 완료 후 (선택) Feature 브랜치 삭제:
```bash
git branch -d <feature-branch>
```

---

## Phase 4: 문서 작업 (`task-completion` agent)

**IMPORTANT**: 이 Phase는 Task 도구를 사용하여 **`task-completion`** subagent에 위임합니다.

`task-completion` agent는 `.claude/agents/task-completion.md`에 정의되어 있으며, 개발 로그 생성, INDEX.md 갱신, 스펙 파일 처리의 상세 로직을 내장하고 있습니다.

### 전달할 데이터

Task 도구 호출 시 다음 정보를 prompt에 포함합니다:

```
작업 데이터:
- work_type: <work_type>
- work_id: <work_id>
- platform: <platform>
- title: <title>
- work_path: <work_path>
- date: <오늘 날짜 YYYY-MM-DD>
- commit_hash: <Phase 3에서 얻은 hash>
- commit_message: <커밋 메시지 전체>

구현 내용:
<implementation_steps - 진행 과정 상세>

생성된 파일:
<files_created - 경로, 라인 수, 설명>

테스트 결과:
<test_result - 명령어, 테스트 수, 소요 시간>

이슈 및 해결: <issues 또는 "없음">
기술적 결정: <decisions 또는 "없음">
다음 작업: <next_work 또는 "없음">
```

### agent가 수행하는 작업

1. **개발 로그 파일 생성** - `docs/execution/06-개발 로그/<주차>/` 에 로그 파일 생성
2. **00-INDEX.md 업데이트** - 주차별 테이블, 작업 유형별 인덱스, 통계 갱신
3. **spec.md Part 체크리스트 업데이트** - 완료된 Part의 `- [ ]`를 `- [x]`로 변경
4. **모든 Part 완료 시** - `docs/specs/<번호>-<기능명>/` 폴더를 `docs/specs/completed/`로 이동
5. **기획문서 페이지 인덱스 완료 여부 업데이트** (Feature만) - `docs/specs/기획문서-페이지-인덱스.md`의 전체 페이지 인덱스에서 해당 Feature 관련 페이지의 완료 열 갱신 (P: 부분 완료, V: 완료)

상세 로직은 `.claude/agents/task-completion.md` 참조.

---

## 완료 보고

모든 Phase가 완료되면 사용자에게 결과를 보고합니다:

```
작업 완료 처리 완료

요약:
- 작업: <work_id> - <title>
- 테스트: 통과 (N개)
- 코드 커밋: <commit_hash> - <commit_message>
- 개발 로그: docs/execution/06-개발 로그/<주차>/<파일명>
- INDEX 업데이트: 완료
- 스펙 업데이트: 완료 (해당 시) / N/A
- 페이지 인덱스 업데이트: 완료 (해당 시) / N/A
```

---

## 주의사항

1. **Phase 순서 엄수**: 반드시 1->2->3->4 순서로 실행
2. **테스트 실패 = 전체 중단**: Phase 2 실패 시 Phase 3~4 실행 금지
3. **정보 부족 시 질문**: Phase 1에서 정보가 부족하면 추측하지 말고 사용자에게 질문
4. **기존 형식 유지**: 개발 로그, INDEX.md의 기존 형식과 일관성 유지 (기존 파일을 반드시 참조)
5. **subagent 결과 확인**: Phase 4 완료 후 생성된 파일을 간단히 검증
6. **매핑 테이블 참조**: 커밋 메시지, INDEX 항목, 태그 등은 반드시 "공통 매핑 테이블"을 참조
7. **문서는 커밋 불필요**: `docs/` 디렉토리는 `.gitignore`에 포함되어 있어 git 추적 대상이 아니므로 문서 커밋은 수행하지 않습니다
