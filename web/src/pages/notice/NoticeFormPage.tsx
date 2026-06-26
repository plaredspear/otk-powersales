import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Form, Input, Row, Select, Space, Spin, message } from 'antd';
import type { FormInstance } from 'antd';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import { useNoticeDetail } from '@/hooks/notice/useNoticeDetail';
import { useNoticeFormMeta } from '@/hooks/notice/useNoticeFormMeta';
import { useCreateNotice, useUpdateNotice } from '@/hooks/notice/useNoticeMutation';
import { useAuth } from '@/hooks/useAuth';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const QUILL_MODULES = {
  toolbar: [
    ['bold', 'italic', 'underline'],
    [{ header: 1 }, { header: 2 }],
    [{ list: 'ordered' }, { list: 'bullet' }],
    ['link'],
  ],
};

interface FormValues {
  title: string;
  scope: string;
  category: string;
  content: string;
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

  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { user } = useAuth();
  const { data: formMeta, isLoading: metaLoading } = useNoticeFormMeta();

  const { data: notice, isLoading: detailLoading } = useNoticeDetail(isEdit ? noticeId : 0);
  const createMutation = useCreateNotice();
  const updateMutation = useUpdateNotice();

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
      form.setFieldsValue({
        title: notice.title,
        scope: notice.scope ?? undefined,
        category: notice.category,
        content: notice.content,
      });
    }
  }, [isEdit, notice, form]);

  const handleSubmit = async (values: FormValues) => {
    // 지점공지의 지점/지점코드는 백엔드가 공지 소유자(등록자) 소속 지점을 권위로 강제하므로 전송하지 않는다.
    const payload = {
      title: values.title,
      scope: values.scope,
      category: values.category,
      content: values.content,
      branch: null,
      branchCode: null,
    };

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: noticeId, data: payload });
        message.success('공지사항이 수정되었습니다');
        navigate(`/notices/${noticeId}`);
      } else {
        await createMutation.mutateAsync(payload);
        message.success('공지사항이 등록되었습니다');
        navigate('/notices');
      }
    } catch {
      message.error(isEdit ? '공지사항 수정에 실패했습니다' : '공지사항 등록에 실패했습니다');
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
    <div style={{ padding: 16, maxWidth: 1200 }}>
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ scope: '현장여사원', category: 'COMPANY' }}
      >
        <Row gutter={24}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="scope"
              label="공개범위"
              rules={[{ required: true, message: '공개범위를 선택해주세요' }]}
            >
              <Select
                options={formMeta?.scopes.map((s) => ({ value: s.code, label: s.name }))}
              />
            </Form.Item>
          </Col>
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
              rules={[{ required: true, message: '내용을 입력해주세요' }]}
            >
              <ReactQuill theme="snow" modules={QUILL_MODULES} style={{ minHeight: 200 }} />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item style={{ marginTop: 24 }}>
          <Space>
            <Button onClick={() => navigate(isEdit ? `/notices/${noticeId}` : '/notices')}>
              취소
            </Button>
            <Button type="primary" htmlType="submit" loading={isSubmitting}>
              저장
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  );
}
