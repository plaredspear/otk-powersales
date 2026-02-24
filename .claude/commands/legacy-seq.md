---
description: 기존소스 기능별 Sequence Diagram 문서 자동 생성
runOn: project
---

# 기존소스 Sequence Diagram 생성

기존소스(Spring MVC + JSP + MyBatis)의 기능을 분석하여 Mermaid sequence diagram 포함 문서를 생성합니다.

## 입력 파라미터

사용자 인자: $ARGUMENTS

인자가 비어 있으면 사용자에게 기능명을 질문합니다.

## 워크플로우

### 1. 기능명 추출

`$ARGUMENTS`에서 기능명(한글)을 추출합니다.

예시: `/legacy-seq 로그인` → 기능명 = "로그인"

### 2. 모듈 매핑 확인

`docs/plan/기존소스/summaries/_index.md` 파일을 읽어서:
- 기능명에 대응하는 모듈(섹션)을 찾습니다
- 해당 모듈의 호출 체인과 레이어별 파일 매핑을 파악합니다

모듈 매핑 예시:

| 기능명 | 모듈 | 관련 요약 파일 |
|--------|------|--------------|
| 로그인 | login | controller/LoginController.md, service/LoginService.md, mapper/loginMapper.md, view/login.md, config/overview.md |
| 주문 | order | controller/OrderController.md, service/OrderService.md, mapper/orderMapper.md, view/order.md |
| 현장토크 | fieldTalk | controller/FieldTalkController.md, service/FieldTalkService.md, mapper/fieldTalkMapper.md, view/fieldTalk.md |

기능명이 _index.md의 어떤 모듈에도 매칭되지 않으면, 사용자에게 가능한 모듈 목록을 보여주고 선택을 요청합니다.

### 3. 분석 에이전트 호출

`legacy-analyzer` subagent(Task tool, subagent_type="legacy-analyzer")에 분석을 위임합니다.

프롬프트에 다음 정보를 포함합니다:

```
기능명: <기능명>
모듈명: <모듈명>
템플릿 경로: .claude/guides/legacy-seq-template.md
원본 소스 루트: docs/plan/기존소스/otg_PowerSales-master/
요약 파일 루트: docs/plan/기존소스/summaries/
_index.md 경로: docs/plan/기존소스/summaries/_index.md

관련 요약 파일 목록:
- <_index.md에서 파악한 파일 목록>

호출 체인:
<_index.md 호출 체인 섹션에서 복사>

분석 절차:
1. 템플릿 파일을 읽고 출력 형식을 파악하세요
2. 요약 파일들을 읽어 전체 구조를 파악하세요
3. 원본 소스를 직접 읽어 상세 메서드 호출 관계를 추적하세요
4. 템플릿 형식에 맞춰 Mermaid sequenceDiagram + 분석 문서를 작성하세요
5. 완성된 문서 내용을 반환하세요 (Write는 하지 마세요)
```

### 4. 결과 저장

에이전트가 반환한 문서 내용을 아래 경로에 저장합니다:

```
docs/plan/기존소스/summaries/docs/<모듈명>.md
```

### 5. SVG 이미지 생성

저장된 문서에서 Mermaid 코드 블록을 추출하여 SVG 이미지를 생성합니다:

```bash
# 1. Mermaid 코드 블록 추출
python3 -c "
import re, pathlib
text = pathlib.Path('docs/plan/기존소스/summaries/docs/<모듈명>.md').read_text()
m = re.search(r'\`\`\`mermaid\n(.*?)\`\`\`', text, re.DOTALL)
if m:
    pathlib.Path('/tmp/<모듈명>-seq.mmd').write_text(m.group(1))
"

# 2. attachments 디렉토리 생성 + SVG 변환
mkdir -p docs/plan/기존소스/summaries/docs/attachments
npx @mermaid-js/mermaid-cli -i /tmp/<모듈명>-seq.mmd -o docs/plan/기존소스/summaries/docs/attachments/<모듈명>-sequence.svg -b white
```

SVG 생성 후 문서의 `## 처리 흐름 Sequence Diagram` 바로 아래에 옵시디언 임베드 링크가 있는지 확인하고, 없으면 추가합니다:

```markdown
![[attachments/<모듈명>-sequence.svg]]
```

### 6. 완료 안내

```
✅ <기능명> 기능 분석 문서가 생성되었습니다.
📄 문서: docs/plan/기존소스/summaries/docs/<모듈명>.md
🖼️ SVG:  docs/plan/기존소스/summaries/docs/attachments/<모듈명>-sequence.svg
```
