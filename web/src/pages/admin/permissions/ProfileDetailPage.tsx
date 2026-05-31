import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Input, Space, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useProfile } from '@/hooks/admin/useAdminPermission';
import type { AssignedUserSummary } from '@/api/admin/permission';

const { Title } = Typography;

const PAGE_SIZE = 20;

/**
 * Spec #803 — Profile 상세 페이지.
 *
 * 메타 + 시스템 권한 비트 + 부여된 사용자 일람 (paginate + 검색).
 */
export default function ProfileDetailPage() {
  const { profileId: rawId } = useParams<{ profileId: string }>();
  const profileId = rawId ? Number(rawId) : undefined;
  const navigate = useNavigate();
  const [userPage, setUserPage] = useState(0);
  const [userKeyword, setUserKeyword] = useState<string | undefined>(undefined);

  const { hasSystemPermission } = usePermission();
  const canEdit = hasSystemPermission('MANAGE_USERS');

  const { data, isLoading, isError, error } = useProfile(profileId, {
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
          message="Profile 상세 조회 실패"
          description={(error as Error)?.message ?? 'Profile 을 찾을 수 없습니다'}
          action={<Button onClick={() => navigate('/admin/permissions/profiles')}>목록으로</Button>}
        />
      </div>
    );
  }

  const renderBit = (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>);

  const userColumns: ColumnsType<AssignedUserSummary> = [
    { title: '사번', dataIndex: 'employeeCode', key: 'employeeCode', width: 160 },
    { title: '이름', dataIndex: 'employeeName', key: 'employeeName' },
    { title: 'Username', dataIndex: 'username', key: 'username' },
  ];

  const crudColumns = <T extends ObjectPermissionRow | CustomPermissionRow>(): ColumnsType<T> => [
    { title: 'READ', dataIndex: 'canRead', key: 'canRead', width: 80, align: 'center', render: (v: boolean) => renderBit(v) },
    { title: 'CREATE', dataIndex: 'canCreate', key: 'canCreate', width: 80, align: 'center', render: (v: boolean) => renderBit(v) },
    { title: 'EDIT', dataIndex: 'canEdit', key: 'canEdit', width: 80, align: 'center', render: (v: boolean) => renderBit(v) },
    { title: 'DELETE', dataIndex: 'canDelete', key: 'canDelete', width: 80, align: 'center', render: (v: boolean) => renderBit(v) },
  ];

  const objectColumns: ColumnsType<ObjectPermissionRow> = [
    { title: '자원', key: 'resource', render: (_, r) => r.entity ?? r.sfApiName },
    ...crudColumns<ObjectPermissionRow>(),
  ];

  const customColumns: ColumnsType<CustomPermissionRow> = [
    { title: '자원', dataIndex: 'resource', key: 'resource' },
    ...crudColumns<CustomPermissionRow>(),
  ];

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Button onClick={() => navigate('/admin/permissions/profiles')}>← 목록으로</Button>
        {canEdit && (
          <Button type="primary" onClick={() => navigate(`/admin/permissions/profiles/${profileId}/edit`)}>
            권한 편집
          </Button>
        )}
      </Space>

      <Title level={4}>
        {data.name}
        {data.isLocallyModified && (
          <Tag color="orange" style={{ marginLeft: 12 }}>
            ⚠️ 신규 시스템에서 수정됨
          </Tag>
        )}
      </Title>

      <Card title="메타" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="Profile ID">{data.profileId}</Descriptions.Item>
          <Descriptions.Item label="Name">{data.name}</Descriptions.Item>
          <Descriptions.Item label="User Type">{data.userType ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="Description" span={2}>
            {data.description ?? '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="시스템 권한 비트" style={{ marginBottom: 12 }}>
        <Descriptions column={5} bordered size="small">
          <Descriptions.Item label="VIEW_ALL_DATA">{renderBit(data.flags.viewAllData)}</Descriptions.Item>
          <Descriptions.Item label="MODIFY_ALL_DATA">{renderBit(data.flags.modifyAllData)}</Descriptions.Item>
          <Descriptions.Item label="VIEW_ALL_USERS">{renderBit(data.flags.viewAllUsers)}</Descriptions.Item>
          <Descriptions.Item label="MANAGE_USERS">{renderBit(data.flags.manageUsers)}</Descriptions.Item>
          <Descriptions.Item label="API_ENABLED">{renderBit(data.flags.apiEnabled)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={`Object Permissions (${data.objectPermissions.length})`} style={{ marginBottom: 12 }}>
        <Table<ObjectPermissionRow>
          dataSource={data.objectPermissions}
          rowKey="sfApiName"
          columns={objectColumns}
          size="small"
          pagination={false}
          locale={{ emptyText: '부여된 객체권한 없음' }}
        />
      </Card>

      {data.customPermissions.length > 0 && (
        <Card title={`Custom Permissions (${data.customPermissions.length})`} style={{ marginBottom: 12 }}>
          <Table<CustomPermissionRow>
            dataSource={data.customPermissions}
            rowKey="resource"
            columns={customColumns}
            size="small"
            pagination={false}
          />
        </Card>
      )}

      <Card title={`부여된 사용자 (${data.assignedUsers.totalElements})`}>
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
        <Table<AssignedUserSummary>
          dataSource={data.assignedUsers.content}
          rowKey="userId"
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
    </div>
  );
}
