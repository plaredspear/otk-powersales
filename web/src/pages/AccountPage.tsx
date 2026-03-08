import { useState } from 'react';
import { Alert, Button, Input, Select, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useAccounts } from '@/hooks/account/useAccounts';
import type { Account } from '@/api/account';

const { Title } = Typography;

const ABC_TYPE_TAG: Record<string, string> = {
  대형마트: 'blue',
  슈퍼: 'green',
  편의점: 'orange',
};

const STATUS_TAG: Record<string, string> = {
  활성: 'green',
  비활성: 'red',
};

const ABC_TYPE_OPTIONS = [
  { value: '', label: 'ABC유형 전체' },
  { value: '대형마트', label: '대형마트' },
  { value: '슈퍼', label: '슈퍼' },
  { value: '편의점', label: '편의점' },
];

const STATUS_OPTIONS = [
  { value: '', label: '상태 전체' },
  { value: '활성', label: '활성' },
  { value: '비활성', label: '비활성' },
];

const PAGE_SIZE = 20;

export default function AccountPage() {
  const [abcType, setAbcType] = useState<string | undefined>();
  const [branchCode, setBranchCode] = useState<string | undefined>();
  const [accountStatusName, setAccountStatusName] = useState<string | undefined>();
  const [keyword, setKeyword] = useState<string | undefined>();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useAccounts({
    keyword,
    abcType,
    branchCode,
    accountStatusName,
    page,
    size: PAGE_SIZE,
  });

  const columns: ColumnsType<Account> = [
    { title: '거래처코드', dataIndex: 'externalKey', width: 110, render: (val: string | null) => val ?? '-' },
    { title: '거래처명', dataIndex: 'name', width: 180, ellipsis: true, render: (val: string | null) => val ?? '-' },
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
    <div style={{ padding: 24 }}>
      <Title level={4} style={{ marginBottom: 16 }}>거래처 관리</Title>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
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
          onSearch={(val) => { setKeyword(val || undefined); setPage(0); }}
        />
      </div>

      <Table
        rowKey="externalKey"
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
    </div>
  );
}
