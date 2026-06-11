import { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Space,
  Tag,
  Spin,
  Tooltip,
} from 'antd';
import type { Employee, EmployeeDetail } from '@/api/employee';
import { useEmployee } from '@/hooks/employee/useEmployee';
import { usePermission } from '@/hooks/usePermission';
import EmployeeEditModal from '@/pages/employee/components/EmployeeEditModal';
import PasswordResetModal from '@/pages/employee/components/PasswordResetModal';
import DeviceResetModal from '@/pages/employee/components/DeviceResetModal';
import WorkHistorySection from '@/pages/employee/components/WorkHistorySection';

function toEmployeeListItem(detail: EmployeeDetail): Employee {
  return {
    id: detail.id,
    employeeCode: detail.employeeCode,
    name: detail.name,
    status: detail.status,
    gender: detail.gender,
    orgName: detail.orgName,
    costCenterCode: detail.costCenterCode,
    role: detail.role,
    startDate: detail.startDate,
    endDate: detail.endDate,
    appLoginActive: detail.appLoginActive,
    workPhone: detail.workPhone,
    jikchak: detail.jikchak,
    jikwee: detail.jikwee,
    jikgub: detail.jikgub,
    jobCode: detail.jobCode,
    appointmentDate: detail.appointmentDate,
    ordDetailNode: detail.ordDetailNode,
    jikjong: detail.jikjong,
    workEmail: detail.workEmail,
    phone: detail.phone,
    // 만나이 / 근속년수는 목록 응답 전용 계산 필드 — 상세 응답에는 없음
    age: null,
    yearsOfService: null,
  };
}

const STATUS_TAG: Record<string, string> = {
  재직: 'green',
  휴직: 'orange',
  퇴직: 'red',
};

const ORIGIN_TAG: Record<string, { color: string; label: string }> = {
  SAP: { color: 'blue', label: 'SAP 인입' },
  MANUAL: { color: 'gold', label: '수동 등록' },
};

export default function EmployeeDetailPage() {
  const { employeeId: rawId } = useParams<{ employeeId: string }>();
  const employeeId = rawId ? Number(rawId) : undefined;
  const navigate = useNavigate();
  const location = useLocation();
  // 진입 맥락별 "목록으로" 대상: 여사원 현황(/female-employee/...) → 여사원 목록, 그 외(설정 사원목록 /employee/...) → 설정 사원 목록.
  const listBasePath = location.pathname.startsWith('/female-employee')
    ? '/female-employee'
    : '/settings/employees';
  // 목록에서 넘어온 경우 직전 목록의 query string(page/필터)을 붙여 복귀 — "목록으로" 시 조건 초기화 방지.
  const listSearch = (location.state as { listSearch?: string } | null)?.listSearch ?? '';
  const listPath = `${listBasePath}${listSearch}`;
  const { hasEntityPermission, hasSystemPermission } = usePermission();
  const canEdit = hasEntityPermission('employee', 'EDIT');
  const canReset = hasSystemPermission('MANAGE_USERS');

  const [editOpen, setEditOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [deviceOpen, setDeviceOpen] = useState(false);

  const { data: employee, isLoading, isError, error, refetch } = useEmployee(employeeId);

  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError || !employee) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="사원 상세 조회 실패"
          description={(error as Error)?.message ?? '사원을 찾을 수 없습니다'}
          action={
            <Space>
              <Button onClick={() => refetch()}>재시도</Button>
              <Button onClick={() => navigate(listPath)}>목록으로</Button>
            </Space>
          }
        />
      </div>
    );
  }

  const isSapOrigin = employee.origin === 'SAP';
  const editDisabled = !canEdit || isSapOrigin;
  const editTooltip = isSapOrigin
    ? 'SAP 가 원천인 사원은 web admin 에서 수정할 수 없습니다 (SAP 인입을 통해서만 갱신됩니다)'
    : !canEdit
      ? '수정 권한이 없습니다'
      : '';

  const credentialsDisabled = employee.appLoginActive !== true || !canReset;
  const credentialsTooltip =
    employee.appLoginActive !== true
      ? '앱 로그인이 비활성화된 사원입니다. 사원 정보를 먼저 활성화해 주세요.'
      : !canReset
        ? '계정 초기화 권한이 없습니다'
        : '';

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate(listPath)}>← 목록으로</Button>
        <Tooltip title={editTooltip}>
          <Button type="primary" disabled={editDisabled} onClick={() => setEditOpen(true)}>
            수정
          </Button>
        </Tooltip>
        <Tooltip title={credentialsTooltip}>
          <Button danger disabled={credentialsDisabled} onClick={() => setPasswordOpen(true)}>
            비밀번호 초기화
          </Button>
        </Tooltip>
        <Tooltip title={credentialsTooltip}>
          <Button danger disabled={credentialsDisabled} onClick={() => setDeviceOpen(true)}>
            단말 초기화
          </Button>
        </Tooltip>
      </Space>

      <Card title="인사 정보" style={{ marginBottom: 12 }}>
        <Descriptions column={3} bordered size="small">
          <Descriptions.Item label="사번">{employee.employeeCode}</Descriptions.Item>
          <Descriptions.Item label="이름">{employee.name}</Descriptions.Item>
          <Descriptions.Item label="성별">{employee.gender ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="상태">
            {employee.status ? (
              <Tag color={STATUS_TAG[employee.status] ?? undefined}>{employee.status}</Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="생년월일">{employee.birthDate ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="발령일">{employee.appointmentDate ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="입사일">{employee.startDate ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="퇴사일">{employee.endDate ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="원천">
            {employee.origin ? (
              <Tag color={ORIGIN_TAG[employee.origin]?.color}>
                {ORIGIN_TAG[employee.origin]?.label ?? employee.origin}
              </Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="조직 정보" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="지점코드">{employee.costCenterCode ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="조직명">{employee.orgName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="위치코드">{employee.locationCode ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="근무지역">{employee.workArea ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="직무 정보" style={{ marginBottom: 12 }}>
        <Descriptions column={3} bordered size="small">
          <Descriptions.Item label="직무코드">{employee.jobCode ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="직종">{employee.jikjong ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="직위">{employee.jikwee ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="직책">{employee.jikchak ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="직급">{employee.jikgub ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="근무형태">{employee.workType ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="발령명" span={3}>
            {employee.ordDetailNode ?? '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="연락처" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="전화번호">{employee.phone ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="집 전화">{employee.homePhone ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="업무 전화">{employee.workPhone ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="사무실 전화">{employee.officePhone ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="업무 이메일">{employee.workEmail ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="개인 이메일">{employee.email ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="앱 설정" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="권한">
            {employee.role ?? '-'}
          </Descriptions.Item>
          <Descriptions.Item label="앱 로그인">
            {employee.appLoginActive === true ? (
              <Tag color="blue">활성</Tag>
            ) : (
              <Tag>비활성</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="시스템 접근 잠금">
            {employee.lockingFlag === true ? <Tag color="red">잠금</Tag> : <Tag>해제</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="전문행사조">
            {employee.professionalPromotionTeam ?? '-'}
          </Descriptions.Item>
          <Descriptions.Item label="GPS 동의" span={2}>
            {employee.agreementFlag === true ? (
              <Tag color="green">동의</Tag>
            ) : (
              <Tag>미동의</Tag>
            )}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="근무 정보" style={{ marginBottom: 12 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="CRM 근무형태">{employee.crmWorkType ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="CRM 근무 시작일">
            {employee.crmWorkStartDate ?? '-'}
          </Descriptions.Item>
          <Descriptions.Item label="총 연차">{employee.totalAnnualLeave ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="사용 연차">{employee.usedAnnualLeave ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      {employeeId && <WorkHistorySection employeeId={employeeId} />}

      {editOpen && (
        <EmployeeEditModal
          employee={employee}
          open={true}
          onClose={() => setEditOpen(false)}
        />
      )}
      {passwordOpen && (
        <PasswordResetModal
          employee={toEmployeeListItem(employee)}
          open={true}
          onClose={() => setPasswordOpen(false)}
        />
      )}
      {deviceOpen && (
        <DeviceResetModal
          employee={toEmployeeListItem(employee)}
          open={true}
          onClose={() => setDeviceOpen(false)}
        />
      )}
    </div>
  );
}
