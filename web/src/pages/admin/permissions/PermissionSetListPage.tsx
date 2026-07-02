import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Space, Spin, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { usePermissionSets } from '@/hooks/admin/useAdminPermission';
import { usePermission } from '@/hooks/usePermission';
import type { PermissionSetSummary } from '@/api/admin/permission';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';

const { Title } = Typography;

/**
 * Spec #803 — PermissionSet 일람 페이지.
 * Spec #837 — 신규 PS 등록 버튼 + 출처 (SF/신규) + dirty 플래그 컬럼 추가.
 */
export default function PermissionSetListPage() {
  const navigate = useNavigate();
  const { data, isLoading, isError, error, refetch, isFetching } = usePermissionSets();
  const { hasSystemPermission } = usePermission();
  const canManage = hasSystemPermission('MANAGE_USERS');

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
    {
      title: '출처',
      dataIndex: 'sfOrigin',
      key: 'sfOrigin',
      width: 110,
      filters: [
        { text: 'SF 출처', value: true },
        { text: '신규', value: false },
      ],
      onFilter: (val, row) => row.sfOrigin === val,
      render: (v: boolean) => (v ? <Tag color="blue">SF 출처</Tag> : <Tag color="purple">신규</Tag>),
    },
    {
      title: '로컬 수정',
      dataIndex: 'isLocallyModified',
      key: 'isLocallyModified',
      width: 110,
      filters: [{ text: '⚠️ 로컬 수정', value: true }],
      onFilter: (val, row) => row.isLocallyModified === val,
      render: (v: boolean) => (v ? <Tag color="orange">⚠️ 로컬 수정</Tag> : null),
    },
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
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Title level={4} style={{ margin: 0 }}>PermissionSet 관리</Title>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          {canManage && (
            <Button type="primary" onClick={() => navigate('/admin/permissions/permission-sets/new')}>
              + 신규 PS 등록
            </Button>
          )}
        </Space>
      </Space>
      <Card>
        <ResizableTable<PermissionSetSummary>
          dataSource={data ?? []}
          rowKey="permissionSetId"
          columns={columns}
          pagination={{ pageSize: 50 }}
          locale={listTableLocale()}
          size="small"
          onRow={(row) => ({
            onClick: () => navigate(`/admin/permissions/permission-sets/${row.permissionSetId}`),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>
    </div>
  );
}
