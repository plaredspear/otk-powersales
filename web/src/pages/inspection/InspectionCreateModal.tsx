import { useEffect, useState } from 'react';
import {
  App,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Switch,
  Upload,
} from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import dayjs, { type Dayjs } from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import { fetchAccounts } from '@/api/account';
import { fetchProducts } from '@/api/product';
import { fetchThemes } from '@/api/inspectionThemes';
import { fetchUsers } from '@/api/user';
import { useCreateInspection, useUpdateInspection } from '@/hooks/inspections/useInspections';
import type {
  InspectionCategory,
  InspectionDetail,
  InspectionFieldTypeCode,
} from '@/api/inspections';

interface Props {
  open: boolean;
  onClose: () => void;
  /** 지정 시 수정 모드 — 상세 데이터로 초기값 채움. null/undefined 면 등록 모드. */
  editDetail?: InspectionDetail | null;
}

interface FormValues {
  category: InspectionCategory;
  themeId: number;
  accountId: number;
  employeeId: number;
  inspectionDate: Dayjs;
  fieldTypeCode: InspectionFieldTypeCode;
  description?: string;
  productCode?: string;
  competitorName?: string;
  competitorActivity?: string;
  competitorTasting?: boolean;
  competitorProductName?: string;
  competitorProductPrice?: number;
  competitorSalesQuantity?: number;
}

const FIELD_TYPE_OPTIONS = [
  { value: 'MAIN_SHELF', label: '본매대' },
  { value: 'EVENT_SHELF', label: '행사매대' },
  { value: 'TASTING', label: '시식' },
  { value: 'ETC', label: '기타' },
];

const MAX_PHOTO = 10;

interface Option {
  value: number | string;
  label: string;
}

export default function InspectionCreateModal({ open, onClose, editDetail }: Props) {
  const { message } = App.useApp();
  const [form] = Form.useForm<FormValues>();
  const category = Form.useWatch('category', form);
  const createMutation = useCreateInspection();
  const updateMutation = useUpdateInspection();
  const isEdit = editDetail != null;

  const [accountKeyword, setAccountKeyword] = useState('');
  const [productKeyword, setProductKeyword] = useState('');
  const [employeeKeyword, setEmployeeKeyword] = useState('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  // 수정 진입 시 현재 선택값을 옵션에 보존(검색 결과 밖일 수 있음).
  const [accountPatch, setAccountPatch] = useState<Option[]>([]);
  const [productPatch, setProductPatch] = useState<Option[]>([]);
  const [employeePatch, setEmployeePatch] = useState<Option[]>([]);

  // 수정 모드 진입 시 상세 데이터로 폼 + 보존 옵션 채움.
  useEffect(() => {
    if (open && editDetail) {
      form.setFieldsValue({
        category: editDetail.category as InspectionCategory,
        themeId: editDetail.themeId,
        accountId: editDetail.accountId,
        employeeId: editDetail.employeeId,
        inspectionDate: dayjs(editDetail.inspectionDate),
        fieldTypeCode: editDetail.fieldTypeCode as InspectionFieldTypeCode,
        description: editDetail.description ?? undefined,
        productCode: editDetail.productCode ?? undefined,
        competitorName: editDetail.competitorName ?? undefined,
        competitorActivity: editDetail.competitorActivity ?? undefined,
        competitorTasting: editDetail.competitorTasting ?? undefined,
        competitorProductName: editDetail.competitorProductName ?? undefined,
        competitorProductPrice: editDetail.competitorProductPrice ?? undefined,
        competitorSalesQuantity: editDetail.competitorSalesQuantity ?? undefined,
      });
      setAccountPatch([{ value: editDetail.accountId, label: editDetail.accountName }]);
      setEmployeePatch([{ value: editDetail.employeeId, label: editDetail.employeeName }]);
      setProductPatch(
        editDetail.productCode
          ? [{ value: editDetail.productCode, label: editDetail.productName ?? editDetail.productCode }]
          : [],
      );
    }
  }, [open, editDetail, form]);

  const { data: themes } = useQuery({
    queryKey: ['inspection-create', 'themes'],
    queryFn: () => fetchThemes({ page: 0, size: 100 }),
    enabled: open,
  });
  const { data: accounts } = useQuery({
    queryKey: ['inspection-create', 'accounts', accountKeyword],
    queryFn: () => fetchAccounts({ keyword: accountKeyword || undefined, page: 0, size: 20 }),
    enabled: open,
  });
  const { data: products } = useQuery({
    queryKey: ['inspection-create', 'products', productKeyword],
    queryFn: () => fetchProducts({ keyword: productKeyword || undefined, page: 0, size: 20 }),
    enabled: open && category === 'OWN',
  });
  const { data: employees } = useQuery({
    queryKey: ['inspection-create', 'employees', employeeKeyword],
    queryFn: () => fetchUsers({ keyword: employeeKeyword || undefined, isActive: true, page: 0, size: 20 }),
    enabled: open,
  });

  const mergeOptions = (patch: Option[], fromSearch: Option[]): Option[] => {
    const merged = [...patch];
    for (const opt of fromSearch) {
      if (!merged.some((m) => m.value === opt.value)) merged.push(opt);
    }
    return merged;
  };

  const themeOptions = (themes?.content ?? []).map((t) => ({
    value: t.id,
    label: `${t.name ?? ''} ${t.title ?? ''}`.trim(),
  }));
  const accountOptions = mergeOptions(
    accountPatch,
    (accounts?.content ?? []).map((a) => ({
      value: a.id,
      label: `${a.name ?? ''}${a.externalKey ? ` (${a.externalKey})` : ''}`,
    })),
  );
  const productOptions = mergeOptions(
    productPatch,
    (products?.content ?? []).map((p) => ({
      value: p.productCode ?? '',
      label: `${p.name ?? ''}${p.productCode ? ` (${p.productCode})` : ''}`,
    })),
  );
  const employeeOptions = mergeOptions(
    employeePatch,
    (employees?.content ?? []).map((u) => ({
      value: u.id,
      label: `${u.name ?? u.username} (${u.employeeCode})`,
    })),
  );

  const close = () => {
    form.resetFields();
    setFileList([]);
    setAccountKeyword('');
    setProductKeyword('');
    setEmployeeKeyword('');
    setAccountPatch([]);
    setProductPatch([]);
    setEmployeePatch([]);
    onClose();
  };

  const handleSubmit = async () => {
    const v = await form.validateFields();
    const request = {
      themeId: v.themeId,
      accountId: v.accountId,
      employeeId: v.employeeId,
      inspectionDate: v.inspectionDate.format('YYYY-MM-DD'),
      category: v.category,
      fieldTypeCode: v.fieldTypeCode,
      description: v.description ?? null,
      productCode: v.category === 'OWN' ? v.productCode ?? null : null,
      competitorName: v.category === 'COMPETITOR' ? v.competitorName ?? null : null,
      competitorActivity: v.category === 'COMPETITOR' ? v.competitorActivity ?? null : null,
      competitorTasting: v.category === 'COMPETITOR' ? v.competitorTasting ?? null : null,
      competitorProductName: v.category === 'COMPETITOR' ? v.competitorProductName ?? null : null,
      competitorProductPrice: v.category === 'COMPETITOR' ? v.competitorProductPrice ?? null : null,
      competitorSalesQuantity: v.category === 'COMPETITOR' ? v.competitorSalesQuantity ?? null : null,
    };
    try {
      if (isEdit && editDetail) {
        await updateMutation.mutateAsync({ id: editDetail.id, request });
        message.success('현장점검이 수정되었습니다');
      } else {
        await createMutation.mutateAsync({
          request,
          photos: fileList.map((f) => f.originFileObj as File).filter(Boolean),
        });
        message.success('현장점검이 등록되었습니다');
      }
      close();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '저장에 실패했습니다');
    }
  };

  return (
    <Modal
      title={isEdit ? '현장점검 수정' : '현장점검 등록'}
      open={open}
      onOk={handleSubmit}
      onCancel={close}
      confirmLoading={createMutation.isPending || updateMutation.isPending}
      okText={isEdit ? '수정' : '등록'}
      cancelText="취소"
      width={560}
      destroyOnClose
    >
      <Form form={form} layout="vertical" initialValues={{ category: 'OWN' }} style={{ marginTop: 16 }}>
        <Form.Item label="분류" name="category" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'OWN', label: '자사' },
              { value: 'COMPETITOR', label: '경쟁사' },
            ]}
          />
        </Form.Item>
        <Form.Item label="테마" name="themeId" rules={[{ required: true, message: '테마를 선택하세요' }]}>
          <Select showSearch placeholder="테마 선택" optionFilterProp="label" options={themeOptions} />
        </Form.Item>
        <Form.Item label="거래처" name="accountId" rules={[{ required: true, message: '거래처를 선택하세요' }]}>
          <Select
            showSearch
            placeholder="거래처 검색"
            filterOption={false}
            onSearch={setAccountKeyword}
            options={accountOptions}
          />
        </Form.Item>
        <Form.Item label="점검사원" name="employeeId" rules={[{ required: true, message: '점검사원을 선택하세요' }]}>
          <Select
            showSearch
            placeholder="점검사원 검색 (이름/사번)"
            filterOption={false}
            onSearch={setEmployeeKeyword}
            options={employeeOptions}
          />
        </Form.Item>
        <Form.Item label="점검일" name="inspectionDate" rules={[{ required: true, message: '점검일을 선택하세요' }]}>
          <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
        </Form.Item>
        <Form.Item label="현장유형" name="fieldTypeCode" rules={[{ required: true, message: '현장유형을 선택하세요' }]}>
          <Select placeholder="현장유형 선택" options={FIELD_TYPE_OPTIONS} />
        </Form.Item>

        {category === 'OWN' && (
          <>
            <Form.Item label="제품" name="productCode">
              <Select
                showSearch
                allowClear
                placeholder="제품 검색"
                filterOption={false}
                onSearch={setProductKeyword}
                options={productOptions}
              />
            </Form.Item>
            <Form.Item label="설명" name="description">
              <Input.TextArea rows={3} maxLength={4000} />
            </Form.Item>
          </>
        )}

        {category === 'COMPETITOR' && (
          <>
            <Form.Item label="경쟁사명" name="competitorName">
              <Input maxLength={100} />
            </Form.Item>
            <Form.Item label="경쟁사 활동내용" name="competitorActivity">
              <Input.TextArea rows={2} maxLength={2000} />
            </Form.Item>
            <Form.Item label="시식여부" name="competitorTasting" valuePropName="checked">
              <Switch checkedChildren="예" unCheckedChildren="아니오" />
            </Form.Item>
            <Form.Item label="경쟁사 상품명" name="competitorProductName">
              <Input maxLength={250} />
            </Form.Item>
            <Form.Item label="경쟁사 상품가격" name="competitorProductPrice">
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
            <Form.Item label="판매수량" name="competitorSalesQuantity">
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
          </>
        )}

        {!isEdit && (
          <Form.Item label={`사진 (최대 ${MAX_PHOTO}건)`}>
            <Upload
              listType="picture-card"
              fileList={fileList}
              beforeUpload={() => false}
              onChange={({ fileList: fl }) => setFileList(fl.slice(0, MAX_PHOTO))}
              accept="image/*"
              multiple
            >
              {fileList.length < MAX_PHOTO && <div>+ 업로드</div>}
            </Upload>
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
}
