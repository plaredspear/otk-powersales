import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Input, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useUsers } from '@/hooks/user/useUsers';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type { UserSummary } from '@/api/user';

const PAGE_SIZE = 20;

const ACTIVE_OPTIONS = [
  { value: '', label: '활성 전체' },
  { value: 'true', label: '활성' },
  { value: 'false', label: '비활성' },
];

export default function UserListPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState<string | undefined>();
  const [isActive, setIsActive] = useState<boolean | undefined>();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useUsers({
    keyword,
    isActive,
    page,
    size: PAGE_SIZE,
  });

  const handleRowClick = useThrottleClick((id: number) => navigate(`/users/${id}`));

  const columns: ColumnsType<UserSummary> = [
    { title: '#', width: 60, render: (_v, _r, index) => page * PAGE_SIZE + index + 1 },
    { title: 'username', dataIndex: 'username', width: 220 },
    { title: '사번', dataIndex: 'employeeCode', width: 120 },
    { title: '이름', dataIndex: 'name', width: 120, render: (val: string | null) => val ?? '-' },
    { title: 'Profile', dataIndex: 'profileName', width: 120, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '지점', dataIndex: 'branch', width: 140, render: (val: string | null) => val ?? '-' },
    { title: '부서', dataIndex: 'department', width: 140, render: (val: string | null) => val ?? '-' },
    {
      title: '활성',
      dataIndex: 'isActive',
      width: 80,
      align: 'center',
      render: (val: boolean) => (val ? <Tag color="blue">활성</Tag> : <Tag>비활성</Tag>),
    },
    {
      title: '마지막 로그인',
      dataIndex: 'lastLoginAt',
      width: 160,
      render: (val: string | null) => (val ? val.substring(0, 16).replace('T', ' ') : '-'),
    },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="사용자 목록을 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={<Button onClick={() => refetch()}>재시도</Button>}
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 140 }}
          value={isActive === undefined ? '' : String(isActive)}
          options={ACTIVE_OPTIONS}
          onChange={(val) => {
            setIsActive(val === '' ? undefined : val === 'true');
            setPage(0);
          }}
        />
        <Input.Search
          placeholder="username / 사번 / 이름 검색"
          allowClear
          style={{ width: 320 }}
          onSearch={(val) => {
            setKeyword(val || undefined);
            setPage(0);
          }}
        />
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.id),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
