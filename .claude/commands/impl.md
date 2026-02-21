---
description: 구현 모드 활성화 - GitHub Issue 스펙을 기반으로 코드 구현
runOn: project
---

# 구현 모드

**IMPORTANT**: `.claude/guides/impl-mode.md`를 읽고 그 내용을 따라 진행합니다.

## 입력 파라미터 분석

사용자가 전달한 인자를 파싱합니다: $ARGUMENTS

- Issue 번호가 있으면 (예: `#42`): 해당 Issue의 구현을 시작
- 인자가 없으면: 사용자에게 어떤 작업을 구현할지 질문

## 워크플로우

1. **impl-mode.md** 가이드를 읽습니다
2. **Issue 본문 읽기**: `gh issue view <번호>`로 스펙 확인
3. 컨텍스트에 맞는 플랫폼별 개발 가이드를 읽습니다
4. Feature 브랜치 생성: `feature/#<번호>-<설명>`
5. Task 단위로 구현을 진행합니다
6. 각 Task 완료 시 테스트 실행 → 자동 커밋 (커밋 메시지에 `#<번호>` 포함)
7. 모든 Task 완료 후 PR 생성 (`Closes #<번호>`)
