# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. 용어 정의 (Terminology)

### 문서 관련
- **프로젝트 가이드**: 이 CLAUDE.md 파일. 프로젝트의 최상위 규칙과 가이드라인.
- **기존소스**: `docs/기존소스/` 디렉토리의 파일. 기존 시스템의 레퍼런스 소스코드.
- **현재소스**: `backend/`, `mobile/` 디렉토리의 소스코드. 현재 개발 중인 실제 코드베이스.

### 작업 관련
- **Feature (기능)**: 사용자에게 제공하는 하나의 독립적인 기능 단위 (예: 로그인, 홈화면, 주문현황)
- **Issue**: GitHub Issue. 스펙/요구사항의 단위. Issue 번호(`#42`)가 작업 식별자
- **Part (파트)**: 하나의 Issue를 한 세션에서 완료할 수 있는 크기로 분할한 작업 단위. 식별자: `#<Issue번호>-P<순번>` (예: `#42-P1`)
- **Task (태스크)**: Part 내의 세부 작업
- **플랫폼 약어**: M (Mobile/Flutter), B (Backend/Spring Boot), W (Web/React), I (Infra/Terraform)

---

## 2. 프로젝트 개요

**오뚜기 파워세일즈 앱 서비스** - B2B 영업사원 실적 조회 및 목표 관리 시스템

- Platform: iOS + Android (Flutter) + Web (관리자 대시보드)
- Backend: Spring Boot 3.x API
- Auth: 사번 + 비밀번호 로그인
- Data Sources: Orora 영업 시스템, SAP, 대형마트 EDI

### 저장소 구조

```
otoki/                          # 프로젝트 루트
├── mobile/                     # Flutter app
├── backend/                    # Spring Boot API
├── infra/                      # Terraform IaC (AWS)
├── .claude/                    # Claude Code 설정 (가이드, 커맨드)
├── docs/                       # 문서 디렉토리 (git 추적 대상 아님)
└── CLAUDE.md                   # 이 파일
```

**IMPORTANT**: `docs/`는 `.gitignore`에 의해 git 추적 대상에서 제외됩니다.

### Tech Stack

| 플랫폼 | 핵심 기술 |
|--------|----------|
| **Mobile** | Flutter 3.x, Riverpod 2.0, Dio + Retrofit, fl_chart, Hive, flutter_secure_storage |
| **Backend** | Spring Boot 3.x (Java/Kotlin), PostgreSQL, Redis, JWT, Firebase Admin SDK |
| **Web** | React 18 + Vite + TypeScript, Zustand + TanStack Query, Ant Design, ECharts |
| **Infra** | Terraform >= 1.11, AWS (VPC, ECS Fargate, RDS PostgreSQL, ElastiCache Redis, ALB, ECR, ACM/Route53) |

### 아키텍처
- Clean Architecture (domain/data/presentation layers)
- Repository pattern
- UseCase-based business logic

### 외부 시스템 연동
- **Orora 영업**: 고객품목실적일, 농협 EDI
- **SAP/알라딘**: 목표 입력 연동
- **데이터레이크**: 대형마트 EDI (이마트, 홈플러스, 롯데마트)

### 보안/인증
- JWT token-based authentication
- Secure token storage (flutter_secure_storage)
- Password encryption + Auto token refresh

---

## 3. 작업 모드

**IMPORTANT**: 작업 시작 시 사용자의 요청을 분석하여 적절한 모드의 가이드를 반드시 읽은 후 진행합니다.

### 워크플로우 (GitHub Issue + PR 기반)

```
1. 스펙 작성 (/spec) → docs/specs/ 에 .md 파일로 저장
2. AI 리뷰 (/spec-review) → 터미널에 리포트 출력
3. 사용자 검토 → 승인 또는 수정
4. 부모 Issue 등록 (전체 스펙 + Part 체크리스트)
5. Part별 Feature 브랜치 + Draft PR 생성 (로컬에서 `gh pr create`)
   - PR 본문: 부모 Issue 참조 + Part 범위 요약
6. PR 리뷰 코멘트에 `@claude` → Claude Code Action 실행 → 구현 → Push
7. PR 리뷰 코멘트로 피드백 → @claude 수정 지시 → 반복
8. Ready for Review → Merge → 부모 Issue 체크리스트 업데이트
9. 모든 Part 완료 → 부모 Issue close
```

### Part PR 본문 형식

```markdown
Spec: #<부모Issue번호>
Part: P<순번>/<총Part수>
Prerequisites: #<선행PR번호> 또는 "None"

### Scope
[이 Part에서 구현할 내용 - 부모 Issue 스펙에서 해당 부분만 발췌/요약]

### Files
[대상 파일 목록]
```

### Part PR 생성 예시

```bash
git checkout -b feature/#42-P1-domain
gh pr create --draft \
  --title "feat: 매출현황 도메인 레이어 (#42-P1)" \
  --body "Spec: #42
Part: P1/3
Prerequisites: None

### Scope
- Entity, Repository 인터페이스
- UseCase 구현"
# PR 생성 후 리뷰 코멘트에 @claude 를 달아 구현 트리거
```

### 모드 판별 규칙

| 사용자 요청 패턴 | 모드 | 로드할 가이드 |
|----------------|------|-------------|
| 구현/개발/코드 작성/테스트/수정/버그, `/impl` 명령어 | **구현** | `.claude/guides/impl-mode.md` 읽기 |

### 모드별 역할

- **구현 모드**: GitHub Issue의 스펙을 기반으로 코드 구현. 테스트 작성. PR 생성.

---

## 4. 스펙 관리 규칙

- **스펙은 `docs/specs/`에 .md 파일로 작성** 후 사람 리뷰를 거쳐 **GitHub Issue로 등록**: Issue 번호가 최종 스펙 식별자
- **Issue 제목에 작업 유형 표기**: `[Feature] 매출현황`, `[Bug] 로그인 오류` 등 (또는 GitHub Labels 활용)
- **완료된 Issue**: PR merge 시 자동 close

---

## 5. 레퍼런스

### 문서 디렉토리 구조

**IMPORTANT**: 사용자가 "문서"라고 언급하면 `docs/` 폴더를 문서 루트로 인식합니다.

```
docs/
├── plan/                 # 설계 및 계획 문서
│   ├── 00-프로젝트 개요.md
│   ├── 01-기능 요구사항/
│   ├── 02-화면 설계.md
│   ├── 03-기술 스택.md
│   └── 04-API 설계.md
├── specs/                # 스펙 파일 (사람 리뷰용, Issue 등록 전 단계)
└── execution/                # 실행 및 작업 기록
    └── 08-프로젝트 관리 방법론.md
```

### 가이드 문서 목록

| 문서 | 경로 | 모드 |
|------|------|------|
| 구현 가이드 | `.claude/guides/impl-mode.md` | 구현 |
| Backend 컨벤션 | `.claude/guides/backend-conventions.md` | 구현 (Backend) |
| Mobile 컨벤션 | `.claude/guides/mobile-conventions.md` | 구현 (Flutter) |
| 스펙 리뷰 기준 | `.claude/guides/spec-review-criteria.md` | 스펙 리뷰 |
