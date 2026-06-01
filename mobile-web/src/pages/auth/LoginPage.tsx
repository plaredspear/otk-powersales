import { useState } from 'react';
import { Button, Form, Input, Typography, App as AntdApp } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

/**
 * 로그인 (Wave 2 #17 의 최소 구현 — 앱 구동/인증 가드용).
 * 사번(8자리) + 비밀번호. 단말 UUID/자동로그인은 디바이스 쉘 단계(Wave 3)에서 보강.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const login = useAuthStore((s) => s.login);
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { employeeCode: string; password: string }) => {
    setLoading(true);
    try {
      await login(values.employeeCode.trim(), values.password);
      // 강제 비밀번호 변경 필요 시 변경 화면으로(레거시 resetPwd 흐름). AuthGuard 도 동일 가드.
      // GPS 동의(requiresGpsConsent) 가드는 Wave 3(디바이스) 범위.
      const dest = useAuthStore.getState().passwordChangeRequired ? '/password/change' : '/';
      navigate(dest, { replace: true });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '로그인에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '64px 24px', minHeight: '100vh' }}>
      <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
        오토기 현장
      </Typography.Title>
      <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 32 }}>
        사번과 비밀번호로 로그인하세요
      </Typography.Paragraph>
      <Form layout="vertical" onFinish={onFinish} requiredMark={false} size="large">
        <Form.Item
          name="employeeCode"
          rules={[{ required: true, message: '사번을 입력하세요' }]}
        >
          <Input prefix={<UserOutlined />} placeholder="사번 (8자리)" inputMode="numeric" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '비밀번호를 입력하세요' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" />
        </Form.Item>
        <Form.Item style={{ marginTop: 12 }}>
          <Button type="primary" htmlType="submit" block loading={loading}>
            로그인
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
}
