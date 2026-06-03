import { Card, Space, Typography, Button, List } from 'antd';
import { ArrowRightOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

const { Paragraph, Text } = Typography;

const SAP_INTERFACES: { id: string; name: string }[] = [
  { id: 'SAP_INTERFACE_LOAN_INQUIRY', name: '거래처 여신 한도/잔액 조회' },
  { id: 'SAP_INTERFACE_ORDER_REQUEST_DETAIL', name: '주문 요청 상세 조회' },
  { id: 'SAP_INTERFACE_ORDER_REQUEST_CANCEL', name: '주문 취소 요청' },
  { id: 'SAP_INTERFACE_ORDER_REQUEST_REGIST', name: '주문 등록' },
  { id: 'SAP_INTERFACE_ATTENDANCE', name: '일일 출근 현황 송신' },
  { id: 'SAP_INTERFACE_DISPLAY_MASTER', name: '진열 마스터 송신' },
  { id: 'SAP_INTERFACE_PPT_MASTER', name: '전문행사조 마스터 송신' },
];

/**
 * SAP Outbound 안내 탭 (외부 API 테스트 통합 페이지).
 *
 * SAP 아웃바운드는 7개 인터페이스 테스트 + 호출 이력 + 대기 큐 + 카탈로그를 갖춘
 * 전용 페이지(SAP Outbound)가 이미 존재하므로, 여기서는 중복 구현 없이 링크로 안내한다.
 */
export default function SapOutboundLinkTab() {
  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="SAP Outbound 테스트">
        <Paragraph>
          SAP 아웃바운드 인터페이스는 호출 이력 · 대기 큐(Outbox) · API 카탈로그 ·{' '}
          <Text strong>테스트 송신</Text> 탭을 갖춘 전용 페이지에서 테스트할 수 있습니다.
        </Paragraph>
        <Link to="/admin/tools/sap-outbound">
          <Button type="primary" icon={<ArrowRightOutlined />}>
            SAP Outbound 페이지로 이동
          </Button>
        </Link>
      </Card>

      <Card title="테스트 가능한 SAP 인터페이스">
        <List
          size="small"
          dataSource={SAP_INTERFACES}
          renderItem={(item) => (
            <List.Item>
              <Space>
                <Text code>{item.id}</Text>
                <Text type="secondary">{item.name}</Text>
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </Space>
  );
}
