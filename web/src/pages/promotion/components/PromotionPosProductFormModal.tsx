import { useEffect, useMemo, useState } from 'react';
import { Form, InputNumber, Modal, Select, Spin, message } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { fetchProductsForPromotionLookup } from '@/api/product';
import {
  useCreatePromotionPosProduct,
  useUpdatePromotionPosProduct,
} from '@/hooks/promotion/usePromotionPosProductMutation';
import type { PromotionPosProduct } from '@/api/promotionPosProduct';

interface Props {
  promotionId: number;
  open: boolean;
  editing: PromotionPosProduct | null;
  onClose: () => void;
}

interface FormValues {
  productId?: number | null;
  price?: number | null;
}

export default function PromotionPosProductFormModal({
  promotionId,
  open,
  editing,
  onClose,
}: Props) {
  const [form] = Form.useForm<FormValues>();
  const [productKeyword, setProductKeyword] = useState('');

  const createMutation = useCreatePromotionPosProduct(promotionId);
  const updateMutation = useUpdatePromotionPosProduct(promotionId);
  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  // 편집 모드 시 초기값 세팅 / 신규 생성 시 reset
  useEffect(() => {
    if (!open) return;
    if (editing) {
      form.setFieldsValue({
        productId: editing.productId,
        price: editing.price,
      });
      setProductKeyword(editing.productName ?? '');
    } else {
      form.resetFields();
      setProductKeyword('');
    }
  }, [open, editing, form]);

  // 제품 검색 — 활성 product 전체 (SF filteredLookupInfo: null 정합)
  const { data: productPage, isFetching } = useQuery({
    queryKey: ['admin', 'products', 'pos-lookup', productKeyword],
    queryFn: () =>
      fetchProductsForPromotionLookup({
        keyword: productKeyword.length >= 1 ? productKeyword : undefined,
        size: 20,
      }),
    enabled: open,
  });

  const productOptions = useMemo(() => {
    const opts = (productPage?.content ?? []).map((p) => ({
      value: p.id,
      label: `${p.name ?? '-'}${p.productCode ? ` (${p.productCode})` : ''}`,
    }));
    // 편집 모드인 경우 현재 productId 가 검색결과에 없으면 추가
    if (editing?.productId != null && !opts.some((o) => o.value === editing.productId)) {
      opts.unshift({
        value: editing.productId,
        label: `${editing.productName ?? '-'}${editing.productCode ? ` (${editing.productCode})` : ''}`,
      });
    }
    return opts;
  }, [productPage, editing]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const payload = {
      productId: values.productId ?? null,
      price: values.price ?? null,
    };
    try {
      if (editing) {
        await updateMutation.mutateAsync({ id: editing.id, data: payload });
        message.success('상세 POS품목을 수정했습니다');
      } else {
        await createMutation.mutateAsync(payload);
        message.success('상세 POS품목을 생성했습니다');
      }
      onClose();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '저장에 실패했습니다');
    }
  };

  return (
    <Modal
      title={editing ? '상세 POS품목 편집' : '새 상세 POS품목'}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      okText="저장"
      cancelText="취소"
      confirmLoading={isSubmitting}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item label="제품" name="productId">
          <Select
            allowClear
            showSearch
            placeholder="제품 검색..."
            filterOption={false}
            onSearch={setProductKeyword}
            options={productOptions}
            notFoundContent={isFetching ? <Spin size="small" /> : '검색 결과가 없습니다'}
          />
        </Form.Item>
        <Form.Item
          label="금액"
          name="price"
          rules={[
            {
              type: 'number',
              min: 0,
              message: '금액은 0 이상이어야 합니다',
            },
          ]}
        >
          <InputNumber<number>
            style={{ width: '100%' }}
            min={0}
            precision={0}
            formatter={(v) => (v != null ? `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : '')}
            parser={(v) => (v ? Number(v.replace(/,/g, '')) : 0) as number}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
