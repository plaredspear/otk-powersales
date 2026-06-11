import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
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
import { testClaimRegist } from '@/api/claims';
import type {
  ClaimRegistTestInput,
  ClaimRegistTestResult,
} from '@/api/claims';

const { Text } = Typography;

const DATE_FORMAT = 'YYYY-MM-DD';

/** SF ClaimType1 picklist (DKRetail__ClaimType1__c). */
const CLAIM_TYPE1_OPTIONS = [
  { value: 'A', label: 'A — 포장불량' },
  { value: 'B', label: 'B — 이물혼입' },
  { value: 'C', label: 'C — 내용물이상' },
];

/** SF PurchaseMethod picklist (A/B/C). B(개인카드)·C(현금) 선택 시 영수증·금액 필수. */
const PURCHASE_METHOD_OPTIONS = [
  { value: 'A', label: 'A — 법인카드' },
  { value: 'B', label: 'B — 개인카드' },
  { value: 'C', label: 'C — 현금' },
];

interface FormValues {
  sapAccountCode: string;
  productCode: string;
  employeeCode: string;
  dateType: 'EXPIRY_DATE' | 'MANUFACTURE_DATE';
  dateValue: Dayjs;
  claimDate: Dayjs;
  claimType1: string;
  claimType2: string;
  quantity: number;
  description: string;
  purchaseMethod?: string;
  amount?: number;
  requestType?: string;
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
 * SF Apex REST `/ClaimRegist` 전송 테스트 탭 (외부 API 테스트 통합 페이지).
 *
 * 백엔드 `POST /api/v1/admin/claim-regist/test` 를 호출해 입력한 파라미터로 apiMap 을 구성하고
 * SF 로 직접 POST 한다. 신규 DB(claim 테이블)에는 저장하지 않으며, SF 가 돌려준 RESULT_CODE/RESULT_MSG
 * 와 전송한 payload 미리보기를 그대로 노출한다. 이미지 3종은 모두 선택 — 미첨부 시 빈 Buffer 로 전송된다.
 * SYSTEM(MODIFY_ALL_DATA) 권한 필요.
 */
export default function ClaimRegistTab() {
  const [form] = Form.useForm<FormValues>();
  const [result, setResult] = useState<ClaimRegistTestResult | null>(null);
  const [claimPhotoList, setClaimPhotoList] = useState<UploadFile[]>([]);
  const [partPhotoList, setPartPhotoList] = useState<UploadFile[]>([]);
  const [receiptPhotoList, setReceiptPhotoList] = useState<UploadFile[]>([]);

  const dateType = Form.useWatch('dateType', form);

  const mutation = useMutation<ClaimRegistTestResult, Error, ClaimRegistTestInput>({
    mutationFn: testClaimRegist,
  });

  const handleFinish = async (values: FormValues) => {
    const payload: ClaimRegistTestInput = {
      sapAccountCode: values.sapAccountCode.trim(),
      productCode: values.productCode.trim(),
      employeeCode: values.employeeCode.trim(),
      dateType: values.dateType,
      expirationDate:
        values.dateType === 'EXPIRY_DATE'
          ? values.dateValue?.format(DATE_FORMAT)
          : undefined,
      manufacturingDate:
        values.dateType === 'MANUFACTURE_DATE'
          ? values.dateValue?.format(DATE_FORMAT)
          : undefined,
      claimDate: values.claimDate.format(DATE_FORMAT),
      claimType1: values.claimType1,
      claimType2: values.claimType2.trim(),
      quantity: String(values.quantity),
      description: values.description.trim(),
      purchaseMethod: values.purchaseMethod || undefined,
      amount: values.amount != null ? String(values.amount) : undefined,
      requestType: values.requestType?.trim() || undefined,
      claimPhoto: firstFile(claimPhotoList),
      partPhoto: firstFile(partPhotoList),
      receiptPhoto: firstFile(receiptPhotoList),
    };

    try {
      const response = await mutation.mutateAsync(payload);
      setResult(response);
      notification[response.success ? 'success' : 'warning']({
        key: 'claim-regist-test',
        message: response.success
          ? 'SF 전송 성공 (RESULT_CODE=200)'
          : `SF 전송 실패 (RESULT_CODE=${response.resultCode ?? '-'})`,
        description: response.resultMsg ?? undefined,
      });
    } catch (err) {
      setResult(null);
      notification.error({
        key: 'claim-regist-test-error',
        message: 'SF ClaimRegist 호출 실패',
        description: err instanceof Error ? err.message : '잠시 후 다시 시도해주세요.',
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
      <Card title="클레임 전송 (POST /api/v1/admin/claim-regist/test)">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          disabled={mutation.isPending}
          initialValues={{ dateType: 'EXPIRY_DATE', quantity: 1 }}
        >
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="거래처 SAP 코드"
                name="sapAccountCode"
                rules={[{ required: true, message: '거래처 SAP 코드는 필수입니다' }]}
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
              <Form.Item label="기한 종류" name="dateType" rules={[{ required: true }]}>
                <Select
                  options={[
                    { value: 'EXPIRY_DATE', label: '유통기한 (ExpirationDate)' },
                    { value: 'MANUFACTURE_DATE', label: '제조일자 (ManufacturingDate)' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label={dateType === 'MANUFACTURE_DATE' ? '제조일자' : '유통기한'}
                name="dateValue"
                rules={[{ required: true, message: '날짜는 필수입니다' }]}
              >
                <DatePicker style={{ width: '100%' }} format={DATE_FORMAT} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="발생일자 (ClaimDate)"
                name="claimDate"
                rules={[{ required: true, message: '발생일자는 필수입니다' }]}
              >
                <DatePicker style={{ width: '100%' }} format={DATE_FORMAT} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="클레임 대분류 (ClaimType1)"
                name="claimType1"
                rules={[{ required: true, message: '대분류는 필수입니다' }]}
              >
                <Select options={CLAIM_TYPE1_OPTIONS} placeholder="선택" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="클레임 소분류 (ClaimType2)"
                name="claimType2"
                rules={[{ required: true, message: '소분류는 필수입니다' }]}
                tooltip="SF picklist value 직접 입력 (예: AA, BC, CF). 대분류에 종속됩니다."
              >
                <Input placeholder="예: AA" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="불량 수량 (Quantity)"
                name="quantity"
                rules={[{ required: true, message: '수량은 필수입니다' }]}
              >
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="불량 내역 (Description)"
            name="description"
            rules={[{ required: true, message: '불량 내역은 필수입니다' }]}
          >
            <Input.TextArea rows={2} maxLength={4000} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="구매 방법 (PurchaseMethod)" name="purchaseMethod">
                <Select
                  allowClear
                  options={PURCHASE_METHOD_OPTIONS}
                  placeholder="선택 (B/C 면 금액·영수증 필수)"
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="구매 금액 (Amount)" name="amount">
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  placeholder="미입력 시 null 전송"
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="요청 사항 (RequestType)"
                name="requestType"
                tooltip='SF multipicklist 표시명. 다중은 ";" 구분 (예: "상담;교환")'
              >
                <Input placeholder='예: 상담;교환' />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="불량 사진 (ClaimImage)">
                <Upload {...uploadProps(claimPhotoList, setClaimPhotoList)}>
                  <Button icon={<UploadOutlined />}>이미지 선택</Button>
                </Upload>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="일부인 사진 (PartImage)">
                <Upload {...uploadProps(partPhotoList, setPartPhotoList)}>
                  <Button icon={<UploadOutlined />}>이미지 선택</Button>
                </Upload>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item label="영수증 사진 (ReceiptImage)">
                <Upload {...uploadProps(receiptPhotoList, setReceiptPhotoList)}>
                  <Button icon={<UploadOutlined />}>이미지 선택</Button>
                </Upload>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={mutation.isPending}>
              SF 전송
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {result && (
        <Card
          title={
            <Space>
              <Text strong>SF 응답</Text>
              <Tag color={result.success ? 'green' : 'red'}>
                RESULT_CODE = {result.resultCode ?? '-'}
              </Tag>
            </Space>
          }
        >
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {result.resultMsg && (
              <Text>
                <strong>RESULT_MSG:</strong> {result.resultMsg}
              </Text>
            )}
            <div>
              <Text type="secondary">전송 payload (apiMap)</Text>
              <pre style={PRE_STYLE}>{prettyPrintJson(result.requestPayload)}</pre>
            </div>
            <div>
              <Text type="secondary">SF 응답 본문 (raw)</Text>
              <pre style={PRE_STYLE}>{prettyPrintJson(result.rawResponse)}</pre>
            </div>
          </Space>
        </Card>
      )}
    </Space>
  );
}
