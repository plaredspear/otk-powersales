import { Modal, Form, Input, Select, DatePicker, Switch, notification, Alert } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import type { EmployeeDetail, EmployeeUpdateRequest, FemaleEmployeeFormMeta } from '@/api/employee';
import { useUpdateEmployee } from '@/hooks/employee/useEmployee';
import { APP_AUTHORITY_OPTIONS, type AppAuthority } from '@/constants/userRole';
import { PPT_TEAM_TYPES, type PPTTeamType } from '@/constants/pptTeamType';

interface EmployeeEditModalProps {
  employee: EmployeeDetail;
  open: boolean;
  onClose: () => void;
  /**
   * 서버 form-meta (재직상태 / 권한 / 전문행사조 옵션).
   *
   * 여사원 상세는 `/female-employees/form-meta` 응답을 넘겨 서버를 단일 출처로 삼는다.
   * 설정 사원 상세(`employee` 권한 진입) 는 전용 endpoint 가 없어 넘기지 않으며, 이 경우
   * 아래 프론트 상수로 폴백한다. 로딩 중에도 undefined 라 폴백이 적용된다.
   */
  formMeta?: FemaleEmployeeFormMeta;
}

interface FormValues {
  status?: string;
  role?: AppAuthority;
  orgName?: string;
  costCenterCode?: string;
  workArea?: string;
  jobCode?: string;
  jikwee?: string;
  jikchak?: string;
  jikgub?: string;
  ordDetailNode?: string;
  appointmentDate?: Dayjs;
  startDate?: Dayjs;
  endDate?: Dayjs;
  homePhone?: string;
  workPhone?: string;
  officePhone?: string;
  workEmail?: string;
  email?: string;
  appLoginActive?: boolean;
  lockingFlag?: boolean;
  professionalPromotionTeam?: PPTTeamType;
}

// --- form-meta 미제공(설정 사원 상세 진입 / 로딩 중) 시 폴백 상수 ---

const STATUS_OPTIONS = [
  { value: '재직', label: '재직' },
  { value: '휴직', label: '휴직' },
  { value: '퇴직', label: '퇴직' },
];

const PPT_OPTIONS = PPT_TEAM_TYPES.map((v) => ({ value: v, label: v }));

const ROLE_SELECT_OPTIONS = APP_AUTHORITY_OPTIONS.map((opt) => ({
  value: opt.value,
  label: opt.label,
}));

export default function EmployeeEditModal({
  employee,
  open,
  onClose,
  formMeta,
}: EmployeeEditModalProps) {
  const [form] = Form.useForm<FormValues>();
  const mutation = useUpdateEmployee();

  // 서버 form-meta 우선, 미제공이면 프론트 상수 폴백.
  const statusOptions = formMeta?.statuses ?? STATUS_OPTIONS;
  const roleOptions = formMeta?.roles ?? ROLE_SELECT_OPTIONS;
  const pptOptions = formMeta?.professionalPromotionTeams ?? PPT_OPTIONS;

  const initial: FormValues = {
    status: employee.status ?? undefined,
    role: employee.role ?? undefined,
    orgName: employee.orgName ?? undefined,
    costCenterCode: employee.costCenterCode ?? undefined,
    workArea: employee.workArea ?? undefined,
    jobCode: employee.jobCode ?? undefined,
    jikwee: employee.jikwee ?? undefined,
    jikchak: employee.jikchak ?? undefined,
    jikgub: employee.jikgub ?? undefined,
    ordDetailNode: employee.ordDetailNode ?? undefined,
    appointmentDate: employee.appointmentDate ? dayjs(employee.appointmentDate) : undefined,
    startDate: employee.startDate ? dayjs(employee.startDate) : undefined,
    endDate: employee.endDate ? dayjs(employee.endDate) : undefined,
    homePhone: employee.homePhone ?? undefined,
    workPhone: employee.workPhone ?? undefined,
    officePhone: employee.officePhone ?? undefined,
    workEmail: employee.workEmail ?? undefined,
    email: employee.email ?? undefined,
    appLoginActive: employee.appLoginActive ?? false,
    lockingFlag: employee.lockingFlag ?? false,
    professionalPromotionTeam: employee.professionalPromotionTeam ?? undefined,
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const request: EmployeeUpdateRequest = {
        ...values,
        appointmentDate: values.appointmentDate?.format('YYYY-MM-DD'),
        startDate: values.startDate?.format('YYYY-MM-DD'),
        endDate: values.endDate?.format('YYYY-MM-DD'),
      };
      await mutation.mutateAsync({ employeeId: employee.id, request });
      notification.success({ message: '사원 정보가 수정되었습니다' });
      onClose();
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        // validation 실패 — antd 가 자동으로 표시
        return;
      }
      notification.error({
        message: '사원 정보 수정 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류',
      });
    }
  };

  return (
    <Modal
      title={`사원 정보 수정 — ${employee.name} (${employee.employeeCode})`}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      okText="저장"
      cancelText="취소"
      width={780}
      confirmLoading={mutation.isPending}
      destroyOnHidden
    >
      {employee.origin === 'SAP' && (
        <Alert
          type="error"
          showIcon
          message="SAP 원천 사원은 수정 불가"
          description="SAP 가 원천인 사원은 web admin 에서 수정할 수 없습니다. SAP 인입을 통해서만 갱신됩니다."
          style={{ marginBottom: 16 }}
        />
      )}
      <Form form={form} layout="vertical" initialValues={initial}>
        <Form.Item name="status" label="재직 상태">
          <Select options={statusOptions} allowClear placeholder="선택" />
        </Form.Item>
        <Form.Item name="role" label="권한">
          <Select options={roleOptions} allowClear placeholder="선택" />
        </Form.Item>
        <Form.Item name="orgName" label="조직명">
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="costCenterCode" label="지점코드">
          <Input maxLength={10} />
        </Form.Item>
        <Form.Item name="workArea" label="근무지역">
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="jobCode" label="직무코드">
          <Input maxLength={40} />
        </Form.Item>
        <Form.Item name="jikwee" label="직위">
          <Input maxLength={40} />
        </Form.Item>
        <Form.Item name="jikchak" label="직책">
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="jikgub" label="직급">
          <Input maxLength={40} />
        </Form.Item>
        <Form.Item name="ordDetailNode" label="발령명">
          <Input maxLength={255} />
        </Form.Item>
        <Form.Item name="appointmentDate" label="발령일">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="startDate" label="입사일">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="endDate" label="퇴사일">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="homePhone" label="집 전화">
          <Input maxLength={255} />
        </Form.Item>
        <Form.Item name="workPhone" label="업무 전화">
          <Input maxLength={255} />
        </Form.Item>
        <Form.Item name="officePhone" label="사무실 전화">
          <Input maxLength={40} />
        </Form.Item>
        <Form.Item
          name="workEmail"
          label="업무 이메일"
          rules={[{ type: 'email', message: '이메일 형식이 올바르지 않습니다' }]}
        >
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item
          name="email"
          label="개인 이메일"
          rules={[{ type: 'email', message: '이메일 형식이 올바르지 않습니다' }]}
        >
          <Input maxLength={100} />
        </Form.Item>
        {/*
          '일반' 은 미배정 복귀를 뜻하는 명시적 선택지 — 서버가 null 저장으로 해석한다.
          비워두면(allowClear) 값 미전송이라 기존 값이 유지된다.
        */}
        <Form.Item name="professionalPromotionTeam" label="전문행사조">
          <Select options={pptOptions} allowClear placeholder="선택" />
        </Form.Item>
        <Form.Item name="appLoginActive" label="앱 로그인 활성" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item
          name="lockingFlag"
          label="시스템 접근 잠금 (켜면 앱 로그인 자동 비활성화)"
          valuePropName="checked"
        >
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
}
