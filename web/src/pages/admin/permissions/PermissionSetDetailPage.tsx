import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Input, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { usePermissionSet } from '@/hooks/admin/useAdminPermission';
import { usePermission } from '@/hooks/usePermission';
import { useAuthStore } from '@/stores/authStore';
import type { AssignedPermissionSetUserSummary, ObjectPermissionRow } from '@/api/admin/permission';
import AddUserToPermissionSetModal from './components/AddUserToPermissionSetModal';
import RevokeAssignmentConfirmModal from './components/RevokeAssignmentConfirmModal';

const { Title } = Typography;

const PAGE_SIZE = 20;

interface RevokeTarget {
  assignmentId: number;
  userLabel: string;
  isSelf: boolean;
}

/**
 * Spec #803 — PermissionSet 상세 페이지 (조회).
 * Spec #804 — MANAGE_USERS 보유 시 사용자 부여/회수 액션 추가.
 */
export default function PermissionSetDetailPage() {
  const { permissionSetId: rawId } = useParams<{ permissionSetId: string }>();
  const permissionSetId = rawId ? Number(rawId) : undefined;
  const navigate = useNavigate();
  const [userPage, setUserPage] = useState(0);
  const [userKeyword, setUserKeyword] = useState<string | undefined>(undefined);
  const [addUserModalOpen, setAddUserModalOpen] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState<RevokeTarget | null>(null);

  const { hasSystemPermission } = usePermission();
  const currentUserId = useAuthStore((s) => s.user?.id);
  const canManage = hasSystemPermission('MANAGE_USERS');

  const { data, isLoading, isError, error } = usePermissionSet(permissionSetId, {
    userPage,
    userSize: PAGE_SIZE,
    userKeyword,
  });

  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="PermissionSet 상세 조회 실패"
          description={(error as Error)?.message ?? 'PermissionSet 을 찾을 수 없습니다'}
          action={<Button onClick={() => navigate('/admin/permissions/permission-sets')}>목록으로</Button>}
        />
      </div>
    );
  }

  const renderBit = (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>);
  const renderDeleteBit = (v: boolean) => (v ? <Tag color="red">✓</Tag> : <Tag>-</Tag>);

  const objectColumns: ColumnsType<ObjectPermissionRow> = [
    {
      title: 'Entity',
      key: 'entity',
      render: (_, row) =>
        row.entity ? (
          row.entity
        ) : (
          <Tooltip title={`SF API name '${row.sfApiName}' 가 신규 시스템 entity 매핑에 없습니다`}>
            <Tag>(unresolved)</Tag>
          </Tooltip>
        ),
    },
    { title: 'SF API Name', dataIndex: 'sfApiName', key: 'sfApiName' },
    { title: 'READ', dataIndex: 'canRead', key: 'canRead', render: renderBit, width: 80 },
    { title: 'CREATE', dataIndex: 'canCreate', key: 'canCreate', render: renderBit, width: 80 },
    { title: 'EDIT', dataIndex: 'canEdit', key: 'canEdit', render: renderBit, width: 80 },
    { title: 'DELETE', dataIndex: 'canDelete', key: 'canDelete', render: renderDeleteBit, width: 80 },
  ];

  const userColumns: ColumnsType<AssignedPermissionSetUserSummary> = [
    { title: '사번', dataIndex: 'employeeCode', key: 'employeeCode', width: 160 },
    { title: '이름', dataIndex: 'employeeName', key: 'employeeName' },
    { title: 'Username', dataIndex: 'username', key: 'username' },
  ];

  if (canManage) {
    userColumns.push({
      title: '액션',
      key: 'action',
      width: 100,
      render: (_, row) => {
        const isSelfMU = row.userId === currentUserId && (data.flags?.modifyAllData ?? false);
        return (
          <Button
            danger
            size="small"
            disabled={isSelfMU}
            title={isSelfMU ? '자기 자신의 MODIFY_ALL_DATA 부여는 회수할 수 없습니다' : undefined}
            onClick={() =>
              setRevokeTarget({
                assignmentId: row.assignmentId,
                userLabel: row.employeeName ?? row.username,
                isSelf: isSelfMU,
              })
            }
          >
            회수
          </Button>
        );
      },
    });
  }

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate('/admin/permissions/permission-sets')}>← 목록으로</Button>
      </Space>

      <Title level={4}>{data.label ?? data.name}</Title>

      <Card title="메타" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="ID">{data.permissionSetId}</Descriptions.Item>
          <Descriptions.Item label="SFID">{data.sfid ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Name">{data.name}</Descriptions.Item>
          <Descriptions.Item label="Label">{data.label ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Description" span={2}>
            {data.description ?? '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="시스템 권한 비트" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="VIEW_ALL_DATA">{renderBit(data.flags?.viewAllData ?? false)}</Descriptions.Item>
          <Descriptions.Item label="MODIFY_ALL_DATA">{renderBit(data.flags?.modifyAllData ?? false)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={`Entity × CRUD 매트릭스 (${data.objectPermissions.length} entity)`} style={{ marginBottom: 12 }}>
        <Table<ObjectPermissionRow>
          dataSource={data.objectPermissions}
          rowKey="sfApiName"
          columns={objectColumns}
          size="small"
          pagination={false}
        />
      </Card>

      <Card
        title={`부여된 사용자 (${data.assignedUsers.totalElements})`}
        extra={
          canManage && data.flags?.permissionSetFlagsId ? (
            <Button type="primary" size="small" onClick={() => setAddUserModalOpen(true)}>
              사용자 추가
            </Button>
          ) : null
        }
      >
        <Space style={{ marginBottom: 12 }}>
          <Input.Search
            placeholder="사번/이름 검색"
            allowClear
            onSearch={(value) => {
              setUserKeyword(value || undefined);
              setUserPage(0);
            }}
            style={{ width: 240 }}
          />
        </Space>
        <Table<AssignedPermissionSetUserSummary>
          dataSource={data.assignedUsers.content}
          rowKey="assignmentId"
          columns={userColumns}
          size="small"
          pagination={{
            current: userPage + 1,
            pageSize: PAGE_SIZE,
            total: data.assignedUsers.totalElements,
            onChange: (p) => setUserPage(p - 1),
          }}
        />
      </Card>

      {data.flags?.permissionSetFlagsId && (
        <AddUserToPermissionSetModal
          open={addUserModalOpen}
          permissionSetFlagsId={data.flags.permissionSetFlagsId}
          permissionSetLabel={data.label ?? data.name}
          onClose={() => setAddUserModalOpen(false)}
        />
      )}

      {revokeTarget && (
        <RevokeAssignmentConfirmModal
          open={!!revokeTarget}
          assignmentId={revokeTarget.assignmentId}
          permissionSetLabel={data.label ?? data.name}
          userLabel={revokeTarget.userLabel}
          onClose={() => setRevokeTarget(null)}
        />
      )}
    </div>
  );
}
