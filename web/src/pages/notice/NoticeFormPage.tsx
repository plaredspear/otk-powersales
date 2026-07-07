import { useContext, useEffect, useMemo, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Form, Input, Row, Select, Space, Spin, message } from 'antd';
import type { FormInstance } from 'antd';
import ReactQuill, { Quill } from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import './NoticeContentEditor.css';
import { useNoticeDetail } from '@/hooks/notice/useNoticeDetail';
import { useNoticeFormMeta } from '@/hooks/notice/useNoticeFormMeta';
import { useCreateNotice, useUpdateNotice } from '@/hooks/notice/useNoticeMutation';
import { useAuth } from '@/hooks/useAuth';
import { uploadNoticeInlineImage } from '@/api/notice';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import MobileNoticePreview from './MobileNoticePreview';

// 본문 인라인 이미지 허용 타입/용량 (백엔드 StorageConstants 와 정합).
const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif', 'image/webp'];
const MAX_IMAGE_BYTES = 20 * 1024 * 1024; // 20MB

// 정렬(align)은 Quill 기본이 class 기반(ql-align-*)이라 CSS 가 없는 환경(모바일 HtmlWidget)에서 렌더되지 않는다.
// 인라인 style(text-align) 로 출력하도록 style attributor 를 등록해 웹/모바일 렌더를 일치시킨다.
// (color/background 는 기본이 이미 인라인 style 이라 별도 등록 불요.)
Quill.register('formats/align', Quill.import('attributors/style/align'), true);

// 공개범위는 '현장여사원'으로 고정한다(레거시 영업사원 공지 미사용). 폼에서 선택 UI 를 노출하지 않고
// 저장 시 항상 이 값을 전송한다. 백엔드 scope 는 @NotBlank 필수라 값 자체는 계속 채워 보내야 한다.
const FIXED_SCOPE = '현장여사원';

interface FormValues {
  title: string;
  category: string;
  content: string;
}

/**
 * 본문 리치텍스트 에디터 — antd Form.Item 의 controlled 필드로 동작.
 *
 * Form.Item 이 주입하는 value(현재 HTML)/onChange 를 ReactQuill 에 연결한다.
 * (직전에는 ReactQuill 을 <div> 로 감싸 Form.Item 의 value/onChange 가 에디터로
 *  전달되지 않아, 본문이 폼 값으로 수집되지 못하고 미리보기/저장에 누락되던 버그가 있었다.)
 * 붙여넣기 이미지 삽입은 wrapper div 의 onPaste 로 처리한다.
 * 드래그앤드롭은 Quill root(.ql-editor) 에 capture 단계 리스너를 직접 걸어 처리한다
 *  (wrapper 버블링으로는 Quill 기본 drop 삽입을 막지 못해 base64+presigned 2중 삽입되던 버그 회피).
 */
function ContentEditor({
  value,
  onChange,
  quillRef,
  modules,
  onPaste,
}: {
  value?: string;
  onChange?: (html: string) => void;
  quillRef: React.RefObject<ReactQuill | null>;
  modules: Record<string, unknown>;
  onPaste: (e: React.ClipboardEvent) => void;
}) {
  return (
    <div className="notice-content-editor" onPaste={onPaste}>
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

function BranchField({
  form,
  branchName,
}: {
  form: FormInstance<FormValues>;
  branchName: string | null;
}) {
  const categoryValue = Form.useWatch('category', form);
  if (categoryValue !== 'BRANCH') return null;

  // 지점공지의 지점/지점코드는 백엔드가 공지 소유자(등록자) 소속 지점을 권위로 강제 저장한다.
  // 사용자가 임의 지점을 고를 수 없도록 읽기전용으로 해당 지점만 표시한다.
  // - 신규: 등록자(로그인 사용자) 소속 지점
  // - 수정: 공지 소유자 소속 지점 (= 기존 저장된 지점)
  return (
    <Form.Item
      label="지점"
      extra="지점공지는 등록자 소속 지점으로 저장됩니다."
    >
      <Input value={branchName ?? '소속 지점 정보 없음'} disabled />
    </Form.Item>
  );
}

export default function NoticeFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;
  const noticeId = Number(id);

  const [form] = Form.useForm<FormValues>();

  // 에디터에는 만료되는 presigned previewUrl 을 보여주되, 저장 본문에는 placeholder 가 들어가야 한다.
  // previewUrl → placeholder 매핑을 보관했다가 submit 직전에 본문 HTML 을 치환한다.
  const previewToPlaceholder = useRef<Map<string, string>>(new Map());
  // 이번 편집 세션에서 업로드한 인라인 이미지 refid 누적. 저장 시 서버가 본문에서 빠진 이미지를
  // 정리(S3+soft-delete)하는 대상 판별에 넘긴다. (삽입 후 삭제한 이미지의 고아 파일 방지)
  const sessionUploadedRefids = useRef<Set<string>>(new Set());
  const quillRef = useRef<ReactQuill>(null);

  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { user } = useAuth();
  const { data: formMeta, isLoading: metaLoading } = useNoticeFormMeta();

  const { data: notice, isLoading: detailLoading } = useNoticeDetail(isEdit ? noticeId : 0);
  const createMutation = useCreateNotice();
  const updateMutation = useUpdateNotice();

  // 조장/지점장은 지점공지만 작성 가능 → 신규 작성 시 카테고리 기본값을 지점공지(BRANCH)로.
  // (카테고리 옵션 자체도 서버 form-meta 가 role 기준으로 지점공지만 내려준다.)
  const isBranchNoticeOnly = user?.role === '조장' || user?.role === '지점장';
  const defaultCategory = isBranchNoticeOnly ? 'BRANCH' : 'COMPANY';

  // 모바일 미리보기용 실시간 폼 값 watch (제목/카테고리/본문).
  const watchedTitle = Form.useWatch('title', form) ?? '';
  const watchedCategory = Form.useWatch('category', form);
  const watchedContent = Form.useWatch('content', form) ?? '';
  const watchedCategoryName =
    formMeta?.categories.find((c) => c.code === watchedCategory)?.name ?? '';

  // 지점공지 폼에 표시할 지점명.
  // - 수정: 공지에 이미 저장된 지점(소유자 지점)을 그대로 표시
  // - 신규: 등록자(로그인 사용자) 소속 지점명 (백엔드 저장값과 동일하게 form-meta 에서 코드로 매칭)
  const myBranchName =
    formMeta?.branches.find((b) => b.branchCode === user?.costCenterCode)?.branchName ??
    user?.orgName ??
    null;
  const branchFieldName = isEdit ? (notice?.branch ?? null) : myBranchName;

  useEffect(() => {
    if (isEdit) {
      setDynamicTitle(notice?.title ?? null);
    }
    return () => setDynamicTitle(null);
  }, [isEdit, notice?.title, setDynamicTitle]);

  useEffect(() => {
    if (isEdit && notice) {
      // 상세조회 본문은 백엔드가 placeholder 를 presigned URL 로 rewrite 하되 data-refid 는 보존한 상태다
      // (<img src="https://.../private/{uniqueKey}?..." data-refid="{refid}">).
      // Quill 은 로드 시 img 의 src 만 인식하고 data-refid 를 버리므로, 저장 시 presigned URL 을 다시
      // placeholder 로 되돌릴 매핑(presigned src → placeholder)을 에디터 로드 전에 미리 확보해 둔다.
      // presigned URL 은 만료되는 임시값이라 저장 본문에 남기지 않는다(저장 직전 replacePreviewsWithPlaceholders 가 복원).
      registerExistingImagePlaceholders(notice.content);
      form.setFieldsValue({
        title: notice.title,
        category: notice.category,
        content: notice.content,
      });
    }
  }, [isEdit, notice, form]);

  // 파일 1건 업로드 → 에디터 현재 커서 위치에 presigned 이미지 삽입 + placeholder 매핑 보관.
  const uploadAndInsert = async (file: File) => {
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
      const result = await uploadNoticeInlineImage(file);
      previewToPlaceholder.current.set(result.previewUrl, result.placeholder);
      sessionUploadedRefids.current.add(result.refid);
      const range = editor.getSelection(true);
      const index = range ? range.index : editor.getLength();
      editor.insertEmbed(index, 'image', result.previewUrl, 'user');
      editor.setSelection(index + 1, 0);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '이미지 업로드에 실패했습니다');
    } finally {
      hide();
    }
  };

  // Quill toolbar 이미지 버튼 핸들러 — 파일 선택 다이얼로그.
  const imageHandler = () => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/*');
    input.onchange = async () => {
      const file = input.files?.[0];
      if (file) await uploadAndInsert(file);
    };
    input.click();
  };

  // 붙여넣기 핸들러 (clipboardData.files 로 잡히는 이미지 파일 케이스).
  // HTML(<img src="data:...">) 형태 붙여넣기는 아래 useEffect 의 clipboard matcher 가 별도 처리.
  const handlePaste = async (e: React.ClipboardEvent) => {
    const files = Array.from(e.clipboardData?.files ?? []).filter((f) => f.type.startsWith('image/'));
    if (files.length === 0) return;
    e.preventDefault();
    for (const file of files) await uploadAndInsert(file);
  };

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

  // 붙여넣기 시 본문에 base64 로 박히는 인라인 이미지를 정상 업로드 경로로 돌린다.
  // clipboardData.files 로 잡히지 않고 HTML(<img src="data:...">) 형태로 들어오는 경우(스크린샷 붙여넣기 등),
  // Quill 기본 동작은 base64 를 그대로 본문에 삽입한다 → (1) 본문 비대화 (2) 모바일에서 http 아닌 src 렌더 불가.
  // Quill clipboard matcher 로 data URI IMG 노드만 delta 에서 제거하고 비동기 업로드→presigned 삽입으로 대체한다
  // (텍스트 등 나머지 붙여넣기 내용은 보존). 백엔드도 저장 시점에 동일 정규화를 수행하는 이중 방어.
  useEffect(() => {
    const editor = quillRef.current?.getEditor();
    if (!editor) return;
    const Delta = Quill.import('delta') as new () => unknown;
    // react-quill-new 의 clipboard 타입에 addMatcher 가 노출되지 않아 최소 인터페이스로 좁혀 접근한다.
    const clipboard = (editor as unknown as {
      clipboard: { addMatcher: (selector: string, fn: (node: Node, delta: unknown) => unknown) => void };
    }).clipboard;
    clipboard.addMatcher('IMG', (node, delta) => {
      const src = node instanceof HTMLElement ? (node.getAttribute('src') ?? '') : '';
      if (src.startsWith('data:image/')) {
        const file = dataUriToFile(src);
        if (file) void uploadAndInsert(file);
        return new Delta();
      }
      return delta;
    });
    // addMatcher 는 누적 등록되므로 에디터 mount 시 1회만 등록한다.

    // 드래그앤드롭 이미지 삽입 — Quill root(.ql-editor) 에 capture 단계로 리스너를 직접 걸어
    // Quill 기본 drop 삽입(base64 <img>)을 선제 차단하고 정상 업로드 경로 1회만 태운다.
    // (wrapper div 버블링 방식은 하위 contenteditable 에서 이미 벌어진 Quill 기본 삽입을 막지 못해
    //  base64 + presigned 2중 삽입되던 버그가 있었다.)
    const root = editor.root;
    const onDrop = (e: DragEvent) => {
      const files = Array.from(e.dataTransfer?.files ?? []).filter((f) => f.type.startsWith('image/'));
      if (files.length === 0) return;
      e.preventDefault();
      e.stopPropagation();
      // 여러 파일 drop 시 handlePaste 와 동일하게 순차 업로드해 삽입 순서/커서를 안정화한다
      // (동시 발사하면 삽입 위치가 업로드 응답 도착 순서에 좌우됨).
      void (async () => {
        for (const file of files) await uploadAndInsert(file);
      })();
    };
    const onDragOver = (e: DragEvent) => e.preventDefault();
    root.addEventListener('drop', onDrop, { capture: true });
    root.addEventListener('dragover', onDragOver, { capture: true });
    return () => {
      root.removeEventListener('drop', onDrop, { capture: true } as EventListenerOptions);
      root.removeEventListener('dragover', onDragOver, { capture: true } as EventListenerOptions);
    };
    // uploadAndInsert / dataUriToFile 는 ref·모듈상수·안정 import 만 참조하므로 재구독 불필요.
    // clipboard matcher(addMatcher) 와 drop 리스너는 에디터 mount 시 1회만 등록해야 한다
    // (재실행 시 matcher 누적 / 리스너 중복). 정체성 안정화가 로직상 필수라 deps 를 비운다.
  }, []);

  const quillModules = useMemo(
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  // 수정 화면 진입 시, 서버 상세조회 본문의 기존 이미지(<img src="presigned" data-refid="{refid}">)에서
  // presigned src → placeholder 매핑을 미리 등록한다. Quill 이 로드하며 data-refid 를 버리기 전에 원본 HTML 에서
  // 추출해야 하므로 setFieldsValue 직전에 호출한다. 저장 직전 replacePreviewsWithPlaceholders 가 이 매핑으로
  // presigned URL 을 placeholder 로 되돌려, 만료 URL 이 DB 본문에 영구 저장되는 것을 막는다.
  const registerExistingImagePlaceholders = (html: string | null | undefined) => {
    if (!html) return;
    // src / data-refid 속성 순서에 무관하게 태그 전체를 잡아 각 속성을 개별 파싱한다.
    for (const m of html.matchAll(/<img\b[^>]*>/gi)) {
      const tag = m[0];
      const src = /\bsrc\s*=\s*"([^"]*)"/i.exec(tag)?.[1];
      const refid = /\bdata-refid\s*=\s*"([^"]*)"/i.exec(tag)?.[1];
      if (src && refid && /^https?:/i.test(src)) {
        // 백엔드 placeholder 형식과 동일하게 재구성 (NoticeImagePlaceholder.build 정합).
        previewToPlaceholder.current.set(src, `<img src="notice-image://${refid}" data-refid="${refid}">`);
      }
    }
  };

  // 저장 직전 본문의 presigned previewUrl 을 placeholder 로 치환 (만료 URL 영구 저장 방지).
  const replacePreviewsWithPlaceholders = (html: string): string => {
    let result = html;
    for (const [previewUrl, placeholder] of previewToPlaceholder.current) {
      // Quill 이 src 에 넣은 previewUrl 을 포함한 <img ...> 태그 전체를 placeholder 로 교체.
      const escaped = previewUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const imgTagRegex = new RegExp(`<img[^>]*src="${escaped}"[^>]*>`, 'g');
      result = result.replace(imgTagRegex, placeholder);
    }
    return result;
  };

  // 저장/발행 버튼 공통 제출. publish=false 임시저장(DRAFT), true 발행(PUBLISHED).
  // antd onFinish 는 인자를 넘길 수 없어, 버튼 onClick 에서 validateFields 후 직접 호출한다.
  const submit = async (publish: boolean) => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return; // 검증 실패 시 antd 가 필드 에러 표시
    }

    // 지점공지의 지점/지점코드는 백엔드가 공지 소유자(등록자) 소속 지점을 권위로 강제하므로 전송하지 않는다.
    const payload = {
      title: values.title,
      scope: FIXED_SCOPE,
      category: values.category,
      content: replacePreviewsWithPlaceholders(values.content),
      branch: null,
      branchCode: null,
      // 이번 세션 업로드분 중 최종 본문에서 빠진 이미지를 서버가 정리하도록 전달.
      sessionUploadedRefids: Array.from(sessionUploadedRefids.current),
      publish,
    };

    const savedMsg = publish ? '발행되었습니다' : '임시저장되었습니다';
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: noticeId, data: payload });
        message.success(`공지사항이 ${savedMsg}`);
        navigate(`/notices/${noticeId}`);
      } else {
        await createMutation.mutateAsync(payload);
        message.success(`공지사항이 ${savedMsg}`);
        navigate('/notices');
      }
    } catch {
      message.error(isEdit ? '공지사항 저장에 실패했습니다' : '공지사항 등록에 실패했습니다');
    }
  };

  if (metaLoading || (isEdit && detailLoading)) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  return (
    <div
      style={{
        padding: 16,
        maxWidth: 1280,
        display: 'flex',
        gap: 24,
        alignItems: 'flex-start',
      }}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ category: defaultCategory }}
        style={{ flex: 1, minWidth: 0, maxWidth: 820 }}
      >
        {/* 공개범위(scope)는 '현장여사원'으로 고정되어 폼에 노출하지 않는다(저장 시 자동 전송). */}
        <Row gutter={24}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="category"
              label="카테고리"
              rules={[{ required: true, message: '카테고리를 선택해주세요' }]}
            >
              <Select
                options={formMeta?.categories.map((c) => ({ value: c.code, label: c.name }))}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col xs={24} sm={12} />
          <Col xs={24} sm={12}>
            <BranchField form={form} branchName={branchFieldName} />
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={24}>
            <Form.Item
              name="title"
              label="제목"
              rules={[{ required: true, message: '제목을 입력해주세요' }]}
            >
              <Input maxLength={200} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={24}>
            <Form.Item
              name="content"
              label="내용"
              extra="이미지는 툴바 버튼, 드래그앤드롭, 붙여넣기로 본문에 넣을 수 있습니다."
              rules={[{ required: true, message: '내용을 입력해주세요' }]}
            >
              <ContentEditor
                quillRef={quillRef}
                modules={quillModules}
                onPaste={handlePaste}
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item style={{ marginTop: 24 }}>
          <Space>
            <Button onClick={() => navigate(isEdit ? `/notices/${noticeId}` : '/notices')}>
              취소
            </Button>
            <Button onClick={() => submit(false)} loading={isSubmitting}>
              임시저장
            </Button>
            <Button type="primary" onClick={() => submit(true)} loading={isSubmitting}>
              발행
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <div style={{ position: 'sticky', top: 16, flexShrink: 0 }}>
        <MobileNoticePreview
          title={watchedTitle}
          categoryName={watchedCategoryName}
          isCompanyCategory={watchedCategory === 'COMPANY'}
          content={watchedContent}
        />
      </div>
    </div>
  );
}
