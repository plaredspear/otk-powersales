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

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate('/admin/permissions/profiles')}>← 목록으로</Button>
      </Space>

      <Title level={4}>{data.name}</Title>

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
