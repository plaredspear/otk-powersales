import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useUsers } from '@/hooks/user/useUsers';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type { UserSummary } from '@/api/user';
import { fetchProfiles } from '@/api/admin/permission';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';

const PAGE_SIZE = 20;

const ACTIVE_OPTIONS = [
  { value: '', label: '활성 전체' },
  { value: 'true', label: '활성' },
  { value: 'false', label: '비활성' },
];

interface UserListAppliedFilters {
  keyword?: string;
  isActive?: boolean;
  profileId?: number;
}

export default function UserListPage() {
  const navigate = useNavigate();
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 applied 로 반영 (필터 변경만으로 조회하지 않음)
  const [keywordInput, setKeywordInput] = useState('');
  const [isActiveInput, setIsActiveInput] = useState('');
  const [profileIdInput, setProfileIdInput] = useState('');
  const [applied, setApplied] = useState<UserListAppliedFilters>({});
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(PAGE_SIZE);

  const { data, isLoading, isError, error, refetch, isFetching } = useUsers({
    ...applied,
    page,
    size,
  });

  const handleSearch = () => {
    setPage(0);
    setApplied({
      keyword: keywordInput || undefined,
      isActive: isActiveInput === '' ? undefined : isActiveInput === 'true',
      profileId: profileIdInput === '' ? undefined : Number(profileIdInput),
    });
  };

  const { data: profiles } = useQuery({
    queryKey: ['admin', 'permissions', 'profiles'],
    queryFn: fetchProfiles,
  });

  const profileOptions = [
    { value: '', label: '프로파일 전체' },
    ...(profiles ?? []).map((p) => ({ value: String(p.profileId), label: p.name })),
  ];

  const handleRowClick = useThrottleClick((id: number) => navigate(`/users/${id}`));

  const columns: ColumnsType<UserSummary> = [
    { title: '#', width: 60, render: (_v, _r, index) => page * size + index + 1 },
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
          value={isActiveInput}
          options={ACTIVE_OPTIONS}
          onChange={setIsActiveInput}
        />
        <Select
          style={{ width: 200 }}
          value={profileIdInput}
          options={profileOptions}
          onChange={setProfileIdInput}
        />
        <Input
          placeholder="username / 사번 / 이름 검색"
          allowClear
          style={{ width: 320 }}
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
        <Space style={{ marginLeft: 'auto' }}>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </Space>
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        pagination={buildListPagination({
          page: data?.page ?? page,
          pageSize: size,
          total: data?.totalElements ?? 0,
          onPageChange: setPage,
          onSizeChange: (nextSize) => {
            setSize(nextSize);
            setPage(0);
          },
        })}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.id),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
