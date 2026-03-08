import { useContext, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Form, Input, Select, Space, Spin, Typography, message } from 'antd';
import type { FormInstance } from 'antd';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import { useNoticeDetail } from '@/hooks/notice/useNoticeDetail';
import { useNoticeFormMeta } from '@/hooks/notice/useNoticeFormMeta';
import { useCreateNotice, useUpdateNotice } from '@/hooks/notice/useNoticeMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const { Title } = Typography;

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
  category: string;
  branch?: { value: string; label: string };
  content: string;
}

function BranchField({
  form,
  branches,
}: {
  form: FormInstance<FormValues>;
  branches: Array<{ branchCode: string; branchName: string }>;
}) {
  const categoryValue = Form.useWatch('category', form);
  if (categoryValue !== 'BRANCH') return null;

  return (
    <Form.Item
      name="branch"
      label="지점"
      rules={[{ required: true, message: '지점을 선택해주세요' }]}
    >
      <Select
        showSearch
        labelInValue
        placeholder="지점 선택"
        optionFilterProp="label"
        options={branches.map((b) => ({
          value: b.branchCode,
          label: b.branchName,
        }))}
      />
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
  const { data: formMeta, isLoading: metaLoading } = useNoticeFormMeta();
  const { data: notice, isLoading: detailLoading } = useNoticeDetail(isEdit ? noticeId : 0);
  const createMutation = useCreateNotice();
  const updateMutation = useUpdateNotice();

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
        category: notice.category,
        content: notice.content,
        branch: notice.branchCode
          ? { value: notice.branchCode, label: notice.branch ?? '' }
          : undefined,
      });
    }
  }, [isEdit, notice, form]);

  const handleSubmit = async (values: FormValues) => {
    const payload = {
      title: values.title,
      category: values.category,
      content: values.content,
      branch: values.category === 'BRANCH' && values.branch ? values.branch.label : null,
      branch_code: values.category === 'BRANCH' && values.branch ? values.branch.value : null,
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
    <div style={{ padding: 24, maxWidth: 800 }}>
      <Title level={4}>{isEdit ? '공지사항 수정' : '공지사항 작성'}</Title>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ category: 'COMPANY' }}
      >
        <Form.Item
          name="category"
          label="카테고리"
          rules={[{ required: true, message: '카테고리를 선택해주세요' }]}
        >
          <Select
            options={formMeta?.categories.map((c) => ({ value: c.code, label: c.name }))}
            onChange={() => form.setFieldValue('branch', undefined)}
          />
        </Form.Item>

        <BranchField form={form} branches={formMeta?.branches ?? []} />

        <Form.Item
          name="title"
          label="제목"
          rules={[{ required: true, message: '제목을 입력해주세요' }]}
        >
          <Input maxLength={200} />
        </Form.Item>

        <Form.Item
          name="content"
          label="내용"
          rules={[{ required: true, message: '내용을 입력해주세요' }]}
        >
          <ReactQuill theme="snow" modules={QUILL_MODULES} style={{ minHeight: 200 }} />
        </Form.Item>

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
