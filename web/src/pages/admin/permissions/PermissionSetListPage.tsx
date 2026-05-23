import { useNavigate } from 'react-router-dom';
import { Alert, Card, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { usePermissionSets } from '@/hooks/admin/useAdminPermission';
import type { PermissionSetSummary } from '@/api/admin/permission';

const { Title } = Typography;

/**
 * Spec #803 — PermissionSet 일람 페이지.
 */
export default function PermissionSetListPage() {
  const navigate = useNavigate();
  const { data, isLoading, isError, error } = usePermissionSets();

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
        <Alert type="error" message="PermissionSet 일람 조회 실패" description={(error as Error)?.message} />
      </div>
    );
  }

  const renderBit = (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>);

  const columns: ColumnsType<PermissionSetSummary> = [
    { title: 'Name', dataIndex: 'name', key: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
    { title: 'Label', dataIndex: 'label', key: 'label' },
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: 'VIEW_ALL_DATA', dataIndex: 'viewAllData', key: 'viewAllData', render: renderBit, width: 130 },
    { title: 'MODIFY_ALL_DATA', dataIndex: 'modifyAllData', key: 'modifyAllData', render: renderBit, width: 140 },
    {
      title: 'Entity 수',
      dataIndex: 'objectPermissionCount',
      key: 'objectPermissionCount',
      width: 100,
      sorter: (a, b) => a.objectPermissionCount - b.objectPermissionCount,
    },
    {
      title: '부여 사용자 수',
      dataIndex: 'assignedUserCount',
      key: 'assignedUserCount',
      width: 120,
      sorter: (a, b) => a.assignedUserCount - b.assignedUserCount,
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <Title level={4}>PermissionSet 관리</Title>
      <Card>
        <Table<PermissionSetSummary>
          dataSource={data ?? []}
          rowKey="permissionSetId"
          columns={columns}
          pagination={{ pageSize: 50 }}
          size="small"
          onRow={(row) => ({ onClick: () => navigate(`/admin/permissions/permission-sets/${row.permissionSetId}`), style: { cursor: 'pointer' } })}
        />
      </Card>
    </div>
  );
}
