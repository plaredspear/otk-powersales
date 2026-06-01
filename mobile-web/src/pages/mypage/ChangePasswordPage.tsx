import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Typography, App as AntdApp } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { changePassword } from '@/api/password';
import { useAuthStore } from '@/stores/authStore';
import DetailHeader from '@/components/DetailHeader';

/**
 * 비밀번호 변경 — 강제/자발 통합 (레거시 chgPwd / resetPwd).
 * - 강제(passwordChangeRequired): 현재 비밀번호 입력 없음, 뒤로가기 차단
 * - 자발: VerifyPasswordPage 검증 통과 후 진입(state.verified). 미검증 진입 시 검증 화면으로.
 */
export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = AntdApp.useApp();
  const passwordChangeRequired = useAuthStore((s) => s.passwordChangeRequired);
  const applyTokens = useAuthStore((s) => s.applyTokens);
  const [loading, setLoading] = useState(false);

  const state = location.state as { verified?: boolean; currentPassword?: string } | null;
  const isForce = passwordChangeRequired;

  // 자발인데 검증을 안 거쳤으면 검증 화면으로
  if (!isForce && !state?.verified) {
    return <Navigate to="/password/verify" replace />;
  }

  const onFinish = async (values: { newPassword: string; confirm: string }) => {
    if (values.newPassword !== values.confirm) {
      message.error('새 비밀번호가 일치하지 않습니다');
      return;
    }
    setLoading(true);
    try {
      const result = await changePassword({
        newPassword: values.newPassword,
        currentPassword: isForce ? undefined : state?.currentPassword,
      });
      applyTokens(result.accessToken, result.refreshToken);
      message.success('비밀번호가 변경되었습니다');
      navigate('/', { replace: true });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '비밀번호 변경에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <DetailHeader title="비밀번호 변경" />
      {isForce && (
        <Alert
          type="warning"
          showIcon
          message="비밀번호 변경 필요"
          description="보안을 위해 새 비밀번호로 변경해야 계속 이용할 수 있습니다."
          style={{ marginBottom: 12 }}
        />
      )}
      <Card styles={{ body: { padding: 20 } }}>
        <Form layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            name="newPassword"
            rules={[
              { required: true, message: '새 비밀번호를 입력하세요' },
              { min: 4, message: '비밀번호는 4글자 이상이어야 합니다' },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="새 비밀번호" />
          </Form.Item>
          <Form.Item
            name="confirm"
            rules={[{ required: true, message: '새 비밀번호를 다시 입력하세요' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="새 비밀번호 확인" />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
            비밀번호 정책은 서버 기준으로 검증됩니다.
          </Typography.Paragraph>
          <Button type="primary" htmlType="submit" block loading={loading}>
            변경
          </Button>
        </Form>
      </Card>
    </>
  );
}
