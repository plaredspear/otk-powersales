import { useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Form, Input, Row, Select, Space, Spin, Upload, message } from 'antd';
import type { UploadFile } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import { useEducationDetail } from '@/hooks/education/useEducationDetail';
import { useEducationCategories } from '@/hooks/education/useEducationCategories';
import { useCreateEducation, useUpdateEducation } from '@/hooks/education/useEducationMutation';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const MAX_FILES = 20;
const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

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
  content: string;
}

export default function EducationFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [form] = Form.useForm<FormValues>();
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { data: categories, isLoading: catLoading } = useEducationCategories();
  const { data: education, isLoading: detailLoading } = useEducationDetail(isEdit ? id! : '');
  const createMutation = useCreateEducation();
  const updateMutation = useUpdateEducation();

  useEffect(() => {
    if (isEdit) {
      setDynamicTitle(education?.title ?? null);
    }
    return () => setDynamicTitle(null);
  }, [isEdit, education?.title, setDynamicTitle]);

  useEffect(() => {
    if (isEdit && education) {
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
    const formData = new FormData();
    formData.append('title', values.title);
    formData.append('content', values.content);
    formData.append('category', values.category);

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
    } catch {
      message.error(isEdit ? '교육 자료 수정에 실패했습니다' : '교육 자료 등록에 실패했습니다');
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
    <div style={{ padding: 16, maxWidth: 1200 }}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
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
              rules={[{ required: true, message: '내용을 입력해주세요' }]}
            >
              <ReactQuill theme="snow" modules={QUILL_MODULES} style={{ minHeight: 200 }} />
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
    </div>
  );
}
