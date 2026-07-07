import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Input, Select, Space, Tag, Tooltip, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import { useEmployees } from '@/hooks/employee/useEmployees';
import { useEmployeeBranches } from '@/hooks/employee/useEmployeeBranches';
import type { Employee } from '@/api/employee';
import { APP_AUTHORITY_OPTIONS, type AppAuthority } from '@/constants/userRole';
import { usePermission } from '@/hooks/usePermission';
import DeviceResetModal from '@/pages/employee/components/DeviceResetModal';
import PasswordResetModal from '@/pages/employee/components/PasswordResetModal';
import EmployeeRegisterModal from '@/pages/employee/components/EmployeeRegisterModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

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

const DEVICE_TOOLTIP =
  '단말 바인딩(deviceUuid)이 해제됩니다. 사원이 다음에 어떤 단말로 로그인하더라도 새 단말로 자동 등록됩니다.';
const PASSWORD_TOOLTIP =
  "임시 비밀번호 'pwrs1234!' 로 초기화됩니다. 사원은 다음 로그인 시 비밀번호 변경을 요구받습니다.";
const INACTIVE_NOTICE = '앱 로그인이 비활성화된 사원입니다. 사원 정보를 먼저 활성화해 주세요.';

export default function EmployeeListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소를
  // 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
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
  // page/size/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: { status: '', costCenterCode: '', keyword: '', role: '' },
  });
  const { status, costCenterCode, keyword, role } = filters;
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [statusInput, setStatusInput] = useState(status);
  const [costCenterCodeInput, setCostCenterCodeInput] = useState(costCenterCode);
  const [keywordInput, setKeywordInput] = useState(keyword);
  const [roleInput, setRoleInput] = useState(role);

  // 지점 셀렉터 옵션 — 전 지점(전사). 사원 목록이 전사 조회이므로 옵션도 전사 지점이며
  // costCenterCode 는 표시 필터로만 작동한다(보안축 아님).
  const { data: branches } = useEmployeeBranches();
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));

  const handleSearch = () => {
    setFilters({
      status: statusInput,
      costCenterCode: costCenterCodeInput,
      keyword: keywordInput,
      role: roleInput,
    });
  };
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
    size,
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

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center', flexShrink: 0 }}>
        <Select
          style={{ width: 140 }}
          value={statusInput ?? ''}
          options={STATUS_OPTIONS}
          onChange={(val) => setStatusInput(val || '')}
        />
        <Select
          placeholder="지점 (전체)"
          style={{ width: 160 }}
          value={costCenterCodeInput || undefined}
          options={branchOptions}
          allowClear
          showSearch
          optionFilterProp="label"
          onChange={(val) => setCostCenterCodeInput(val || '')}
        />
        <Select
          style={{ width: 140 }}
          value={roleInput ?? ''}
          options={ROLE_FILTER_OPTIONS}
          onChange={(val) => setRoleInput(val || '')}
        />
        <Input
          placeholder="사번 또는 이름 검색"
          allowClear
          value={keywordInput ?? ''}
          style={{ width: 240 }}
          onChange={(e) => setKeywordInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
        <Space style={{ marginLeft: 'auto' }}>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          {canWrite && (
            <Button type="primary" onClick={() => setRegisterOpen(true)}>
              + 신규 사원 등록
            </Button>
          )}
        </Space>
      </div>

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이가 scrollY 로 body 스크롤. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="employeeCode"
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
