import { useState } from 'react';
import { Button, Card, Form, Input, Typography, App as AntdApp } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { verifyPassword } from '@/api/password';
import DetailHeader from '@/components/DetailHeader';

/** 자발 비밀번호 변경 1단계 — 현재 비밀번호 확인 (레거시 mypage/modify). */
export default function VerifyPasswordPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { currentPassword: string }) => {
    setLoading(true);
    try {
      await verifyPassword(values.currentPassword);
      // 검증 통과 → 변경 화면(자발 모드). 현재 비밀번호를 state 로 전달.
      navigate('/password/change', {
        state: { verified: true, currentPassword: values.currentPassword },
      });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '비밀번호 확인에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <DetailHeader title="비밀번호 확인" />
      <Card styles={{ body: { padding: 20 } }}>
        <Typography.Paragraph type="secondary">
          비밀번호 변경을 위해 현재 비밀번호를 입력하세요.
        </Typography.Paragraph>
        <Form layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item
            name="currentPassword"
            rules={[{ required: true, message: '현재 비밀번호를 입력하세요' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="현재 비밀번호" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            확인
          </Button>
        </Form>
      </Card>
    </>
  );
}
