import { useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Tooltip,
  message,
} from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import { useCreatePromotion, useUpdatePromotion } from '@/hooks/promotion/usePromotionMutation';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { fetchAccounts } from '@/api/account';
import { fetchProducts } from '@/api/product';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import type { PromotionFormData } from '@/api/promotion';

const { TextArea } = Input;

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
  promotionName?: string;
  promotionTypeId: number;
  accountId: number;
  startDate: dayjs.Dayjs;
  endDate: dayjs.Dayjs;
  primaryProductId: number;
  otherProduct?: string;
  message?: string;
  standLocation: string;
  remark?: string;
}

export default function PromotionFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;
  const promotionId = Number(id);

  const [form] = Form.useForm<FormValues>();
  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { data: promotion, isLoading: detailLoading } = usePromotion(isEdit ? promotionId : 0);
  const { data: formMeta, isLoading: formMetaLoading } = usePromotionFormMeta();
  const createMutation = useCreatePromotion();
  const updateMutation = useUpdatePromotion();

  const [accountOptions, setAccountOptions] = useState<AccountOption[]>([]);
  const [productOptions, setProductOptions] = useState<ProductOption[]>([]);
  const [accountSearching, setAccountSearching] = useState(false);
  const [productSearching, setProductSearching] = useState(false);

  const promotionTypeOptions =
    formMeta?.promotionTypes.map((t) => ({ value: t.id, label: t.name })) ?? [];

  const standLocationOptions =
    formMeta?.standLocations.map((s) => ({ value: s.name, label: s.name })) ?? [];

  useEffect(() => {
    if (isEdit) {
      setDynamicTitle(promotion?.promotionNumber ?? null);
    }
    return () => setDynamicTitle(null);
  }, [isEdit, promotion?.promotionNumber, setDynamicTitle]);

  useEffect(() => {
    if (isEdit && promotion) {
      form.setFieldsValue({
        promotionName: promotion.promotionName ?? undefined,
        promotionTypeId: promotion.promotionTypeId ?? undefined,
        accountId: promotion.accountId,
        startDate: dayjs(promotion.startDate),
        endDate: dayjs(promotion.endDate),
        primaryProductId: promotion.primaryProductId ?? undefined,
        otherProduct: promotion.otherProduct ?? undefined,
        message: promotion.message ?? undefined,
        standLocation: promotion.standLocation ?? undefined,
        remark: promotion.remark ?? undefined,
      });

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

  const handleAccountSearch = async (keyword: string) => {
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
  };

  const handleProductSearch = async (keyword: string) => {
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
  };

  const handleSubmit = async (values: FormValues) => {
    const payload: PromotionFormData = {
      promotion_type_id: values.promotionTypeId,
      account_id: values.accountId,
      start_date: values.startDate.format('YYYY-MM-DD'),
      end_date: values.endDate.format('YYYY-MM-DD'),
      primary_product_id: values.primaryProductId,
      other_product: values.otherProduct || null,
      message: values.message || null,
      stand_location: values.standLocation,
      remark: values.remark || null,
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

  if ((isEdit && detailLoading) || formMetaLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  return (
    <div style={{ padding: 16, maxWidth: 1200 }}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Card title="정보" style={{ marginBottom: 16 }}>
          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="promotionName"
                label={
                  <span>
                    행사명{' '}
                    <Tooltip title="행사명은 대표상품명으로 자동 지정됩니다">
                      <InfoCircleOutlined style={{ color: '#999' }} />
                    </Tooltip>
                  </span>
                }
              >
                <Input disabled placeholder="대표상품을 선택하면 자동 설정됩니다" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
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
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="startDate"
                label="시작일"
                rules={[{ required: true, message: '시작일을 선택해주세요' }]}
              >
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
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
            </Col>
          </Row>

          <Row gutter={24}>
            <Col span={24}>
              <Form.Item
                name="message"
                label="메시지"
                rules={[{ max: 255, message: '255자 이하로 입력해주세요' }]}
              >
                <TextArea rows={3} maxLength={255} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="promotionTypeId"
                label="행사유형"
                rules={[{ required: true, message: '행사유형을 선택해주세요' }]}
              >
                <Select placeholder="행사유형 선택" options={promotionTypeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="standLocation"
                label="매대위치"
                rules={[{ required: true, whitespace: true, message: '매대위치를 입력해주세요' }]}
              >
                <Select placeholder="매대위치 선택" options={standLocationOptions} />
              </Form.Item>
            </Col>
          </Row>

        </Card>

        <Card title="행사품목" style={{ marginBottom: 16 }}>
          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="primaryProductId"
                label="대표상품"
                rules={[{ required: true, message: '대표상품을 선택해주세요' }]}
              >
                <Select
                  showSearch
                  allowClear
                  placeholder="상품 검색 (2자 이상 입력)"
                  filterOption={false}
                  onSearch={handleProductSearch}
                  loading={productSearching}
                  options={productOptions}
                  notFoundContent={productSearching ? <Spin size="small" /> : null}
                  onChange={(value: number | undefined) => {
                    if (value) {
                      const selected = productOptions.find((p) => p.value === value);
                      const nameOnly = selected?.label?.replace(/\s*\(.*\)$/, '') ?? '';
                      form.setFieldValue('promotionName', nameOnly);
                    } else {
                      form.setFieldValue('promotionName', undefined);
                    }
                  }}
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="otherProduct"
                label="기타상품"
                rules={[{ max: 200, message: '200자 이하로 입력해주세요' }]}
              >
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col span={24}>
              <Form.Item
                name="remark"
                label="비고"
                rules={[{ max: 200, message: '200자 이하로 입력해주세요' }]}
              >
                <Input maxLength={200} />
              </Form.Item>
            </Col>
          </Row>
        </Card>

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
