import { useCallback, useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import { useCreatePromotion, useUpdatePromotion } from '@/hooks/promotion/usePromotionMutation';
import { fetchAccounts } from '@/api/account';
import { fetchProducts } from '@/api/product';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import type { PromotionFormData } from '@/api/promotion';

const { Title } = Typography;
const { TextArea } = Input;

const CATEGORY_TAG: Record<string, string> = {
  라면: 'red',
  냉장: 'blue',
  냉동: 'cyan',
  만두: 'orange',
};

const PROMOTION_TYPE_OPTIONS = [
  { value: '일반행사', label: '일반행사' },
  { value: '특별행사', label: '특별행사' },
];

interface AccountOption {
  value: number;
  label: string;
}

interface ProductOption {
  value: number;
  label: string;
  category1: string | null;
}

interface FormValues {
  promotionName: string;
  promotionType?: string;
  accountId: number;
  startDate: dayjs.Dayjs;
  endDate: dayjs.Dayjs;
  primaryProductId?: number;
  otherProduct?: string;
  message?: string;
  standLocation?: string;
  targetAmount?: number;
}

export default function PromotionFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;
  const promotionId = Number(id);

  const [form] = Form.useForm<FormValues>();
  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { data: promotion, isLoading: detailLoading } = usePromotion(isEdit ? promotionId : 0);
  const createMutation = useCreatePromotion();
  const updateMutation = useUpdatePromotion();

  const [accountOptions, setAccountOptions] = useState<AccountOption[]>([]);
  const [productOptions, setProductOptions] = useState<ProductOption[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [accountSearching, setAccountSearching] = useState(false);
  const [productSearching, setProductSearching] = useState(false);

  useEffect(() => {
    if (isEdit) {
      setDynamicTitle(promotion?.promotionNumber ?? null);
    }
    return () => setDynamicTitle(null);
  }, [isEdit, promotion?.promotionNumber, setDynamicTitle]);

  useEffect(() => {
    if (isEdit && promotion) {
      form.setFieldsValue({
        promotionName: promotion.promotionName,
        promotionType: promotion.promotionType ?? undefined,
        accountId: promotion.accountId,
        startDate: dayjs(promotion.startDate),
        endDate: dayjs(promotion.endDate),
        primaryProductId: promotion.primaryProductId ?? undefined,
        otherProduct: promotion.otherProduct ?? undefined,
        message: promotion.message ?? undefined,
        standLocation: promotion.standLocation ?? undefined,
        targetAmount: promotion.targetAmount ?? undefined,
      });
      setSelectedCategory(promotion.category ?? null);

      // Set initial options for edit mode
      if (promotion.accountName) {
        setAccountOptions([{ value: promotion.accountId, label: promotion.accountName }]);
      }
      if (promotion.primaryProductId && promotion.primaryProductName) {
        setProductOptions([
          {
            value: promotion.primaryProductId,
            label: promotion.primaryProductName,
            category1: promotion.category,
          },
        ]);
      }
    }
  }, [isEdit, promotion, form]);

  const handleAccountSearch = useCallback(async (keyword: string) => {
    if (keyword.length < 2) return;
    setAccountSearching(true);
    try {
      const result = await fetchAccounts({ keyword, size: 20 });
      setAccountOptions(
        result.content
          .filter((a) => a.id != null && a.name != null)
          .map((a) => ({
            value: a.id!,
            label: `${a.name} (${a.externalKey ?? ''})`,
          })),
      );
    } finally {
      setAccountSearching(false);
    }
  }, []);

  const handleProductSearch = useCallback(async (keyword: string) => {
    if (keyword.length < 2) return;
    setProductSearching(true);
    try {
      const result = await fetchProducts({ keyword, size: 20 });
      setProductOptions(
        result.content
          .filter((p) => p.id != null && p.name != null)
          .map((p) => ({
            value: p.id!,
            label: `${p.name} (${p.productCode ?? ''})`,
            category1: p.category1,
          })),
      );
    } finally {
      setProductSearching(false);
    }
  }, []);

  const handleProductChange = useCallback(
    (value: number | undefined) => {
      if (!value) {
        setSelectedCategory(null);
        return;
      }
      const product = productOptions.find((p) => p.value === value);
      setSelectedCategory(product?.category1 ?? null);
    },
    [productOptions],
  );

  const handleSubmit = async (values: FormValues) => {
    const payload: PromotionFormData = {
      promotion_name: values.promotionName,
      promotion_type: values.promotionType || null,
      account_id: values.accountId,
      start_date: values.startDate.format('YYYY-MM-DD'),
      end_date: values.endDate.format('YYYY-MM-DD'),
      primary_product_id: values.primaryProductId || null,
      other_product: values.otherProduct || null,
      message: values.message || null,
      stand_location: values.standLocation || null,
      target_amount: values.targetAmount ?? null,
    };

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: promotionId, data: payload });
        message.success('행사마스터가 수정되었습니다');
        navigate(`/promotions/${promotionId}`);
      } else {
        const result = await createMutation.mutateAsync(payload);
        message.success('행사마스터가 등록되었습니다');
        navigate(`/promotions/${result.id}`);
      }
    } catch {
      message.error(isEdit ? '행사마스터 수정에 실패했습니다' : '행사마스터 등록에 실패했습니다');
    }
  };

  if (isEdit && detailLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  return (
    <div style={{ padding: 24, maxWidth: 800 }}>
      <Title level={4}>{isEdit ? '행사마스터 수정' : '행사마스터 등록'}</Title>

      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="promotionName"
          label="행사명"
          rules={[
            { required: true, message: '행사명을 입력해주세요' },
            { max: 200, message: '200자 이하로 입력해주세요' },
          ]}
        >
          <Input maxLength={200} />
        </Form.Item>

        <Form.Item name="promotionType" label="행사유형">
          <Select allowClear placeholder="행사유형 선택" options={PROMOTION_TYPE_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="accountId"
          label="거래처"
          rules={[{ required: true, message: '거래처를 선택해주세요' }]}
        >
          <Select
            showSearch
            placeholder="거래처 검색 (2자 이상 입력)"
            filterOption={false}
            onSearch={handleAccountSearch}
            loading={accountSearching}
            options={accountOptions}
            notFoundContent={accountSearching ? <Spin size="small" /> : null}
          />
        </Form.Item>

        <Form.Item
          name="startDate"
          label="시작일"
          rules={[{ required: true, message: '시작일을 선택해주세요' }]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          name="endDate"
          label="종료일"
          dependencies={['startDate']}
          rules={[
            { required: true, message: '종료일을 선택해주세요' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                const start = getFieldValue('startDate');
                if (!value || !start || !value.isBefore(start)) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('종료일은 시작일 이후여야 합니다'));
              },
            }),
          ]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="primaryProductId" label="대표상품">
          <Select
            showSearch
            allowClear
            placeholder="상품 검색 (2자 이상 입력)"
            filterOption={false}
            onSearch={handleProductSearch}
            onChange={handleProductChange}
            loading={productSearching}
            options={productOptions}
            notFoundContent={productSearching ? <Spin size="small" /> : null}
          />
        </Form.Item>

        <Form.Item label="카테고리">
          {selectedCategory ? (
            <Tag color={CATEGORY_TAG[selectedCategory]}>{selectedCategory}</Tag>
          ) : (
            <span style={{ color: '#999' }}>-</span>
          )}
        </Form.Item>

        <Form.Item
          name="otherProduct"
          label="기타상품"
          rules={[{ max: 500, message: '500자 이하로 입력해주세요' }]}
        >
          <Input maxLength={500} />
        </Form.Item>

        <Form.Item
          name="standLocation"
          label="매대위치"
          rules={[{ max: 200, message: '200자 이하로 입력해주세요' }]}
        >
          <Input maxLength={200} />
        </Form.Item>

        <Form.Item
          name="message"
          label="메시지"
          rules={[{ max: 1000, message: '1000자 이하로 입력해주세요' }]}
        >
          <TextArea rows={3} maxLength={1000} />
        </Form.Item>

        <Form.Item
          name="targetAmount"
          label="목표금액"
          rules={[{ type: 'number', min: 0, message: '0 이상의 금액을 입력해주세요' }]}
        >
          <InputNumber
            style={{ width: '100%' }}
            formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
            parser={(value) => Number(value?.replace(/,/g, '') ?? 0)}
            placeholder="원 단위"
          />
        </Form.Item>

        <Form.Item style={{ marginTop: 24 }}>
          <Space>
            <Button
              onClick={() => navigate(isEdit ? `/promotions/${promotionId}` : '/promotions')}
            >
              취소
            </Button>
            <Button type="primary" htmlType="submit" loading={isSubmitting}>
              저장
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  );
}
