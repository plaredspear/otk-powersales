import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Input, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { usePermissionSet } from '@/hooks/admin/useAdminPermission';
import type { AssignedUserSummary, ObjectPermissionRow } from '@/api/admin/permission';

const { Title } = Typography;

const PAGE_SIZE = 20;

/**
 * Spec #803 — PermissionSet 상세 페이지.
 *
 * 메타 + 시스템 권한 비트 + entity×CRUD 매트릭스 + 부여 사용자 일람.
 */
export default function PermissionSetDetailPage() {
  const { permissionSetId: rawId } = useParams<{ permissionSetId: string }>();
  const permissionSetId = rawId ? Number(rawId) : undefined;
  const navigate = useNavigate();
  const [userPage, setUserPage] = useState(0);
  const [userKeyword, setUserKeyword] = useState<string | undefined>(undefined);

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

  const userColumns: ColumnsType<AssignedUserSummary> = [
    { title: '사번', dataIndex: 'employeeCode', key: 'employeeCode', width: 160 },
    { title: '이름', dataIndex: 'employeeName', key: 'employeeName' },
    { title: 'Username', dataIndex: 'username', key: 'username' },
  ];

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
