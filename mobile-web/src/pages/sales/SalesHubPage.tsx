import { Card, Col, Row, Tag, Typography } from 'antd';
import {
  BarChartOutlined,
  CalendarOutlined,
  DesktopOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';

interface SalesEntry {
  to?: string;
  label: string;
  icon: ReactNode;
  color: string;
  /** Wave 5(데이터 거래처레벨 한정) 표시 */
  limited?: boolean;
}

const ENTRIES: SalesEntry[] = [
  { to: '/sales/monthly', label: '월매출', icon: <BarChartOutlined />, color: '#fa8c16' },
  { to: '/promotions', label: '행사매출', icon: <CalendarOutlined />, color: '#eb2f96' },
  { label: '전산매출', icon: <DesktopOutlined />, color: '#13c2c2', limited: true },
  { label: 'POS매출', icon: <ShoppingCartOutlined />, color: '#722ed1', limited: true },
];

/** 매출 현황 허브 (레거시 promotion/month/main2). */
export default function SalesHubPage() {
  const navigate = useNavigate();
  return (
    <div>
      <Row gutter={[12, 12]}>
        {ENTRIES.map((e) => (
          <Col span={12} key={e.label}>
            <Card
              hoverable={!!e.to}
              styles={{ body: { padding: 20, textAlign: 'center' } }}
              onClick={() => e.to && navigate(e.to)}
              style={e.to ? undefined : { opacity: 0.6 }}
            >
              <span style={{ fontSize: 30, color: e.color }}>{e.icon}</span>
              <div style={{ marginTop: 10, fontWeight: 600 }}>{e.label}</div>
              {e.limited && (
                <Tag color="orange" style={{ marginTop: 6 }}>
                  준비중
                </Tag>
              )}
            </Card>
          </Col>
        ))}
      </Row>
      <Typography.Paragraph type="secondary" style={{ fontSize: 12, marginTop: 12 }}>
        전산·POS 매출은 데이터 연동(거래처 레벨) 확정 후 제공됩니다.
      </Typography.Paragraph>
    </div>
  );
}
