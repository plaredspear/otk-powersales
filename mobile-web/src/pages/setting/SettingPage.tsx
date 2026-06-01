import { Card, Descriptions, List, Typography } from 'antd';
import DetailHeader from '@/components/DetailHeader';
import { useAuthStore } from '@/stores/authStore';

/**
 * 앱 정보 / 설정 (레거시 setting/view.jsp). API 불필요.
 * 앱 버전은 빌드 상수. (디바이스 쉘 단계에서 Capacitor App.getInfo() 로 대체 가능)
 */
const APP_VERSION = '0.1.0';

export default function SettingPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <>
      <DetailHeader title="앱 정보 / 설정" />
      <Card style={{ marginBottom: 12 }} styles={{ body: { padding: 16 } }}>
        <Descriptions column={1} size="small">
          <Descriptions.Item label="앱 버전">{APP_VERSION}</Descriptions.Item>
          <Descriptions.Item label="사용자">{user?.name ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="사번">{user?.employeeCode ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="소속">{user?.orgName ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card styles={{ body: { padding: 0 } }}>
        <List>
          <List.Item style={{ padding: 16 }}>
            <List.Item.Meta
              title="오픈소스 라이선스"
              description={
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  React, Ant Design, TanStack Query 등 오픈소스를 사용합니다.
                </Typography.Text>
              }
            />
          </List.Item>
        </List>
      </Card>

      <Typography.Paragraph
        type="secondary"
        style={{ textAlign: 'center', fontSize: 12, marginTop: 16 }}
      >
        © OTOKI PowerSales Mobile
      </Typography.Paragraph>
    </>
  );
}
