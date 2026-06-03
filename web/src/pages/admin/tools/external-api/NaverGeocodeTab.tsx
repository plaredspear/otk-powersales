import { useState } from 'react';
import { Button, Card, Form, Input, Space, Typography, notification } from 'antd';
import { AxiosError } from 'axios';
import { useNaverGeocodeTest } from '@/hooks/admin/useNaverGeocodeTest';
import type { NaverGeocodeTestResponse } from '@/api/admin/naverGeocode';
import { isApiErrorBody } from '@/api/types';

interface FormValues {
  address: string;
}

const ADDRESS_MAX_LENGTH = 200;

function prettyPrintJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

/**
 * Naver Cloud Map Geocode API 테스트 탭 (외부 API 테스트 통합 페이지).
 *
 * 백엔드 `POST /api/v1/admin/naver-geocode/test` 를 호출해 Naver API 응답 본문을
 * 가공 없이 raw JSON 으로 노출한다. 권한 `NAVER_GEOCODE_TEST` 필요.
 */
export default function NaverGeocodeTab() {
  const [form] = Form.useForm<FormValues>();
  const [result, setResult] = useState<NaverGeocodeTestResponse | null>(null);
  const mutation = useNaverGeocodeTest();
  const address = Form.useWatch('address', form);
  const trimmedAddress = (address || '').trim();
  const submitDisabled =
    trimmedAddress.length === 0 || trimmedAddress.length > ADDRESS_MAX_LENGTH;

  const handleFinish = async (values: FormValues) => {
    try {
      const response = await mutation.mutateAsync({ address: values.address.trim() });
      setResult(response);
    } catch (err) {
      setResult(null);
      const errorBody = err instanceof AxiosError ? err.response?.data : undefined;
      if (isApiErrorBody(errorBody) && errorBody.error?.code === 'NAVER_GEOCODE_API_FAILED') {
        notification.error({
          key: 'naver-geocode-test-failed',
          message: 'Naver API 호출 실패',
          description: '잠시 후 다시 시도해주세요.',
        });
      }
    }
  };

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="주소 변환 (POST /api/v1/admin/naver-geocode/test)">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          disabled={mutation.isPending}
        >
          <Form.Item
            label="주소"
            name="address"
            rules={[
              { required: true, message: '주소를 입력해주세요' },
              { max: ADDRESS_MAX_LENGTH, message: '주소는 200자 이내로 입력해주세요' },
            ]}
          >
            <Input
              placeholder="예: 서울특별시 강남구 테헤란로 123"
              maxLength={ADDRESS_MAX_LENGTH + 1}
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={mutation.isPending}
              disabled={submitDisabled}
            >
              변환
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {result && (
        <Card title="Naver API 응답 (raw)">
          <Space direction="vertical" size="small" style={{ width: '100%' }}>
            <Typography.Text>
              <strong>입력 주소:</strong> {result.input}
            </Typography.Text>
            <pre
              data-testid="naver-geocode-raw-response"
              style={{
                background: '#f5f5f5',
                padding: 12,
                borderRadius: 4,
                margin: 0,
                maxHeight: 480,
                overflow: 'auto',
                fontFamily: 'Menlo, Consolas, monospace',
                fontSize: 12,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
              }}
            >
              {prettyPrintJson(result.rawResponse)}
            </pre>
          </Space>
        </Card>
      )}
    </Space>
  );
}
