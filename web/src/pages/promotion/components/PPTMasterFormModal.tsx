import { useEffect, useState, useCallback } from 'react';
import { Modal, Form, Select, DatePicker, Checkbox, Button, Alert, message } from 'antd';
import dayjs from 'dayjs';
import { fetchEmployeesForPromotionLookup, type Employee } from '@/api/employee';
import { fetchAccountsForPromotionLookup, type Account } from '@/api/account';
import { useCreatePPTMaster, useUpdatePPTMaster } from '@/hooks/promotion/usePPTMasters';
import { usePPTMasterFormMeta } from '@/hooks/promotion/usePPTMasterFormMeta';
import type { PPTMaster } from '@/api/pptMaster';
import { apiErrorMessage } from '@/lib/apiErrorMessage';
import { type PPTTeamType } from '@/constants/pptTeamType';

interface FormValues {
  employeeId: number;
  accountId: number;
  teamType: PPTTeamType;
  startDate: dayjs.Dayjs;
  endDate: dayjs.Dayjs | null;
  isConfirmed: boolean;
}

interface Props {
  open: boolean;
  editingItem: PPTMaster | null;
  cloneSource?: PPTMaster | null;
  onClose: () => void;
}

export default function PPTMasterFormModal({ open, editingItem, cloneSource, onClose }: Props) {
  const [form] = Form.useForm<FormValues>();
  const createMutation = useCreatePPTMaster();
  const updateMutation = useUpdatePPTMaster();
  const { data: formMeta } = usePPTMasterFormMeta();

  // 전문행사조 유형 옵션 — 서버 form-meta 를 단일 출처로 사용(프론트 상수 하드코딩 제거).
  const teamTypeOptions =
    formMeta?.teamTypes.map((t) => ({ value: t.value, label: t.name })) ?? [];

  // 사원에 반영된 이력이 있는 마스터는 서버가 사원 / 전문행사조 / 시작일 변경과 종료일 소급을 막는다
  // (반영된 사원 값이 근거를 잃고 남는 것을 차단). 복제는 신규 등록이므로 잠그지 않는다.
  const isApplied = editingItem?.applied === true;

  const [employeeOptions, setEmployeeOptions] = useState<Employee[]>([]);
  const [accountOptions, setAccountOptions] = useState<Account[]>([]);
  const [employeeLoading, setEmployeeLoading] = useState(false);
  const [accountLoading, setAccountLoading] = useState(false);

  useEffect(() => {
    // 복제 모드는 신규 등록(create) 으로 동작하되 폼 초기값을 cloneSource 로 채운다.
    const presetItem = editingItem ?? cloneSource ?? null;
    if (open && presetItem) {
      form.setFieldsValue({
        employeeId: presetItem.employeeId,
        accountId: presetItem.accountId,
        teamType: presetItem.teamType as PPTTeamType,
        startDate: dayjs(presetItem.startDate),
        endDate: presetItem.endDate ? dayjs(presetItem.endDate) : null,
        // 복제 시 확정 상태는 인계하지 않고 미확정으로 초기화
        isConfirmed: editingItem ? presetItem.isConfirmed : false,
      });
      setEmployeeOptions([
        {
          id: presetItem.employeeId,
          employeeCode: presetItem.employeeCode,
          name: presetItem.employeeName,
          status: null,
          gender: null,
          orgName: presetItem.branchName,
          costCenterCode: presetItem.branchCode,
          role: null,
          startDate: null,
          endDate: null,
          appLoginActive: null,
          workPhone: null,
          jikchak: null,
          jikwee: null,
          jikgub: null,
          jobCode: null,
          appointmentDate: null,
          ordDetailNode: null,
          jikjong: null,
          workEmail: null,
          phone: null,
          age: null,
          yearsOfService: null,
        },
      ]);
      setAccountOptions([
        {
          id: presetItem.accountId,
          externalKey: presetItem.accountCode,
          name: presetItem.accountName,
          abcType: null,
          branchCode: null,
          branchName: null,
          employeeCode: null,
          address1: null,
          phone: null,
          accountStatusName: null,
          accountType: null,
          zipCode: null,
          representative: null,
          ownerName: null,
          geocodeUnresolved: false,
          geocodeFailCount: 0,
        },
      ]);
    } else if (open) {
      form.resetFields();
      form.setFieldValue('isConfirmed', false);
      setEmployeeOptions([]);
      setAccountOptions([]);
    }
  }, [open, editingItem, cloneSource, form]);

  const searchEmployees = useCallback(async (keyword: string) => {
    if (!keyword || keyword.length < 2) return;
    setEmployeeLoading(true);
    try {
      const result = await fetchEmployeesForPromotionLookup({ keyword, size: 20 });
      setEmployeeOptions(result.content);
    } catch {
      // ignore
    } finally {
      setEmployeeLoading(false);
    }
  }, []);

  const searchAccounts = useCallback(async (keyword: string) => {
    if (!keyword || keyword.length < 2) return;
    setAccountLoading(true);
    try {
      const result = await fetchAccountsForPromotionLookup({ keyword, size: 20 });
      setAccountOptions(result.content);
    } catch {
      // ignore
    } finally {
      setAccountLoading(false);
    }
  }, []);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        employeeId: values.employeeId,
        accountId: values.accountId,
        teamType: values.teamType,
        startDate: values.startDate.format('YYYY-MM-DD'),
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : null,
        isConfirmed: values.isConfirmed ?? false,
      };

      if (editingItem) {
        await updateMutation.mutateAsync({ id: editingItem.id, data: payload });
        message.success('수정되었습니다');
      } else {
        await createMutation.mutateAsync(payload);
        message.success('등록되었습니다');
      }
      onClose();
    } catch (err) {
      // 중복 유효 마스터(409) / 반영 마스터 수정 차단(409) / 종료일 소급(400) 등 사유가 여러 갈래라
      // 서버 메시지를 그대로 노출한다.
      message.error(apiErrorMessage(err, '저장에 실패했습니다'));
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  const modalTitle = editingItem
    ? '전문행사조 마스터 수정'
    : cloneSource
    ? '전문행사조 마스터 복제'
    : '전문행사조 마스터 등록';

  return (
    <Modal
      title={modalTitle}
      open={open}
      onCancel={onClose}
      width={520}
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button onClick={onClose}>취소</Button>
          <Button type="primary" onClick={handleSave} loading={isSaving}>
            저장
          </Button>
        </div>
      }
    >
      {isApplied && (
        <Alert
          type="info"
          showIcon
          style={{ marginTop: 8 }}
          message="이미 사원에 반영된 마스터입니다"
          description="사원 / 전문행사조 / 시작일은 변경할 수 없습니다. 배정을 끝내려면 종료일을 오늘 이후로 지정해 주세요."
        />
      )}
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        {/* 항목 순서는 SF '새 전문행사조 마스터' 모달과 동일하게: 거래처 → 사원 → 시작일 → 확정 → 종료일 → 전문행사조 */}
        <Form.Item
          name="accountId"
          label="거래처"
          rules={[{ required: true, message: '거래처를 선택해주세요' }]}
        >
          <Select
            showSearch
            placeholder="거래처코드 또는 거래처명으로 검색 (2자 이상)"
            filterOption={false}
            onSearch={searchAccounts}
            loading={accountLoading}
            options={accountOptions.map((acc) => ({
              value: acc.id,
              // 거래처 검색은 지점 무관 전사 검색이라 동명 거래처 구분을 위해 지점명을 함께 노출한다.
              // preset(수정/복제) 항목은 목록 응답에 거래처 지점이 없어(행의 branchCode 는 사원 지점 축)
              // 지점명을 붙이지 않는다 — 잘못된 지점을 표시하지 않기 위함.
              label:
                `${acc.name ?? ''} (${acc.externalKey ?? ''})` +
                (acc.branchName ? ` · ${acc.branchName}` : ''),
            }))}
          />
        </Form.Item>

        <Form.Item
          name="employeeId"
          label="사원"
          rules={[{ required: true, message: '사원을 선택해주세요' }]}
        >
          <Select
            showSearch
            placeholder="사번 또는 이름으로 검색 (2자 이상)"
            filterOption={false}
            onSearch={searchEmployees}
            loading={employeeLoading}
            disabled={isApplied}
            options={employeeOptions.map((emp) => ({
              value: emp.id,
              label: `${emp.name} (${emp.employeeCode})${emp.orgName ? ` ${emp.orgName}` : ''}`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="startDate"
          label="시작일"
          rules={[{ required: true, message: '시작일을 선택해주세요' }]}
        >
          <DatePicker style={{ width: '100%' }} disabled={isApplied} />
        </Form.Item>

        <Form.Item name="isConfirmed" valuePropName="checked">
          <Checkbox>확정 여부</Checkbox>
        </Form.Item>

        <Form.Item name="endDate" label="종료일">
          {/* 수정 시 종료일 소급 금지 — 과거로 종료하면 해제 배치(종료일 = 오늘)가 잡지 못해 사원 값이 잔존한다. */}
          <DatePicker
            style={{ width: '100%' }}
            disabledDate={
              editingItem ? (d) => !!d && d.isBefore(dayjs().startOf('day')) : undefined
            }
          />
        </Form.Item>

        <Form.Item
          name="teamType"
          label="전문행사조"
          rules={[{ required: true, message: '전문행사조를 선택해주세요' }]}
        >
          <Select placeholder="전문행사조 선택" options={teamTypeOptions} disabled={isApplied} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
