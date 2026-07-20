import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Alert, Typography, notification } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { changePassword, ChangePasswordError } from '@/api/auth';
import { PASSWORD_MIN_LENGTH, hasEnoughCharacterTypes } from '@/lib/passwordPolicy';
import PasswordPolicyChecklist from '@/components/PasswordPolicyChecklist';

const { Title, Paragraph } = Typography;

interface ChangePasswordForm {
  newPassword: string;
  confirmPassword: string;
}

/**
 * 강제 비밀번호 변경 화면.
 *
 * 임시 비밀번호(운영자 리셋)로 로그인한 관리자는 이 화면에서 새 비밀번호로 변경하기 전까지
 * 다른 페이지로 이동할 수 없다 (ProtectedRoute 가드 + backend WebPasswordChangeRequiredFilter).
 * 변경 성공 시 backend 가 발급한 새 토큰(클레임 password_change_required=false)으로 교체 후
 * 대시보드로 이동한다.
 *
 * 강제 변경 전용 화면이므로 현재 비밀번호 입력은 받지 않는다 (backend 도 강제 상태에선 미검증).
 */
export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<ChangePasswordForm>();
  const newPassword = Form.useWatch('newPassword', form) ?? '';
  const passwordChangeRequired = useAuthStore((s) => s.passwordChangeRequired);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const setTokens = useAuthStore((s) => s.setTokens);
  const clearPasswordChangeRequired = useAuthStore((s) => s.clearPasswordChangeRequired);
  const logout = useAuthStore((s) => s.logout);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  // 강제 변경 상태가 아니면 이 화면에 머무를 이유가 없다.
  if (!passwordChangeRequired) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (values: ChangePasswordForm) => {
    setLoading(true);
    setError(null);
    try {
      const data = await changePassword({ newPassword: values.newPassword });
      // 새 토큰(password_change_required=false)으로 교체 후 강제 플래그 해제.
      setTokens(data.accessToken, data.refreshToken);
      clearPasswordChangeRequired();
      notification.success({
        message: '비밀번호가 변경되었습니다',
        description: '새 비밀번호로 로그인되었습니다.',
        duration: 4,
      });
      navigate('/', { replace: true });
    } catch (err) {
      // 잔존/만료 토큰으로 인한 401 — 이 화면에선 refresh 로 복구할 수 없다.
      // 로그아웃(세션 정리)으로 라우터 가드 데드락을 풀고 재로그인으로 유도한다.
      if (err instanceof ChangePasswordError && err.sessionExpired) {
        logout();
        notification.warning({
          message: '세션이 만료되었습니다',
          description: '다시 로그인해 주세요.',
          duration: 4,
        });
        navigate('/login', { replace: true });
        return;
      }
      setError(err instanceof Error ? err.message : '비밀번호 변경 중 오류가 발생했습니다');
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
      <Card style={{ width: 420 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
          비밀번호 변경
        </Title>
        <Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 24 }}>
          임시 비밀번호로 로그인하셨습니다. 계속하려면 새 비밀번호로 변경해 주세요.
        </Paragraph>

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

        <Form<ChangePasswordForm>
          form={form}
          onFinish={handleSubmit}
          layout="vertical"
          autoComplete="off"
        >
          <Form.Item
            label="새 비밀번호"
            name="newPassword"
            extra={<PasswordPolicyChecklist password={newPassword} />}
            rules={[
              { required: true, message: '새 비밀번호를 입력하세요' },
              {
                min: PASSWORD_MIN_LENGTH,
                message: `비밀번호는 ${PASSWORD_MIN_LENGTH}자 이상이어야 합니다`,
              },
              {
                validator: (_, value) =>
                  !value || hasEnoughCharacterTypes(value)
                    ? Promise.resolve()
                    : Promise.reject(
                        new Error('영문 대/소문자·숫자·특수문자 중 3종 이상을 조합해주세요'),
                      ),
              },
            ]}
          >
            <Input.Password placeholder="새 비밀번호를 입력하세요" size="large" />
          </Form.Item>

          <Form.Item
            label="새 비밀번호 확인"
            name="confirmPassword"
            dependencies={['newPassword']}
            hasFeedback
            rules={[
              { required: true, message: '새 비밀번호를 다시 입력하세요' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('비밀번호가 일치하지 않습니다'));
                },
              }),
            ]}
          >
            <Input.Password placeholder="새 비밀번호를 다시 입력하세요" size="large" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 8 }}>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              비밀번호 변경
            </Button>
          </Form.Item>

          {/* 세션이 죽어 변경도 못 하고 다른 화면으로도 못 빠져나가는 데드락 탈출구.
              logout() 이 강제 변경 플래그까지 정리해 라우터 가드 루프를 끊는다. */}
          <Button
            type="link"
            block
            onClick={() => {
              logout();
              navigate('/login', { replace: true });
            }}
          >
            다시 로그인
          </Button>
        </Form>
      </Card>
    </div>
  );
}
