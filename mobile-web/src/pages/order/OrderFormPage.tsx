import { Button, Card, DatePicker, Divider, Form, Input, InputNumber, Select, Space, Typography, App as AntdApp } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { useQuery, useMutation } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import { fetchMyAccounts } from '@/api/accounts';
import { createOrderRequest, type OrderCreateLine } from '@/api/orders';
import DetailHeader from '@/components/DetailHeader';

interface LineForm {
  productCode: string;
  unit: string;
  quantity: number;
  quantityPieces: number;
  quantityBoxes: number;
}

interface OrderForm {
  accountId: number;
  deliveryDate: Dayjs;
  totalAmount: number;
  lines: LineForm[];
}

/**
 * 주문서 작성 (레거시 order/my/write).
 *
 * ⚠️ 권고/편차(T1): 레거시는 제품 단가·여신(loan)·마감시각 가드·SAP 그룹 로직을 포함한다.
 * 본 화면은 view-first 기본형(거래처+납품일+품목 라인+총액 직접 입력 → 등록)이며,
 * 단가 자동계산·여신 조회·마감 가드는 backend 연동 단계(또는 Wave 후속)에서 보강한다.
 */
export default function OrderFormPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const accountsQuery = useQuery({ queryKey: ['my-accounts', ''], queryFn: () => fetchMyAccounts() });

  const mutation = useMutation({
    mutationFn: (v: OrderForm) => {
      const lines: OrderCreateLine[] = v.lines.map((l, i) => ({
        lineNumber: i,
        productCode: l.productCode,
        quantity: l.quantity,
        unit: l.unit,
        quantityPieces: l.quantityPieces,
        quantityBoxes: l.quantityBoxes,
      }));
      return createOrderRequest({
        accountId: v.accountId,
        deliveryDate: v.deliveryDate.format('YYYY-MM-DD'),
        totalAmount: v.totalAmount,
        lines,
      });
    },
    onSuccess: (res) => {
      message.success(`주문이 등록되었습니다 (${res.orderRequestNumber})`);
      navigate(`/orders/${res.orderRequestId}`, { replace: true });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '주문 등록에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="주문서 작성" />
      <Card styles={{ body: { padding: 16 } }}>
        <Form<OrderForm>
          layout="vertical"
          requiredMark={false}
          initialValues={{ lines: [{ unit: 'BOX', quantity: 1, quantityPieces: 1, quantityBoxes: 1 }] }}
          onFinish={(v) => mutation.mutate(v)}
        >
          <Form.Item name="accountId" label="거래처" rules={[{ required: true, message: '거래처를 선택하세요' }]}>
            <Select
              showSearch
              placeholder="거래처 선택"
              loading={accountsQuery.isLoading}
              optionFilterProp="label"
              options={accountsQuery.data?.stores.map((s) => ({
                value: s.accountId,
                label: `${s.accountName} (${s.accountCode})`,
              }))}
            />
          </Form.Item>
          <Form.Item name="deliveryDate" label="납품일" rules={[{ required: true, message: '납품일을 선택하세요' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Divider orientation="left">주문 품목</Divider>
          <Form.List name="lines">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Card key={field.key} size="small" style={{ marginBottom: 10 }}>
                    <Space style={{ justifyContent: 'space-between', width: '100%' }}>
                      <Typography.Text strong>품목 {field.name + 1}</Typography.Text>
                      {fields.length > 1 && (
                        <Button type="text" danger icon={<MinusCircleOutlined />} onClick={() => remove(field.name)} />
                      )}
                    </Space>
                    <Form.Item name={[field.name, 'productCode']} rules={[{ required: true, message: '제품코드' }]} style={{ marginBottom: 8 }}>
                      <Input placeholder="제품코드" />
                    </Form.Item>
                    <Space wrap>
                      <Form.Item name={[field.name, 'unit']} rules={[{ required: true }]} noStyle>
                        <Select style={{ width: 90 }} options={[{ value: 'BOX' }, { value: 'EA' }]} />
                      </Form.Item>
                      <Form.Item name={[field.name, 'quantity']} rules={[{ required: true }]} noStyle>
                        <InputNumber min={0} placeholder="수량" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'quantityBoxes']} rules={[{ required: true }]} noStyle>
                        <InputNumber min={0} placeholder="박스" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'quantityPieces']} rules={[{ required: true }]} noStyle>
                        <InputNumber min={1} placeholder="낱개" />
                      </Form.Item>
                    </Space>
                  </Card>
                ))}
                <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({ unit: 'BOX', quantity: 1, quantityPieces: 1, quantityBoxes: 1 })}>
                  품목 추가
                </Button>
              </>
            )}
          </Form.List>

          <Form.Item name="totalAmount" label="총 주문금액" rules={[{ required: true, message: '총액을 입력하세요' }]} style={{ marginTop: 16 }}>
            <InputNumber min={1} style={{ width: '100%' }} addonAfter="원" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            주문 등록
          </Button>
        </Form>
      </Card>
    </>
  );
}
