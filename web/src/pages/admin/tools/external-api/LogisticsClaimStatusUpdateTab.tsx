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
  Typography,
} from 'antd';

const { Text } = Typography;

const DATE_FORMAT = 'YYYY-MM-DD';

/**
 * SF 물류 클레임(제안) 상태 picklist (DKRetail__Proposal__c.Status__c) 임시 후보.
 * 상세 값/계약은 추후 확정 예정 — 현재는 레이아웃 구성을 위한 placeholder.
 */
const CLAIM_STATUS_OPTIONS = [
  { value: 'RECEIVED', label: '접수' },
  { value: 'IN_PROGRESS', label: '처리중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'REJECTED', label: '반려' },
];

/**
 * SF 물류 클레임 상태 업데이트 전송 테스트 탭 (외부 API 테스트 통합 페이지).
 *
 * UI 레이아웃은 "SF 클레임 상태 업데이트" 탭과 동일한 형태(Card + 세로 Form + Row/Col)로 구성한다.
 * 상세 파라미터 계약과 실제 SF 전송 로직은 추후 반영 예정으로, 현재 단계에서는 폼 레이아웃만
 * 제공하며 백엔드/SF 호출은 수행하지 않는다(버튼 비활성화).
 */
export default function LogisticsClaimStatusUpdateTab() {
  const [form] = Form.useForm();

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="물류 클레임 상태 업데이트 (전송 로직 추후 반영)">
        <Form form={form} layout="vertical" initialValues={{ status: 'RECEIVED' }}>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="물류 클레임 식별자 (Proposal No)"
                name="proposalNo"
                rules={[{ required: true, message: '물류 클레임 식별자는 필수입니다' }]}
              >
                <Input placeholder="SF Proposal 식별자" />
              </Form.Item>
            </Col>
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
                label="변경할 상태 (Status)"
                name="status"
                rules={[{ required: true, message: '상태는 필수입니다' }]}
              >
                <Select options={CLAIM_STATUS_OPTIONS} placeholder="선택" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="처리일자 (ProcessDate)"
                name="processDate"
                rules={[{ required: true, message: '처리일자는 필수입니다' }]}
              >
                <DatePicker style={{ width: '100%' }} format={DATE_FORMAT} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item label="처리 내용 (Comment)" name="comment">
            <Input.TextArea rows={2} maxLength={4000} />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Space>
              <Button type="primary" htmlType="submit" disabled>
                SF 전송
              </Button>
              <Text type="secondary">전송 로직은 추후 반영 예정입니다.</Text>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </Space>
  );
}
