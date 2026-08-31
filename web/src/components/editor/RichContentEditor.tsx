import { useEffect, useMemo, useRef } from 'react';
import { message } from 'antd';
import ReactQuill, { Quill } from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import './RichContentEditor.css';
import { isUnrecoverableImageSrc, uniqueKeyFromSrc } from '@/lib/inlineImage';

// 본문 인라인 이미지 허용 타입/용량 (백엔드 StorageConstants 와 정합).
const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif', 'image/webp'];
const MAX_IMAGE_BYTES = 20 * 1024 * 1024; // 20MB

// 정렬(align)은 Quill 기본이 class 기반(ql-align-*)이라 CSS 가 없는 환경(모바일 HtmlWidget)에서 렌더되지 않는다.
// 인라인 style(text-align) 로 출력하도록 style attributor 를 등록해 웹/모바일 렌더를 일치시킨다.
// (color/background 는 기본이 이미 인라인 style 이라 별도 등록 불요.)
Quill.register('formats/align', Quill.import('attributors/style/align'), true);

/** 인라인 이미지 업로드 API 의 응답 (공지/교육 공통 형태). */
export interface InlineImageUploadResult {
  refid: string;
  placeholder: string;
  previewUrl: string;
}

export interface RichContentEditorProps {
  /** antd Form.Item 이 주입하는 현재 본문 HTML. */
  value?: string;
  /** antd Form.Item 이 주입하는 변경 콜백. */
  onChange?: (html: string) => void;
  /** 도메인별 인라인 이미지 업로드 API (예: uploadNoticeInlineImage / uploadEducationInlineImage). */
  uploadInlineImage: (file: File) => Promise<InlineImageUploadResult>;
  /**
   * 업로드된 이미지의 uniqueKey → placeholder 매핑을 보관하는 ref.
   * 저장 직전 폼이 `replacePreviewsWithPlaceholders` 로 본문을 되돌릴 때 쓴다.
   */
  previewToPlaceholder: React.RefObject<Map<string, string>>;
  /**
   * 이번 편집 세션에서 업로드한 refid 누적 ref.
   * 저장 시 서버가 본문에서 빠진 이미지를 정리하는 대상 판별에 넘긴다.
   */
  sessionUploadedRefids: React.RefObject<Set<string>>;
}

/**
 * 본문 리치텍스트 에디터 — 공지 / 교육 공용. antd Form.Item 의 controlled 필드로 동작한다.
 *
 * Form.Item 이 주입하는 value(현재 HTML)/onChange 를 ReactQuill 에 연결한다.
 * (ReactQuill 을 <div> 로 감싸면 Form.Item 의 value/onChange 가 에디터로 전달되지 않아 본문이 폼 값으로
 *  수집되지 못하고 미리보기/저장에 누락된다.)
 *
 * 이미지 삽입 경로 3가지(툴바 버튼 / 붙여넣기 / 드래그앤드롭)를 모두 정상 업로드 경로로 일원화한다.
 * 붙여넣기·드롭은 Quill root(.ql-editor)에 **capture 단계** 리스너를 직접 걸어 처리한다 — wrapper 버블링으로는
 * 하위 contenteditable 에서 이미 벌어진 Quill 기본 삽입을 막지 못해 base64+presigned 2중 삽입이 발생한다.
 */
export default function RichContentEditor({
  value,
  onChange,
  uploadInlineImage,
  previewToPlaceholder,
  sessionUploadedRefids,
}: RichContentEditorProps) {
  const quillRef = useRef<ReactQuill>(null);
  // 이번 붙여넣기에서 걸러낸 로컬 전용 이미지(file:/blob: 등) 개수 + 복구 예약 여부.
  // matcher 는 이미지 노드마다 호출되므로, 붙여넣기 1회당 복구를 한 번만 돌리기 위한 상태다.
  const droppedLocalImages = useRef(0);
  const localImageRecoveryScheduled = useRef(false);
  // 콜백/ref 를 최신값으로 유지 — 아래 useEffect 는 리스너 중복 등록을 막으려 mount 시 1회만 실행된다.
  const uploadRef = useRef(uploadInlineImage);
  uploadRef.current = uploadInlineImage;

  // 파일 1건 업로드 → 에디터 현재 커서 위치에 presigned 이미지 삽입 + placeholder 매핑 보관.
  const uploadAndInsert = useRef(async (file: File) => {
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      message.error('이미지 파일(PNG, JPG, GIF, WEBP)만 첨부할 수 있습니다');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      message.error('이미지 용량은 최대 20MB까지 가능합니다');
      return;
    }
    const editor = quillRef.current?.getEditor();
    if (!editor) return;

    const hide = message.loading('이미지 업로드 중...', 0);
    try {
      const result = await uploadRef.current(file);
      // 매핑 키는 presigned URL 전문이 아니라 불변 uniqueKey — Quill 이 속성값의 `&` 를 `&amp;` 로
      // 이스케이프해 URL 전문 매칭은 어긋난다 (lib/inlineImage.ts 주석 참조).
      const key = uniqueKeyFromSrc(result.previewUrl);
      if (key) previewToPlaceholder.current?.set(key, result.placeholder);
      sessionUploadedRefids.current?.add(result.refid);
      const range = editor.getSelection(true);
      const index = range ? range.index : editor.getLength();
      editor.insertEmbed(index, 'image', result.previewUrl, 'user');
      editor.setSelection(index + 1, 0);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '이미지 업로드에 실패했습니다');
    } finally {
      hide();
    }
  });

  // Quill toolbar 이미지 버튼 핸들러 — 파일 선택 다이얼로그.
  const imageHandler = () => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/*');
    input.onchange = async () => {
      const file = input.files?.[0];
      if (file) await uploadAndInsert.current(file);
    };
    input.click();
  };

  useEffect(() => {
    const editor = quillRef.current?.getEditor();
    if (!editor) return;
    const Delta = Quill.import('delta') as new () => unknown;
    const root = editor.root;
    const insert = uploadAndInsert.current;

    // base64 data URI → File 변환 (붙여넣기 HTML 안의 인라인 이미지 정규화용).
    const dataUriToFile = (dataUri: string): File | null => {
      const match = /^data:(image\/[a-zA-Z0-9.+-]+);base64,(.*)$/is.exec(dataUri);
      if (!match) return null;
      const [, mime, b64] = match;
      try {
        const bin = atob(b64);
        const bytes = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i += 1) bytes[i] = bin.charCodeAt(i);
        const ext = (mime.split('/')[1] ?? 'png').replace('jpeg', 'jpg');
        return new File([bytes], `pasted.${ext}`, { type: mime });
      } catch {
        return null;
      }
    };

    /**
     * Windows 에서 Word/한글 문서의 이미지를 붙여넣으면 클립보드 HTML 에 `file:///...clip_image001.png`
     * 만 담기고 `clipboardData.files` 는 비어 있는 경우가 많다 (브라우저가 로컬 경로를 읽지 못해 에디터에서
     * 바로 깨진다). 이때 비동기 Clipboard API 로 같은 클립보드의 **비트맵**을 다시 읽어 정상 업로드 경로로
     * 돌린다. 권한 거부/미지원/비트맵 부재면 빈 배열을 반환해 안내 메시지로 폴백한다.
     */
    const readClipboardImageFiles = async (): Promise<File[]> => {
      const clipboard = navigator.clipboard as { read?: () => Promise<ClipboardItem[]> } | undefined;
      if (!clipboard?.read) return [];
      try {
        const items = await clipboard.read();
        const files: File[] = [];
        for (const item of items) {
          const type = item.types.find((t) => t.startsWith('image/'));
          if (!type) continue;
          const blob = await item.getType(type);
          const ext = (type.split('/')[1] ?? 'png').replace('jpeg', 'jpg');
          files.push(new File([blob], `pasted.${ext}`, { type }));
        }
        return files;
      } catch {
        // 권한 거부(NotAllowedError) 등 — 안내 폴백.
        return [];
      }
    };

    // 걸러낸 로컬 이미지가 있으면 붙여넣기 1회당 한 번만 복구를 시도한다
    // (matcher 는 이미지 노드마다 호출되므로 macrotask 로 모아서 실행).
    const scheduleLocalImageRecovery = () => {
      if (localImageRecoveryScheduled.current) return;
      localImageRecoveryScheduled.current = true;
      setTimeout(() => {
        localImageRecoveryScheduled.current = false;
        const dropped = droppedLocalImages.current;
        droppedLocalImages.current = 0;
        if (dropped === 0) return;
        void (async () => {
          const files = await readClipboardImageFiles();
          if (files.length > 0) {
            for (const file of files) await insert(file);
            return;
          }
          message.warning(
            `문서에서 복사한 이미지 ${dropped}건은 그대로 붙여넣을 수 없습니다. ` +
              '이미지 파일을 직접 첨부하거나 화면 캡처(Windows: Win+Shift+S) 후 붙여넣어 주세요.',
          );
        })();
      }, 0);
    };

    // 붙여넣기 파일 케이스 — clipboardData.files 로 잡히는 이미지. Quill root 에 capture 단계로 걸어
    // Quill 기본 붙여넣기(base64 <img> 삽입 + 아래 matcher 발동)를 선제 차단하고 정상 업로드 경로 1회만 태운다.
    // (스크린샷 붙여넣기는 clipboard 에 files 와 <img src="data:..."> 가 동시에 담기므로, 둘을 상호 배제하지
    //  않으면 같은 이미지가 본문에 2번 삽입된다.)
    const onPaste = (e: ClipboardEvent) => {
      const files = Array.from(e.clipboardData?.files ?? []).filter((f) => f.type.startsWith('image/'));
      if (files.length === 0) return;
      e.preventDefault();
      e.stopPropagation();
      void (async () => {
        for (const file of files) await insert(file);
      })();
    };
    root.addEventListener('paste', onPaste, { capture: true });

    // 붙여넣기 base64 HTML 케이스 — clipboardData.files 로 잡히지 않고 HTML(<img src="data:...">) 형태로만
    // 들어오는 경우. Quill 기본 동작은 base64 를 그대로 본문에 삽입한다 → (1) 본문 비대화 (2) 모바일에서 http
    // 아닌 src 렌더 불가. clipboard matcher 로 data URI IMG 노드만 delta 에서 제거하고 비동기 업로드→presigned
    // 삽입으로 대체한다 (텍스트 등 나머지 붙여넣기 내용은 보존). 백엔드도 저장 시점에 동일 정규화(이중 방어).
    // react-quill-new 의 clipboard 타입에 addMatcher 가 노출되지 않아 최소 인터페이스로 좁혀 접근한다.
    const clipboard = (editor as unknown as {
      clipboard: { addMatcher: (selector: string, fn: (node: Node, delta: unknown) => unknown) => void };
    }).clipboard;
    clipboard.addMatcher('IMG', (node, delta) => {
      const src = node instanceof HTMLElement ? (node.getAttribute('src') ?? '') : '';
      if (src.startsWith('data:image/')) {
        const file = dataUriToFile(src);
        if (file) void insert(file);
        return new Delta();
      }
      // 로컬 전용 참조(file:/blob:/cid: — Windows 워드·한글 붙여넣기)는 본문에 넣지 않는다. 넣어 봐야
      // 에디터에서도 깨지고 저장도 막힌다. 대신 같은 클립보드의 비트맵을 비동기로 다시 읽어 업로드를 시도하고,
      // 실패하면 안내한다.
      if (src && isUnrecoverableImageSrc(src)) {
        droppedLocalImages.current += 1;
        scheduleLocalImageRecovery();
        return new Delta();
      }
      return delta;
    });
    // addMatcher 는 누적 등록되므로 에디터 mount 시 1회만 등록한다.

    // 드래그앤드롭 이미지 삽입 — Quill root(.ql-editor) 에 capture 단계로 리스너를 직접 걸어
    // Quill 기본 drop 삽입(base64 <img>)을 선제 차단하고 정상 업로드 경로 1회만 태운다.
    const onDrop = (e: DragEvent) => {
      const files = Array.from(e.dataTransfer?.files ?? []).filter((f) => f.type.startsWith('image/'));
      if (files.length === 0) return;
      e.preventDefault();
      e.stopPropagation();
      // 여러 파일 drop 시 붙여넣기와 동일하게 순차 업로드해 삽입 순서/커서를 안정화한다
      // (동시 발사하면 삽입 위치가 업로드 응답 도착 순서에 좌우됨).
      void (async () => {
        for (const file of files) await insert(file);
      })();
    };
    const onDragOver = (e: DragEvent) => e.preventDefault();
    root.addEventListener('drop', onDrop, { capture: true });
    root.addEventListener('dragover', onDragOver, { capture: true });
    return () => {
      root.removeEventListener('paste', onPaste, { capture: true } as EventListenerOptions);
      root.removeEventListener('drop', onDrop, { capture: true } as EventListenerOptions);
      root.removeEventListener('dragover', onDragOver, { capture: true } as EventListenerOptions);
    };
    // clipboard matcher(addMatcher) 와 리스너는 에디터 mount 시 1회만 등록해야 한다
    // (재실행 시 matcher 누적 / 리스너 중복 → 붙여넣기 1회에 이미지가 N개 삽입된다).
    // 정체성 안정화가 로직상 필수라 deps 를 비운다 — 변하는 값은 위 uploadRef 로 최신화한다.
  }, []);

  const modules = useMemo(
    () => ({
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ['bold', 'italic', 'underline', 'strike'],
          // 폰트 색 / 배경색 — Quill 기본이 인라인 style(color/background-color) 로 출력되어
          // 웹 상세(DOMPurify) 와 모바일 HtmlWidget 양쪽에서 렌더된다.
          [{ color: [] }, { background: [] }],
          [{ list: 'ordered' }, { list: 'bullet' }],
          [{ align: [] }],
          ['link', 'image'],
          ['clean'],
        ],
        handlers: { image: imageHandler },
      },
    }),
    // imageHandler 는 ref 만 참조하므로 재생성 불필요.
    [],
  );

  return (
    <div className="rich-content-editor">
      <ReactQuill
        ref={quillRef}
        theme="snow"
        modules={modules}
        value={value ?? ''}
        onChange={(html) => onChange?.(html)}
      />
    </div>
  );
}
