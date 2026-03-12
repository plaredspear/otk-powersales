import { useState } from 'react';
import { Alert, Button, Input, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEmployees } from '@/hooks/employee/useEmployees';
import type { Employee } from '@/api/employee';

const STATUS_TAG: Record<string, string> = {
  재직: 'green',
  휴직: 'orange',
  퇴직: 'red',
};

const AUTHORITY_OPTIONS = [
  { value: '', label: '권한 전체' },
  { value: '조장', label: '조장' },
  { value: '지점장', label: '지점장' },
  { value: '영업부장', label: '영업부장' },
  { value: '사업부장', label: '사업부장' },
  { value: '영업본부장', label: '영업본부장' },
  { value: '영업지원실', label: '영업지원실' },
];

const STATUS_OPTIONS = [
  { value: '', label: '상태 전체' },
  { value: '재직', label: '재직' },
  { value: '휴직', label: '휴직' },
  { value: '퇴직', label: '퇴직' },
];

const PAGE_SIZE = 20;

export default function EmployeePage() {
  const [status, setStatus] = useState<string | undefined>();
  const [costCenterCode, setCostCenterCode] = useState<string | undefined>();
  const [keyword, setKeyword] = useState<string | undefined>();
  const [appAuthority, setAppAuthority] = useState<string | undefined>();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useEmployees({
    status,
    costCenterCode,
    keyword,
    appAuthority,
    page,
    size: PAGE_SIZE,
  });

  const columns: ColumnsType<Employee> = [
    { title: '사번', dataIndex: 'employeeId', width: 100 },
    { title: '이름', dataIndex: 'name', width: 120 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 80,
      align: 'center',
      render: (val: string | null) =>
        val ? <Tag color={STATUS_TAG[val] ?? undefined}>{val}</Tag> : '-',
    },
    { title: '소속', dataIndex: 'orgName', width: 150, render: (val: string | null) => val ?? '-' },
    { title: '지점코드', dataIndex: 'costCenterCode', width: 100, align: 'center', render: (val: string | null) => val ?? '-' },
    {
      title: '권한',
      dataIndex: 'appAuthority',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    { title: '입사일', dataIndex: 'startDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    {
      title: '앱활성',
      dataIndex: 'appLoginActive',
      width: 80,
      align: 'center',
      render: (val: boolean | null) =>
        val ? <Tag color="blue">활성</Tag> : <Tag>비활성</Tag>,
    },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="사원 목록을 불러오지 못했습니다"
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
          value={status ?? ''}
          options={STATUS_OPTIONS}
          onChange={(val) => { setStatus(val || undefined); setPage(0); }}
        />
        <Input
          placeholder="지점코드"
          allowClear
          style={{ width: 140 }}
          value={costCenterCode ?? ''}
          onChange={(e) => { setCostCenterCode(e.target.value || undefined); setPage(0); }}
        />
        <Select
          style={{ width: 140 }}
          value={appAuthority ?? ''}
          options={AUTHORITY_OPTIONS}
          onChange={(val) => { setAppAuthority(val || undefined); setPage(0); }}
        />
        <Input.Search
          placeholder="사번 또는 이름 검색"
          allowClear
          style={{ width: 240 }}
          onSearch={(val) => { setKeyword(val || undefined); setPage(0); }}
        />
      </div>

      <Table
        rowKey="employeeId"
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
      />
    </div>
  );
}
