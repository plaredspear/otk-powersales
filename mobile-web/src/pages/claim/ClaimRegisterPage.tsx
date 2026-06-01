import { useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Select, Space, Typography, App as AntdApp } from 'antd';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchClaimFormData, createClaim } from '@/api/claims';
import { fetchMyAccounts } from '@/api/accounts';
import DetailHeader from '@/components/DetailHeader';
import PhotoInput from '@/components/PhotoInput';

interface ClaimForm {
  accountId?: number;
  productCode?: string;
  claimType1?: string;
  claimType2?: string;
  defectDescription?: string;
  defectQuantity?: number;
  purchaseAmount?: number;
  purchaseMethodCode?: string;
  requestTypeCode?: string[];
}

export default function ClaimRegisterPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm<ClaimForm>();
  const claimType1 = Form.useWatch('claimType1', form);
  const [defectPhoto, setDefectPhoto] = useState<File | null>(null);
  const [labelPhoto, setLabelPhoto] = useState<File | null>(null);
  const [receiptPhoto, setReceiptPhoto] = useState<File | null>(null);

  const formData = useQuery({ queryKey: ['claim-form-data'], queryFn: fetchClaimFormData });
  const accounts = useQuery({ queryKey: ['my-accounts', ''], queryFn: () => fetchMyAccounts() });

  const subcategories =
    formData.data?.categories.find((c) => c.id === claimType1)?.subcategories ?? [];

  const mutation = useMutation({
    mutationFn: (v: ClaimForm) => {
      if (!defectPhoto || !labelPhoto) throw new Error('불량/라벨 사진은 필수입니다');
      return createClaim(
        {
          accountId: v.accountId,
          productCode: v.productCode,
          claimType1: v.claimType1,
          claimType2: v.claimType2,
          defectDescription: v.defectDescription,
          defectQuantity: v.defectQuantity,
          purchaseAmount: v.purchaseAmount,
          purchaseMethodCode: v.purchaseMethodCode,
          requestTypeCode: v.requestTypeCode?.join(';'),
        },
        { defectPhoto, labelPhoto, receiptPhoto }
      );
    },
    onSuccess: (res) => {
      message.success('클레임이 등록되었습니다');
      navigate(`/claims/${res.claimId}`, { replace: true });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '등록에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="클레임 등록" />
      <Card styles={{ body: { padding: 16 } }}>
        <Form form={form} layout="vertical" requiredMark={false} onFinish={(v) => mutation.mutate(v)}>
          <Form.Item name="accountId" label="거래처" rules={[{ required: true, message: '거래처를 선택하세요' }]}>
            <Select
              showSearch
              placeholder="거래처 선택"
              loading={accounts.isLoading}
              optionFilterProp="label"
              options={accounts.data?.stores.map((s) => ({
                value: s.accountId,
                label: `${s.accountName} (${s.accountCode})`,
              }))}
            />
          </Form.Item>
          <Form.Item name="productCode" label="제품코드" rules={[{ required: true, message: '제품코드를 입력하세요' }]}>
            <Input placeholder="제품코드" />
          </Form.Item>
          <Space style={{ width: '100%' }} size={8}>
            <Form.Item name="claimType1" label="클레임 종류1" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Select
                placeholder="종류1"
                onChange={() => form.setFieldValue('claimType2', undefined)}
                options={formData.data?.categories.map((c) => ({ value: c.id, label: c.name }))}
              />
            </Form.Item>
            <Form.Item name="claimType2" label="클레임 종류2" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Select
                placeholder="종류2"
                disabled={!claimType1}
                options={subcategories.map((s) => ({ value: s.id, label: s.name }))}
              />
            </Form.Item>
          </Space>
          <Form.Item name="defectQuantity" label="불량 수량">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="purchaseAmount" label="구매 금액">
            <InputNumber min={0} style={{ width: '100%' }} addonAfter="원" />
          </Form.Item>
          <Form.Item name="purchaseMethodCode" label="구매 방법">
            <Select
              allowClear
              placeholder="구매 방법"
              options={formData.data?.purchaseMethods.map((p) => ({ value: p.code, label: p.name }))}
            />
          </Form.Item>
          <Form.Item name="requestTypeCode" label="요청 사항">
            <Select
              mode="multiple"
              maxCount={4}
              placeholder="요청 사항 (최대 4)"
              options={formData.data?.requestTypes.map((r) => ({ value: r.code, label: r.name }))}
            />
          </Form.Item>
          <Form.Item name="defectDescription" label="불량 내용">
            <Input.TextArea rows={3} placeholder="불량 내용" />
          </Form.Item>

          <Typography.Title level={5}>사진</Typography.Title>
          <Space wrap size={16} style={{ marginBottom: 16 }}>
            <div>
              <div style={{ fontSize: 12, marginBottom: 4 }}>불량 사진 *</div>
              <PhotoInput label="불량 촬영" value={defectPhoto} onChange={setDefectPhoto} />
            </div>
            <div>
              <div style={{ fontSize: 12, marginBottom: 4 }}>라벨 사진 *</div>
              <PhotoInput label="라벨 촬영" value={labelPhoto} onChange={setLabelPhoto} />
            </div>
            <div>
              <div style={{ fontSize: 12, marginBottom: 4 }}>영수증 (선택)</div>
              <PhotoInput label="영수증 촬영" value={receiptPhoto} onChange={setReceiptPhoto} />
            </div>
          </Space>

          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            클레임 등록
          </Button>
        </Form>
      </Card>
    </>
  );
}
