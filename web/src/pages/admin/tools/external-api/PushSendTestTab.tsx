import { useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Space,
  Tag,
  notification,
} from 'antd';
import { usePushTest } from '@/hooks/admin/usePushTest';
import type { PushTestResponse } from '@/api/admin/pushTest';

interface FormValues {
  employeeCode: string;
  title: string;
  body: string;
}

const EMPLOYEE_CODE_MAX_LENGTH = 20;
const TITLE_MAX_LENGTH = 100;
const BODY_MAX_LENGTH = 200;

/**
 * FCM push 발송 테스트 탭 (외부 API 테스트 통합 페이지).
 *
 * 백엔드 `POST /api/v1/admin/push/test` 를 호출해 입력 사번에 등록된 FCM 토큰으로 임의 제목/본문의
 * 테스트 알림을 1건 발송한다. 실제 발송 여부는 서버의 FCM 활성 설정 + 프로필에 좌우되며, 응답의
 * success/failure 집계와 요약 메시지로 결과를 확인한다. 권한 `MODIFY_ALL_DATA`(SYSTEM_ADMIN) 필요.
 */
export default function PushSendTestTab() {
  const [form] = Form.useForm<FormValues>();
  const [result, setResult] = useState<PushTestResponse | null>(null);
  const mutation = usePushTest();

  const handleFinish = async (values: FormValues) => {
    try {
      const response = await mutation.mutateAsync({
        employeeCode: values.employeeCode.trim(),
        title: values.title.trim(),
        body: values.body.trim(),
      });
      setResult(response);
      if (response.successCount > 0) {
        notification.success({
          key: 'push-test-success',
          message: 'push 발송 완료',
          description: response.message,
        });
      } else {
        notification.warning({
          key: 'push-test-not-sent',
          message: 'push 미발송',
          description: response.message,
        });
      }
    } catch (err) {
      setResult(null);
      notification.error({
        key: 'push-test-failed',
        message: 'push 발송 테스트 호출 실패',
        description: err instanceof Error ? err.message : '잠시 후 다시 시도해주세요.',
      });
    }
  };

  const resultTag = (() => {
    if (!result) return null;
    if (!result.tokenRegistered) return <Tag color="orange">토큰 미등록</Tag>;
    if (result.successCount > 0) return <Tag color="green">발송 성공</Tag>;
    return <Tag color="red">미발송/실패</Tag>;
  })();

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="push 발송 (POST /api/v1/admin/push/test)">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          disabled={mutation.isPending}
          initialValues={{ title: '테스트 알림', body: '푸시 발송 테스트입니다.' }}
        >
          <Form.Item
            label="사번"
            name="employeeCode"
            rules={[
              { required: true, message: '사번을 입력해주세요' },
              { max: EMPLOYEE_CODE_MAX_LENGTH, message: '사번은 20자 이내로 입력해주세요' },
            ]}
          >
            <Input placeholder="예: 00012345" maxLength={EMPLOYEE_CODE_MAX_LENGTH + 1} />
          </Form.Item>
          <Form.Item
            label="제목"
            name="title"
            rules={[
              { required: true, message: '제목을 입력해주세요' },
              { max: TITLE_MAX_LENGTH, message: '제목은 100자 이내로 입력해주세요' },
            ]}
          >
            <Input placeholder="알림 제목" maxLength={TITLE_MAX_LENGTH + 1} />
          </Form.Item>
          <Form.Item
            label="본문"
            name="body"
            rules={[
              { required: true, message: '본문을 입력해주세요' },
              { max: BODY_MAX_LENGTH, message: '본문은 200자 이내로 입력해주세요' },
            ]}
          >
            <Input.TextArea
              placeholder="알림 본문"
              maxLength={BODY_MAX_LENGTH + 1}
              autoSize={{ minRows: 2, maxRows: 4 }}
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={mutation.isPending}>
              push 발송
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {result && (
        <Card title={<Space>발송 결과 {resultTag}</Space>}>
          <Descriptions
            column={1}
            size="small"
            bordered
            items={[
              { key: 'employeeCode', label: '사번', children: result.employeeCode },
              { key: 'employeeName', label: '사원명', children: result.employeeName ?? '-' },
              {
                key: 'tokenRegistered',
                label: 'FCM 토큰',
                children: result.tokenRegistered
                  ? (result.maskedToken ?? '등록됨')
                  : '미등록',
              },
              { key: 'successCount', label: '성공 건수', children: String(result.successCount) },
              { key: 'failureCount', label: '실패 건수', children: String(result.failureCount) },
              { key: 'message', label: '요약', children: result.message },
            ]}
          />
        </Card>
      )}
    </Space>
  );
}
