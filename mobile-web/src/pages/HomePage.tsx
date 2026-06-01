import { Alert, Badge, Card, List, Progress, Tag, Typography } from 'antd';
import {
  SoundOutlined,
  ReadOutlined,
  ShopOutlined,
  BarChartOutlined,
  WarningOutlined,
  CarOutlined,
  SafetyOutlined,
  FieldTimeOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchHome } from '@/api/home';
import { useAuthStore } from '@/stores/authStore';
import { LoadingState, ErrorState } from '@/components/PageStates';

interface Shortcut {
  to: string;
  label: string;
  icon: ReactNode;
  color: string;
}

const SHORTCUTS: Shortcut[] = [
  { to: '/notices', label: '공지', icon: <SoundOutlined />, color: '#1677ff' },
  { to: '/education', label: '교육', icon: <ReadOutlined />, color: '#52c41a' },
  { to: '/accounts', label: '거래처', icon: <ShopOutlined />, color: '#722ed1' },
  { to: '/sales', label: '매출', icon: <BarChartOutlined />, color: '#fa8c16' },
  { to: '/claims', label: '클레임', icon: <WarningOutlined />, color: '#f5222d' },
  { to: '/logistics-claims', label: '물류', icon: <CarOutlined />, color: '#13c2c2' },
  { to: '/safety-check', label: '안전점검', icon: <SafetyOutlined />, color: '#13a8a8' },
  { to: '/product-expiration', label: '유통기한', icon: <FieldTimeOutlined />, color: '#a0d911' },
];

export default function HomePage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const query = useQuery({ queryKey: ['home'], queryFn: fetchHome });

  const home = query.data;
  const attendance = home?.attendanceSummary;
  const attendancePct =
    attendance && attendance.totalCount > 0
      ? Math.round((attendance.registeredCount / attendance.totalCount) * 100)
      : 0;

  return (
    <div>
      <Card style={{ marginBottom: 12 }} styles={{ body: { padding: 16 } }}>
        <Typography.Text type="secondary">안녕하세요</Typography.Text>
        <Typography.Title level={4} style={{ margin: '4px 0 0' }}>
          {user?.name ?? '현장 사원'}님
        </Typography.Title>
        {user?.orgName && <Typography.Text type="secondary">{user.orgName}</Typography.Text>}
      </Card>

      {/* 바로가기 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10, marginBottom: 12 }}>
        {SHORTCUTS.map((s) => (
          <button
            key={s.to}
            onClick={() => navigate(s.to)}
            style={{
              border: 'none',
              background: '#fff',
              borderRadius: 12,
              padding: '14px 4px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 6,
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: 24, color: s.color }}>{s.icon}</span>
            <span style={{ fontSize: 11, color: '#333' }}>{s.label}</span>
          </button>
        ))}
      </div>

      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState onRetry={query.refetch} />}

      {home && (
        <>
          {home.safetyCheckRequired && (
            <Alert
              type="warning"
              showIcon
              message="오늘 안전점검이 필요합니다"
              action={
                <a onClick={() => navigate('/safety-check')} style={{ whiteSpace: 'nowrap' }}>
                  점검하기
                </a>
              }
              style={{ marginBottom: 12 }}
            />
          )}

          {home.expiryAlert && (
            <Alert
              type="info"
              showIcon
              message={`유통기한 임박 ${home.expiryAlert.expiryCount}건`}
              description={`${home.expiryAlert.branchName} · ${home.expiryAlert.employeeName}`}
              style={{ marginBottom: 12 }}
            />
          )}

          {attendance && (
            <Card size="small" title="오늘 출근 현황" style={{ marginBottom: 12 }}>
              <Typography.Text type="secondary">
                {attendance.registeredCount} / {attendance.totalCount} 등록
              </Typography.Text>
              <Progress percent={attendancePct} size="small" />
            </Card>
          )}

          <Card size="small" title="오늘 일정" style={{ marginBottom: 12 }}>
            <List
              dataSource={home.todaySchedules}
              locale={{ emptyText: '오늘 일정이 없습니다' }}
              renderItem={(s) => (
                <List.Item>
                  <List.Item.Meta
                    title={s.accountName ?? s.employeeName}
                    description={
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {s.workCategory}
                        {s.workType ? ` · ${s.workType}` : ''}
                      </Typography.Text>
                    }
                  />
                  <Badge
                    status={s.isCommuteRegistered ? 'success' : 'default'}
                    text={s.isCommuteRegistered ? '출근' : '미출근'}
                  />
                </List.Item>
              )}
            />
          </Card>

          {home.notices.length > 0 && (
            <Card size="small" title="공지">
              <List
                dataSource={home.notices}
                renderItem={(n) => (
                  <List.Item style={{ cursor: 'pointer' }} onClick={() => navigate(`/notices/${n.id}`)}>
                    <List.Item.Meta
                      title={
                        <Typography.Text ellipsis style={{ maxWidth: '100%' }}>
                          {n.title}
                        </Typography.Text>
                      }
                      description={<Tag>{n.categoryName}</Tag>}
                    />
                  </List.Item>
                )}
              />
            </Card>
          )}
        </>
      )}
    </div>
  );
}
