import { useState } from 'react';
import { Alert, Button, Checkbox, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useLocation, useNavigate } from 'react-router-dom';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import { useAccounts } from '@/hooks/account/useAccounts';
import { useAccountBranches } from '@/hooks/account/useAccountBranches';
import { usePermission } from '@/hooks/usePermission';
import type { Account } from '@/api/account';
import AdminAccountCreateModal from './admin/accounts/AdminAccountCreateModal';
import AccountDeleteAction from './admin/accounts/components/AccountDeleteAction';

const ABC_TYPE_TAG: Record<string, string> = {
  대형마트: 'blue',
  슈퍼: 'green',
  편의점: 'orange',
};

const STATUS_TAG: Record<string, string> = {
  거래: 'green',
  폐업: 'red',
};

const ABC_TYPE_OPTIONS = [
  { value: '', label: 'ABC유형 전체' },
  { value: '대형마트', label: '대형마트' },
  { value: '슈퍼', label: '슈퍼' },
  { value: '편의점', label: '편의점' },
];

const STATUS_OPTIONS = [
  { value: '', label: '상태 전체' },
  { value: '거래', label: '활성' },
  { value: '폐업', label: '비활성' },
];

export default function AccountPage() {
  const location = useLocation();
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소를
  // 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/size/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: { abcType: '', branchCode: '', accountStatusName: '', keyword: '', coordinatesMissing: '' },
  });
  // 좌표 미수신 필터는 문자열 'true' 로 URL 에 보관 — 스케줄 잡 좌표변환 패널 링크(`?coordinatesMissing=true`)
  // 로 진입 시 자동 복원된다.
  const coordinatesMissing = filters.coordinatesMissing === 'true';
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [abcTypeInput, setAbcTypeInput] = useState<string | undefined>(
    () => filters.abcType || undefined,
  );
  const [branchCodeInput, setBranchCodeInput] = useState<string | undefined>(
    () => filters.branchCode || undefined,
  );
  const [accountStatusNameInput, setAccountStatusNameInput] = useState<string | undefined>(
    () => filters.accountStatusName || undefined,
  );
  const [keywordInput, setKeywordInput] = useState<string | undefined>(
    () => filters.keyword || undefined,
  );
  const [coordinatesMissingInput, setCoordinatesMissingInput] = useState<boolean>(
    () => filters.coordinatesMissing === 'true',
  );
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const navigate = useNavigate();
  const { data: branches } = useAccountBranches();
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));
  const singleBranch = branches?.length === 1 ? branches[0] : null;
  const isMultiBranch = (branches?.length ?? 0) > 1;

  const { hasEntityPermission } = usePermission();
  const canCreateAccount = hasEntityPermission('account', 'EDIT');
  const canDeleteAccount = hasEntityPermission('account', 'DELETE');
  // 거래처 수정(주소)은 상세 페이지로 일원화 — 목록 "관리" 컬럼은 삭제 액션만 담당.
  const showActionsColumn = canDeleteAccount;

  // 상세 페이지에서 "목록으로" 복귀 시 직전 검색 조건(applied 기준)을 복원하기 위한 query string.
  // URL 에는 applied 필터/page/size 만 기록되므로 현재 query string 을 그대로 전달한다.
  const goToDetail = (id: number) => {
    navigate(`/account/${id}`, { state: { listSearch: location.search } });
  };

  const handleSearch = () => {
    setFilters({
      abcType: abcTypeInput ?? '',
      branchCode: branchCodeInput ?? '',
      accountStatusName: accountStatusNameInput ?? '',
      keyword: keywordInput ?? '',
      coordinatesMissing: coordinatesMissingInput ? 'true' : '',
    });
  };

  const { data, isLoading, isError, error, refetch, isFetching } = useAccounts({
    keyword: filters.keyword || undefined,
    abcType: filters.abcType || undefined,
    branchCode: filters.branchCode || undefined,
    accountStatusName: filters.accountStatusName || undefined,
    coordinatesMissing: coordinatesMissing || undefined,
    page,
    size,
  });

  const columns: ColumnsType<Account> = [
    { title: '거래처코드', dataIndex: 'externalKey', width: 110, render: (val: string | null) => val ?? '-' },
    {
      title: '거래처명',
      dataIndex: 'name',
      width: 180,
      ellipsis: true,
      render: (val: string | null, account: Account) => (
        <Button type="link" style={{ padding: 0, height: 'auto' }} onClick={() => goToDetail(account.id)}>
          {val ?? '-'}
        </Button>
      ),
    },
    {
      title: 'ABC유형',
      dataIndex: 'abcType',
      width: 100,
      align: 'center',
      render: (val: string | null) =>
        val ? <Tag color={ABC_TYPE_TAG[val] ?? undefined}>{val}</Tag> : '-',
    },
    { title: '지점명', dataIndex: 'branchName', width: 100, render: (val: string | null) => val ?? '-' },
    { title: '담당사원', dataIndex: 'employeeCode', width: 100, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '주소', dataIndex: 'address1', width: 200, ellipsis: true, render: (val: string | null) => val ?? '-' },
    { title: '전화번호', dataIndex: 'phone', width: 120, render: (val: string | null) => val ?? '-' },
    {
      title: '상태',
      dataIndex: 'accountStatusName',
      width: 80,
      align: 'center',
      render: (val: string | null) =>
        val ? <Tag color={STATUS_TAG[val] ?? undefined}>{val}</Tag> : '-',
    },
    ...(showActionsColumn
      ? [
          {
            title: '관리',
            key: 'actions',
            width: 160,
            align: 'center' as const,
            render: (_: unknown, account: Account) => (
              <Space size={4}>
                {canDeleteAccount && <AccountDeleteAction account={account} />}
              </Space>
            ),
          },
        ]
      : []),
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="거래처 목록을 불러오지 못했습니다"
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

      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', flexShrink: 0 }}>
        <Space wrap>
          {isMultiBranch && (
            <Select
              placeholder="지점 (전체)"
              style={{ width: 160 }}
              value={branchCodeInput || undefined}
              options={branchOptions}
              allowClear
              showSearch
              optionFilterProp="label"
              onChange={(val) => setBranchCodeInput(val || undefined)}
            />
          )}
          {singleBranch && (
            <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
              지점: {singleBranch.branchName}
            </Tag>
          )}
          <Select
            style={{ width: 140 }}
            value={abcTypeInput ?? ''}
            options={ABC_TYPE_OPTIONS}
            onChange={(val) => setAbcTypeInput(val || undefined)}
          />
          <Select
            style={{ width: 140 }}
            value={accountStatusNameInput ?? ''}
            options={STATUS_OPTIONS}
            onChange={(val) => setAccountStatusNameInput(val || undefined)}
          />
          <Input
            placeholder="거래처코드 또는 거래처명 검색"
            allowClear
            style={{ width: 280 }}
            value={keywordInput ?? ''}
            onChange={(e) => setKeywordInput(e.target.value || undefined)}
            onPressEnter={handleSearch}
          />
          <Checkbox
            checked={coordinatesMissingInput}
            onChange={(e) => setCoordinatesMissingInput(e.target.checked)}
          >
            좌표 미수신만
          </Checkbox>
          <Button type="primary" onClick={handleSearch}>
            조회
          </Button>
        </Space>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          {canCreateAccount && (
            <Button type="primary" onClick={() => setCreateModalOpen(true)}>
              신규 등록
            </Button>
          )}
        </Space>
      </Space>

      {coordinatesMissing && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16, flexShrink: 0 }}
          message="좌표 미수신 거래처만 표시 중"
          description="위/경도가 비어 있는 '거래' 상태 거래처(주소·거래처코드 보유)만 조회합니다. 스케줄 잡 '거래처 좌표변환'(매일 02시)이 이 거래처들의 좌표를 네이버 Geocode API 로 보강하며, 조회에 실패한 거래처는 다음 실행 때 다시 대상이 됩니다."
        />
      )}
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
        />
      </div>

      <AdminAccountCreateModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
      />
    </div>
  );
}
