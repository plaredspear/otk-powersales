# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. 용어 정의 (Terminology)

### 문서 관련
- **프로젝트 가이드**: 이 CLAUDE.md 파일. 프로젝트의 최상위 규칙과 가이드라인.
- **기존소스**: `docs/plan/old_source/` 디렉토리의 파일. 기존 시스템의 레퍼런스 소스코드.
- **현재소스**: `backend/`, `mobile/` 디렉토리의 소스코드. 현재 개발 중인 실제 코드베이스.

### 작업 관련
- **Feature (기능)**: 사용자에게 제공하는 하나의 독립적인 기능 단위 (예: 로그인, 홈화면, 주문현황)
- **Spec (스펙)**: `docs/specs/<번호>-<기능명>/` 폴더. 스펙/요구사항의 단위. 순차 번호가 작업 식별자
- **Part (파트)**: 하나의 Spec을 플랫폼별 또는 규모별로 분할한 작업 단위. 파일명: `P<순번>-<플랫폼>.md` (예: `P1-B.md`). 단일 플랫폼이면 `spec-B.md` 하나로 관리
- **Task (태스크)**: Part 내의 세부 작업
- **플랫폼 약어**: M (Mobile/Flutter), B (Backend/Spring Boot), W (Web/React), I (Infra/Terraform)

---

## 2. 프로젝트 개요

**오뚜기 파워세일즈 앱 서비스** - B2B 영업사원 실적 조회 및 목표 관리 시스템

- Platform: iOS + Android (Flutter) + Web (관리자 대시보드)
- Backend: Spring Boot 3.x API
- Auth: 사번 + 비밀번호 로그인
- Data Sources: Orora 영업 시스템, SAP, 대형마트 EDI

### 저장소 구조 (Git Worktree)

Bare repository + worktree 방식으로 운영합니다.

```
otoki/                          # 프로젝트 루트
├── .bare/                      # Bare repository (git 데이터)
├── main/                       # [main] 메인 브랜치 worktree (머지 전용)
│   ├── mobile/                 #   Flutter app
│   ├── backend/                #   Spring Boot API
│   ├── infra/                  #   Terraform IaC (AWS)
│   ├── .claude/                #   Claude Code 설정 (가이드, 커맨드)
│   ├── docs/                   #   문서 디렉토리 (git 추적 대상 아님)
│   └── CLAUDE.md               #   이 파일
├── impl/                       # [impl] 구현 작업 worktree (Backend/Mobile/Infra 모든 플랫폼)
└── docs-spec/                  # [docs-spec] 스펙/문서 작업 worktree
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

### 개발 환경 특이사항
- **RDS 퍼블릭 접근 (dev only)**: dev 환경 RDS PostgreSQL은 `publicly_accessible = true`로 설정되어 있음. 로컬 IDE에서 직접 DB 접속 가능. 허용 IP는 `infra/envs/dev.tfvars`의 `rds_allowed_cidrs`로 제한. (Spec #89)

---

## 3. 작업 모드

**IMPORTANT**: 작업 시작 시 사용자의 요청을 분석하여 적절한 모드의 가이드를 반드시 읽은 후 진행합니다.

### 워크플로우 (docs/specs/ 폴더 기반)

```
1. 스펙 작성 (/spec) → docs/specs/backlog/<번호>-<기능명>/ 에 스펙 생성
2. AI 리뷰 (/spec-review) → 터미널에 리포트 출력
3. 사용자 검토 → 승인 → docs/specs/ready/ 로 이동
4. /impl → ready/ 에서 스펙 읽기 → impl worktree에서 구현 + 테스트 + 커밋
5. /complete-task → main worktree에서 merge --no-ff → impl worktree rebase
6. 완료 → docs/specs/completed/ 로 이동
```

### 머지 예시

`impl` worktree에서 모든 플랫폼(Backend/Mobile/Infra)을 구현하고, `main` worktree에서 머지합니다.
별도 feature 브랜치는 만들지 않습니다 (worktree가 격리 역할을 대체).

```bash
# main worktree에서 머지
cd /path/to/main
git merge --no-ff impl -m "refactor(backend): User 엔티티 레거시 스키마 정렬 (64)"

# impl worktree로 돌아와서 main 동기화
cd /path/to/impl
git rebase main
```

### 모드 판별 규칙

| 사용자 요청 패턴 | 모드 | 로드할 가이드 |
|----------------|------|-------------|
| 구현/개발/코드 작성/테스트/수정/버그, `/impl` 명령어 | **구현** | `.claude/guides/impl-mode.md` 읽기 |

### 모드별 역할

- **구현 모드**: docs/specs/ready/ 폴더의 승인된 스펙을 기반으로 코드 구현. 테스트 작성. main 머지.

---

## 4. 스펙 관리 규칙

- **스펙은 3단계 폴더로 관리**: `backlog/` → `ready/` → `completed/`
  - `docs/specs/backlog/<번호>-<기능명>/` — 초안, 리뷰 전
  - `docs/specs/ready/<번호>-<기능명>/` — 리뷰 완료 + 승인, 구현 대기
  - `docs/specs/completed/<번호>-<기능명>/` — 구현 완료
- **파일 구조**: 단일 플랫폼 → `spec-{P}.md`, 복수 플랫폼 → `spec.md` + `P<N>-{P}.md`. 모든 스펙 파일에 플랫폼 접미사 필수
- **스펙 번호는 순차 부여**: `backlog/` + `ready/` + `completed/` 에서 최대 번호 + 1
- **상태 전환**: 승인 시 `backlog/` → `ready/` 이동, 구현 완료 시 `ready/` → `completed/` 이동

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
├── specs/                # 스펙 폴더 (칸반: backlog → ready → completed)
│   ├── backlog/          #   초안, 리뷰 전
│   │   └── 92-web-admin-init/
│   ├── ready/            #   리뷰 완료 + 승인, 구현 대기
│   │   └── (승인된 스펙들)
│   ├── completed/        #   구현 완료
│   │   └── (완료된 스펙들)
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

### API 스펙

- **파일**: `backend/openapi.json` — 전체 REST API 엔드포인트가 포함된 OpenAPI 3.1 spec
- **용도**: API 계약 확인 시 컨트롤러 파일을 일일이 읽지 않고 이 파일 1회 Read로 파악
- **재생성**: `cd backend && ./gradlew generateOpenApiDocs` (API 변경 후 실행)
