import { useState } from 'react';
import { Alert, Button, Card, Descriptions, Space, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { fetchEmployeePermissions, type AssignedPermissionSet, type EntityPermissionRow } from '@/api/employee';
import { usePermission } from '@/hooks/usePermission';
import { useAuthStore } from '@/stores/authStore';
import AddPermissionSetAssignmentModal from '@/pages/admin/permissions/components/AddPermissionSetAssignmentModal';
import RevokeAssignmentConfirmModal from '@/pages/admin/permissions/components/RevokeAssignmentConfirmModal';

const { Text } = Typography;

interface Props {
  employeeId: number;
}

interface RevokeTarget {
  assignmentId: number;
  permissionSetLabel: string;
  isSelfManageUsers: boolean;
}

/**
 * Spec #802 — 직원 SF 권한 read-only section.
 * Spec #804 — MANAGE_USERS 보유 운영자는 부여/회수 가능.
 *
 * SF org 분리 정책 (spec #803/804 Q1 옵션 1) — web admin 이 부여 SoT.
 */
export default function SfPermissionSection({ employeeId }: Props) {
  const { hasSystemPermission } = usePermission();
  const currentUserId = useAuthStore((s) => s.user?.id);
  const canManage = hasSystemPermission('MANAGE_USERS');

  const [addModalOpen, setAddModalOpen] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState<RevokeTarget | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['employee', employeeId, 'permissions'],
    queryFn: () => fetchEmployeePermissions(employeeId),
  });

  if (isLoading) {
    return (
      <Card title="SF 권한" style={{ marginBottom: 12 }}>
        <Spin />
      </Card>
    );
  }

  if (isError) {
    return (
      <Card title="SF 권한" style={{ marginBottom: 12 }}>
        <Alert type="error" message="권한 조회 실패" description={(error as Error)?.message} />
      </Card>
    );
  }

  if (!data) return null;

  const isOwnEmployee = !!data.userId && data.userId === currentUserId;

  const permissionSetColumns: ColumnsType<AssignedPermissionSet> = [
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
  ];

  if (canManage) {
    permissionSetColumns.push({
      title: '액션',
      key: 'action',
      width: 100,
      render: (_, row) => {
        const selfMU = isOwnEmployee && row.modifyAllData;
        return (
          <Button
            danger
            size="small"
            disabled={selfMU}
            title={selfMU ? '자기 자신의 MODIFY_ALL_DATA 부여는 회수할 수 없습니다' : undefined}
            onClick={() =>
              setRevokeTarget({
                assignmentId: row.assignmentId,
                permissionSetLabel: row.permissionSetName,
                isSelfManageUsers: selfMU,
              })
            }
          >
            회수
          </Button>
        );
      },
    });
  }

  const entityColumns: ColumnsType<EntityPermissionRow> = [
    { title: 'Entity', dataIndex: 'entity', key: 'entity', width: 240 },
    { title: 'READ', dataIndex: 'canRead', key: 'canRead', render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>) },
    { title: 'CREATE', dataIndex: 'canCreate', key: 'canCreate', render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>) },
    { title: 'EDIT', dataIndex: 'canEdit', key: 'canEdit', render: (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>) },
    { title: 'DELETE', dataIndex: 'canDelete', key: 'canDelete', render: (v: boolean) => (v ? <Tag color="red">✓</Tag> : <Tag>-</Tag>) },
  ];

  const userLabel = data.username || `User #${data.userId}`;

  return (
    <Card
      title="SF 권한"
      style={{ marginBottom: 12 }}
      extra={
        canManage && data.userId ? (
          <Button type="primary" size="small" onClick={() => setAddModalOpen(true)}>
            PermissionSet 부여 추가
          </Button>
        ) : null
      }
    >
      <Alert
        type="info"
        showIcon
        message={
          canManage
            ? 'web admin 이 부여 SoT 입니다. 부여/회수는 다음 로그인 시점부터 사용자에게 반영됩니다.'
            : 'SF 권한은 운영자 (MANAGE_USERS) 만 부여/회수 가능합니다. 본 화면은 read-only 표시입니다.'
        }
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

      <Space style={{ marginTop: 8, marginBottom: 8 }}>
        <Text strong>부여된 PermissionSet ({data.permissionSets.length}개)</Text>
      </Space>
      {data.permissionSets.length === 0 ? (
        <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          부여된 PermissionSet 이 없습니다.
        </Text>
      ) : (
        <Table<AssignedPermissionSet>
          dataSource={data.permissionSets}
          rowKey="assignmentId"
          pagination={false}
          size="small"
          style={{ marginBottom: 16 }}
          columns={permissionSetColumns}
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

      {data.userId && (
        <AddPermissionSetAssignmentModal
          open={addModalOpen}
          userId={data.userId}
          userLabel={userLabel}
          onClose={() => setAddModalOpen(false)}
        />
      )}

      {revokeTarget && (
        <RevokeAssignmentConfirmModal
          open={!!revokeTarget}
          assignmentId={revokeTarget.assignmentId}
          permissionSetLabel={revokeTarget.permissionSetLabel}
          userLabel={userLabel}
          onClose={() => setRevokeTarget(null)}
        />
      )}
    </Card>
  );
}
