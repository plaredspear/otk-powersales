import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Alert, Typography } from 'antd';
import { useAuthStore } from '@/stores/authStore';

const { Title } = Typography;

interface LoginForm {
  employeeId: string;
  password: string;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const { isAuthenticated, login } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (values: LoginForm) => {
    setLoading(true);
    setError(null);
    try {
      await login(values.employeeId, values.password);
      navigate('/', { replace: true });
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('로그인 중 오류가 발생했습니다');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
          파워세일즈 관리시스템
        </Title>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            closable
            style={{ marginBottom: 16 }}
            onClose={() => setError(null)}
          />
        )}

        <Form<LoginForm> onFinish={handleSubmit} layout="vertical" autoComplete="off">
          <Form.Item
            label="사번"
            name="employeeId"
            rules={[{ required: true, message: '사번을 입력하세요' }]}
          >
            <Input placeholder="사번을 입력하세요" size="large" />
          </Form.Item>

          <Form.Item
            label="비밀번호"
            name="password"
            rules={[{ required: true, message: '비밀번호를 입력하세요' }]}
          >
            <Input.Password placeholder="비밀번호를 입력하세요" size="large" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              로그인
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
