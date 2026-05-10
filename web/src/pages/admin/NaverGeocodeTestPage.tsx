import { useState } from 'react';
import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  Space,
  Table,
  Typography,
  notification,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { AxiosError } from 'axios';
import { useNaverGeocodeTest } from '@/hooks/admin/useNaverGeocodeTest';
import type {
  NaverGeocodeTestResponse,
  NaverGeocodeTestResult,
} from '@/api/admin/naverGeocode';
import { isApiErrorBody } from '@/api/types';

const { Title } = Typography;

interface FormValues {
  address: string;
}

const ADDRESS_MAX_LENGTH = 200;

interface IndexedResult extends NaverGeocodeTestResult {
  _key: number;
}

const COLUMNS: ColumnsType<IndexedResult> = [
  {
    title: '도로명 주소',
    dataIndex: 'roadAddress',
    key: 'roadAddress',
    width: 240,
    render: (value: string | null) => value || '-',
  },
  {
    title: '지번 주소',
    dataIndex: 'jibunAddress',
    key: 'jibunAddress',
    width: 240,
    render: (value: string | null) => value || '-',
  },
  {
    title: '위도 (latitude)',
    dataIndex: 'latitude',
    key: 'latitude',
    width: 120,
    render: (value: string | null) => value || '-',
  },
  {
    title: '경도 (longitude)',
    dataIndex: 'longitude',
    key: 'longitude',
    width: 120,
    render: (value: string | null) => value || '-',
  },
];

export default function NaverGeocodeTestPage() {
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
      <Title level={3} style={{ margin: 0 }}>
        Naver Geocode 변환 테스트
      </Title>

      <Card title="주소 변환">
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
            <Input placeholder="예: 서울특별시 강남구 테헤란로 123" maxLength={ADDRESS_MAX_LENGTH + 1} />
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
        <Card title="변환 결과">
          <Space direction="vertical" size="small" style={{ width: '100%' }}>
            <Typography.Text>
              <strong>입력 주소:</strong> {result.input}
            </Typography.Text>
            <Typography.Text>
              <strong>매칭 건수:</strong> {result.matchedCount}건
            </Typography.Text>
            {result.matchedCount === 0 ? (
              <Empty description="변환 결과 없음. 주소를 다시 확인해주세요." />
            ) : (
              <Table<IndexedResult>
                rowKey="_key"
                columns={COLUMNS}
                dataSource={result.results.map((r, idx) => ({ ...r, _key: idx }))}
                pagination={false}
                size="small"
              />
            )}
          </Space>
        </Card>
      )}
    </Space>
  );
}
