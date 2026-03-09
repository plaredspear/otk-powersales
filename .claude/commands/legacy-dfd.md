---
description: 레거시 시스템 Data Flow Diagram 생성/업데이트
runOn: project
---

# 레거시 DFD 업데이트

레거시 시스템의 데이터 흐름도(DFD)를 생성하거나 업데이트합니다.

## 스크립트 목록

| 스크립트 | 관점 | 출력 |
|---------|------|------|
| `docs/plan/generate_legacy_dfd.py` | 전체 시스템 (5계층) | `otoki-legacy-dfd.png` |
| `docs/plan/generate_heroku_dfd.py` | Heroku 중심 (데이터 원천 추적) | `otoki-heroku-dfd.png` |

## 워크플로우

1. `$ARGUMENTS` 파싱 — 수정 지시사항 확인 (예: "heroku write path 강조", "전체 EDI 흐름 추가")
2. 대상 스크립트 결정:
   - "heroku" 키워드 → `generate_heroku_dfd.py`
   - "전체" 또는 키워드 없음 → `generate_legacy_dfd.py`
   - "둘 다" → 양쪽 모두
3. 스크립트 읽기 → 지시사항에 따라 수정 → 실행
4. 생성된 PNG를 Read tool로 열어 시각적 확인
5. 결과 보고 (변경사항 요약, 출력 경로)

## 규칙

- DFD 스크립트가 "소스 오브 트루스" — skill은 이 스크립트를 수정/실행하는 래퍼
- graphviz 라이브러리 사용 (`generate_legacy_diagram.py` 패턴)
- 출력 경로: `docs-spec/docs/plan/` (docs-spec worktree 기준)
