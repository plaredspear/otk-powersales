#!/usr/bin/env bash
# OVIP 연동 API 가이드 (docs/guide/ovip-integration-api.md) 를 PDF 로 렌더링한다.
# 의존: npx (marked), 로컬 Chrome/Edge (headless --print-to-pdf).
# 사용: scripts/render-ovip-doc-pdf.sh
set -euo pipefail

SRC_DEFAULT="docs/guide/ovip-integration-api.md"
SRC="${1:-$SRC_DEFAULT}"
OUT="${SRC%.md}.pdf"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -f "$SRC" ]]; then
  echo "원본 마크다운을 찾을 수 없습니다: $SRC" >&2
  exit 1
fi

# 1) Markdown -> HTML fragment
npx --yes marked -i "$SRC" -o "$TMP/body.html"

# 2) 인쇄용 스타일로 감싼 완전한 HTML 조립 (한글 = 시스템 폰트)
cat > "$TMP/doc.html" <<'HTML'
<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<title>OVIP 연동 API 가이드</title>
<style>
  @page { size: A4; margin: 18mm 16mm; }
  * { box-sizing: border-box; }
  body {
    font-family: "Apple SD Gothic Neo", "Noto Sans KR", "Malgun Gothic", sans-serif;
    font-size: 10.5pt; line-height: 1.6; color: #1a1a1a; margin: 0;
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
  }
  h1 { font-size: 20pt; border-bottom: 3px solid #c0392b; padding-bottom: 8px; margin: 0 0 16px; }
  h2 { font-size: 15pt; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin: 26px 0 12px; page-break-after: avoid; }
  h3 { font-size: 12.5pt; margin: 18px 0 8px; page-break-after: avoid; }
  h4 { font-size: 11pt; margin: 14px 0 6px; page-break-after: avoid; }
  p, li { margin: 6px 0; }
  a { color: #2255aa; text-decoration: none; word-break: break-all; }
  code {
    font-family: "SFMono-Regular", "D2Coding", "Consolas", monospace;
    background: #f2f3f5; padding: 1px 5px; border-radius: 3px; font-size: 9pt; word-break: break-all;
  }
  pre {
    background: #f6f8fa; border: 1px solid #e1e4e8; border-radius: 6px;
    padding: 10px 12px; overflow-x: auto; page-break-inside: avoid;
  }
  pre code { background: none; padding: 0; font-size: 8.8pt; line-height: 1.45; }
  table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 9.3pt; page-break-inside: avoid; }
  th, td { border: 1px solid #d0d7de; padding: 5px 8px; text-align: left; vertical-align: top; }
  th { background: #eef1f4; font-weight: 600; }
  tr:nth-child(even) td { background: #fafbfc; }
  blockquote {
    margin: 10px 0; padding: 8px 14px; background: #fff8e6;
    border-left: 4px solid #f0ad4e; color: #5a4a1a; page-break-inside: avoid;
  }
  blockquote p { margin: 4px 0; }
  hr { border: none; border-top: 1px solid #e1e4e8; margin: 22px 0; }
  h1, h2 { page-break-inside: avoid; }
</style>
</head>
<body>
HTML
cat "$TMP/body.html" >> "$TMP/doc.html"
echo "</body></html>" >> "$TMP/doc.html"

# 3) 로컬 Chrome/Edge headless 로 PDF 인쇄
CHROME=""
for c in \
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge" \
  "/Applications/Chromium.app/Contents/MacOS/Chromium"; do
  [[ -x "$c" ]] && CHROME="$c" && break
done
if [[ -z "$CHROME" ]]; then
  echo "Chrome/Edge/Chromium 을 찾을 수 없습니다 (headless PDF 인쇄 불가)" >&2
  exit 1
fi

OUT_ABS="$(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")"
"$CHROME" --headless --disable-gpu --no-pdf-header-footer \
  --print-to-pdf="$OUT_ABS" "file://$TMP/doc.html" >/dev/null 2>&1

echo "생성 완료: $OUT_ABS"
