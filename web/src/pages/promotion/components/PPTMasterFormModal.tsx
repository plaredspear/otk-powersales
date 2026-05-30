import { useEffect, useState, useCallback } from 'react';
import { Modal, Form, Select, DatePicker, Checkbox, Button, message } from 'antd';
import dayjs from 'dayjs';
import { fetchEmployeesForPromotionLookup, type Employee } from '@/api/employee';
import { fetchAccountsForPromotionLookup, type Account } from '@/api/account';
import { useCreatePPTMaster, useUpdatePPTMaster } from '@/hooks/promotion/usePPTMasters';
import type { PPTMaster } from '@/api/pptMaster';
import { AxiosError } from 'axios';
import { PPT_TEAM_TYPE_OPTIONS, type PPTTeamType } from '@/constants/pptTeamType';

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
      if (err instanceof AxiosError && err.response?.status === 409) {
        message.error('중복으로 유효한 마스터가 존재합니다');
      } else if (
        err &&
        typeof err === 'object' &&
        'message' in err &&
        typeof (err as { message: unknown }).message === 'string'
      ) {
        message.error((err as { message: string }).message);
      }
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
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
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
            options={employeeOptions.map((emp) => ({
              value: emp.id,
              label: `${emp.name} (${emp.employeeCode})${emp.orgName ? ` ${emp.orgName}` : ''}`,
            }))}
          />
        </Form.Item>

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
              label: `${acc.name ?? ''} (${acc.externalKey ?? ''})`,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="teamType"
          label="전문행사조"
          rules={[{ required: true, message: '전문행사조를 선택해주세요' }]}
        >
          <Select placeholder="전문행사조 선택" options={PPT_TEAM_TYPE_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="startDate"
          label="시작일"
          rules={[{ required: true, message: '시작일을 선택해주세요' }]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="endDate" label="종료일">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="isConfirmed" valuePropName="checked">
          <Checkbox>확정 여부</Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  );
}
