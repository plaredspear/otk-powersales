import { Alert, Button, Form, Input, Space } from 'antd';
import type { FormInstance } from 'antd';

/**
 * 시스템 관리자 수동 등록 폼 (Spec #579).
 *
 * - 사번은 `addonBefore="ADMIN-"` 으로 prefix 시각 분리, 사용자는 본문만 입력.
 * - 비밀번호 정책 클라이언트 검증: 8~64자 + 영문/숫자/특수문자 중 2종 이상 + 동일 문자 4회 반복 금지.
 * - 폼 제출 책임은 부모(`onSubmit`) 위임. 부모가 mutation 호출과 navigate를 담당한다.
 * - 사번 본문(`employeeCodeBody`) 은 폼 내부 상태이며, 부모에는 `ADMIN-{본문}` 합쳐진 `employeeCode` 만 전달한다.
 */

export interface AdminAccountRegisterFormValues {
  employeeCode: string;
  name: string;
  password: string;
  passwordConfirm: string;
  workEmail?: string;
  workPhone?: string;
  orgName?: string;
  costCenterCode?: string;
}

interface RawFormValues {
  employeeCodeBody: string;
  name: string;
  password: string;
  passwordConfirm: string;
  workEmail?: string;
  workPhone?: string;
  orgName?: string;
  costCenterCode?: string;
}

interface AdminAccountRegisterFormProps {
  form: FormInstance<RawFormValues>;
  isSubmitting: boolean;
  onSubmit: (values: AdminAccountRegisterFormValues) => Promise<void> | void;
  onCancel: () => void;
}

const EMPLOYEE_CODE_BODY_PATTERN = /^[A-Za-z0-9_-]{1,30}$/;
const SPECIAL_CHARS = "!@#$%^&*()_+-=[]{};':\"|,.<>/?";

function countCategories(value: string): number {
  let categories = 0;
  if (/[A-Z]/.test(value)) categories++;
  if (/[a-z]/.test(value)) categories++;
  if (/\d/.test(value)) categories++;
  for (const ch of value) {
    if (SPECIAL_CHARS.includes(ch)) {
      categories++;
      break;
    }
  }
  return categories;
}

function hasFourConsecutiveSameChars(value: string): boolean {
  if (value.length < 4) return false;
  let run = 1;
  for (let i = 1; i < value.length; i++) {
    if (value[i] === value[i - 1]) {
      run++;
      if (run >= 4) return true;
    } else {
      run = 1;
    }
  }
  return false;
}

export default function AdminAccountRegisterForm({ form, isSubmitting, onSubmit, onCancel }: AdminAccountRegisterFormProps) {
  const handleFinish = async (values: RawFormValues) => {
    const payload: AdminAccountRegisterFormValues = {
      employeeCode: `ADMIN-${values.employeeCodeBody}`,
      name: values.name,
      password: values.password,
      passwordConfirm: values.passwordConfirm,
      workEmail: values.workEmail,
      workPhone: values.workPhone,
      orgName: values.orgName,
      costCenterCode: values.costCenterCode,
    };
    await onSubmit(payload);
  };

  return (
    <Form<RawFormValues>
      form={form}
      layout="vertical"
      onFinish={handleFinish}
      autoComplete="off"
      requiredMark
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
        message="이 화면은 SAP 인바운드로 동기화되지 않는 시스템 관리자 계정을 별도로 등록합니다."
        description="등록되는 계정은 항상 [시스템관리자] 권한을 가지며, 사번은 반드시 ADMIN- 으로 시작해야 합니다."
      />

      <Form.Item
        label="사번"
        name="employeeCodeBody"
        required
        rules={[
          { required: true, message: '사번을 입력해주세요' },
          {
            validator: (_, value: string) => {
              if (!value) return Promise.resolve();
              if (!EMPLOYEE_CODE_BODY_PATTERN.test(value)) {
                return Promise.reject(
                  new Error('사번은 ADMIN- 으로 시작하고 영문/숫자/하이픈/언더스코어만 사용 (1~30자)'),
                );
              }
              return Promise.resolve();
            },
          },
        ]}
        extra="ADMIN-{사번 본문} 형식. 영문/숫자/-/_ 가능 (1~30자)"
      >
        <Input addonBefore="ADMIN-" placeholder="예: 001" maxLength={30} />
      </Form.Item>

      <Form.Item
        label="이름"
        name="name"
        required
        rules={[
          { required: true, message: '이름을 입력해주세요 (최대 80자)' },
          { max: 80, message: '이름을 입력해주세요 (최대 80자)' },
        ]}
      >
        <Input placeholder="관리자 이름" maxLength={80} />
      </Form.Item>

      <Form.Item
        label="비밀번호"
        name="password"
        required
        rules={[
          { required: true, message: '비밀번호를 입력해주세요' },
          {
            validator: (_, value: string) => {
              if (!value) return Promise.resolve();
              if (value.length < 8 || value.length > 64) {
                return Promise.reject(new Error('비밀번호는 8자 이상 64자 이하여야 합니다'));
              }
              if (countCategories(value) < 2) {
                return Promise.reject(new Error('영문, 숫자, 특수문자 중 2종 이상을 조합해주세요'));
              }
              if (hasFourConsecutiveSameChars(value)) {
                return Promise.reject(new Error('동일한 문자를 4회 이상 연속 사용할 수 없습니다'));
              }
              return Promise.resolve();
            },
          },
        ]}
        extra="8~64자, 영문/숫자/특수문자 중 2종 이상 조합"
      >
        <Input.Password placeholder="초기 비밀번호" maxLength={64} autoComplete="new-password" />
      </Form.Item>

      <Form.Item
        label="비밀번호 확인"
        name="passwordConfirm"
        dependencies={['password']}
        required
        rules={[
          { required: true, message: '비밀번호 확인을 입력해주세요' },
          ({ getFieldValue }) => ({
            validator: (_, value: string) => {
              if (!value || value === getFieldValue('password')) {
                return Promise.resolve();
              }
              return Promise.reject(new Error('비밀번호가 일치하지 않습니다'));
            },
          }),
        ]}
      >
        <Input.Password placeholder="비밀번호 재입력" maxLength={64} autoComplete="new-password" />
      </Form.Item>

      <Form.Item
        label="업무 이메일"
        name="workEmail"
        rules={[
          { type: 'email', message: '올바른 이메일 형식이 아닙니다' },
          { max: 100, message: '이메일은 최대 100자입니다' },
        ]}
      >
        <Input placeholder="(선택) 업무 이메일" maxLength={100} />
      </Form.Item>

      <Form.Item
        label="사무실 전화"
        name="workPhone"
        rules={[{ max: 30, message: '전화번호는 최대 30자입니다' }]}
      >
        <Input placeholder="(선택) 사무실 전화" maxLength={30} />
      </Form.Item>

      <Form.Item
        label="조직명"
        name="orgName"
        rules={[{ max: 100, message: '조직명은 최대 100자입니다' }]}
      >
        <Input placeholder="(선택) 조직명" maxLength={100} />
      </Form.Item>

      <Form.Item
        label="코스트센터 코드"
        name="costCenterCode"
        rules={[{ max: 10, message: '코스트센터 코드는 최대 10자입니다' }]}
      >
        <Input placeholder="(선택) 코스트센터 코드" maxLength={10} />
      </Form.Item>

      <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
        <Space>
          <Button onClick={onCancel}>취소</Button>
          <Button type="primary" htmlType="submit" loading={isSubmitting}>
            등록하기
          </Button>
        </Space>
      </Form.Item>
    </Form>
  );
}
