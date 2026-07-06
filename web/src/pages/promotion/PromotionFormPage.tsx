import { useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
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
  message,
} from 'antd';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import {
  useClonePromotion,
  useCreatePromotion,
  useUpdatePromotion,
} from '@/hooks/promotion/usePromotionMutation';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { fetchAccountsForPromotionLookup, type Account } from '@/api/account';
import { fetchProductsForPromotionLookup } from '@/api/product';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import type { PromotionFormData } from '@/api/promotion';
import AccountAdvancedSearchModal from './components/AccountAdvancedSearchModal';

const { TextArea } = Input;

interface AccountOption {
  value: number;
  label: string;
}

interface ProductOption {
  value: number;
  label: string;
}

interface FormValues {
  promotionType: string;
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
  const [searchParams] = useSearchParams();
  const isEdit = !!id;
  const promotionId = Number(id);
  const cloneFromParam = searchParams.get('cloneFrom');
  const cloneFromId = useMemo(() => {
    const parsed = Number(cloneFromParam);
    return cloneFromParam && !Number.isNaN(parsed) && parsed > 0 ? parsed : 0;
  }, [cloneFromParam]);
  const isClone = !isEdit && cloneFromId > 0;
  const sourceId = isEdit ? promotionId : cloneFromId;

  const [form] = Form.useForm<FormValues>();
  const accountIdValue = Form.useWatch('accountId', form);
  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { data: promotion, isLoading: detailLoading } = usePromotion(sourceId);
  const { data: formMeta, isLoading: formMetaLoading } = usePromotionFormMeta();
  const createMutation = useCreatePromotion();
  const updateMutation = useUpdatePromotion();
  const cloneMutation = useClonePromotion();

  const [accountOptions, setAccountOptions] = useState<AccountOption[]>([]);
  const [productOptions, setProductOptions] = useState<ProductOption[]>([]);
  const [accountSearching, setAccountSearching] = useState(false);
  const [productSearching, setProductSearching] = useState(false);
  const [advancedSearchOpen, setAdvancedSearchOpen] = useState(false);

  const promotionTypeOptions =
    formMeta?.promotionTypes.map((t) => ({ value: t.name, label: t.name })) ?? [];

  const standLocationOptions =
    formMeta?.standLocations.map((s) => ({ value: s.name, label: s.name })) ?? [];

  useEffect(() => {
    if (isEdit) {
      setDynamicTitle(promotion?.promotionNumber ?? null);
    } else if (isClone) {
      setDynamicTitle(promotion?.promotionNumber ? `복제: ${promotion.promotionNumber}` : '복제');
    }
    return () => setDynamicTitle(null);
  }, [isEdit, isClone, promotion?.promotionNumber, setDynamicTitle]);

  useEffect(() => {
    if ((isEdit || isClone) && promotion) {
      form.setFieldsValue({
        promotionType: promotion.promotionType ?? undefined,
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
          },
        ]);
      }
    }
  }, [isEdit, isClone, promotion, form]);

  const handleAccountSearch = async (keyword: string) => {
    if (keyword.length < 2) return;
    setAccountSearching(true);
    try {
      const result = await fetchAccountsForPromotionLookup({ keyword, size: 20 });
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

  const handleAdvancedSearchSelect = (account: Account) => {
    // 고급 검색 그리드에서 고른 거래처를 폼 값 + Select 옵션에 반영 — 기존 빠른 검색 라벨 형식과 동일.
    setAccountOptions([
      { value: account.id, label: `${account.name} (${account.externalKey ?? ''})` },
    ]);
    form.setFieldValue('accountId', account.id);
  };

  const handleProductSearch = async (keyword: string) => {
    if (keyword.length < 2) return;
    setProductSearching(true);
    try {
      const result = await fetchProductsForPromotionLookup({ keyword, size: 20 });
      setProductOptions(
        result.content
          .filter((p) => p.id != null && p.name != null)
          .map((p) => ({
            value: p.id!,
            label: `${p.name} (${p.productCode ?? ''})`,
          })),
      );
    } finally {
      setProductSearching(false);
    }
  };

  const handleSubmit = async (values: FormValues) => {
    const payload: PromotionFormData = {
      promotionType: values.promotionType,
      accountId: values.accountId,
      startDate: values.startDate.format('YYYY-MM-DD'),
      endDate: values.endDate.format('YYYY-MM-DD'),
      primaryProductId: values.primaryProductId,
      otherProduct: values.otherProduct || null,
      message: values.message || null,
      standLocation: values.standLocation,
      remark: values.remark || null,
    };

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: promotionId, data: payload });
        message.success('행사마스터가 수정되었습니다');
        navigate(`/promotions/${promotionId}`);
      } else if (isClone) {
        const result = await cloneMutation.mutateAsync({ sourceId: cloneFromId, data: payload });
        message.success('행사마스터가 복제되었습니다');
        navigate(`/promotions/${result.id}`);
      } else {
        const result = await createMutation.mutateAsync(payload);
        message.success('행사마스터가 등록되었습니다');
        navigate(`/promotions/${result.id}`);
      }
    } catch {
      const failMsg = isEdit
        ? '행사마스터 수정에 실패했습니다'
        : isClone
          ? '행사마스터 복제에 실패했습니다'
          : '행사마스터 등록에 실패했습니다';
      message.error(failMsg);
    }
  };

  if (((isEdit || isClone) && detailLoading) || formMetaLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  const isSubmitting =
    createMutation.isPending || updateMutation.isPending || cloneMutation.isPending;

  return (
    <div style={{ padding: 16, maxWidth: 1200 }}>
      {isClone && (
        <Card
          size="small"
          style={{ marginBottom: 16, backgroundColor: '#e6f4ff', borderColor: '#91caff' }}
        >
          원본 행사마스터 {promotion?.promotionNumber ? `[${promotion.promotionNumber}] ` : ''}
          값을 복사했습니다. 거래처·기간·대표상품 등을 수정한 뒤 저장하면 신규 행사로 등록됩니다.
        </Card>
      )}
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Card title="정보" style={{ marginBottom: 16 }}>
          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="accountId"
                label="거래처"
                rules={[{ required: true, message: '거래처를 선택해주세요' }]}
              >
                <Space.Compact style={{ width: '100%' }}>
                  <Select
                    showSearch
                    style={{ width: '100%' }}
                    placeholder="거래처 검색 (2자 이상 입력)"
                    filterOption={false}
                    onSearch={handleAccountSearch}
                    loading={accountSearching}
                    options={accountOptions}
                    notFoundContent={accountSearching ? <Spin size="small" /> : null}
                    value={accountIdValue}
                    onChange={(v) => form.setFieldValue('accountId', v)}
                  />
                  <Button onClick={() => setAdvancedSearchOpen(true)}>고급 검색</Button>
                </Space.Compact>
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
                name="promotionType"
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
              onClick={() =>
                navigate(
                  isEdit
                    ? `/promotions/${promotionId}`
                    : isClone
                      ? `/promotions/${cloneFromId}`
                      : '/promotions',
                )
              }
            >
              취소
            </Button>
            <Button type="primary" htmlType="submit" loading={isSubmitting}>
              저장
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <AccountAdvancedSearchModal
        open={advancedSearchOpen}
        onClose={() => setAdvancedSearchOpen(false)}
        onSelect={handleAdvancedSearchSelect}
      />
    </div>
  );
}
