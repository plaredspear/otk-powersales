import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useUsers } from '@/hooks/user/useUsers';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import type { UserSummary } from '@/api/user';
import { fetchUserProfileOptions } from '@/api/user';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

const ACTIVE_OPTIONS = [
  { value: '', label: '활성 전체' },
  { value: 'true', label: '활성' },
  { value: 'false', label: '비활성' },
];

export default function UserListPage() {
  const navigate = useNavigate();
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소를
  // 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/size/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  // boolean/number 필터는 string 으로 직렬화 보관, 목록 query 구성 시점에 원 타입으로 변환.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: { keyword: '', isActive: '', profileId: '' },
  });
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [keywordInput, setKeywordInput] = useState(() => filters.keyword);
  const [isActiveInput, setIsActiveInput] = useState(() => filters.isActive);
  const [profileIdInput, setProfileIdInput] = useState(() => filters.profileId);

  const { data, isLoading, isError, error, refetch, isFetching } = useUsers({
    keyword: filters.keyword || undefined,
    isActive: filters.isActive === '' ? undefined : filters.isActive === 'true',
    profileId: filters.profileId === '' ? undefined : Number(filters.profileId),
    page,
    size,
  });

  const handleSearch = () => {
    setFilters({
      keyword: keywordInput,
      isActive: isActiveInput,
      profileId: profileIdInput,
    });
  };

  const { data: profiles } = useQuery({
    queryKey: ['admin', 'users', 'profile-options'],
    queryFn: fetchUserProfileOptions,
  });

  const profileOptions = [
    { value: '', label: '프로파일 전체' },
    ...(profiles ?? []).map((p) => ({ value: String(p.id), label: p.name })),
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
    <div
      ref={containerRef}
      style={{
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        height: containerHeight,
        boxSizing: 'border-box',
        minHeight: 0,
      }}
    >
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', flexShrink: 0 }}>
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

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이가 scrollY 로 body 스크롤. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={isLoading}
          locale={listTableLocale()}
          scroll={{ x: 'max-content', y: scrollY }}
          pagination={buildListPagination({
            page: data?.page ?? page,
            pageSize: size,
            total: data?.totalElements ?? 0,
            // 사이즈 변경 시 setSize 가 page 를 0 으로 자동 리셋(useListQueryParams).
            onPageChange: setPage,
            onSizeChange: setSize,
          })}
          onRow={(record) => ({
            onClick: () => handleRowClick(record.id),
            style: { cursor: 'pointer' },
          })}
        />
      </div>
    </div>
  );
}
