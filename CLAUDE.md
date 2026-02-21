# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. 용어 정의 (Terminology)

### 문서 관련
- **프로젝트 가이드**: 이 CLAUDE.md 파일. 프로젝트의 최상위 규칙과 가이드라인.
- **개발 로그**: `docs/execution/06-개발 로그/` 디렉토리의 날짜별 작업 기록
- **기술 결정 문서**: `docs/execution/07-기술 결정/` 디렉토리의 기술적 의사결정 기록
- **기존소스**: `docs/기존소스/` 디렉토리의 파일. 기존 시스템의 레퍼런스 소스코드.
- **현재소스**: `backend/`, `mobile/` 디렉토리의 소스코드. 현재 개발 중인 실제 코드베이스.

### 작업 관련
- **Feature (기능)**: 사용자에게 제공하는 하나의 독립적인 기능 단위 (예: 로그인, 홈화면, 주문현황)
- **Part (파트)**: 플랫폼별 스펙을 한 세션에서 완료할 수 있는 크기로 분할한 작업 단위. 식별자: `<접두사><번호>-<플랫폼>-P<순번>` (예: F59-M-P1)
- **Task (태스크)**: Part 내의 세부 작업. TodoWrite 도구로 관리됨
- **플랫폼 약어**: M (Mobile/Flutter), B (Backend/Spring Boot), W (Web/React)

---

## 2. 프로젝트 개요

**오뚜기 파워세일즈 앱 서비스** - B2B 영업사원 실적 조회 및 목표 관리 시스템

- Platform: iOS + Android (Flutter) + Web (관리자 대시보드)
- Backend: Spring Boot 3.x API
- Auth: 사번 + 비밀번호 로그인
- Data Sources: Orora 영업 시스템, SAP, 대형마트 EDI

### 저장소 구조 (Git Bare Repo + Worktree)

Git bare repository를 활용하여 플랫폼별 브랜치를 독립 워크트리로 관리합니다.

```
otoki/                          # 워크스페이스 루트
├── .bare/                      # Git bare repository (core)
├── main/                       # main 브랜치 워크트리
│   ├── mobile/                 # Flutter app
│   ├── backend/                # Spring Boot API
│   ├── .claude -> ../.claude   # 공유 심볼릭 링크
│   ├── CLAUDE.md -> ../CLAUDE.md
│   └── docs -> ../docs
├── mobile/                     # mobile 브랜치 워크트리 (모바일 전용 개발)
├── backend/                    # backend 브랜치 워크트리 (백엔드 전용 개발)
├── .claude/                    # Claude Code 설정 (모든 워크트리에서 공유)
├── docs/                       # 문서 디렉토리 (git 추적 대상 아님)
├── CLAUDE.md                   # 이 파일 (모든 워크트리에서 심볼릭 링크로 공유)
├── MOBILE_DEV_GUIDE.md         # Mobile 개발 가이드
└── BACKEND_DEV_GUIDE.md        # Backend 개발 가이드
```

**Git Worktree 목록**:
- `main/` — `main` 브랜치: 전체 코드베이스 (mobile + backend)
- `mobile/` — `mobile` 브랜치: 모바일 전용 개발
- `backend/` — `backend` 브랜치: 백엔드 전용 개발

**IMPORTANT**:
- `docs/`, `.claude/`, `CLAUDE.md`, `CLAUDE.local.md`는 워크스페이스 루트에 위치하며, 각 워크트리에서 심볼릭 링크로 참조합니다.
- 이들은 `.gitignore`에 의해 git 추적 대상에서 제외되므로, `git add`, `git commit` 대상에 포함하지 마세요.

### Tech Stack

| 플랫폼 | 핵심 기술 |
|--------|----------|
| **Mobile** | Flutter 3.x, Riverpod 2.0, Dio + Retrofit, fl_chart, Hive, flutter_secure_storage |
| **Backend** | Spring Boot 3.x (Java/Kotlin), PostgreSQL, Redis, JWT, Firebase Admin SDK |
| **Web** | React 18 + Vite + TypeScript, Zustand + TanStack Query, Ant Design, ECharts |

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

### 모드 판별 규칙

| 사용자 요청 패턴 | 모드 | 로드할 가이드 |
|----------------|------|-------------|
| 구현/개발/코드 작성/테스트/수정/버그, `/impl` 명령어 | **구현** | `.claude/guides/impl-mode.md` 읽기 |

### 모드별 역할

- **구현 모드**: 승인된 스펙을 기반으로 코드 구현. 테스트 작성. Git 워크플로우 준수. 개발 로그 기록.

---

## 4. 스펙 문서 규칙

- **완료된 스펙 수정 금지**: `docs/specs/completed/` 디렉토리의 스펙 문서는 읽기 전용이며 수정하지 않는다. 완료된 기능에 변경이 필요하면 새 번호의 독립 스펙을 생성한다.
- **활성 스펙 수정 제한**: `docs/specs/` 내 활성 스펙도 원칙적으로 새 스펙 생성을 우선하며, 기존 스펙은 참조(읽기)만 한다. (상세: `.claude/guides/spec-mode.md`의 "기존 스펙 수정 금지 원칙")

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
├── specs/                       # 스펙 문서 (spec-mode 가이드 참조)
└── execution/                # 실행 및 작업 기록
    ├── 06-개발 로그/            # 주차별 상세 작업 기록
    ├── 07-기술 결정.md
    └── 08-프로젝트 관리 방법론.md
```

### 가이드 문서 목록

| 문서 | 경로 | 모드 |
|------|------|------|
| 구현 가이드 | `.claude/guides/impl-mode.md` | 구현 |
| Mobile 개발 가이드 | `MOBILE_DEV_GUIDE.md` (워크스페이스 루트) | 구현 (Flutter) |
| Backend 개발 가이드 | `BACKEND_DEV_GUIDE.md` (워크스페이스 루트) | 구현 (Backend) |
