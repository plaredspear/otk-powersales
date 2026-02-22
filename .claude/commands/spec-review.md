---
description: 스펙의 AI 구현 준비도 리뷰 - Part Issue 분할 적합성 포함
runOn: project
---

# 스펙 리뷰 모드

스펙 문서의 AI(Sonnet 4.6) 구현 준비도를 자동 검증합니다.
Part 단위 GitHub Issue 분할 적합성도 함께 평가합니다.

## 입력 파라미터 분석

사용자가 전달한 인자를 파싱합니다: $ARGUMENTS

인자는 다음 형태 중 하나입니다. 형태를 판별하여 적절히 처리합니다:

| 형태 | 예시 | 처리 방식 |
|------|------|----------|
| **인자 없음** | `/spec-review` | 사용자에게 리뷰 대상을 질문 |
| **파일명** | `F22-주문서작성-B.md` | `docs/specs/`에서 파일을 찾아 리뷰 |
| **Feature ID** | `F22-B` | 대응하는 스펙 파일을 탐색하여 리뷰 |
| **GitHub Issue** | `#42` | `gh issue view 42 --json body`로 본문을 가져와 리뷰 |

### 판별 규칙

1. `#숫자` 패턴이면 → **GitHub Issue** → `gh issue view`로 본문 로드
2. `.md` 확장자가 포함되면 → **파일명** → `docs/specs/`에서 탐색
3. `F숫자-` 패턴이면 → **Feature ID** → `docs/specs/`에서 패턴 매칭
4. 인자가 없으면 → 사용자에게 질문:
   - "리뷰할 스펙을 지정해주세요. (파일명, Feature ID, 또는 GitHub Issue #번호)"

### 스펙 로드 절차

**파일명/Feature ID인 경우:**
1. `docs/specs/` 하위에서 Glob으로 파일 탐색
2. 해당 Feature 디렉토리 내 모든 플랫폼 스펙 파일을 수집
3. 플랫폼 판별: 파일명 접미사 (`-B.md`, `-M.md`, `-W.md`)

**GitHub Issue인 경우:**
1. `gh issue view <번호> --json body,title,labels` 실행
2. Issue 본문에서 플랫폼 정보 추출 (라벨 또는 본문 내용 기반)
3. 본문을 스펙 내용으로 사용

## 리뷰 실행

### 플랫폼/작업유형 판별

스펙 내용에서 플랫폼과 작업 유형을 자동 판별합니다:

- **플랫폼**: 파일명 접미사, 개요 테이블의 플랫폼 필드, 또는 Issue 라벨
- **작업 유형**: 파일명 접두사 (F=Feature, R=Refactor, S=System, B=Bug) 또는 Issue 제목

### spec-reviewer 에이전트 호출

Task 도구로 `spec-reviewer` 에이전트를 호출합니다:

```
에이전트에 전달할 프롬프트:

다음 스펙의 AI 구현 준비도를 리뷰해주세요.

- 플랫폼: <판별된 플랫폼>
- 작업 유형: <판별된 작업 유형>
- 스펙 내용:

<로드된 스펙 내용>
```

에이전트가 `.claude/guides/spec-review-criteria.md`를 읽고 리뷰를 수행합니다.

## 결과 출력 및 후속 안내

에이전트로부터 리뷰 리포트를 수신하면 사용자에게 표시하고,
종합 판정에 따라 후속 단계를 안내합니다:

### READY 판정 시

1. 리뷰 리포트를 터미널에 표시합니다
2. Part Issue 본문 초안을 표시합니다
3. **사용자에게 스펙 리뷰를 요청합니다**:

```
리뷰 결과: READY - AI 리뷰 통과

스펙 파일을 에디터에서 최종 확인해주세요:
📄 docs/specs/<파일명>.md

확인 후 터미널에서 승인 또는 수정 의견을 알려주세요.
```

4. 사용자 응답 대기:
   - **승인** ("승인", "OK", "진행" 등) → Issue 등록 절차 진행
   - **수정 요청** (구체적 피드백) → 스펙 파일 수정 후 재리뷰 안내

사용자가 승인하면 `gh issue create`를 사용하여 등록합니다:
1. 부모 Issue 생성 → Issue 번호 획득
2. 각 Part Issue 생성 (부모 Issue 번호를 참조로 포함) → Part Issue 번호 획득
3. 부모 Issue 본문에 Task List 업데이트 (Part Issue 번호로 `- [ ] #번호` 형식)

### NEEDS_WORK 판정 시

1. 리뷰 리포트를 표시합니다
2. FAIL/WARN 항목의 수정 방안을 강조합니다
3. 안내:

```
리뷰 결과: NEEDS_WORK - 수정 후 재리뷰를 권장합니다.

위 FAIL/WARN 항목을 수정한 후 `/spec-review`를 다시 실행해주세요.
```

### NOT_READY 판정 시

1. 리뷰 리포트를 표시합니다
2. FAIL 항목 목록과 수정 가이드를 강조합니다
3. 안내:

```
리뷰 결과: NOT_READY - FAIL 항목을 반드시 수정해야 합니다.

FAIL 항목 N건을 수정한 후 `/spec-review`를 다시 실행해주세요.
Part Issue 등록은 최소 READY 판정 이후 가능합니다.
```
