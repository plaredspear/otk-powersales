import { Alert, Card, Descriptions, Spin, Table, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { fetchEmployeePermissions, type EntityPermissionRow } from '@/api/employee';

const { Text } = Typography;

interface Props {
  employeeId: number;
}

/**
 * Spec #802 — 직원 SF 권한 read-only section (Q4 옵션 1).
 *
 * SF org 의 부여 SoT 결과 (Profile + PermissionSet + entity × CRUD 매트릭스 + 시스템 권한) 를 표시.
 * 본 컴포넌트는 표시 전용 — 부여/회수 액션은 SF org 에서만 가능.
 */
export default function SfPermissionSection({ employeeId }: Props) {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['employee', employeeId, 'permissions'],
    queryFn: () => fetchEmployeePermissions(employeeId),
  });

  if (isLoading) {
    return (
      <Card title="SF 권한 (read-only)" style={{ marginBottom: 12 }}>
        <Spin />
      </Card>
    );
  }

  if (isError) {
    return (
      <Card title="SF 권한 (read-only)" style={{ marginBottom: 12 }}>
        <Alert type="error" message="권한 조회 실패" description={(error as Error)?.message} />
      </Card>
    );
  }

  if (!data) return null;

  const entityColumns = [
    { title: 'Entity', dataIndex: 'entity', key: 'entity', width: 240 },
    {
      title: 'READ',
      dataIndex: 'canRead',
      key: 'canRead',
      render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>),
    },
    {
      title: 'CREATE',
      dataIndex: 'canCreate',
      key: 'canCreate',
      render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>),
    },
    {
      title: 'EDIT',
      dataIndex: 'canEdit',
      key: 'canEdit',
      render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>),
    },
    {
      title: 'DELETE',
      dataIndex: 'canDelete',
      key: 'canDelete',
      render: (v: boolean) => (v ? <Tag color="red">✓</Tag> : <Tag>-</Tag>),
    },
  ];

  return (
    <Card title="SF 권한 (read-only)" style={{ marginBottom: 12 }}>
      <Alert
        type="info"
        showIcon
        message="SF org 의 PermissionSet 부여가 SoT 입니다. 본 화면은 운영 진단용 read-only 표시이며, 부여/회수는 SF org 에서만 가능합니다."
        style={{ marginBottom: 16 }}
      />

      {data.profile && (
        <>
          <Text strong>Profile 시스템 권한</Text>
          <Descriptions column={3} bordered size="small" style={{ marginTop: 8, marginBottom: 16 }}>
            <Descriptions.Item label="Profile 명">{data.profile.profileName}</Descriptions.Item>
            <Descriptions.Item label="View All Data">{data.profile.viewAllData ? '✓' : '-'}</Descriptions.Item>
            <Descriptions.Item label="Modify All Data">{data.profile.modifyAllData ? '✓' : '-'}</Descriptions.Item>
            <Descriptions.Item label="View All Users">{data.profile.viewAllUsers ? '✓' : '-'}</Descriptions.Item>
            <Descriptions.Item label="Manage Users">{data.profile.manageUsers ? '✓' : '-'}</Descriptions.Item>
            <Descriptions.Item label="API Enabled">{data.profile.apiEnabled ? '✓' : '-'}</Descriptions.Item>
          </Descriptions>
        </>
      )}

      <Text strong>부여된 PermissionSet ({data.permissionSets.length}개)</Text>
      {data.permissionSets.length === 0 ? (
        <Text type="secondary" style={{ display: 'block', marginTop: 8, marginBottom: 16 }}>
          부여된 PermissionSet 이 없습니다.
        </Text>
      ) : (
        <Table
          dataSource={data.permissionSets}
          rowKey="permissionSetSfid"
          pagination={false}
          size="small"
          style={{ marginTop: 8, marginBottom: 16 }}
          columns={[
            { title: 'Name', dataIndex: 'permissionSetName', key: 'name' },
            { title: 'SFID', dataIndex: 'permissionSetSfid', key: 'sfid', width: 200 },
            {
              title: 'View All Data',
              dataIndex: 'viewAllData',
              key: 'viewAllData',
              render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>),
            },
            {
              title: 'Modify All Data',
              dataIndex: 'modifyAllData',
              key: 'modifyAllData',
              render: (v: boolean) => (v ? <Tag color="red">✓</Tag> : <Tag>-</Tag>),
            },
          ]}
        />
      )}

      <Text strong>실효 시스템 권한</Text>
      <div style={{ marginTop: 8, marginBottom: 16 }}>
        {data.systemPermissions.length === 0 ? (
          <Text type="secondary">실효 시스템 권한이 없습니다.</Text>
        ) : (
          data.systemPermissions.map((p) => <Tag key={p} color="blue">{p}</Tag>)
        )}
      </div>

      <Text strong>실효 Entity × CRUD 매트릭스 ({data.entityMatrix.length} entity)</Text>
      {data.entityMatrix.length === 0 ? (
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          부여된 entity 권한이 없습니다.
        </Text>
      ) : (
        <Table<EntityPermissionRow>
          dataSource={data.entityMatrix}
          rowKey="entity"
          pagination={false}
          size="small"
          style={{ marginTop: 8 }}
          columns={entityColumns}
        />
      )}
    </Card>
  );
}
