import { useState } from 'react';
import { Alert, Button, Input, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEmployees } from '@/hooks/employee/useEmployees';
import type { Employee } from '@/api/employee';
import { ROLE_OPTIONS_FOR_FILTER, type UserRole } from '@/constants/userRole';

const STATUS_TAG: Record<string, string> = {
  재직: 'green',
  휴직: 'orange',
  퇴직: 'red',
};

const ROLE_FILTER_OPTIONS = [
  { value: '', label: '권한 전체' },
  ...ROLE_OPTIONS_FOR_FILTER.map((opt) => ({ value: opt.value, label: opt.label })),
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
  const [role, setRole] = useState<UserRole | undefined>();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useEmployees({
    status,
    costCenterCode,
    keyword,
    role,
    page,
    size: PAGE_SIZE,
  });

  const columns: ColumnsType<Employee> = [
    { title: '사번', dataIndex: 'employeeCode', width: 100 },
    { title: '이름', dataIndex: 'name', width: 120 },
    { title: '성별', dataIndex: 'gender', width: 60, align: 'center', render: (val: string | null) => val ?? '-' },
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
      dataIndex: 'roleLabel',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    { title: '직책', dataIndex: 'jikchak', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '직위', dataIndex: 'jikwee', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '직급', dataIndex: 'jikgub', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '발령일', dataIndex: 'appointmentDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '발령명', dataIndex: 'ordDetailNode', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '입사일', dataIndex: 'startDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '퇴사일', dataIndex: 'endDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
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
          value={role ?? ''}
          options={ROLE_FILTER_OPTIONS}
          onChange={(val) => { setRole((val || undefined) as UserRole | undefined); setPage(0); }}
        />
        <Input.Search
          placeholder="사번 또는 이름 검색"
          allowClear
          style={{ width: 240 }}
          onSearch={(val) => { setKeyword(val || undefined); setPage(0); }}
        />
      </div>

      <Table
        rowKey="employeeCode"
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
