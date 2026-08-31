import { useContext, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Form, Input, Row, Select, Space, Spin, Upload, message } from 'antd';
import type { UploadFile } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { useEducationDetail } from '@/hooks/education/useEducationDetail';
import { useEducationCategories } from '@/hooks/education/useEducationCategories';
import { useCreateEducation, useUpdateEducation } from '@/hooks/education/useEducationMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import { isApiErrorBody } from '@/api/types';
import { uploadEducationInlineImage } from '@/api/education';
import RichContentEditor from '@/components/editor/RichContentEditor';
import MobileContentPreview from '@/components/editor/MobileContentPreview';
import {
  collectPlaceholderMappings,
  findUnrecoverableImageSrcs,
  replacePreviewsWithPlaceholders as toPlaceholders,
} from '@/lib/inlineImage';

const MAX_FILES = 20;
const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

/**
 * 등록/수정 실패 시 서버가 준 사유를 그대로 노출한다. 고정 문구만 띄우면 파라미터 누락·파일 용량 초과·
 * 스토리지 오류가 한 문장으로 뭉개져 원인 파악이 불가능해진다.
 *
 * api 레이어(`createEducation`/`updateEducation`)가 `Error` 로 감싸 던지므로 axios 응답 본문과
 * `Error.message` 두 경로를 모두 확인한다.
 */
function extractErrorMessage(err: unknown, fallback: string): string {
  const body = (err as { response?: { data?: unknown } })?.response?.data;
  if (isApiErrorBody(body) && body.error?.message) return body.error.message;
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}

interface FormValues {
  title: string;
  category: string;
  content: string;
}

export default function EducationFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [form] = Form.useForm<FormValues>();
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  // 에디터에는 만료되는 presigned previewUrl 을 보여주되, 저장 본문에는 placeholder 가 들어가야 한다.
  // 키를 presigned URL 전문이 아니라 URL 에 내재된 불변 uniqueKey 로 잡는 이유는 lib/inlineImage.ts 참조
  // (Quill 이 속성값의 `&` 를 `&amp;` 로 이스케이프해 URL 전문 매칭이 어긋난다).
  const previewToPlaceholder = useRef<Map<string, string>>(new Map());
  // 이번 편집 세션에서 업로드한 인라인 이미지 refid 누적. 저장 시 서버가 본문에서 빠진 이미지를
  // 정리(S3+soft-delete)하는 대상 판별에 넘긴다. (삽입 후 삭제한 이미지의 고아 파일 방지)
  const sessionUploadedRefids = useRef<Set<string>>(new Set());

  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { data: categories, isLoading: catLoading } = useEducationCategories();
  const { data: education, isLoading: detailLoading } = useEducationDetail(isEdit ? id! : '');
  const createMutation = useCreateEducation();
  const updateMutation = useUpdateEducation();

  // 모바일 미리보기용 실시간 폼 값 watch (제목/카테고리/본문).
  const watchedTitle = Form.useWatch('title', form) ?? '';
  const watchedCategory = Form.useWatch('category', form);
  const watchedContent = Form.useWatch('content', form) ?? '';
  const watchedCategoryName =
    categories?.find((c) => c.eduCode === watchedCategory)?.eduCodeNm ?? '';

  useEffect(() => {
    if (isEdit) {
      setDynamicTitle(education?.title ?? null);
    }
    return () => setDynamicTitle(null);
  }, [isEdit, education?.title, setDynamicTitle]);

  useEffect(() => {
    if (isEdit && education) {
      // 상세조회 본문은 백엔드가 placeholder 를 presigned URL 로 rewrite 하되 data-refid 는 보존한 상태다.
      // Quill 은 로드 시 img 의 src 만 인식하고 data-refid 를 버리므로, 저장 시 presigned URL 을 다시
      // placeholder 로 되돌릴 매핑을 에디터 로드 **전에** 원본 HTML 에서 확보해 둔다.
      for (const [key, placeholder] of collectPlaceholderMappings(education.content)) {
        previewToPlaceholder.current.set(key, placeholder);
      }
      form.setFieldsValue({
        title: education.title,
        category: education.category,
        content: education.content,
      });
      setFileList(
        education.attachments.map((att) => ({
          uid: att.id,
          name: att.fileName,
          status: 'done' as const,
          url: att.fileUrl,
        })),
      );
    }
  }, [isEdit, education, form]);

  const handleSubmit = async (values: FormValues) => {
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

    const formData = new FormData();
    formData.append('title', values.title);
    // 저장 본문에는 만료되는 presigned URL 이 아니라 placeholder 가 들어가야 한다.
    formData.append('content', toPlaceholders(values.content, previewToPlaceholder.current));
    formData.append('category', values.category);
    // 이번 세션 업로드분 중 최종 본문에서 빠진 이미지를 서버가 정리하도록 전달.
    sessionUploadedRefids.current.forEach((refid) =>
      formData.append('sessionUploadedRefids', refid),
    );

    if (isEdit) {
      // keep_file_keys: existing files that are kept
      fileList
        .filter((f) => f.status === 'done')
        .forEach((f) => formData.append('keepFileKeys', f.uid));
      // new files
      fileList
        .filter((f) => f.status !== 'done' && f.originFileObj)
        .forEach((f) => formData.append('files', f.originFileObj as File));
    } else {
      fileList
        .filter((f) => f.originFileObj)
        .forEach((f) => formData.append('files', f.originFileObj as File));
    }

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: id!, formData });
        message.success('교육 자료가 수정되었습니다');
        navigate(`/education/${id}`);
      } else {
        await createMutation.mutateAsync(formData);
        message.success('교육 자료가 등록되었습니다');
        navigate('/education');
      }
    } catch (err) {
      const fallback = isEdit ? '교육 자료 수정에 실패했습니다' : '교육 자료 등록에 실패했습니다';
      message.error(extractErrorMessage(err, fallback));
    }
  };

  const handleBeforeUpload = (file: File) => {
    if (fileList.length >= MAX_FILES) {
      message.error('첨부파일은 최대 20개까지 가능합니다');
      return Upload.LIST_IGNORE;
    }
    if (file.size > MAX_FILE_SIZE) {
      message.error('파일 크기는 50MB 이하만 가능합니다');
      return Upload.LIST_IGNORE;
    }
    return false;
  };

  if (catLoading || (isEdit && detailLoading)) {
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
        onFinish={handleSubmit}
        style={{ flex: 1, minWidth: 0, maxWidth: 820 }}
      >
        <Row gutter={24}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="category"
              label="카테고리"
              rules={[{ required: true, message: '카테고리를 선택해주세요' }]}
            >
              <Select
                placeholder="카테고리 선택"
                options={categories?.map((c) => ({ value: c.eduCode, label: c.eduCodeNm }))}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item
              name="title"
              label="제목"
              rules={[{ required: true, message: '제목을 입력해주세요' }]}
            >
              <Input maxLength={150} />
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
                uploadInlineImage={uploadEducationInlineImage}
                previewToPlaceholder={previewToPlaceholder}
                sessionUploadedRefids={sessionUploadedRefids}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={24}>
            <Form.Item label="첨부파일">
              <Upload
                multiple
                maxCount={MAX_FILES}
                fileList={fileList}
                beforeUpload={handleBeforeUpload}
                onChange={({ fileList: newList }) => setFileList(newList)}
                onRemove={(file) => {
                  setFileList((prev) => prev.filter((f) => f.uid !== file.uid));
                }}
              >
                <Button icon={<UploadOutlined />}>파일 선택</Button>
              </Upload>
              <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>
                * 최대 20개, 개별 50MB 이하
              </div>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item style={{ marginTop: 24 }}>
          <Space>
            <Button onClick={() => navigate(isEdit ? `/education/${id}` : '/education')}>
              취소
            </Button>
            <Button type="primary" htmlType="submit" loading={isSubmitting}>
              저장
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <div style={{ position: 'sticky', top: 16, flexShrink: 0 }}>
        <MobileContentPreview
          title={watchedTitle}
          categoryName={watchedCategoryName}
          badgeVariant="education"
          content={watchedContent}
        />
      </div>
    </div>
  );
}
