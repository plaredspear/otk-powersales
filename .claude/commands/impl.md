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

## 실행 컨텍스트 판별

**PR에서 트리거된 경우** (GitHub Action 환경):
1. PR 본문 읽기: `gh pr view <PR번호> --json body,title,number`
2. 본문에서 `Spec: #<번호>` 패턴으로 부모 Issue 번호 추출
3. 부모 Issue 스펙 읽기: `gh issue view <Issue번호>`
4. 선행 Part PR이 있으면 확인: `gh pr view <선행PR번호>`
5. 브랜치 생성/PR 생성 **스킵** (이미 존재)
6. 구현 → 테스트 → Push

**로컬에서 직접 실행된 경우**:
1. Issue 본문 읽기: `gh issue view <번호>`
2. Feature 브랜치 생성: `feature/#<번호>-<설명>`
3. 구현 → 테스트 → PR 생성

## 워크플로우

1. **impl-mode.md** 가이드를 읽습니다
2. 실행 컨텍스트 판별 (위 참조)
3. 컨텍스트에 맞는 플랫폼별 개발 가이드를 읽습니다
4. Task 단위로 구현을 진행합니다
5. 각 Task 완료 시 테스트 실행 → 자동 커밋 (커밋 메시지에 `#<Issue번호>` 포함)
6. (로컬 실행 시만) 모든 Task 완료 후 PR 생성
