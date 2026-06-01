import { useState } from 'react';
import { Button, Card, DatePicker, Divider, Form, Input, InputNumber, Select, Space, Typography, App as AntdApp } from 'antd';
import { useQuery, useMutation } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import {
  fetchInspectionThemes,
  fetchInspectionFieldTypes,
  registerInspection,
} from '@/api/inspections';
import { fetchMyAccounts } from '@/api/accounts';
import DetailHeader from '@/components/DetailHeader';
import PhotoInput from '@/components/PhotoInput';

interface InspectionForm {
  themeId?: number;
  accountId?: number;
  inspectionDate?: Dayjs;
  fieldTypeCode?: string;
  category?: string;
  description?: string;
  productCode?: string;
  competitorName?: string;
  competitorActivity?: string;
  competitorProductName?: string;
  competitorProductPrice?: number;
  competitorSalesQuantity?: number;
}

/**
 * 현장점검 등록 (레거시 fieldTalk/fieldChk/write).
 * ⚠️ T1: category(InspectionCategory) enum 값 미확정 → 선택 입력(자유). 경쟁사 필드 항상 노출(선택).
 */
export default function InspectionRegisterPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [photos, setPhotos] = useState<(File | null)[]>([null]);

  const themes = useQuery({ queryKey: ['inspection-themes'], queryFn: fetchInspectionThemes });
  const fieldTypes = useQuery({ queryKey: ['inspection-field-types'], queryFn: fetchInspectionFieldTypes });
  const accounts = useQuery({ queryKey: ['my-accounts', ''], queryFn: () => fetchMyAccounts() });

  const mutation = useMutation({
    mutationFn: (v: InspectionForm) =>
      registerInspection(
        {
          themeId: v.themeId,
          accountId: v.accountId,
          inspectionDate: v.inspectionDate?.format('YYYY-MM-DD'),
          fieldTypeCode: v.fieldTypeCode,
          category: v.category,
          description: v.description,
          productCode: v.productCode,
          competitorName: v.competitorName,
          competitorActivity: v.competitorActivity,
          competitorProductName: v.competitorProductName,
          competitorProductPrice: v.competitorProductPrice,
          competitorSalesQuantity: v.competitorSalesQuantity,
        },
        photos.filter((p): p is File => p !== null)
      ),
    onSuccess: () => {
      message.success('현장점검이 등록되었습니다');
      navigate('/inspections', { replace: true });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '등록에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="현장점검 등록" />
      <Card styles={{ body: { padding: 16 } }}>
        <Form<InspectionForm> layout="vertical" requiredMark={false} onFinish={(v) => mutation.mutate(v)}>
          <Form.Item name="themeId" label="테마" rules={[{ required: true, message: '테마를 선택하세요' }]}>
            <Select
              placeholder="테마 선택"
              loading={themes.isLoading}
              options={themes.data?.map((t) => ({ value: t.id, label: t.name }))}
            />
          </Form.Item>
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
          <Form.Item name="fieldTypeCode" label="활동유형" rules={[{ required: true, message: '활동유형을 선택하세요' }]}>
            <Select
              placeholder="활동유형 선택"
              loading={fieldTypes.isLoading}
              options={fieldTypes.data?.map((f) => ({ value: f.code, label: f.name }))}
            />
          </Form.Item>
          <Form.Item name="inspectionDate" label="점검일" rules={[{ required: true, message: '점검일을 선택하세요' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="productCode" label="제품코드">
            <Input placeholder="제품코드 (선택)" />
          </Form.Item>
          <Form.Item name="description" label="점검 내용">
            <Input.TextArea rows={3} placeholder="내용 (선택)" />
          </Form.Item>

          <Divider orientation="left">경쟁사 (선택)</Divider>
          <Space style={{ width: '100%' }} size={8} wrap>
            <Form.Item name="competitorName" label="경쟁사명">
              <Input placeholder="경쟁사명" />
            </Form.Item>
            <Form.Item name="competitorActivity" label="활동">
              <Input placeholder="활동" />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size={8} wrap>
            <Form.Item name="competitorProductName" label="경쟁사 제품">
              <Input placeholder="제품명" />
            </Form.Item>
            <Form.Item name="competitorProductPrice" label="가격">
              <InputNumber min={0} addonAfter="원" />
            </Form.Item>
            <Form.Item name="competitorSalesQuantity" label="판매량">
              <InputNumber min={0} />
            </Form.Item>
          </Space>

          <Typography.Title level={5}>사진</Typography.Title>
          <Space wrap size={16} style={{ marginBottom: 16 }}>
            {photos.map((p, i) => (
              <PhotoInput
                key={i}
                label="촬영"
                value={p}
                onChange={(file) =>
                  setPhotos((prev) => {
                    const next = [...prev];
                    next[i] = file;
                    if (file && i === prev.length - 1 && prev.length < 5) next.push(null);
                    return next;
                  })
                }
              />
            ))}
          </Space>

          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            점검 등록
          </Button>
        </Form>
      </Card>
    </>
  );
}
