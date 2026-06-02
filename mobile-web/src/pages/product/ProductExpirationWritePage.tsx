import { Button, Card, DatePicker, Form, Input, Select, App as AntdApp } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchMyAccounts } from '@/api/accounts';
import {
  createProductExpiration,
  updateProductExpiration,
  type ProductExpirationItem,
} from '@/api/productExpiration';
import DetailHeader from '@/components/DetailHeader';

interface FormValues {
  accountCode?: string;
  productCode?: string;
  productName?: string;
  expirationDate: Dayjs;
  alarmDate: Dayjs;
  description?: string;
}

/** 유통기한 등록/수정 (레거시 product/expiration/write). */
export default function ProductExpirationWritePage() {
  const navigate = useNavigate();
  const { seq } = useParams();
  const location = useLocation();
  const { message } = AntdApp.useApp();
  const queryClient = useQueryClient();
  const [form] = Form.useForm<FormValues>();

  const isEdit = !!seq;
  const editItem = location.state as ProductExpirationItem | null;

  const accountsQuery = useQuery({
    queryKey: ['my-accounts', ''],
    queryFn: () => fetchMyAccounts(),
    enabled: !isEdit,
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const base = {
        expirationDate: values.expirationDate.format('YYYY-MM-DD'),
        alarmDate: values.alarmDate.format('YYYY-MM-DD'),
        description: values.description,
      };
      if (isEdit) {
        return updateProductExpiration(Number(seq), base);
      }
      const acc = accountsQuery.data?.stores.find((s) => s.accountCode === values.accountCode);
      return createProductExpiration({
        ...base,
        accountCode: values.accountCode!,
        accountName: acc?.accountName ?? '',
        productCode: values.productCode!,
        productName: values.productName!,
      });
    },
    onSuccess: () => {
      message.success(isEdit ? '수정되었습니다' : '등록되었습니다');
      queryClient.invalidateQueries({ queryKey: ['product-expiration'] });
      navigate(-1);
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '저장에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title={isEdit ? '유통기한 수정' : '유통기한 등록'} />
      <Card styles={{ body: { padding: 16 } }}>
        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          initialValues={
            isEdit && editItem
              ? {
                  expirationDate: dayjs(editItem.expirationDate),
                  alarmDate: dayjs(editItem.alarmDate),
                  description: editItem.description ?? undefined,
                }
              : undefined
          }
          onFinish={(v) => mutation.mutate(v)}
        >
          {!isEdit && (
            <>
              <Form.Item name="accountCode" label="거래처" rules={[{ required: true, message: '거래처를 선택하세요' }]}>
                <Select
                  showSearch
                  placeholder="거래처 선택"
                  loading={accountsQuery.isLoading}
                  optionFilterProp="label"
                  options={accountsQuery.data?.stores.map((s) => ({
                    value: s.accountCode,
                    label: `${s.accountName} (${s.accountCode})`,
                  }))}
                />
              </Form.Item>
              <Form.Item name="productCode" label="제품코드" rules={[{ required: true, message: '제품코드를 입력하세요' }]}>
                <Input placeholder="제품코드" />
              </Form.Item>
              <Form.Item name="productName" label="제품명" rules={[{ required: true, message: '제품명을 입력하세요' }]}>
                <Input placeholder="제품명" />
              </Form.Item>
            </>
          )}
          <Form.Item name="expirationDate" label="유통기한" rules={[{ required: true, message: '유통기한을 선택하세요' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="alarmDate" label="알림일" rules={[{ required: true, message: '알림일을 선택하세요' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="비고">
            <Input.TextArea rows={3} maxLength={500} showCount placeholder="비고 (선택)" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            {isEdit ? '수정' : '등록'}
          </Button>
        </Form>
      </Card>
    </>
  );
}
