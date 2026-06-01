import { Button, Card, List, Space, Tag, Typography } from 'antd';
import { CalendarOutlined, PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchTeamMembers } from '@/api/leader';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';

/** 여사원 목록 (레거시 employee/main) — 조장 전용. 일별현황·일정추가 진입점 포함. */
export default function LeaderTeamMembersPage() {
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['leader-members'], queryFn: fetchTeamMembers });

  return (
    <>
      <DetailHeader title="여사원 관리" />
      <Space style={{ width: '100%', marginBottom: 12 }}>
        <Button icon={<CalendarOutlined />} onClick={() => navigate('/leader/daily-status')}>
          일별 현황
        </Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/leader/schedule/new')}>
          일정 추가
        </Button>
      </Space>
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.length === 0}
        emptyDescription="배정된 여사원이 없습니다"
      >
        {(members) => (
          <List
            dataSource={members}
            split={false}
            renderItem={(m) => (
              <Card size="small" style={{ marginBottom: 10 }} styles={{ body: { padding: 14 } }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <div>
                    <Typography.Text strong>{m.name}</Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12, marginLeft: 6 }}>
                      {m.employeeCode}
                    </Typography.Text>
                  </div>
                  {m.status && <Tag>{m.status}</Tag>}
                </div>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
