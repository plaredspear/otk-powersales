import { useNavigate } from 'react-router-dom';
import { Alert, Card, Space, Spin, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useProfiles } from '@/hooks/admin/useAdminPermission';
import type { ProfileSummary } from '@/api/admin/permission';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Title } = Typography;

/**
 * Spec #803 — Profile 일람 페이지.
 *
 * 시스템 권한 비트 5종 + 부여된 user 수 표시. 행 클릭으로 상세 페이지 진입.
 */
export default function ProfileListPage() {
  const navigate = useNavigate();
  const { data, isLoading, isError, error, refetch, isFetching } = useProfiles();

  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert type="error" message="Profile 일람 조회 실패" description={(error as Error)?.message} />
      </div>
    );
  }

  const renderBit = (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>);

  const columns: ColumnsType<ProfileSummary> = [
    { title: 'Name', dataIndex: 'name', key: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
    { title: 'User Type', dataIndex: 'userType', key: 'userType' },
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: 'VIEW_ALL_DATA', dataIndex: 'viewAllData', key: 'viewAllData', render: renderBit, width: 130 },
    { title: 'MODIFY_ALL_DATA', dataIndex: 'modifyAllData', key: 'modifyAllData', render: renderBit, width: 140 },
    { title: 'VIEW_ALL_USERS', dataIndex: 'viewAllUsers', key: 'viewAllUsers', render: renderBit, width: 130 },
    { title: 'MANAGE_USERS', dataIndex: 'manageUsers', key: 'manageUsers', render: renderBit, width: 120 },
    { title: 'API_ENABLED', dataIndex: 'apiEnabled', key: 'apiEnabled', render: renderBit, width: 110 },
    {
      title: '부여 사용자 수',
      dataIndex: 'assignedUserCount',
      key: 'assignedUserCount',
      width: 120,
      sorter: (a, b) => a.assignedUserCount - b.assignedUserCount,
    },
  ];

  const totalAssigned = (data ?? []).reduce((sum, p) => sum + p.assignedUserCount, 0);

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Title level={4} style={{ margin: 0 }}>Profile 관리</Title>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
      </Space>
      <Card>
        <ResizableTable<ProfileSummary>
          dataSource={data ?? []}
          rowKey="profileId"
          columns={columns}
          pagination={false}
          size="small"
          onRow={(row) => ({ onClick: () => navigate(`/admin/permissions/profiles/${row.profileId}`), style: { cursor: 'pointer' } })}
          footer={() => `총 부여 사용자 수: ${totalAssigned}`}
        />
      </Card>
    </div>
  );
}
