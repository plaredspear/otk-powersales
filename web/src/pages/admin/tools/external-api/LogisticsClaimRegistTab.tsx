import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  Row,
  Select,
  Space,
  Tag,
  Typography,
  Upload,
  notification,
} from 'antd';
import type { UploadFile } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { testLogisticsClaimRegist } from '@/api/claims';
import type {
  LogisticsClaimRegistTestInput,
  LogisticsClaimRegistTestResult,
} from '@/api/claims';

const { Text } = Typography;

const DATE_FORMAT = 'YYYY-MM-DD';

/**
 * 물류 클레임 항목 (모바일 등록 화면 kSuggestionClaimTypeOptions / 레거시 suggestWrite.jsp 하드코딩 6 옵션).
 * SF `DKRetail__Proposal__c.ClaimType__c` picklist value 와 정합.
 */
const CLAIM_TYPE_OPTIONS = [
  '배송기준 미준수(검수/창고적치 미실시)',
  '취급부주의 제품 파손',
  '배송시간 지연',
  '실물 미입고 / 오입고',
  '용차배송 거래처 트러블',
  '기타',
].map((v) => ({ value: v, label: v }));

interface FormValues {
  sapAccountCode: string;
  productCode: string;
  employeeCode: string;
  claimType: string;
  claimDate: Dayjs;
  title: string;
  description: string;
  carNumber?: string;
}

function firstFile(list?: UploadFile[]): File | undefined {
  return list?.[0]?.originFileObj as File | undefined;
}

function prettyPrintJson(raw: string | null): string {
  if (!raw) return '(응답 본문 없음)';
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

const PRE_STYLE: React.CSSProperties = {
  background: '#f5f5f5',
  padding: 12,
  borderRadius: 4,
  margin: 0,
  maxHeight: 360,
  overflow: 'auto',
  fontFamily: 'Menlo, Consolas, monospace',
  fontSize: 12,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-all',
};

/**
 * SF 물류 클레임 등록(ProposalRegist) 전송 테스트 탭 (외부 API 테스트 통합 페이지).
 *
 * 모바일 물류 클레임 등록 정보로 백엔드 `POST /api/v1/admin/logistics-claim-regist/test` 를 호출해
 * SF Apex REST `IF_REST_MOBILE_ProposalRegist` 전송 payload(apiMap) 미리보기를 구성한다.
 * SF 전송 API 정보 미확보 단계라 실제 SF POST 는 수행하지 않으며, 구성된 payload 미리보기만 노출한다.
 * 사진은 최대 2장(모두 선택). SYSTEM(MODIFY_ALL_DATA) 권한 필요.
 */
export default function LogisticsClaimRegistTab() {
  const [form] = Form.useForm<FormValues>();
  const [result, setResult] = useState<LogisticsClaimRegistTestResult | null>(
    null,
  );
  const [photo1List, setPhoto1List] = useState<UploadFile[]>([]);
  const [photo2List, setPhoto2List] = useState<UploadFile[]>([]);

  const mutation = useMutation<
    LogisticsClaimRegistTestResult,
    Error,
    LogisticsClaimRegistTestInput
  >({
    mutationFn: testLogisticsClaimRegist,
  });

  const handleFinish = async (values: FormValues) => {
    const payload: LogisticsClaimRegistTestInput = {
      sapAccountCode: values.sapAccountCode.trim(),
      productCode: values.productCode.trim(),
      employeeCode: values.employeeCode.trim(),
      claimType: values.claimType,
      claimDate: values.claimDate.format(DATE_FORMAT),
      title: values.title.trim(),
      description: values.description.trim(),
      carNumber: values.carNumber?.trim() || undefined,
      photo1: firstFile(photo1List),
      photo2: firstFile(photo2List),
    };

    try {
      const response = await mutation.mutateAsync(payload);
      setResult(response);
      notification.info({
        key: 'logistics-claim-regist-test',
        message: '물류 클레임 등록 payload 미리보기 생성',
        description: response.note,
      });
    } catch (err) {
      setResult(null);
      notification.error({
        key: 'logistics-claim-regist-test-error',
        message: 'SF 물류 클레임 등록 호출 실패',
        description:
          err instanceof Error ? err.message : '잠시 후 다시 시도해주세요.',
      });
    }
  };

  // Upload 가 자동 업로드하지 않고 파일만 보관하도록.
  const uploadProps = (
    list: UploadFile[],
    setList: (l: UploadFile[]) => void,
  ) => ({
    beforeUpload: () => false as const,
    maxCount: 1,
    fileList: list,
    accept: 'image/*',
    onChange: ({ fileList }: { fileList: UploadFile[] }) =>
      setList(fileList.slice(-1)),
    onRemove: () => setList([]),
  });

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="물류 클레임 전송 (POST /api/v1/admin/logistics-claim-regist/test)">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          disabled={mutation.isPending}
        >
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="거래처 SAP 코드"
                name="sapAccountCode"
                rules={[
                  { required: true, message: '거래처 SAP 코드는 필수입니다' },
                ]}
              >
                <Input placeholder="account.external_key" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="제품 코드"
                name="productCode"
                rules={[{ required: true, message: '제품 코드는 필수입니다' }]}
              >
                <Input placeholder="product.product_code" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="사번 (EmployeeCode)"
                name="employeeCode"
                rules={[{ required: true, message: '사번은 필수입니다' }]}
              >
                <Input placeholder="empcode (SFID 아님)" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="클레임 항목 (claimList)"
                name="claimType"
                rules={[{ required: true, message: '클레임 항목은 필수입니다' }]}
              >
                <Select options={CLAIM_TYPE_OPTIONS} placeholder="항목 선택" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="발생일자 (logclaimDate)"
                name="claimDate"
                rules={[{ required: true, message: '발생일자는 필수입니다' }]}
              >
                <DatePicker style={{ width: '100%' }} format={DATE_FORMAT} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="차량 번호 (CarNumber)"
                name="carNumber"
                tooltip="물류 클레임 선택 입력 (최대 20자)"
              >
                <Input placeholder="선택 입력" maxLength={20} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="제목 (Title)"
            name="title"
            rules={[{ required: true, message: '제목은 필수입니다' }]}
          >
            <Input maxLength={250} placeholder="제안 제목" />
          </Form.Item>

          <Form.Item
            label="내용 (Description)"
            name="description"
            rules={[{ required: true, message: '내용은 필수입니다' }]}
          >
            <Input.TextArea rows={3} placeholder="클레임 상세 내용" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="사진 1 (S3Image1)">
                <Upload {...uploadProps(photo1List, setPhoto1List)}>
                  <Button icon={<UploadOutlined />}>이미지 선택</Button>
                </Upload>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="사진 2 (S3Image2)">
                <Upload {...uploadProps(photo2List, setPhoto2List)}>
                  <Button icon={<UploadOutlined />}>이미지 선택</Button>
                </Upload>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={mutation.isPending}
            >
              payload 미리보기
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {result && (
        <Card
          title={
            <Space>
              <Text strong>SF 전송 payload 미리보기</Text>
              <Tag color="orange">미전송 (preview)</Tag>
            </Space>
          }
        >
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Text type="secondary">{result.note}</Text>
            <div>
              <Text type="secondary">전송 payload (apiMap)</Text>
              <pre style={PRE_STYLE}>
                {prettyPrintJson(result.requestPayload)}
              </pre>
            </div>
          </Space>
        </Card>
      )}
    </Space>
  );
}
