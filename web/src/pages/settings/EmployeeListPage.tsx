import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Input, Select, Space, Tag, Tooltip, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useEmployees } from '@/hooks/employee/useEmployees';
import type { Employee } from '@/api/employee';
import { APP_AUTHORITY_OPTIONS, type AppAuthority } from '@/constants/userRole';
import { usePermission } from '@/hooks/usePermission';
import DeviceResetModal from '@/pages/employee/components/DeviceResetModal';
import PasswordResetModal from '@/pages/employee/components/PasswordResetModal';
import EmployeeRegisterModal from '@/pages/employee/components/EmployeeRegisterModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const STATUS_TAG: Record<string, string> = {
  재직: 'green',
  휴직: 'orange',
  퇴직: 'red',
};

const ROLE_FILTER_OPTIONS = [
  { value: '', label: '권한 전체' },
  ...APP_AUTHORITY_OPTIONS.map((opt) => ({ value: opt.value, label: opt.label })),
];

const STATUS_OPTIONS = [
  { value: '', label: '상태 전체' },
  { value: '재직', label: '재직' },
  { value: '휴직', label: '휴직' },
  { value: '퇴직', label: '퇴직' },
];

const PAGE_SIZE = 20;

const DEVICE_TOOLTIP =
  '단말 바인딩(deviceUuid)이 해제됩니다. 사원이 다음에 어떤 단말로 로그인하더라도 새 단말로 자동 등록됩니다.';
const PASSWORD_TOOLTIP =
  "임시 비밀번호 '1234' 로 초기화됩니다. 사원은 다음 로그인 시 비밀번호 변경을 요구받습니다.";
const INACTIVE_NOTICE = '앱 로그인이 비활성화된 사원입니다. 사원 정보를 먼저 활성화해 주세요.';

export default function EmployeeListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // 상세 진입 시 현재 목록의 query string 을 state 로 넘겨, 상세의 "목록으로" 버튼이 직전 조건으로 복귀하게 한다.
  const goToDetail = (id: number) =>
    navigate(`/employee/${id}`, { state: { listSearch: location.search } });
  const handleCopyEmployeeCode = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      message.success('사번을 복사했습니다');
    } catch {
      message.error('복사에 실패했습니다');
    }
  };
  // page/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입 시 직전 조건 복원.
  const { page, setPage, filters, setFilter } = useListQueryParams({
    defaultFilters: { status: '', costCenterCode: '', keyword: '', role: '' },
  });
  const { status, costCenterCode, keyword, role } = filters;
  const [deviceTarget, setDeviceTarget] = useState<Employee | null>(null);
  const [passwordTarget, setPasswordTarget] = useState<Employee | null>(null);
  const [registerOpen, setRegisterOpen] = useState(false);
  const { hasEntityPermission, hasSystemPermission } = usePermission();
  const canResetCredentials = hasSystemPermission('MANAGE_USERS');
  const canWrite = hasEntityPermission('employee', 'EDIT');

  const { data, isLoading, isError, error, refetch, isFetching } = useEmployees({
    status: status || undefined,
    costCenterCode: costCenterCode || undefined,
    keyword: keyword || undefined,
    role: (role || undefined) as AppAuthority | undefined,
    page,
    size: PAGE_SIZE,
  });

  const columns: ColumnsType<Employee> = [
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 120,
      render: (val: string, record: Employee) => (
        <Space size={4}>
          <a
            onClick={(e) => {
              e.preventDefault();
              goToDetail(record.id);
            }}
            href={`/employee/${record.id}`}
          >
            {val}
          </a>
          <Tooltip title="사번 복사">
            <CopyOutlined
              style={{ color: '#1677ff', cursor: 'pointer' }}
              onClick={() => handleCopyEmployeeCode(val)}
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: '이름',
      dataIndex: 'name',
      width: 120,
    },
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
      dataIndex: "role",
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
    ...(canResetCredentials
      ? [
          {
            title: '계정 관리',
            key: 'credentialReset',
            width: 220,
            align: 'center' as const,
            render: (_: unknown, record: Employee) => {
              const inactive = record.appLoginActive !== true;
              const inactiveTooltip = inactive ? INACTIVE_NOTICE : null;
              return (
                <Space size={4}>
                  <Tooltip title={inactiveTooltip ?? DEVICE_TOOLTIP}>
                    <Button
                      size="small"
                      danger
                      disabled={inactive}
                      onClick={() => setDeviceTarget(record)}
                    >
                      단말 초기화
                    </Button>
                  </Tooltip>
                  <Tooltip title={inactiveTooltip ?? PASSWORD_TOOLTIP}>
                    <Button
                      size="small"
                      danger
                      disabled={inactive}
                      onClick={() => setPasswordTarget(record)}
                    >
                      비밀번호 초기화
                    </Button>
                  </Tooltip>
                </Space>
              );
            },
          },
        ]
      : []),
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

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <Select
          style={{ width: 140 }}
          value={status ?? ''}
          options={STATUS_OPTIONS}
          onChange={(val) => setFilter('status', val || '')}
        />
        <Input
          placeholder="지점코드"
          allowClear
          style={{ width: 140 }}
          value={costCenterCode ?? ''}
          onChange={(e) => setFilter('costCenterCode', e.target.value)}
        />
        <Select
          style={{ width: 140 }}
          value={role ?? ''}
          options={ROLE_FILTER_OPTIONS}
          onChange={(val) => setFilter('role', val || '')}
        />
        <Input.Search
          placeholder="사번 또는 이름 검색"
          allowClear
          defaultValue={keyword ?? ''}
          style={{ width: 240 }}
          onSearch={(val) => setFilter('keyword', val || '')}
        />
        <Space style={{ marginLeft: 'auto' }}>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          {canWrite && (
            <Button type="primary" onClick={() => setRegisterOpen(true)}>
              + 신규 사원 등록
            </Button>
          )}
        </Space>
      </div>

      <ResizableTable
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
      {deviceTarget && (
        <DeviceResetModal
          employee={deviceTarget}
          open={true}
          onClose={() => setDeviceTarget(null)}
        />
      )}
      {passwordTarget && (
        <PasswordResetModal
          employee={passwordTarget}
          open={true}
          onClose={() => setPasswordTarget(null)}
        />
      )}
      {registerOpen && (
        <EmployeeRegisterModal open={true} onClose={() => setRegisterOpen(false)} />
      )}
    </div>
  );
}
