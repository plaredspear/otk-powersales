import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Radio,
  Row,
  Select,
  Space,
  Upload,
  message,
} from 'antd';
import type { UploadFile } from 'antd';
import { useCreateClaim } from '@/hooks/claims/useCreateClaim';
import dayjs from 'dayjs';

/**
 * Spec #829 — Web admin 클레임 등록 페이지.
 *
 * Backend dual-write (DB INSERT → SF Apex push) 호출. 응답 status 별 토스트 분기.
 * 검색 모달 (거래처 / 제품) 은 본 스펙 범위 외 — 텍스트 입력으로 단순화 후 후속 보강.
 */

const CLAIM_TYPE1_OPTIONS = [
  { value: 'A', label: 'A - 포장불량' },
  { value: 'B', label: 'B - 이물혼입' },
  { value: 'C', label: 'C - 내용물이상' },
];

const CLAIM_TYPE2_OPTIONS: Record<string, { value: string; label: string }[]> = {
  A: [
    { value: 'AA', label: 'AA - 내용물[없음/터짐]' },
    { value: 'AB', label: 'AB - 누수/누유' },
    { value: 'AC', label: 'AC - 라벨[없음/접착불량/찢어짐]' },
    { value: 'AD', label: 'AD - 수량부족' },
    { value: 'AE', label: 'AE - 용기[찌그러짐/파손/불량]' },
    { value: 'AF', label: 'AF - 유통기한[미표시/지워짐/불량]' },
    { value: 'AG', label: 'AG - 이종혼입' },
    { value: 'AH', label: 'AH - 중량미달' },
    { value: 'AI', label: 'AI - 포장지[접착불량/찢어짐/빈포장지]' },
    { value: 'AJ', label: 'AJ - 캡[없음/파손]' },
    { value: 'AK', label: 'AK - 케이스[접착불량/찌그러짐]' },
    { value: 'AL', label: 'AL - 포장불량 - 기타' },
  ],
  B: [
    { value: 'BA', label: 'BA - 금속/유리류' },
    { value: 'BB', label: 'BB - 동물성[뼈/털 등]' },
    { value: 'BC', label: 'BC - 플라스틱류' },
    { value: 'BD', label: 'BD - 벌레류[파리/애벌레 등]' },
    { value: 'BE', label: 'BE - 머리카락' },
    { value: 'BF', label: 'BF - 검은이물[면이물/기름때 등]' },
    { value: 'BG', label: 'BG - 이물혼입 - 기타' },
  ],
  C: [
    { value: 'CA', label: 'CA - 이미.이취.쩐내 등' },
    { value: 'CB', label: 'CB - 성형불량/색상/점도 등' },
    { value: 'CC', label: 'CC - 내용물[없음/터짐/분리/마름 등]' },
    { value: 'CD', label: 'CD - 팽창' },
    { value: 'CE', label: 'CE - 곰팡이' },
    { value: 'CF', label: 'CF - 내용물이상 - 기타' },
  ],
};

const PURCHASE_METHOD_OPTIONS = [
  { value: 'A', label: '법인카드' },
  { value: 'B', label: '개인카드' },
  { value: 'C', label: '현금' },
];

const REQUEST_TYPE_OPTIONS = [
  '의견서',
  '상담',
  '긴급처리(FS사업부)',
  '판매취소 필요',
  '생산일정 조율 필요',
  '물량수거 요청',
];

type Values = {
  sapAccountCode: string;
  productCode: string;
  employeeCode: string;
  dateType: 'EXPIRY_DATE' | 'MANUFACTURE_DATE';
  date: dayjs.Dayjs;
  claimDate: dayjs.Dayjs;
  claimType1: 'A' | 'B' | 'C';
  claimType2: string;
  quantity: number;
  description: string;
  purchaseMethod?: 'A' | 'B' | 'C';
  amount?: number;
  requestType?: string[];
};

export default function ClaimCreatePage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<Values>();
  const [claimType1, setClaimType1] = useState<'A' | 'B' | 'C' | undefined>();
  const [purchaseMethod, setPurchaseMethod] = useState<'A' | 'B' | 'C' | undefined>();
  const [claimPhotos, setClaimPhotos] = useState<UploadFile[]>([]);
  const [partPhotos, setPartPhotos] = useState<UploadFile[]>([]);
  const [receiptPhotos, setReceiptPhotos] = useState<UploadFile[]>([]);
  const { mutate: createClaim, isPending } = useCreateClaim();

  const claimType2Options = useMemo(
    () => (claimType1 ? CLAIM_TYPE2_OPTIONS[claimType1] : []),
    [claimType1],
  );

  const receiptRequired = purchaseMethod === 'B' || purchaseMethod === 'C';

  const onSubmit = (values: Values) => {
    const claimPhoto = claimPhotos[0]?.originFileObj;
    const partPhoto = partPhotos[0]?.originFileObj;
    const receiptPhoto = receiptPhotos[0]?.originFileObj;

    if (!claimPhoto) {
      message.error('클레임 사진은 필수입니다');
      return;
    }
    if (!partPhoto) {
      message.error('일부인 사진은 필수입니다');
      return;
    }
    if (receiptRequired && !receiptPhoto) {
      message.error('개인카드/현금 구매 시 영수증 사진은 필수입니다');
      return;
    }

    createClaim(
      {
        sapAccountCode: values.sapAccountCode,
        productCode: values.productCode,
        employeeCode: values.employeeCode,
        dateType: values.dateType,
        expirationDate: values.dateType === 'EXPIRY_DATE' ? values.date.format('YYYY-MM-DD') : undefined,
        manufacturingDate: values.dateType === 'MANUFACTURE_DATE' ? values.date.format('YYYY-MM-DD') : undefined,
        claimDate: values.claimDate.format('YYYY-MM-DD'),
        claimType1: values.claimType1,
        claimType2: values.claimType2,
        quantity: String(values.quantity),
        description: values.description,
        purchaseMethod: values.purchaseMethod,
        amount: values.amount != null ? String(values.amount) : undefined,
        requestType: values.requestType?.join(';'),
        claimPhoto,
        partPhoto,
        receiptPhoto,
      },
      {
        onSuccess: (data) => {
          if (data.status === 'SENT') {
            message.success('클레임이 등록되어 SF에 전송되었습니다');
          } else {
            message.warning(
              `클레임은 등록되었으나 SF 전송에 실패했습니다: ${data.sfResultMsg ?? '연동 오류'}. 상세에서 재전송하세요`,
            );
          }
          navigate(`/claims/${data.claimId}`);
        },
        onError: (err) => {
          message.error(`등록 실패: ${err.message}`);
        },
      },
    );
  };

  return (
    <div style={{ padding: 16 }}>
      <Card title="클레임 신규 등록">
        <Form
          form={form}
          layout="vertical"
          onFinish={onSubmit}
          initialValues={{
            dateType: 'EXPIRY_DATE',
            claimDate: dayjs(),
          }}
        >
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="거래처 SAP 코드"
                name="sapAccountCode"
                rules={[{ required: true, message: '거래처 SAP 코드는 필수입니다' }]}
              >
                <Input placeholder="예: SAP-001" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="제품 코드"
                name="productCode"
                rules={[{ required: true, message: '제품 코드는 필수입니다' }]}
              >
                <Input placeholder="예: PROD-001" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="사번 (대리 등록 대상)"
                name="employeeCode"
                rules={[{ required: true, message: '사번은 필수입니다' }]}
              >
                <Input placeholder="예: 10023456" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="기한 종류" name="dateType" rules={[{ required: true }]}>
                <Radio.Group>
                  <Radio value="EXPIRY_DATE">유통기한</Radio>
                  <Radio value="MANUFACTURE_DATE">제조일자</Radio>
                </Radio.Group>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="기한 날짜"
                name="date"
                rules={[{ required: true, message: '기한 날짜는 필수입니다' }]}
              >
                <DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="발생일자"
                name="claimDate"
                rules={[
                  { required: true, message: '발생일자는 필수입니다' },
                  () => ({
                    validator(_rule, value) {
                      if (value && value.isAfter(dayjs(), 'day')) {
                        return Promise.reject(new Error('발생일자를 다시한번 확인해주십시오.'));
                      }
                      return Promise.resolve();
                    },
                  }),
                ]}
              >
                <DatePicker
                  format="YYYY-MM-DD"
                  style={{ width: '100%' }}
                  disabledDate={(current) => current && current.isAfter(dayjs(), 'day')}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="대분류"
                name="claimType1"
                rules={[{ required: true, message: '대분류는 필수입니다' }]}
              >
                <Select
                  options={CLAIM_TYPE1_OPTIONS}
                  onChange={(v) => {
                    setClaimType1(v as 'A' | 'B' | 'C');
                    form.setFieldValue('claimType2', undefined);
                  }}
                  placeholder="대분류 선택"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="소분류"
                name="claimType2"
                rules={[{ required: true, message: '소분류는 필수입니다' }]}
              >
                <Select options={claimType2Options} disabled={!claimType1} placeholder="소분류 선택" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="불량 수량 (EA)"
                name="quantity"
                rules={[
                  { required: true, message: '불량 수량은 필수입니다' },
                  { type: 'number', min: 0.0001, message: '불량 수량은 양수여야 합니다' },
                ]}
              >
                <InputNumber style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="불량 내역"
            name="description"
            rules={[
              { required: true, message: '불량 내역은 필수입니다' },
              { max: 4000, message: '최대 4000자까지 입력 가능합니다' },
            ]}
          >
            <Input.TextArea rows={4} placeholder="제품 이상 사항을 자세히 입력하세요" />
          </Form.Item>

          <Card title="구매 정보 (선택)" size="small" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item label="구매 방법" name="purchaseMethod">
                  <Select
                    options={PURCHASE_METHOD_OPTIONS}
                    allowClear
                    placeholder="구매 방법"
                    onChange={(v) => setPurchaseMethod(v as 'A' | 'B' | 'C' | undefined)}
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label={`구매 금액 (원)${receiptRequired ? ' *' : ''}`}
                  name="amount"
                  rules={
                    receiptRequired
                      ? [
                          { required: true, message: '구매 금액은 필수입니다' },
                          { type: 'number', min: 0.0001, message: '구매 금액은 양수여야 합니다' },
                        ]
                      : []
                  }
                >
                  <InputNumber style={{ width: '100%' }} min={0} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="요청사항 (최대 4개)" name="requestType">
                  <Select
                    mode="multiple"
                    placeholder="요청사항"
                    maxCount={4}
                    options={REQUEST_TYPE_OPTIONS.map((v) => ({ value: v, label: v }))}
                  />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          <Card title="첨부 사진" size="small" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item label="클레임 사진 *" required>
                  <Upload
                    listType="picture"
                    maxCount={1}
                    beforeUpload={() => false}
                    fileList={claimPhotos}
                    onChange={({ fileList }) => setClaimPhotos(fileList)}
                    accept="image/*"
                  >
                    <Button>파일 선택</Button>
                  </Upload>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="일부인 사진 *" required>
                  <Upload
                    listType="picture"
                    maxCount={1}
                    beforeUpload={() => false}
                    fileList={partPhotos}
                    onChange={({ fileList }) => setPartPhotos(fileList)}
                    accept="image/*"
                  >
                    <Button>파일 선택</Button>
                  </Upload>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label={`영수증 사진${receiptRequired ? ' *' : ''}`}>
                  <Upload
                    listType="picture"
                    maxCount={1}
                    beforeUpload={() => false}
                    fileList={receiptPhotos}
                    onChange={({ fileList }) => setReceiptPhotos(fileList)}
                    accept="image/*"
                  >
                    <Button>파일 선택</Button>
                  </Upload>
                </Form.Item>
              </Col>
            </Row>
            {receiptRequired && (
              <Alert
                type="info"
                message="개인카드/현금 구매 시 영수증 사진이 필수입니다."
                style={{ marginTop: 8 }}
              />
            )}
          </Card>

          <Space>
            <Button onClick={() => navigate('/claims')}>취소</Button>
            <Button type="primary" htmlType="submit" loading={isPending}>
              등록
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
}
