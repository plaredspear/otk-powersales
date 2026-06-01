import { useEffect, useState } from 'react';
import { Alert, Button, Card, Form, InputNumber, Input, Space, App as AntdApp } from 'antd';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchDailySalesForm,
  closeDailySales,
  saveDailySalesDraft,
  type DailySalesCloseRequest,
} from '@/api/dailySales';
import DetailHeader from '@/components/DetailHeader';
import PhotoInput from '@/components/PhotoInput';
import { QueryBoundary } from '@/components/PageStates';

interface SalesForm {
  basePrice?: number;
  primarySalesQuantity?: number;
  primarySalesPrice?: number;
  primaryProductAmount?: number;
  otherSalesQuantity?: number;
  otherSalesAmount?: number;
  description?: string;
}

/** 일 매출 등록 (레거시 promotion/event/write). 행사 상세의 본인 행에서 진입. */
export default function DailySalesPage() {
  const { promotionEmployeeId } = useParams();
  const peId = Number(promotionEmployeeId);
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm<SalesForm>();
  const [image, setImage] = useState<File | null>(null);

  const query = useQuery({
    queryKey: ['daily-sales', peId],
    queryFn: () => fetchDailySalesForm(peId),
    enabled: Number.isFinite(peId) && peId > 0,
  });

  useEffect(() => {
    if (query.data) {
      form.setFieldsValue({
        basePrice: query.data.basePrice ?? undefined,
        primarySalesQuantity: query.data.primarySalesQuantity ?? undefined,
        primarySalesPrice: query.data.primarySalesPrice ?? undefined,
        primaryProductAmount: query.data.primaryProductAmount ?? undefined,
        otherSalesQuantity: query.data.otherSalesQuantity ?? undefined,
        otherSalesAmount: query.data.otherSalesAmount ?? undefined,
        description: query.data.description ?? undefined,
      });
    }
  }, [query.data, form]);

  const toRequest = (v: SalesForm): DailySalesCloseRequest => ({ ...v });

  const draftMutation = useMutation({
    mutationFn: (v: SalesForm) => saveDailySalesDraft(peId, toRequest(v), image),
    onSuccess: () => message.success('임시저장되었습니다'),
    onError: (e) => message.error(e instanceof Error ? e.message : '임시저장에 실패했습니다'),
  });

  const closeMutation = useMutation({
    mutationFn: (v: SalesForm) => closeDailySales(peId, toRequest(v), image),
    onSuccess: () => {
      message.success('일매출이 마감되었습니다');
      navigate(-1);
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '마감에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="일매출 등록" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(d) => (
          <Card styles={{ body: { padding: 16 } }}>
            {d.isClosed && <Alert type="success" showIcon message="이미 마감된 일매출입니다" style={{ marginBottom: 12 }} />}
            {!d.editable && !d.isClosed && (
              <Alert type="warning" showIcon message="입력 권한이 없습니다(본인/미마감만 가능)" style={{ marginBottom: 12 }} />
            )}
            <Form form={form} layout="vertical" requiredMark={false} disabled={!d.editable}>
              <Form.Item name="basePrice" label="기준가">
                <InputNumber min={0} style={{ width: '100%' }} addonAfter="원" />
              </Form.Item>
              <Space style={{ width: '100%' }} size={8} wrap>
                <Form.Item name="primarySalesQuantity" label="주력 판매수량">
                  <InputNumber min={0} />
                </Form.Item>
                <Form.Item name="primarySalesPrice" label="주력 판매단가">
                  <InputNumber min={0} addonAfter="원" />
                </Form.Item>
              </Space>
              <Form.Item name="primaryProductAmount" label="주력 매출액">
                <InputNumber min={0} style={{ width: '100%' }} addonAfter="원" />
              </Form.Item>
              <Space style={{ width: '100%' }} size={8} wrap>
                <Form.Item name="otherSalesQuantity" label="기타 판매수량">
                  <InputNumber min={0} />
                </Form.Item>
                <Form.Item name="otherSalesAmount" label="기타 매출액">
                  <InputNumber min={0} addonAfter="원" />
                </Form.Item>
              </Space>
              <Form.Item name="description" label="비고">
                <Input.TextArea rows={2} placeholder="비고 (선택)" />
              </Form.Item>
              <div style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 12, marginBottom: 4 }}>진열 사진</div>
                <PhotoInput label="진열 촬영" value={image} onChange={setImage} />
              </div>
              {d.editable && (
                <Space style={{ width: '100%' }} direction="vertical">
                  <Button
                    block
                    loading={draftMutation.isPending}
                    onClick={() => draftMutation.mutate(form.getFieldsValue())}
                  >
                    임시저장
                  </Button>
                  <Button
                    type="primary"
                    block
                    loading={closeMutation.isPending}
                    onClick={() => closeMutation.mutate(form.getFieldsValue())}
                  >
                    마감
                  </Button>
                </Space>
              )}
            </Form>
          </Card>
        )}
      </QueryBoundary>
    </>
  );
}
