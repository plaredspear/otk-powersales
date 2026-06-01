import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Input, Space, Spin, Tabs, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { usePermissionSet } from '@/hooks/admin/useAdminPermission';
import { usePermission } from '@/hooks/usePermission';
import { useAuthStore } from '@/stores/authStore';
import type {
  AssignedPermissionSetUserSummary,
  CustomPermissionRow,
  ObjectPermissionRow,
} from '@/api/admin/permission';
import AddUserToPermissionSetModal from './components/AddUserToPermissionSetModal';
import PermissionSetChangeLogTab from './components/PermissionSetChangeLogTab';
import PermissionSetDeleteConfirmModal from './components/PermissionSetDeleteConfirmModal';
import RevokeAssignmentConfirmModal from './components/RevokeAssignmentConfirmModal';
import ResizableTable from '@/components/common/ResizableTable';

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
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);

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

  const customColumns: ColumnsType<CustomPermissionRow> = [
    { title: '자원', dataIndex: 'resource', key: 'resource' },
    { title: 'READ', dataIndex: 'canRead', key: 'canRead', render: renderBit, width: 80 },
    { title: 'CREATE', dataIndex: 'canCreate', key: 'canCreate', render: renderBit, width: 80 },
    { title: 'EDIT', dataIndex: 'canEdit', key: 'canEdit', render: renderBit, width: 80 },
    { title: 'DELETE', dataIndex: 'canDelete', key: 'canDelete', render: renderDeleteBit, width: 80 },
  ];

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Button onClick={() => navigate('/admin/permissions/permission-sets')}>← 목록으로</Button>
        {canManage && (
          <Space>
            <Button type="primary" onClick={() => navigate(`/admin/permissions/permission-sets/${permissionSetId}/edit`)}>
              편집
            </Button>
            <Tooltip title={data.sfOrigin ? 'SF 출처 PermissionSet 은 삭제할 수 없습니다 (Stage1 재적재 정합 보호)' : undefined}>
              <Button danger disabled={data.sfOrigin} onClick={() => setDeleteModalOpen(true)}>
                삭제
              </Button>
            </Tooltip>
          </Space>
        )}
      </Space>

      <Title level={4}>
        {data.label ?? data.name}
        {data.isLocallyModified && (
          <Tag color="orange" style={{ marginLeft: 12 }}>⚠️ 신규 시스템에서 수정됨</Tag>
        )}
        {data.sfOrigin && <Tag color="blue" style={{ marginLeft: 4 }}>SF 출처</Tag>}
      </Title>

      <Card title="메타" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="ID">{data.permissionSetId}</Descriptions.Item>
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
        <ResizableTable<ObjectPermissionRow>
          dataSource={data.objectPermissions}
          rowKey="sfApiName"
          columns={objectColumns}
          size="small"
          pagination={false}
        />
      </Card>

      {data.customPermissions.length > 0 && (
        <Card title={`Custom Permissions (${data.customPermissions.length} 자원)`} style={{ marginBottom: 12 }}>
          <ResizableTable<CustomPermissionRow>
            dataSource={data.customPermissions}
            rowKey="resource"
            columns={customColumns}
            size="small"
            pagination={false}
          />
        </Card>
      )}

      <Tabs
        defaultActiveKey="users"
        items={[
          {
            key: 'users',
            label: `부여된 사용자 (${data.assignedUsers.totalElements})`,
            children: (
              <>
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
                  {canManage && data.flags?.permissionSetFlagsId && (
                    <Button type="primary" size="small" onClick={() => setAddUserModalOpen(true)}>
                      사용자 추가
                    </Button>
                  )}
                </Space>
                <ResizableTable<AssignedPermissionSetUserSummary>
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
              </>
            ),
          },
          {
            key: 'change-log',
            label: '변경 이력',
            children: permissionSetId ? <PermissionSetChangeLogTab permissionSetId={permissionSetId} /> : null,
          },
        ]}
      />

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

      {permissionSetId && (
        <PermissionSetDeleteConfirmModal
          open={deleteModalOpen}
          permissionSetId={permissionSetId}
          permissionSetName={data.name}
          permissionSetLabel={data.label ?? data.name}
          assignedUserCount={data.assignedUsers.totalElements}
          onClose={() => setDeleteModalOpen(false)}
        />
      )}
    </div>
  );
}
