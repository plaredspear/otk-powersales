import { useState } from 'react';
import { Alert, Button, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate, useSearchParams } from 'react-router-dom';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { useAccounts } from '@/hooks/account/useAccounts';
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

const PAGE_SIZE = 20;

export default function AccountPage() {
  // 상세 → "목록으로" 복귀 시 query string 으로 전달된 직전 검색 조건을 초기 state 로 복원.
  const [searchParams] = useSearchParams();
  const [abcType, setAbcType] = useState<string | undefined>(
    () => searchParams.get('abcType') ?? undefined,
  );
  const [branchCode, setBranchCode] = useState<string | undefined>(
    () => searchParams.get('branchCode') ?? undefined,
  );
  const [accountStatusName, setAccountStatusName] = useState<string | undefined>(
    () => searchParams.get('accountStatusName') ?? undefined,
  );
  const [keyword, setKeyword] = useState<string | undefined>(
    () => searchParams.get('keyword') ?? undefined,
  );
  const [page, setPage] = useState(() => {
    const p = Number(searchParams.get('page'));
    return Number.isFinite(p) && p > 0 ? p : 0;
  });
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const navigate = useNavigate();
  const { hasEntityPermission } = usePermission();
  const canCreateAccount = hasEntityPermission('account', 'EDIT');
  const canDeleteAccount = hasEntityPermission('account', 'DELETE');
  // 거래처 수정(주소)은 상세 페이지로 일원화 — 목록 "관리" 컬럼은 삭제 액션만 담당.
  const showActionsColumn = canDeleteAccount;

  // 상세 페이지에서 "목록으로" 복귀 시 직전 검색 조건을 복원하기 위한 query string.
  const listSearch = (() => {
    const sp = new URLSearchParams();
    if (keyword) sp.set('keyword', keyword);
    if (abcType) sp.set('abcType', abcType);
    if (branchCode) sp.set('branchCode', branchCode);
    if (accountStatusName) sp.set('accountStatusName', accountStatusName);
    if (page > 0) sp.set('page', String(page));
    const qs = sp.toString();
    return qs ? `?${qs}` : '';
  })();

  const goToDetail = (id: number) => {
    navigate(`/account/${id}`, { state: { listSearch } });
  };

  const { data, isLoading, isError, error, refetch, isFetching } = useAccounts({
    keyword,
    abcType,
    branchCode,
    accountStatusName,
    page,
    size: PAGE_SIZE,
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
    <div style={{ padding: 16 }}>

      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap' }}>
        <Space wrap>
          <Select
            style={{ width: 140 }}
            value={abcType ?? ''}
            options={ABC_TYPE_OPTIONS}
            onChange={(val) => { setAbcType(val || undefined); setPage(0); }}
          />
          <Input
            placeholder="지점코드"
            allowClear
            style={{ width: 140 }}
            value={branchCode ?? ''}
            onChange={(e) => { setBranchCode(e.target.value || undefined); setPage(0); }}
          />
          <Select
            style={{ width: 140 }}
            value={accountStatusName ?? ''}
            options={STATUS_OPTIONS}
            onChange={(val) => { setAccountStatusName(val || undefined); setPage(0); }}
          />
          <Input.Search
            placeholder="거래처코드 또는 거래처명 검색"
            allowClear
            style={{ width: 280 }}
            defaultValue={keyword ?? ''}
            onSearch={(val) => { setKeyword(val || undefined); setPage(0); }}
          />
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

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        locale={{ emptyText: '검색 결과가 없습니다' }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
      />

      <AdminAccountCreateModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
      />
    </div>
  );
}
