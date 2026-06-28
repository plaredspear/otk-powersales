import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Input, Select, Space, Tag, Tooltip } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFemaleEmployees } from '@/hooks/employee/useEmployees';
import { useFemaleEmployeeBranches } from '@/hooks/employee/useFemaleEmployeeBranches';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { EXCEL_EXPORT_MAX_ROWS } from '@/lib/excelDownload';
import { FEMALE_EMPLOYEE_EXPORT_PATH, type Employee } from '@/api/employee';
import { usePermission } from '@/hooks/usePermission';
import DeviceResetModal from '@/pages/employee/components/DeviceResetModal';
import PasswordResetModal from '@/pages/employee/components/PasswordResetModal';
import EmployeeRegisterModal from '@/pages/employee/components/EmployeeRegisterModal';

const STATUS_TAG: Record<string, string> = {
  재직: 'green',
  휴직: 'orange',
  퇴직: 'red',
};

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

export default function EmployeePage() {
  const navigate = useNavigate();
  const location = useLocation();
  // 상세 진입 시 현재 목록의 query string 을 state 로 넘겨, 상세의 "목록으로" 버튼이 직전 조건으로 복귀하게 한다.
  const goToDetail = (id: number) =>
    navigate(`/female-employee/${id}`, { state: { listSearch: location.search } });
  // page/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입 시 직전 조건 복원.
  const { page, setPage, filters, setFilter } = useListQueryParams({
    defaultFilters: { status: '', costCenterCode: '', keyword: '' },
  });
  const { status, costCenterCode, keyword } = filters;

  // 지점 셀렉터 — 권한별 지점 화이트리스트 (전문행사조와 동일 backend resolver).
  //  - 다중 지점: Select 로 선택 → costCenterCode 필터로 전송
  //  - 단일 지점(조장 등): 고정 Tag 로 지점명 표시. costCenterCode 는 빈 값이라
  //    backend 가 본인 소속 지점으로 자동 스코프(applyBranchScope)하므로 별도 전송 불필요.
  const { data: branches } = useFemaleEmployeeBranches();
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));
  const singleBranch = branches?.length === 1 ? branches[0] : null;
  const isMultiBranch = (branches?.length ?? 0) > 1;

  const [deviceTarget, setDeviceTarget] = useState<Employee | null>(null);
  const [passwordTarget, setPasswordTarget] = useState<Employee | null>(null);
  const [registerOpen, setRegisterOpen] = useState(false);
  const { hasEntityPermission, hasSystemPermission } = usePermission();
  const canResetCredentials = hasSystemPermission('MANAGE_USERS');
  // 여사원 현황은 조회 전용 정책 — 등록/수정 버튼은 female_employee:EDIT 보유자에게만 노출.
  // (여사원 READ 만 받은 조장은 미노출.) 등록/수정 API 자체는 employee 가드 유지.
  const canWrite = hasEntityPermission('female_employee', 'EDIT');

  const { data, isLoading, isError, error, refetch, isFetching } = useFemaleEmployees({
    status: status || undefined,
    costCenterCode: costCenterCode || undefined,
    keyword: keyword || undefined,
    page,
    size: PAGE_SIZE,
  });

  const { run: runExport, downloading: exporting } = useExcelDownload();

  // 현재 적용된 검색 조건(URL filters)을 그대로 전량 export (목록과 동일 가시 범위).
  const handleExport = () => {
    runExport(FEMALE_EMPLOYEE_EXPORT_PATH, '여사원현황.xlsx', {
      params: {
        ...(status ? { status } : {}),
        ...(costCenterCode ? { costCenterCode } : {}),
        ...(keyword ? { keyword } : {}),
      },
      totalCount: data?.totalElements,
      maxRows: EXCEL_EXPORT_MAX_ROWS,
    });
  };

  const columns: ColumnsType<Employee> = [
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 100,
      render: (val: string, record: Employee) => (
        <a
          onClick={(e) => {
            e.preventDefault();
            goToDetail(record.id);
          }}
          href={`/female-employee/${record.id}`}
        >
          {val}
        </a>
      ),
    },
    {
      title: '이름',
      dataIndex: 'name',
      width: 120,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 80,
      align: 'center',
      render: (val: string | null) =>
        val ? <Tag color={STATUS_TAG[val] ?? undefined}>{val}</Tag> : '-',
    },
    { title: '소속', dataIndex: 'orgName', width: 150, render: (val: string | null) => val ?? '-' },
    {
      title: '권한',
      dataIndex: "role",
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    { title: '직종명', dataIndex: 'jikjong', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '직책', dataIndex: 'jikchak', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '직위', dataIndex: 'jikwee', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '직급', dataIndex: 'jikgub', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '이메일(회사)', dataIndex: 'workEmail', width: 180, render: (val: string | null) => val ?? '-' },
    { title: '전화번호(HP)', dataIndex: 'phone', width: 130, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '발령일', dataIndex: 'appointmentDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '발령명', dataIndex: 'ordDetailNode', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '입사일', dataIndex: 'startDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '퇴사일', dataIndex: 'endDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '만나이', dataIndex: 'age', width: 80, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '근속년수', dataIndex: 'yearsOfService', width: 90, align: 'center', render: (val: string | null) => val ?? '-' },
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
        {isMultiBranch && (
          <Select
            placeholder="지점 (전체)"
            value={costCenterCode || undefined}
            onChange={(v) => setFilter('costCenterCode', v ?? '')}
            style={{ width: 160 }}
            options={branchOptions}
            allowClear
            showSearch
            optionFilterProp="label"
          />
        )}
        {singleBranch && (
          <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
            지점: {singleBranch.branchName}
          </Tag>
        )}
        <Select
          style={{ width: 140 }}
          value={status ?? ''}
          options={STATUS_OPTIONS}
          onChange={(val) => setFilter('status', val)}
        />
        <Input.Search
          placeholder="사번 또는 이름 검색"
          allowClear
          defaultValue={keyword ?? ''}
          style={{ width: 240 }}
          onSearch={(val) => setFilter('keyword', val)}
        />
        <Space style={{ marginLeft: 'auto' }}>
          <Button icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
            엑셀 다운로드
          </Button>
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
        <EmployeeRegisterModal
          open={true}
          onClose={() => setRegisterOpen(false)}
          detailBasePath="/female-employee"
        />
      )}
    </div>
  );
}
