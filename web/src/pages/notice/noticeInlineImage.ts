/**
 * 공지 본문 인라인 이미지의 presigned URL ↔ placeholder 변환 (순수 함수).
 *
 * 본문 DB 에는 만료 없는 placeholder `<img src="notice-image://{refid}" data-refid="{refid}">` 만 저장하고,
 * 조회 시점에 백엔드가 presigned URL 로 rewrite 한다. 에디터는 presigned 로 보여주되 **저장 직전 반드시**
 * placeholder 로 되돌려야 한다 — 만료되는 URL(30분)이 본문에 저장되면 그 시점부터 이미지가 영구히 깨진다.
 *
 * ## 매칭 키를 uniqueKey 로 잡는 이유 (실제 사고: 공지 2393)
 * 직전 구현은 presigned URL **전문**을 키로 치환했다. 그런데 Quill 이 돌려주는 HTML 은 속성값의 `&` 를
 * `&amp;` 로 이스케이프하므로, 서명 쿼리스트링(`...&X-Amz-Signature=...`)이 어긋나 치환이 통째로 실패했고
 * 만료 URL 이 DB 에 그대로 저장됐다. URL 에 내재된 uniqueKey(`?` 앞 경로의 `private/` 이후)는 이스케이프
 * 대상 문자가 없고 재서명에도 불변이라 안전한 키다 (백엔드 `NoticeImagePlaceholder` 와 동일 규칙).
 *
 * 순수 함수로 분리해 두어 회귀 테스트(noticeInlineImage.test.ts)가 이 규칙을 고정한다.
 */

/** private S3 key 의 세그먼트 prefix. presigned URL path 에서 이 뒤가 uniqueKey. */
export const PRIVATE_PATH_SEGMENT = 'private/';

/** `<img ...>` 태그 전체. 속성 순서와 무관하게 태그를 잡고 각 속성은 개별 파싱한다. */
const IMG_TAG_REGEX = /<img\b[^>]*>/gi;
const SRC_ATTR_REGEX = /\bsrc\s*=\s*"([^"]*)"/i;
const REFID_ATTR_REGEX = /\bdata-refid\s*=\s*"([^"]*)"/i;

/**
 * presigned URL 에서 불변 uniqueKey(= upload_file.unique_key) 추출.
 * 서명 쿼리스트링은 매번 바뀌고 HTML 이스케이프도 받으므로 `?` 앞 경로만 사용한다.
 * "private/" 를 포함하지 않는 src(placeholder scheme, 외부 URL 등)는 null.
 */
export function uniqueKeyFromSrc(src: string): string | null {
  const path = src.split('?')[0];
  const idx = path.indexOf(PRIVATE_PATH_SEGMENT);
  if (idx < 0) return null;
  return path.slice(idx + PRIVATE_PATH_SEGMENT.length) || null;
}

/** 백엔드 `NoticeImagePlaceholder.build` 와 동일 형식의 placeholder 태그. */
export function buildPlaceholder(refid: string): string {
  return `<img src="notice-image://${refid}" data-refid="${refid}">`;
}

/**
 * 서버 상세조회 본문(`<img src="presigned" data-refid="{refid}">`)에서 uniqueKey → placeholder 매핑 추출.
 * Quill 은 로드하며 data-refid 를 버리므로, 에디터에 싣기 **전** 원본 HTML 에서 뽑아 둬야 한다.
 */
export function collectPlaceholderMappings(html: string | null | undefined): Map<string, string> {
  const map = new Map<string, string>();
  if (!html) return map;
  for (const m of html.matchAll(IMG_TAG_REGEX)) {
    const tag = m[0];
    const src = SRC_ATTR_REGEX.exec(tag)?.[1];
    const refid = REFID_ATTR_REGEX.exec(tag)?.[1];
    if (!src || !refid || !/^https?:/i.test(src)) continue;
    const key = uniqueKeyFromSrc(src);
    if (key) map.set(key, buildPlaceholder(refid));
  }
  return map;
}

/** 본문 placeholder 의 커스텀 스킴 (백엔드 NoticeImagePlaceholder.SCHEME 와 동일 값). */
const PLACEHOLDER_SCHEME = 'notice-image://';

/**
 * 저장해도 살릴 수 없는 이미지 참조인지 판정한다 (default-deny).
 *
 * 살릴 수 있는 것만 열거하고 나머지를 전부 막는다:
 * - `http(s)://` — 저장 시 서버가 내려받아 S3 로 이관한다 (NoticeService.normalizeInlineExternalImages)
 * - `data:image/` — 붙여넣기 즉시 업로드로 변환되고, 남더라도 서버가 정규화한다
 * - `notice-image://` — 이미 우리 placeholder
 *
 * 나머지(`file:` = 워드/한글이 만든 작성자 PC 로컬 경로, `blob:` = 브라우저 메모리, `cid:` = 메일 첨부,
 * 상대경로 등)는 저장 순간 죽는 참조다. Windows 에서 Word/한글 문서의 이미지를 붙여넣으면 클립보드에
 * 비트맵 대신 `file:///.../clip_image001.png` HTML 만 담기는 경우가 많아 실제로 자주 발생한다.
 */
export function isUnrecoverableImageSrc(src: string): boolean {
  const lower = src.trim().toLowerCase();
  if (!lower) return false;
  if (lower.startsWith('http://') || lower.startsWith('https://')) return false;
  if (lower.startsWith('data:image/')) return false;
  if (lower.startsWith(PLACEHOLDER_SCHEME)) return false;
  return true;
}

/**
 * 본문에서 저장해도 살릴 수 없는 이미지 참조를 찾는다 (저장 전 사용자 안내용).
 * 반환값이 비어 있지 않으면 그대로 저장해 봐야 깨진 이미지가 되므로, 툴바/드래그앤드롭으로 다시 넣도록 안내한다.
 */
export function findUnrecoverableImageSrcs(html: string | null | undefined): string[] {
  if (!html) return [];
  const found: string[] = [];
  for (const m of html.matchAll(IMG_TAG_REGEX)) {
    const src = SRC_ATTR_REGEX.exec(m[0])?.[1];
    if (src && isUnrecoverableImageSrc(src)) found.push(src);
  }
  return found;
}

/**
 * 저장 직전 본문의 presigned `<img>` 를 placeholder 로 치환한다.
 * 매핑에 없는 src(외부 링크 등)와 placeholder 태그는 원본 그대로 둔다 — 백엔드가 저장 시점에 한 번 더
 * 정규화(`NoticeService.normalizeInlinePresignedImages`)하므로 여기서 누락돼도 DB 에는 남지 않는다.
 */
export function replacePreviewsWithPlaceholders(
  html: string,
  mappings: Map<string, string>,
): string {
  return html.replace(IMG_TAG_REGEX, (tag) => {
    const src = SRC_ATTR_REGEX.exec(tag)?.[1];
    if (!src) return tag;
    const key = uniqueKeyFromSrc(src);
    if (!key) return tag;
    return mappings.get(key) ?? tag;
  });
}
