# 기존소스 Sequence Diagram 문서 템플릿

> 이 파일은 `/legacy-seq` 커맨드가 호출하는 분석 에이전트의 출력 형식을 정의합니다.
> 에이전트는 이 템플릿의 섹션 순서와 작성 규칙을 반드시 따릅니다.

---

## 출력 구조

```markdown
# <기능명> 기능 분석

## 처리 흐름 Sequence Diagram

![[attachments/<모듈명>-sequence.svg]]

(Mermaid sequenceDiagram — 아래 작성 규칙 참조)

## 개요

(1~2줄 요약: 이 기능이 무엇인지, 어떤 흐름으로 동작하는지)

## 관련 소스 파일

(파일-역할 테이블)

## 상세 분석

(레이어별 분석: View → Controller → Config/Security → Service → Mapper)

## 주요 클래스별 메서드 정리

(클래스별 메서드-설명 테이블)

## 핵심 분기점

(비즈니스 로직 주요 분기 요약)
```

**별도 파일**: `<모듈명>-bdd.md` — BDD 검증 시나리오 (아래 "BDD 시나리오 작성 규칙" 참조)

---

## 섹션별 작성 규칙

### 1. 처리 흐름 Sequence Diagram

#### participant 규칙

- **W (웹/클라이언트)**: `participant W as W: Browser (<JSP 파일명>)`
- **S (서버 각 클래스)**: `participant <Alias> as S: <클래스명>`
  - Alias는 클래스명 약어 사용 (예: `LC` = LoginController, `LS` = LoginService)
- **DB**: `participant DB as S: Database`

#### 호출 표현

- 서버 측은 **클래스별 메서드 호출 관계**를 포함
- 메서드명과 주요 파라미터를 화살표 위에 표기
- `activate/deactivate` 대신 `+/-` 축약 사용
- 분기는 `alt/else/end`로 표현
- 주석은 `Note over` 사용

#### Mermaid 예약어 회피 규칙

Mermaid에서 예약어로 사용되는 단어가 participant alias나 텍스트에 포함되면 렌더링 오류가 발생합니다.

| 예약어 | 회피 방법 | 예시 |
|--------|----------|------|
| `Details` | alias 변경 | `AuthDtl`, `Dtl` 등 |
| `Note` | alias 변경 | `Nt`, `Noti` 등 |
| `Title` | alias 변경 | `Ttl` 등 |
| `end` | 텍스트 내 사용 시 줄바꿈 회피 | 메시지 안에서만 사용 가능 |
| `loop`, `alt`, `opt`, `par`, `rect` | alias로 사용 금지 | 풀네임 또는 약어 변경 |
| `as` | 메시지 텍스트에 단독 사용 주의 | 문장 내 자연스럽게 포함은 OK |

#### 기타 주의사항

- 한 다이어그램이 너무 길면(participant 15개 이상) 주요 흐름과 서브 흐름으로 분리
- 외부 API 호출은 `participant Ext as S: External API (Orora 등)` 형태로 포함

### 2. 개요

- 1~2줄로 기능의 핵심 동작 요약
- 사용하는 주요 기술/패턴 언급 (예: "Spring Security formLogin 기반", "MyBatis 임시저장 CRUD")

### 3. 관련 소스 파일

| 파일 | 역할 |
|------|------|
| `<상대 경로>` | <한 줄 설명> |

- 경로는 `$ROOT` 기준 상대 경로 (예: `controller/LoginController.java`)
- View, Controller, Config/Security, Service, Mapper 순서로 나열

### 4. 상세 분석

레이어별로 소제목을 붙여 분석:

```markdown
### <View 파일명> 상세
- 폼 구성, 입력 필드, JS 동작 등

### <Controller> 상세
- URL 매핑, 비즈니스 로직 흐름

### Config/Security (해당 시)
- 설정 내용, 필터 체인 등

### <Service> → <Mapper> (DB 연동)
- 메서드별 SQL 동작
```

### 5. 주요 클래스별 메서드 정리

클래스별로 테이블 작성:

```markdown
### <클래스명>
| 메서드 | 설명 |
|--------|------|
| `메서드명(파라미터)` | 동작 설명 |
```

- public 메서드 위주
- 파라미터 타입은 간략히 (예: `String`, `Map` 등)

### 6. 핵심 분기점

- 비즈니스 로직의 주요 분기를 **불릿 포인트**로 요약
- 각 분기의 조건과 결과를 명시
- 설정값에 의한 동작 변경도 포함

---

## BDD 시나리오 작성 규칙

분석 문서의 "핵심 분기점"을 기반으로 BDD 검증 시나리오를 **별도 파일**(`<모듈명>-bdd.md`)로 생성합니다.

### 출력 파일

```
docs/plan/old_source/summaries/docs/<모듈명>-bdd.md
```

### 문서 구조

```markdown
# <기능명> 기능 — BDD 검증 시나리오

> **원본 분석**: [[<모듈명>]] (Sequence Diagram + 구현 상세)
> **목적**: 기존 기능의 행위를 정의하고, 새 프로젝트에서 동일 행위를 검증하는 기준으로 사용

---

## 시나리오 범위 기준
(포함/제외 테이블)

## Feature: <기능 그룹 1>
(Gherkin 시나리오)

## Feature: <기능 그룹 2>
...

## 시나리오 ↔ 핵심 분기점 매핑
(분기점별 커버리지 테이블)
```

### 범위 기준

#### 포함 (시나리오로 작성)

| 기준 | 근거 |
|------|------|
| 분석 문서 "핵심 분기점"에 해당하는 비즈니스 규칙 | 각 분기점이 최소 1개 시나리오에 매핑 |
| 사용자가 인지하는 성공/실패 결과 | 새 프로젝트에서도 동일 UX 보장 |
| 설정값에 의해 달라지는 동작 | 운영 환경 설정 변경 시 영향 범위 명시 |
| 기능과 결합된 후속 비즈니스 규칙 | 기능 테스트 시 함께 검증 필요 |

#### 제외 (시나리오에서 생략)

| 제외 항목 | 이유 |
|----------|------|
| 구현 기술 세부사항 (프레임워크 내부, 렌더링, 파싱) | 새 프로젝트 기술 스택이 다름 |
| 특정 HTTP URL/리다이렉트 경로 | 라우팅은 구현 결정. "홈으로 이동"처럼 추상화 |
| UI 구현 (쿠키, JS 이벤트, CSS) | 클라이언트 UI 테스트 범위 |
| 개발/테스트 전용 엔드포인트 | 운영 기능이 아님 |
| 내부 데이터 구조 (세션 속성명, DB 컬럼명) | 구현 세부사항 |

### Gherkin 작성 규칙

- **Feature 단위 분리**: 독립적 비즈니스 관심사별로 Feature를 나눔 (인증, 권한, 기기 관리 등)
- **Background**: Feature 내 공통 전제 조건
- **Scenario Outline + Examples**: 동일 분기의 웹/모바일 변형, 열거형 규칙(권한 매핑 등)은 통합
- **추상화 수준**: 구현 세부사항 대신 행위를 기술
  - Good: `Then 홈 화면으로 이동한다`
  - Bad: `Then /home/home으로 리다이렉트된다`
- **분기점 주석**: 관련 핵심 분기점을 `# ← 핵심 분기점: ...` 주석으로 시나리오 위에 표기

### 매핑 테이블

문서 하단에 "핵심 분기점 ↔ Feature/시나리오" 매핑 테이블을 포함하여 커버리지를 확인합니다:

```markdown
## 시나리오 ↔ 핵심 분기점 매핑

| 핵심 분기점 | Feature | 시나리오 수 |
|------------|---------|:---------:|
| <분기점 1> | <Feature 명> | N |
| ...        | ...     | ... |

**총 N Feature / M Scenario** (Outline Examples 포함 시 K 케이스)
```

---

## SVG 이미지 생성 규칙

문서 저장 후, Mermaid 코드 블록에서 SVG 이미지를 생성하여 옵시디언 임베드로 포함합니다.

### 생성 절차

1. 문서에서 Mermaid 코드 블록을 임시 파일로 추출
2. `npx @mermaid-js/mermaid-cli`로 SVG 변환
3. 문서와 같은 경로의 `attachments/` 디렉토리에 저장
4. 문서 상단 Sequence Diagram 섹션에 옵시디언 임베드 링크 삽입

### 변환 명령어

```bash
# 1. Mermaid 코드 블록 추출
python3 -c "
import re, pathlib
text = pathlib.Path('<문서경로>').read_text()
m = re.search(r'\`\`\`mermaid\n(.*?)\`\`\`', text, re.DOTALL)
if m:
    pathlib.Path('/tmp/<모듈명>-seq.mmd').write_text(m.group(1))
"

# 2. SVG 변환
npx @mermaid-js/mermaid-cli -i /tmp/<모듈명>-seq.mmd -o <문서디렉토리>/attachments/<모듈명>-sequence.svg -b white
```

### 파일 배치

```
docs/plan/old_source/summaries/docs/
├── <모듈명>.md                           # 분석 문서
└── attachments/
    └── <모듈명>-sequence.svg             # Sequence Diagram 이미지
```

### 임베드 위치

`## 처리 흐름 Sequence Diagram` 섹션에서 Mermaid 코드 블록 **위**에 삽입:

```markdown
## 처리 흐름 Sequence Diagram

![[attachments/<모듈명>-sequence.svg]]

```mermaid
sequenceDiagram
    ...
```
```
