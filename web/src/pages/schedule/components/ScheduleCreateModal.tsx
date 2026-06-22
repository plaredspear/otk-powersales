import { useEffect, useState } from 'react';
import { Modal, Form, DatePicker, Select, message, Alert, Row, Col, Typography, Badge, Checkbox } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { AxiosError } from 'axios';
import EmployeeSelect from './EmployeeSelect';
import AccountSelect from './AccountSelect';
import { useScheduleCreate, useScheduleUpdate, useScheduleDetail } from '@/hooks/schedule/useScheduleCreate';
import type { ScheduleCreateRequest, ScheduleListItem } from '@/api/schedule';
import { isApiErrorBody } from '@/api/types';

const { Text } = Typography;

const WORK_TYPE1_OPTIONS = [{ label: '진열', value: '진열' }];

const WORK_TYPE3_OPTIONS = [
  { label: '고정', value: '고정' },
  { label: '격고', value: '격고' },
  { label: '순회', value: '순회' },
];

const WORK_TYPE4_OPTIONS = [
  { label: '상온', value: '상온' },
  { label: '냉동/냉장', value: '냉동/냉장' },
];

const WORK_TYPE5_OPTIONS = [
  { label: '상시', value: '상시' },
  { label: '임시', value: '임시' },
];

const CALC_HINT = '저장 시 이 필드가 계산됨';

export interface ScheduleCreateModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  /** edit 모드일 때 초기값 + scheduleId. 없으면 create 모드. */
  editTarget?: ScheduleListItem | null;
}

interface FormValues {
  employeeCode?: string;
  accountCode?: string;
  typeOfWork1?: string;
  typeOfWork3?: string;
  typeOfWork4?: string;
  typeOfWork5?: string;
  startDate?: Dayjs;
  endDate?: Dayjs | null;
}

/** SF 「저장 시 이 필드가 계산됨」 readonly 정보 1행 (라벨 + 값 + 회색 캡션). */
function CalcField({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>{label}</div>
      <div style={{ minHeight: 22 }}>{value ?? <Text type="secondary">-</Text>}</div>
      <Text type="secondary" italic style={{ fontSize: 12 }}>
        {CALC_HINT}
      </Text>
    </div>
  );
}

/** 유효 신호등 — SF Valid__c 이미지(green/yellow/red) 동등. */
function ValidLight({ valid }: { valid: string | null | undefined }) {
  if (valid === '유효') return <Badge color="green" text="유효" />;
  if (valid === '예정') return <Badge color="gold" text="예정" />;
  if (valid === '종료') return <Badge color="red" text="종료" />;
  return <Text type="secondary">-</Text>;
}

/**
 * 진열사원 스케줄 단건 신규 등록 / 편집 Modal.
 *
 * SF 「진열사원 스케줄 마스터」 편집 레이아웃 정합 — 좌측 편집 필드 +
 * 우측 readonly 계산 정보 (SF 「저장 시 이 필드가 계산됨」). 편집 모드에서는
 * `GET /api/v1/admin/display-work-schedule/{id}` 로 상세를 조회해 readonly 정보를 채운다.
 *
 * 검증/자동채움은 backend `POST /api/v1/admin/display-work-schedule` (신규) 또는 `PUT /{id}` (편집) 에서 일괄 처리.
 * 편집 모드에서 confirmed=true 인 레코드를 ADMIN_GRADE 외 사용자가 종료일 외 필드를 변경 시도하면
 * backend 가 `SCHEDULE_EDIT_BLOCKED_AFTER_CONFIRM` (409) 로 차단.
 */
export default function ScheduleCreateModal({ open, onClose, onSuccess, editTarget }: ScheduleCreateModalProps) {
  const [form] = Form.useForm<FormValues>();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const createMutation = useScheduleCreate();
  const updateMutation = useScheduleUpdate();

  const isEdit = editTarget != null;
  const mutation = isEdit ? updateMutation : createMutation;

  const { data: detail, isLoading: detailLoading } = useScheduleDetail(editTarget?.id ?? null, open && isEdit);

  useEffect(() => {
    if (!open) return;
    setErrorMessage(null);
    if (isEdit && detail) {
      form.setFieldsValue({
        employeeCode: detail.employeeCode || undefined,
        accountCode: detail.accountCode ?? undefined,
        typeOfWork1: detail.typeOfWork1 ?? '진열',
        typeOfWork3: detail.typeOfWork3 ?? undefined,
        typeOfWork4: detail.typeOfWork4 ?? undefined,
        typeOfWork5: detail.typeOfWork5 ?? undefined,
        startDate: detail.startDate ? dayjs(detail.startDate) : undefined,
        endDate: detail.endDate ? dayjs(detail.endDate) : null,
      });
    } else if (!isEdit) {
      form.resetFields();
      form.setFieldValue('typeOfWork1', '진열');
    }
  }, [open, isEdit, detail, form]);

  const handleOk = async () => {
    setErrorMessage(null);
    try {
      const values = await form.validateFields();
      if (!values.startDate) {
        setErrorMessage('시작일은 필수입니다');
        return;
      }
      const payload: ScheduleCreateRequest = {
        employeeCode: values.employeeCode!,
        accountCode: values.accountCode!,
        typeOfWork1: values.typeOfWork1!,
        typeOfWork3: values.typeOfWork3!,
        typeOfWork4: values.typeOfWork4!,
        typeOfWork5: values.typeOfWork5!,
        startDate: values.startDate.format('YYYY-MM-DD'),
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : null,
      };
      if (isEdit && editTarget) {
        const result = await updateMutation.mutateAsync({ id: editTarget.id, payload });
        message.success(`스케줄이 수정되었습니다 (사원: ${result.employeeName} / 거래처: ${result.accountName ?? '-'})`);
      } else {
        const result = await createMutation.mutateAsync(payload);
        message.success(`스케줄이 등록되었습니다 (사원: ${result.employeeName} / 거래처: ${result.accountName ?? '-'})`);
      }
      onSuccess();
      onClose();
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        // Form validation error — antd 가 자체 표시
        return;
      }
      // 백엔드 에러 봉투 { success:false, error:{ code, message } } 에서 메시지 추출.
      // 4xx 는 axios 가 throw 하므로 응답 본문은 err.response.data 에 들어 있다.
      const msg =
        err instanceof AxiosError && isApiErrorBody(err.response?.data)
          ? err.response!.data.error!.message
          : err instanceof Error
            ? err.message
            : '스케줄 저장에 실패했습니다';
      setErrorMessage(msg);
    }
  };

  const numberFmt = (v: number | null | undefined) => (v != null ? v.toLocaleString('ko-KR') : '-');

  return (
    <Modal
      title={isEdit ? '진열사원 스케줄 편집' : '진열사원 스케줄 신규 등록'}
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      okText="저장"
      cancelText="취소"
      confirmLoading={mutation.isPending}
      width={isEdit ? 880 : 560}
    >
      {errorMessage && (
        <Alert type="error" message={errorMessage} style={{ marginBottom: 16 }} closable onClose={() => setErrorMessage(null)} />
      )}
      {isEdit && detail?.confirmed && (
        <Alert
          type="warning"
          message="확정된 스케줄입니다. 시스템 관리자 / 영업지원이 아닌 경우 종료일 외 필드 변경 시 저장이 차단됩니다."
          style={{ marginBottom: 16 }}
        />
      )}
      {isEdit && (
        <div style={{ textAlign: 'right', marginBottom: 8 }}>
          <Text type="danger">*</Text> <Text type="secondary">= 필수 정보</Text>
        </div>
      )}

      <Row gutter={32}>
        {/* 좌측: 편집 가능 필드 */}
        <Col span={isEdit ? 12 : 24}>
          {isEdit && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>No.</div>
              <div>{detail?.name ?? '-'}</div>
            </div>
          )}
          {isEdit && (
            <div style={{ marginBottom: 16 }}>
              <Checkbox checked={detail?.confirmed ?? false} disabled>
                확정
              </Checkbox>
            </div>
          )}
          <Form form={form} layout="vertical" preserve={false}>
            <Form.Item name="employeeCode" label="성명" rules={[{ required: true, message: '성명은 필수입니다' }]}>
              <EmployeeSelect
                value={form.getFieldValue('employeeCode')}
                initialLabel={detail ? `${detail.employeeName}(${detail.employeeCode})` : undefined}
                onChange={(code) => form.setFieldValue('employeeCode', code)}
              />
            </Form.Item>

            <Form.Item name="accountCode" label="거래처명" rules={[{ required: true, message: '거래처명은 필수입니다' }]}>
              <AccountSelect
                value={form.getFieldValue('accountCode')}
                initialLabel={detail?.accountName ?? undefined}
                onChange={(code) => form.setFieldValue('accountCode', code)}
              />
            </Form.Item>

            <Form.Item name="typeOfWork1" label="근무형태1" rules={[{ required: true, message: '근무형태1은 필수입니다' }]}>
              <Select placeholder="진열" options={WORK_TYPE1_OPTIONS} />
            </Form.Item>

            <Form.Item name="typeOfWork3" label="근무형태3" rules={[{ required: true, message: '근무형태3은 필수입니다' }]}>
              <Select placeholder="고정 / 격고 / 순회" options={WORK_TYPE3_OPTIONS} />
            </Form.Item>

            <Form.Item name="typeOfWork4" label="근무형태4" rules={[{ required: true, message: '근무형태4는 필수입니다' }]}>
              <Select placeholder="상온 / 냉동·냉장" options={WORK_TYPE4_OPTIONS} />
            </Form.Item>

            <Form.Item name="typeOfWork5" label="근무형태5" rules={[{ required: true, message: '근무형태5는 필수입니다' }]}>
              <Select placeholder="상시 / 임시" options={WORK_TYPE5_OPTIONS} />
            </Form.Item>

            <Form.Item name="startDate" label="시작일" rules={[{ required: true, message: '시작일은 필수입니다' }]}>
              <DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} placeholder="시작일" />
            </Form.Item>

            <Form.Item name="endDate" label="종료일">
              <DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} placeholder="종료일 (선택)" />
            </Form.Item>
          </Form>
        </Col>

        {/* 우측: readonly 계산 정보 (SF 「저장 시 이 필드가 계산됨」) — 편집 모드에서만 */}
        {isEdit && (
          <Col span={12}>
            <CalcField label="사번" value={detailLoading ? '…' : detail?.employeeCode} />
            <CalcField label="지점명" value={detailLoading ? '…' : detail?.branchName} />
            <CalcField label="직위" value={detailLoading ? '…' : detail?.title} />
            <CalcField label="재직상태" value={detailLoading ? '…' : detail?.employmentStatus} />
            <CalcField label="거래처코드" value={detailLoading ? '…' : detail?.accountCode} />
            <CalcField label="거래처상태" value={detailLoading ? '…' : detail?.accountStatus} />
            <CalcField label="거래처유형" value={detailLoading ? '…' : detail?.accountType} />
            <CalcField label="유효" value={detailLoading ? '…' : <ValidLight valid={detail?.valid} />} />
            <CalcField label="유효데이터" value={detailLoading ? '…' : detail?.validData} />
            <CalcField label="전월매출" value={detailLoading ? '…' : numberFmt(detail?.lastMonthRevenue)} />
            <CalcField label="조직유형" value={detailLoading ? '…' : detail?.costCenterCode} />
          </Col>
        )}
      </Row>
    </Modal>
  );
}
