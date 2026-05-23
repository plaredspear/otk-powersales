import { Modal, Form, Input, Select, DatePicker, notification, Alert } from 'antd';
import { useNavigate } from 'react-router-dom';
import dayjs, { Dayjs } from 'dayjs';
import type { EmployeeManualRegisterRequest } from '@/api/employee';
import { useManualRegisterEmployee } from '@/hooks/employee/useEmployee';
import type { AppAuthority } from '@/constants/userRole';
import { PPT_TEAM_TYPES, type PPTTeamType } from '@/constants/pptTeamType';

interface EmployeeRegisterModalProps {
  open: boolean;
  onClose: () => void;
}

interface FormValues {
  employeeCode: string;
  name: string;
  role?: AppAuthority;
  orgName?: string;
  costCenterCode?: string;
  jobCode?: string;
  jikwee?: string;
  jikchak?: string;
  jikgub?: string;
  startDate?: Dayjs;
  homePhone?: string;
  workPhone?: string;
  workEmail?: string;
  professionalPromotionTeam?: PPTTeamType;
}

const PPT_OPTIONS = PPT_TEAM_TYPES.map((v) => ({ value: v, label: v }));

// 수동 등록 가능 역할 — SF DKRetail__AppAuthority__c picklist 4종. 시스템 관리자는 별도 ADMIN-prefix endpoint.
const MANUAL_REGISTER_ROLES: Array<{ value: AppAuthority; label: string }> = [
  { value: '여사원', label: '여사원' },
  { value: '조장', label: '조장' },
  { value: '지점장', label: '지점장' },
  { value: 'AccountViewAll', label: 'AccountViewAll' },
];

export default function EmployeeRegisterModal({ open, onClose }: EmployeeRegisterModalProps) {
  const [form] = Form.useForm<FormValues>();
  const mutation = useManualRegisterEmployee();
  const navigate = useNavigate();

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const request: EmployeeManualRegisterRequest = {
        employeeCode: values.employeeCode,
        name: values.name,
        role: values.role,
        orgName: values.orgName,
        costCenterCode: values.costCenterCode,
        jobCode: values.jobCode,
        jikwee: values.jikwee,
        jikchak: values.jikchak,
        jikgub: values.jikgub,
        startDate: values.startDate?.format('YYYY-MM-DD'),
        homePhone: values.homePhone,
        workPhone: values.workPhone,
        workEmail: values.workEmail,
        professionalPromotionTeam: values.professionalPromotionTeam,
      };
      const result = await mutation.mutateAsync(request);
      notification.success({
        message: '사원이 등록되었습니다',
        description:
          '비밀번호 초기화 권한 보유자가 "비밀번호 초기화" 를 실행해 임시 비밀번호를 발급해 주세요.',
      });
      form.resetFields();
      onClose();
      navigate(`/employee/${result.id}`);
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        return;
      }
      notification.error({
        message: '사원 등록 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류',
      });
    }
  };

  return (
    <Modal
      title="신규 사원 등록"
      open={open}
      onOk={handleSubmit}
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      okText="등록"
      cancelText="취소"
      width={680}
      confirmLoading={mutation.isPending}
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        message="수동 등록 사원 (origin=MANUAL)"
        description="본 endpoint 로 등록된 사원은 SAP 인입 갱신 대상에서 제외됩니다. 등록 후 별도로 「비밀번호 초기화」 를 실행해 임시 비밀번호를 발급해야 사원이 모바일 앱에 로그인할 수 있습니다."
        style={{ marginBottom: 16 }}
      />
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          startDate: dayjs(),
        }}
      >
        <Form.Item
          name="employeeCode"
          label="사번"
          rules={[
            { required: true, message: '사번은 필수입니다' },
            {
              pattern: /^[A-Za-z0-9_-]{1,20}$/,
              message: '영문·숫자·하이픈·언더스코어 20자 이하',
            },
          ]}
        >
          <Input maxLength={20} placeholder="예: 100123" />
        </Form.Item>
        <Form.Item
          name="name"
          label="이름"
          rules={[{ required: true, message: '이름은 필수입니다' }]}
        >
          <Input maxLength={80} />
        </Form.Item>
        <Form.Item name="role" label="권한">
          <Select options={MANUAL_REGISTER_ROLES} allowClear placeholder="선택 (미선택 가능)" />
        </Form.Item>
        <Form.Item name="orgName" label="조직명">
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="costCenterCode" label="지점코드">
          <Input maxLength={10} />
        </Form.Item>
        <Form.Item name="jobCode" label="직무코드">
          <Input maxLength={40} placeholder="예: 영업직, 판촉직" />
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
        <Form.Item name="startDate" label="입사일">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="homePhone" label="집 전화">
          <Input maxLength={255} />
        </Form.Item>
        <Form.Item name="workPhone" label="업무 전화">
          <Input maxLength={255} />
        </Form.Item>
        <Form.Item
          name="workEmail"
          label="업무 이메일"
          rules={[{ type: 'email', message: '이메일 형식이 올바르지 않습니다' }]}
        >
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="professionalPromotionTeam" label="전문행사조">
          <Select options={PPT_OPTIONS} allowClear placeholder="선택" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
