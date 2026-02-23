---
description: 다음 승인 스펙 자동 탐색 + 구현 + 완료 처리 (impl-batch 연동)
runOn: project
---

# impl-next: 다음 승인 스펙 구현

**IMPORTANT**: 이 커맨드는 4개 Phase를 순서대로 실행합니다. 각 Phase에서 실패 시 상태 마커를 출력하고 즉시 종료합니다.

```
Phase 1: 다음 승인 스펙 탐색
Phase 2: 구현 (impl-mode.md 따름)
Phase 3: 완료 처리 (complete-task 인라인)
Phase 4: 상태 마커 출력
```

---

## Phase 1: 다음 승인 스펙 탐색

어느 worktree에서든 실행 가능합니다. 플랫폼 구분 없이 승인된 스펙을 찾습니다.

### Step 1: 폴더 목록 수집

`docs/specs/` 내 폴더 목록을 수집합니다.

**제외 대상**: `completed/`, `templates/`, `_legacy/`, 파일(폴더가 아닌 것)

### Step 2: 각 폴더에서 스펙 파일 찾기

각 폴더에서 다음 우선순위로 스펙 파일을 탐색합니다 (플랫폼 무관):

1. `spec-{P}.md` — 단일 플랫폼 (`spec-B.md`, `spec-M.md`)
2. `P*-{P}.md` — 멀티 플랫폼 Part (`P1-B.md`, `P2-M.md`)
3. `*-{P}.md` — 비표준 네이밍 (`F65-...-B.md`)
4. `spec.md` — 멀티 플랫폼 부모 (승인 상태 확인용)

### Step 3: 승인 상태 확인

스펙 파일(또는 멀티 플랫폼의 `spec.md`)에서 **승인 이력** 테이블을 파싱합니다.

**승인 조건 (둘 다 충족해야 함)**:
- `AI 리뷰` 행의 상태 컬럼이 `PASS` 또는 `READY`
- `사람 리뷰` 행의 상태 컬럼이 `PASS` 또는 `승인`

두 조건 모두 충족하면 승인된 스펙입니다. 하나라도 `-`이거나 누락이면 미승인 → 스킵.

### Step 4: Part 완료 여부 확인 (멀티 플랫폼 스펙)

`spec.md`에 Part 체크리스트가 있는 경우:
- `- [x] P1-B:` → 이미 완료, 스킵
- `- [ ] P1-B:` → 미완료, 구현 대상
- 여러 Part 중 **첫 번째 미완료 Part**를 구현 대상으로 선택

단일 플랫폼 스펙(`spec-B.md` 등)은 Part 체크리스트 없이 스펙 전체가 구현 대상.

### Step 5: 의존성 확인

메타데이터 테이블의 `의존` 필드에서 참조 스펙 번호를 추출합니다.

예: `의존 | Spec 69 (Entity 비활성화)` → 스펙 번호 69

**충족 조건**: `docs/specs/completed/` 에 해당 스펙 번호의 폴더가 존재 (예: `69-*` 패턴)

미충족 시 해당 스펙을 스킵합니다 (의존 스펙이 아직 완료되지 않음).

### Step 6: 정렬 및 선택

모든 필터를 통과한 스펙을 **번호 기준 오름차순 정렬**하여 가장 낮은 번호를 선택합니다.

폴더명에서 번호 추출:
- `F59-단말기바인딩` → 59
- `69-entity-deactivation` → 69
- `F65-Refresh-Token-Rotation` → 65

### 결과 없음

모든 스펙이 미승인이거나 의존성 미충족이면:

```
[IMPL-BATCH:NO_SPECS]
```

출력 후 즉시 종료합니다.

### 결과 출력

선택된 스펙 정보를 표시합니다:

```
다음 구현 대상 스펙:
- 폴더: <폴더명>
- 스펙 번호: <번호>
- 파일: <스펙 파일명>
- 플랫폼: <B/M/W>
- 유형: <Feature/Refactor/System/Bug>
```

---

## Phase 2: 구현

기존 `/impl` 워크플로우와 동일하게 진행합니다.

### 2.1 가이드 읽기

1. `.claude/guides/impl-mode.md` 읽기
2. 스펙 파일 읽기 (Phase 1에서 선택된 파일)
3. 멀티 플랫폼이면 `spec.md`도 함께 읽기 (전체 컨텍스트)
4. 스펙의 플랫폼 필드 기반으로 컨벤션 가이드 읽기:
   - B → `.claude/guides/backend-conventions.md` + `backend/ARCHITECTURE.md`
   - M → `.claude/guides/mobile-conventions.md`

### 2.2 사전 점검

**Step 1: Working tree 상태 확인**
```bash
git status --porcelain
```
- 비어있으면 → Step 2로
- 변경사항 있으면 → `[IMPL-BATCH:FAILED:<spec-id>:DIRTY_WORKTREE]` 출력 후 종료

**Step 2: main 브랜치 동기화**
```bash
git rebase main
```
- 성공 → 구현 시작
- 충돌 → `git rebase --abort` 실행 → `[IMPL-BATCH:FAILED:<spec-id>:REBASE_CONFLICT]` 출력 후 종료

### 2.3 Task 단위 구현

impl-mode.md의 워크플로우를 따릅니다:

1. 스펙의 Task를 순서대로 구현
2. 각 Task 완료 시:
   - 대응 테스트 실행 → 전체 테스트 실행 → regression 확인
   - 빌드/테스트 실패 시 → `[IMPL-BATCH:FAILED:<spec-id>:BUILD_FAILED]` 또는 `[IMPL-BATCH:FAILED:<spec-id>:TEST_FAILED]` 출력 후 종료
3. 자동 커밋 (커밋 메시지에 `(<스펙번호>)` 또는 `(<스펙번호>-P<파트>)` 포함)

---

## Phase 3: 완료 처리

기존 `/complete-task`의 Phase 1~4를 인라인으로 수행합니다.

### 3.1 정보 수집

대화 맥락에서 자동 수집합니다 (사용자 질문 없이):

| 변수 | 수집 방법 |
|------|----------|
| `work_type` | 스펙 메타데이터의 `유형` 필드 |
| `work_id` | 스펙 번호 (멀티 Part면 `<번호>-P<파트>`) |
| `platform` | 스펙 메타데이터의 `플랫폼` 필드 |
| `title` | 스펙 제목 (마크다운 H1) |
| `work_path` | 플랫폼별 경로 (B→`backend/`, M→`mobile/`) |
| `implementation_steps` | Phase 2 구현 과정에서 수집 |
| `files_created` | Phase 2에서 생성/수정한 파일 목록 |
| `test_scenarios` | Phase 2에서 실행한 테스트 목록 |

### 3.2 최종 테스트

| 작업 경로 | 테스트 명령어 |
|----------|-------------|
| `backend/**` | `cd backend && ./gradlew test` |
| `mobile/**` | `cd mobile && flutter test --reporter compact 2>&1 \| tail -1` |
| `docs/**`, 기타 | 테스트 불필요 → Phase 3.3으로 |

테스트 실패 → `[IMPL-BATCH:FAILED:<spec-id>:TEST_FAILED]` 출력 후 종료

### 3.3 Git 커밋 + Main 머지

**커밋 메시지**: complete-task.md의 "공통 매핑 테이블" 참조

```bash
# 1. 최종 커밋 (아직 커밋되지 않은 변경사항이 있는 경우)
git add <관련 파일>
git commit -m "<prefix> <설명적 제목> (<work_id>)"

# 2. main worktree에서 머지
cd /Users/youngsoolim/dev/codapt/otoki/main
git merge --no-ff <현재브랜치> -m "<prefix> <설명적 제목> (<work_id>)"
```

머지 충돌 시 → `[IMPL-BATCH:FAILED:<spec-id>:MERGE_CONFLICT]` 출력 후 종료

### 3.4 dev worktree 동기화

```bash
cd <dev worktree 경로>
git rebase main
```

### 3.5 문서 업데이트

`.claude/agents/task-completion.md`가 존재하면 Task 도구로 위임합니다.

존재하지 않으면 직접 수행:

1. **spec.md Part 체크리스트 업데이트** (멀티 플랫폼):
   - 완료된 Part의 `- [ ] P<N>-<P>:` → `- [x] P<N>-<P>:` 변경

2. **모든 Part 완료 시** (모든 체크박스가 `[x]`이거나 단일 플랫폼):
   - `docs/specs/<폴더>/`를 `docs/specs/completed/<폴더>/`로 이동

---

## Phase 4: 상태 마커 출력

모든 Phase가 성공적으로 완료되면:

```
[IMPL-BATCH:SUCCESS:<spec-id>]
```

`<spec-id>`는 스펙 번호 (예: `69`, `F59-P1`, `F62-P2`)

### 실패 마커 요약

| 마커 | 발생 시점 |
|------|----------|
| `[IMPL-BATCH:NO_SPECS]` | Phase 1: 구현할 승인 스펙 없음 |
| `[IMPL-BATCH:FAILED:<spec-id>:DIRTY_WORKTREE]` | Phase 2.2: 커밋되지 않은 변경사항 |
| `[IMPL-BATCH:FAILED:<spec-id>:REBASE_CONFLICT]` | Phase 2.2: main rebase 충돌 |
| `[IMPL-BATCH:FAILED:<spec-id>:BUILD_FAILED]` | Phase 2.3: 빌드/컴파일 실패 |
| `[IMPL-BATCH:FAILED:<spec-id>:TEST_FAILED]` | Phase 2.3 / 3.2: 테스트 실패 |
| `[IMPL-BATCH:FAILED:<spec-id>:MERGE_CONFLICT]` | Phase 3.3: main 머지 충돌 |

---

## 완료 보고

상태 마커 출력 후, 간략한 요약을 표시합니다:

```
impl-next 완료

스펙: <spec-id> - <title>
유형: <work_type>
플랫폼: <platform>
커밋: <commit_hash_list>
상태: SUCCESS

[IMPL-BATCH:SUCCESS:<spec-id>]
```

---

## 주의사항

1. **Phase 순서 엄수**: 반드시 1→2→3→4 순서로 실행
2. **실패 = 즉시 종료**: 어떤 Phase에서든 실패 시 상태 마커 출력 후 종료 (후속 Phase 실행 금지)
3. **사용자 질문 최소화**: 배치 모드에서 실행될 수 있으므로, 가능한 한 자동 판단. 정보 부족 시에만 질문
4. **상태 마커 필수**: `[IMPL-BATCH:...]` 마커는 반드시 출력해야 함 (impl-batch.sh가 파싱)
5. **기존 커맨드 미사용**: `/impl`이나 `/complete-task`를 호출하지 않고 이 커맨드 내에서 직접 처리
