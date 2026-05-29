import { App, Button, Card, Form, Input, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useCreatePermissionSet } from '@/hooks/admin/usePermissionSetMutation';
import type { AxiosError } from 'axios';

const { Title } = Typography;

interface FormValues {
  name: string;
  label?: string;
  description?: string;
}

/**
 * Spec #837 — PermissionSet 신규 등록 페이지.
 *
 * name 은 영문/숫자/언더스코어 80자 이내 — backend `INVALID_NAME` 응답 회피 위해 client side 1차 검증.
 * 생성 성공 시 편집 페이지로 redirect — 권한 비트 편집 진입 권유.
 */
export default function PermissionSetCreatePage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [form] = Form.useForm<FormValues>();
  const create = useCreatePermissionSet();

  const handleSubmit = async (values: FormValues) => {
    try {
      const response = await create.mutateAsync({
        name: values.name,
        label: values.label ?? null,
        description: values.description ?? null,
      });
      message.success('PermissionSet 이 생성되었습니다');
      navigate(`/admin/permissions/permission-sets/${response.permissionSetId}/edit`);
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string; message?: string } }>;
      const code = err.response?.data?.error?.code;
      if (code === 'NAME_ALREADY_EXISTS') {
        form.setFields([{ name: 'name', errors: ['이미 존재하는 PermissionSet 이름입니다'] }]);
      } else {
        message.error(err.response?.data?.error?.message || 'PermissionSet 생성에 실패했습니다');
      }
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate('/admin/permissions/permission-sets')}>← 목록으로</Button>
      </Space>

      <Title level={4}>신규 PermissionSet 등록</Title>

      <Card style={{ maxWidth: 720 }}>
        <Form<FormValues>
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          autoComplete="off"
        >
          <Form.Item
            label="Name (영문/숫자/언더스코어, 80자 이내)"
            name="name"
            rules={[
              { required: true, message: 'Name 을 입력하세요' },
              { max: 80, message: '80자 이내로 입력하세요' },
              {
                pattern: /^[A-Za-z0-9_]+$/,
                message: '영문/숫자/언더스코어만 사용 가능합니다',
              },
            ]}
          >
            <Input placeholder="예: New_Read_Permission" />
          </Form.Item>

          <Form.Item
            label="Label (한글 라벨, 255자 이내)"
            name="label"
            rules={[{ max: 255, message: '255자 이내로 입력하세요' }]}
          >
            <Input placeholder="예: 신규 조회 권한" />
          </Form.Item>

          <Form.Item
            label="Description (1024자 이내)"
            name="description"
            rules={[{ max: 1024, message: '1024자 이내로 입력하세요' }]}
          >
            <Input.TextArea rows={3} placeholder="이 PermissionSet 의 용도 / 부여 대상" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={create.isPending}>
                생성
              </Button>
              <Button onClick={() => navigate('/admin/permissions/permission-sets')}>취소</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
