import { Modal, Form, Input, Select, DatePicker, Switch, notification, Alert } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import type { EmployeeDetail, EmployeeUpdateRequest } from '@/api/employee';
import { useUpdateEmployee } from '@/hooks/employee/useEmployee';
import { APP_AUTHORITY_OPTIONS, type AppAuthority } from '@/constants/userRole';
import { PPT_TEAM_TYPES, type PPTTeamType } from '@/constants/pptTeamType';

interface EmployeeEditModalProps {
  employee: EmployeeDetail;
  open: boolean;
  onClose: () => void;
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

export default function EmployeeEditModal({ employee, open, onClose }: EmployeeEditModalProps) {
  const [form] = Form.useForm<FormValues>();
  const mutation = useUpdateEmployee();

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
          <Select options={STATUS_OPTIONS} allowClear placeholder="선택" />
        </Form.Item>
        <Form.Item name="role" label="권한">
          <Select options={ROLE_SELECT_OPTIONS} allowClear placeholder="선택" />
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
        <Form.Item name="professionalPromotionTeam" label="전문행사조">
          <Select options={PPT_OPTIONS} allowClear placeholder="선택" />
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
