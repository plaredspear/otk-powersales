import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Alert, Checkbox } from 'antd';
import { useAuthStore } from '@/stores/authStore';

const REMEMBER_USERNAME_KEY = 'login.rememberedUsername';

interface LoginForm {
  username: string;
  password: string;
  remember: boolean;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const { isAuthenticated, login } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const rememberedUsername = localStorage.getItem(REMEMBER_USERNAME_KEY) ?? '';

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (values: LoginForm) => {
    setLoading(true);
    setError(null);
    try {
      await login(values.username, values.password);
      if (values.remember) {
        localStorage.setItem(REMEMBER_USERNAME_KEY, values.username);
      } else {
        localStorage.removeItem(REMEMBER_USERNAME_KEY);
      }
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
        {/*
          제목 텍스트 대신 사이더와 동일한 브랜드 이미지(오뚜기 심볼 + O'mate 워드마크).
          원본이 939x207 이라 높이만 고정하면 가로는 비율대로 약 200px — 카드(400px) 안에 여유롭게 들어간다.

          상하 여백 76px 은 로고 좌우 여백과 맞춘 값이다:
          카드 본문 폭 352px(400 - Card padding 24×2) 에서 로고 폭 약 200px 을 빼면 좌우 각 76px.
        */}
        <div style={{ display: 'flex', justifyContent: 'center', margin: '76px 0' }}>
          <img
            src="/brand-omate.png"
            alt="오뚜기 O'mate"
            style={{ height: 44, width: 'auto', maxWidth: '100%', objectFit: 'contain' }}
          />
        </div>

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

        <Form<LoginForm>
          onFinish={handleSubmit}
          layout="vertical"
          autoComplete="off"
          initialValues={{
            username: rememberedUsername,
            remember: rememberedUsername !== '',
          }}
        >
          <Form.Item
            label="아이디"
            name="username"
            rules={[{ required: true, message: '아이디를 입력하세요' }]}
          >
            <Input placeholder="아이디(이메일)를 입력하세요" size="large" />
          </Form.Item>

          <Form.Item
            label="비밀번호"
            name="password"
            rules={[{ required: true, message: '비밀번호를 입력하세요' }]}
          >
            <Input.Password placeholder="비밀번호를 입력하세요" size="large" />
          </Form.Item>

          <Form.Item name="remember" valuePropName="checked">
            <Checkbox>아이디 저장</Checkbox>
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
