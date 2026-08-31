import { useContext, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Form, Input, Row, Select, Space, Spin, message } from 'antd';
import type { FormInstance } from 'antd';
import { useNoticeDetail } from '@/hooks/notice/useNoticeDetail';
import { useNoticeFormMeta } from '@/hooks/notice/useNoticeFormMeta';
import { useCreateNotice, useUpdateNotice } from '@/hooks/notice/useNoticeMutation';
import { useAuth } from '@/hooks/useAuth';
import { uploadNoticeInlineImage } from '@/api/notice';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import BranchSingleSelect, { type BranchOption } from '@/components/common/BranchSingleSelect';
import RichContentEditor from '@/components/editor/RichContentEditor';
import MobileContentPreview from '@/components/editor/MobileContentPreview';
import {
  collectPlaceholderMappings,
  findUnrecoverableImageSrcs,
  replacePreviewsWithPlaceholders as toPlaceholders,
} from '@/lib/inlineImage';

// 공개범위는 '현장여사원'으로 고정한다(레거시 영업사원 공지 미사용). 폼에서 선택 UI 를 노출하지 않고
// 저장 시 항상 이 값을 전송한다. 백엔드 scope 는 @NotBlank 필수라 값 자체는 계속 채워 보내야 한다.
const FIXED_SCOPE = '현장여사원';

interface FormValues {
  title: string;
  category: string;
  content: string;
  /** 지점공지(BRANCH) 선택 지점코드. 그 외 카테고리에서는 미사용. */
  branchCode?: string;
}

/**
 * antd Form.Item 이 주입하는 value/onChange 를 BranchSingleSelect 에 연결하는 controlled 어댑터.
 * (Form.Item 은 첫 자식에 value/onChange 를 주입하므로 별도 컴포넌트로 감싸 전달한다.)
 */
function BranchSelectField({
  branches,
  value,
  onChange,
  isLoading,
}: {
  branches: BranchOption[];
  value?: string;
  onChange?: (branchCode: string | undefined) => void;
  isLoading?: boolean;
}) {
  return (
    <BranchSingleSelect
      branches={branches}
      value={value}
      onChange={(code) => onChange?.(code)}
      label="지점"
      isLoading={isLoading}
    />
  );
}

function BranchField({
  form,
  branches,
  isLoading,
}: {
  form: FormInstance<FormValues>;
  branches: BranchOption[];
  isLoading?: boolean;
}) {
  const categoryValue = Form.useWatch('category', form);
  if (categoryValue !== 'BRANCH') return null;

  // 지점공지의 지점은 작성자가 권한 스코프(행사마스터/여사원일정과 동일한 화이트리스트) 안에서 고른다.
  // BranchSingleSelect: 전사 권한자는 다중 지점 Select, 단일 지점(조장/지점장 등)은 자동 선택 후 Tag 고정.
  // form 의 branchCode 필드로 관리 → 저장 시 payload 에 실린다.
  return (
    <Form.Item
      name="branchCode"
      label="지점"
      rules={[{ required: true, message: '지점을 선택해주세요' }]}
      extra="지점공지는 선택한 지점으로 저장됩니다."
    >
      <BranchSelectField branches={branches} isLoading={isLoading} />
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
  // uniqueKey → placeholder 매핑을 보관했다가 submit 직전에 본문 HTML 을 치환한다.
  //
  // 키를 presigned URL **전문**이 아니라 URL 에 내재된 불변 uniqueKey 로 잡는다: Quill 이 반환하는 HTML 은
  // 속성값의 `&` 를 `&amp;` 로 이스케이프하므로, 원본 URL 문자열로 매칭하면 서명 쿼리스트링(`...&X-Amz-...`)이
  // 어긋나 치환이 통째로 실패한다. 그 결과 만료되는 presigned URL 이 DB 본문에 그대로 저장돼(공지 2393)
  // 30분 뒤부터 이미지가 영구히 깨졌다. uniqueKey 는 `?` 앞 경로라 이스케이프 영향을 받지 않는다.
  const previewToPlaceholder = useRef<Map<string, string>>(new Map());
  // 이번 편집 세션에서 업로드한 인라인 이미지 refid 누적. 저장 시 서버가 본문에서 빠진 이미지를
  // 정리(S3+soft-delete)하는 대상 판별에 넘긴다. (삽입 후 삭제한 이미지의 고아 파일 방지)
  const sessionUploadedRefids = useRef<Set<string>>(new Set());

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

  // 지점공지 지점 옵션 — 백엔드 form-meta 가 WomenScheduleBranchResolver 권한별 화이트리스트로 내려준다.
  const branchOptions: BranchOption[] = formMeta?.branches ?? [];

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
        // 수정 진입 시 기존 저장 지점코드를 초기값으로 — BranchSingleSelect 가 옵션에 있으면 선택 상태로 표시.
        branchCode: notice.branchCode ?? undefined,
      });
    }
  }, [isEdit, notice, form]);


  // 수정 화면 진입 시, 서버 상세조회 본문의 기존 이미지(<img src="presigned" data-refid="{refid}">)에서
  // uniqueKey → placeholder 매핑을 미리 등록한다. Quill 이 로드하며 data-refid 를 버리기 전에 원본 HTML 에서
  // 추출해야 하므로 setFieldsValue 직전에 호출한다. 저장 직전 replacePreviewsWithPlaceholders 가 이 매핑으로
  // presigned URL 을 placeholder 로 되돌려, 만료 URL 이 DB 본문에 영구 저장되는 것을 막는다.
  // (변환 규칙과 회귀 케이스는 lib/inlineImage.ts / .test.ts 가 소유.)
  const registerExistingImagePlaceholders = (html: string | null | undefined) => {
    for (const [key, placeholder] of collectPlaceholderMappings(html)) {
      previewToPlaceholder.current.set(key, placeholder);
    }
  };

  // 저장 직전 본문의 presigned previewUrl 을 placeholder 로 치환 (만료 URL 영구 저장 방지).
  const replacePreviewsWithPlaceholders = (html: string): string =>
    toPlaceholders(html, previewToPlaceholder.current);

  // 저장/발행 버튼 공통 제출. publish=false 임시저장(DRAFT), true 발행(PUBLISHED).
  // antd onFinish 는 인자를 넘길 수 없어, 버튼 onClick 에서 validateFields 후 직접 호출한다.
  const submit = async (publish: boolean) => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return; // 검증 실패 시 antd 가 필드 에러 표시
    }

    // 저장해도 살릴 수 없는 이미지 참조(file:/blob: — 한글·워드 붙여넣기의 로컬 경로 등)는 미리 차단한다.
    // 외부 http(s) 이미지는 서버가 저장 시 S3 로 이관하므로 여기서 막지 않는다.
    const unrecoverable = findUnrecoverableImageSrcs(values.content);
    if (unrecoverable.length > 0) {
      message.error(
        `본문에 저장할 수 없는 이미지 ${unrecoverable.length}건이 있습니다. ` +
          '이미지는 툴바의 이미지 버튼이나 드래그앤드롭으로 다시 넣어주세요.',
      );
      return;
    }

    // 지점공지(BRANCH)면 작성자가 고른 지점코드를 전송한다(백엔드가 권한 스코프 검증). 그 외 카테고리는
    // 지점 무관이라 미전송(null). 백엔드가 branch 명은 화이트리스트에서 코드로 매칭해 저장한다.
    const isBranch = values.category === 'BRANCH';
    const payload = {
      title: values.title,
      scope: FIXED_SCOPE,
      category: values.category,
      content: replacePreviewsWithPlaceholders(values.content),
      branch: null,
      branchCode: isBranch ? (values.branchCode ?? null) : null,
      // 낙관적 락 — 수정 화면 진입 시 받은 version 을 되돌려보내 동시 편집 충돌(409)을 감지시킨다.
      // (신규 등록은 notice 가 없으므로 undefined.)
      version: isEdit ? notice?.version : undefined,
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
    } catch (e) {
      // 동시 편집 충돌(409) 등 서버가 내려준 구체적 안내 메시지를 그대로 노출한다.
      const fallback = isEdit ? '공지사항 저장에 실패했습니다' : '공지사항 등록에 실패했습니다';
      message.error(e instanceof Error && e.message ? e.message : fallback);
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
            <BranchField form={form} branches={branchOptions} isLoading={metaLoading} />
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
              <RichContentEditor
                uploadInlineImage={uploadNoticeInlineImage}
                previewToPlaceholder={previewToPlaceholder}
                sessionUploadedRefids={sessionUploadedRefids}
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
        <MobileContentPreview
          title={watchedTitle}
          categoryName={watchedCategoryName}
          badgeVariant={watchedCategory === 'COMPANY' ? 'company' : 'other'}
          content={watchedContent}
        />
      </div>
    </div>
  );
}
