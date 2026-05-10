import { useEffect, useMemo, useState } from 'react';
import { Alert, Col, Divider, Form, Input, InputNumber, Modal, Row, Switch, notification } from 'antd';
import type { FormInstance } from 'antd';
import { isAxiosError } from 'axios';
import { useUpdateAccountMutation } from '@/hooks/account/useUpdateAccountMutation';
import { isApiErrorBody } from '@/api/types';
import type { Account, AdminAccountUpdateRequest } from '@/api/account';
import EmployeeSelect from './components/EmployeeSelect';

/**
 * 관리자 웹 거래처 수정 모달. (Spec #643 P2-W)
 *
 * Backend `PUT /api/v1/admin/accounts/{id}` 호출. PUT 부분 갱신 시맨틱 (Q-E):
 * 사용자가 변경한 필드만 PUT body 에 포함, 미변경 필드는 미전송 → Backend 단 보존.
 *
 * **prefill 한계**: 거래처 list endpoint 응답이 일부 필드(`name`/`abcType`/`branchCode`/
 * `branchName`/`employeeCode`/`address1`/`phone`) 만 보유하여, 그 외 필드(이메일/팩스/
 * 영업시간/프리저 등) 는 빈 입력으로 표시된다. 거래처 상세 조회 endpoint 신설은 본 P2-W 비범위
 * (`docs/specs/ready/643-admin-account-update/P2-W.md` §"본 Part 비범위"). 운영 UX 한계로
 * 후속 스펙에서 list 응답 확장 또는 상세 endpoint 도입 검토.
 *
 * Backend `AccountNamePrefix.ALLOWED` 와 동일하게 유지. 운영 변경 시 Backend Constants 와
 * 동시 갱신 (`account/policy/AccountNamePrefix.kt`). #640 P2-W 와 동일 정책.
 */
const ACCOUNT_NAME_PREFIXES = ['(신규)', '(기타)'] as const;
const PREFIX_DISPLAY = ACCOUNT_NAME_PREFIXES.join(' / ');
const PREFIX_GUIDE = `※ ${PREFIX_DISPLAY} 중 1개를 거래처명에 포함해 주세요.`;

interface FormValues {
  name?: string;
  zipCode?: string;
  address1?: string;
  address2?: string;
  branchCode?: string;
  branchName?: string;
  representative?: string;
  phone?: string;
  mobilePhone?: string;
  email?: string;
  fax?: string;
  abcType?: string;
  freezerInstalled?: boolean;
  freezerType?: string;
  closingTime1?: string;
  closingTime2?: string;
  closingTime3?: string;
  industry?: string;
  description?: string;
  website?: string;
  numberOfEmployees?: number;
}

export interface AdminAccountUpdateModalProps {
  open: boolean;
  account: Account | null;
  onClose: () => void;
  onSuccess?: () => void;
}

function nameContainsPrefix(name: string): boolean {
  const trimmed = name.trim();
  return ACCOUNT_NAME_PREFIXES.some((prefix) => trimmed.includes(prefix));
}

/**
 * 변경된 필드만 PUT body 에 포함 (Q-E PUT 부분 갱신 시맨틱).
 *
 * - `formState[field] === initialState[field]` → 미변경 → body 미포함
 * - `formState[field]` 가 변경됨 → body 포함 (빈 문자열도 명시 변경으로 간주, Backend 단에서
 *   `employeeCode = ""` 만 null 동등 처리)
 *
 * 본 단순화는 옵션 A — null 명시 = null 로 덮어쓰기 시맨틱 미지원 (Backend 단 nullable 필드
 * = 보존 처리, 본 P1-B 결정 정합). 운영 UX 상 "필드를 빈 값으로 덮어쓰기" 빈도 낮음.
 */
function diffPayload(initial: FormValues, current: FormValues): AdminAccountUpdateRequest {
  const result: AdminAccountUpdateRequest = {};
  (Object.keys(current) as Array<keyof FormValues>).forEach((key) => {
    const initialValue = initial[key];
    const currentValue = current[key];
    if (initialValue !== currentValue) {
      // empty string ("") 는 그대로 전송 (Backend 단 employeeCode 만 null 동등 처리)
      // Backend 단 다른 필드는 단순 string 갱신
      const target = result as Record<string, unknown>;
      target[key] = currentValue;
    }
  });
  return result;
}

export default function AdminAccountUpdateModal({
  open,
  account,
  onClose,
  onSuccess,
}: AdminAccountUpdateModalProps) {
  const [form] = Form.useForm<FormValues>();
  const [employeeCode, setEmployeeCode] = useState<string | undefined>();
  const [initialValues, setInitialValues] = useState<FormValues>({});
  const [initialEmployeeCode, setInitialEmployeeCode] = useState<string | undefined>();
  const mutation = useUpdateAccountMutation();

  useEffect(() => {
    if (open && account) {
      // 본 P2-W 옵션 A: list 응답이 보유한 필드만 prefill, 그 외는 빈 입력
      const prefilled: FormValues = {
        name: account.name ?? '',
        address1: account.address1 ?? '',
        branchCode: account.branchCode ?? '',
        branchName: account.branchName ?? '',
        phone: account.phone ?? '',
        abcType: account.abcType ?? '',
      };
      form.setFieldsValue(prefilled);
      setInitialValues(prefilled);
      setEmployeeCode(account.employeeCode ?? undefined);
      setInitialEmployeeCode(account.employeeCode ?? undefined);
      mutation.reset();
    }
    if (!open) {
      form.resetFields();
      setEmployeeCode(undefined);
      setInitialValues({});
      setInitialEmployeeCode(undefined);
      mutation.reset();
    }
    // mutation 인스턴스는 매 렌더에서 새로 생성되지 않음
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, account]);

  const handleSubmit = async () => {
    if (!account) return;
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    // 폼 필드 변경분 추출
    const fieldDiff = diffPayload(initialValues, values);

    // employeeCode 는 별도 state — 변경 감지
    const employeeChanged = (employeeCode ?? '') !== (initialEmployeeCode ?? '');
    const payload: AdminAccountUpdateRequest = { ...fieldDiff };
    if (employeeChanged) {
      // Backend 단: ""=null 동등 (변경 안 함). undefined → ""로 변환해야 명시적 빈 선택 전달
      payload.employeeCode = employeeCode ?? '';
    }

    if (Object.keys(payload).length === 0) {
      // 변경 없음 — PUT skip + 모달 close
      notification.info({ message: '변경된 내용이 없습니다.' });
      onClose();
      return;
    }

    try {
      const updated = await mutation.mutateAsync({ id: account.id, payload });
      notification.success({
        message: '거래처가 수정되었습니다.',
        description: updated.name ?? account.name ?? undefined,
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      handleError(err, form);
    }
  };

  const isSubmitting = mutation.isPending;

  const noteText = useMemo(
    () =>
      '※ 거래처 목록 응답에 없는 필드(이메일/팩스/영업시간/프리저 등)는 빈 입력으로 표시됩니다. 변경할 필드만 입력해 주세요. 입력하지 않은 필드는 보존됩니다.',
    [],
  );

  return (
    <Modal
      open={open}
      title={`거래처 수정 — ${account?.name ?? ''}`}
      okText="저장"
      cancelText="취소"
      okButtonProps={{ loading: isSubmitting }}
      cancelButtonProps={{ disabled: isSubmitting }}
      onOk={handleSubmit}
      onCancel={() => {
        if (!isSubmitting) onClose();
      }}
      maskClosable={!isSubmitting}
      closable={!isSubmitting}
      destroyOnHidden
      width={720}
    >
      <Alert type="info" showIcon style={{ marginBottom: 16 }} message={noteText} />

      <Form<FormValues> form={form} layout="vertical" requiredMark={false}>
        <Divider orientation="left" plain>
          기본 정보
        </Divider>
        <Form.Item
          label="거래처명"
          name="name"
          extra={PREFIX_GUIDE}
          rules={[
            { max: 255, message: '거래처명은 255자 이하여야 합니다.' },
            {
              validator: (_rule, value: string | undefined) => {
                if (!value || value.trim().length === 0) {
                  // 변경 후 빈 값 = blank — Backend 단 ACCOUNT_NAME_BLANK
                  return Promise.reject(new Error('거래처명을 입력해 주세요.'));
                }
                if (!nameContainsPrefix(value)) {
                  return Promise.reject(new Error(`거래처명에 ${PREFIX_DISPLAY} 중 1개를 포함해 주세요.`));
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          <Input placeholder="(신규) 강남점" maxLength={255} disabled={isSubmitting} />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="거래처 그룹">
              <Input value={account?.accountStatusName ? '9999 (영업사원 직접 등록)' : '-'} readOnly disabled />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="계정 ID">
              <Input value={account ? String(account.id) : ''} readOnly disabled />
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left" plain>
          주소
        </Divider>
        <Row gutter={16}>
          <Col span={6}>
            <Form.Item
              label="우편번호"
              name="zipCode"
              rules={[{ max: 100, message: '우편번호는 100자 이하여야 합니다.' }]}
            >
              <Input maxLength={100} disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={18}>
            <Form.Item
              label="주소1"
              name="address1"
              rules={[{ max: 120, message: '주소1은 120자 이하여야 합니다.' }]}
            >
              <Input maxLength={120} disabled={isSubmitting} />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item
          label="주소2"
          name="address2"
          rules={[{ max: 120, message: '주소2는 120자 이하여야 합니다.' }]}
          extra="※ 주소 변경 시 좌표는 다음 batch 사이클에서 재산출됩니다."
        >
          <Input maxLength={120} disabled={isSubmitting} />
        </Form.Item>

        <Divider orientation="left" plain>
          담당자
        </Divider>
        <Form.Item
          label="담당 영업사원"
          extra="※ 변경 시 지점 코드 / 지점명은 자동 재계산되지 않습니다 — 필요 시 직접 수정해 주세요."
        >
          <EmployeeSelect
            value={employeeCode}
            disabled={isSubmitting}
            onChange={(code) => setEmployeeCode(code)}
            placeholder="사번 또는 이름으로 검색 (선택 해제 가능)"
          />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="지점 코드"
              name="branchCode"
              rules={[{ max: 100, message: '지점 코드는 100자 이하여야 합니다.' }]}
            >
              <Input maxLength={100} disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="지점명"
              name="branchName"
              rules={[{ max: 250, message: '지점명은 250자 이하여야 합니다.' }]}
            >
              <Input maxLength={250} disabled={isSubmitting} />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item
          label="대표자"
          name="representative"
          rules={[{ max: 100, message: '대표자는 100자 이하여야 합니다.' }]}
        >
          <Input maxLength={100} disabled={isSubmitting} />
        </Form.Item>

        <Divider orientation="left" plain>
          연락처
        </Divider>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="전화"
              name="phone"
              rules={[{ max: 40, message: '전화번호는 40자 이하여야 합니다.' }]}
            >
              <Input maxLength={40} disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="휴대전화"
              name="mobilePhone"
              rules={[{ max: 40, message: '휴대전화는 40자 이하여야 합니다.' }]}
            >
              <Input maxLength={40} disabled={isSubmitting} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="이메일"
              name="email"
              rules={[
                { type: 'email', message: '올바른 이메일 형식이 아닙니다.' },
                { max: 100, message: '이메일은 100자 이하여야 합니다.' },
              ]}
            >
              <Input maxLength={100} disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="팩스"
              name="fax"
              rules={[{ max: 40, message: '팩스는 40자 이하여야 합니다.' }]}
            >
              <Input maxLength={40} disabled={isSubmitting} />
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left" plain>
          영업 정보
        </Divider>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label="ABC 등급"
              name="abcType"
              rules={[{ max: 20, message: 'ABC 등급은 20자 이하여야 합니다.' }]}
            >
              <Input maxLength={20} disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item label="프리저 설치" name="freezerInstalled" valuePropName="checked">
              <Switch disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="프리저 타입"
              name="freezerType"
              rules={[{ max: 20, message: '프리저 타입은 20자 이하여야 합니다.' }]}
            >
              <Input maxLength={20} disabled={isSubmitting} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label="영업시간 1"
              name="closingTime1"
              rules={[{ max: 50, message: '영업시간1은 50자 이하여야 합니다.' }]}
            >
              <Input maxLength={50} disabled={isSubmitting} placeholder="예: 09:00" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="영업시간 2"
              name="closingTime2"
              rules={[{ max: 50, message: '영업시간2는 50자 이하여야 합니다.' }]}
            >
              <Input maxLength={50} disabled={isSubmitting} placeholder="예: 12:00" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="영업시간 3"
              name="closingTime3"
              rules={[{ max: 50, message: '영업시간3은 50자 이하여야 합니다.' }]}
            >
              <Input maxLength={50} disabled={isSubmitting} placeholder="예: 18:00" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item
          label="업종"
          name="industry"
          rules={[{ max: 255, message: '업종은 255자 이하여야 합니다.' }]}
        >
          <Input maxLength={255} disabled={isSubmitting} />
        </Form.Item>
        <Form.Item label="설명" name="description">
          <Input.TextArea rows={3} disabled={isSubmitting} />
        </Form.Item>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="웹사이트"
              name="website"
              rules={[{ max: 255, message: '웹사이트는 255자 이하여야 합니다.' }]}
            >
              <Input maxLength={255} disabled={isSubmitting} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="직원 수" name="numberOfEmployees">
              <InputNumber min={0} style={{ width: '100%' }} disabled={isSubmitting} />
            </Form.Item>
          </Col>
        </Row>

        {account?.externalKey && (
          <>
            <Divider orientation="left" plain>
              시스템 동기 정보 (readonly)
            </Divider>
            <Form.Item label="External Key">
              <Input value={account.externalKey} readOnly disabled />
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
}

function handleError(err: unknown, form: FormInstance<FormValues>): void {
  if (isAxiosError(err)) {
    const status = err.response?.status;
    const body = err.response?.data;
    if (isApiErrorBody(body)) {
      const code = body.error!.code;
      const message = body.error!.message;
      switch (code) {
        case 'ACCOUNT_NAME_DUPLICATE':
          notification.error({ message: '거래처 수정 실패', description: message });
          form.setFields([{ name: 'name', errors: [message] }]);
          return;
        case 'ACCOUNT_NAME_PREFIX_REQUIRED':
        case 'ACCOUNT_NAME_BLANK':
          form.setFields([{ name: 'name', errors: [message] }]);
          notification.error({ message: '거래처 수정 실패', description: message });
          return;
        case 'ACCOUNT_NOT_FOUND':
          notification.error({
            message: '거래처 수정 실패',
            description: '거래처를 찾을 수 없습니다. 목록을 새로고침해 주세요.',
          });
          return;
        case 'EMPLOYEE_NOT_FOUND':
          notification.error({ message: '거래처 수정 실패', description: message });
          return;
        default:
          notification.error({ message: '거래처 수정 실패', description: message });
          return;
      }
    }
    if (status === 401) {
      // axios interceptor 가 로그인 리다이렉트 처리
      return;
    }
    if (status === 403) {
      notification.error({ message: '거래처 수정 실패', description: '수정 권한이 없습니다.' });
      return;
    }
    if (status && status >= 500) {
      notification.error({
        message: '거래처 수정 실패',
        description: '수정 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
      });
      return;
    }
  }
  notification.error({
    message: '거래처 수정 실패',
    description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.',
  });
}
