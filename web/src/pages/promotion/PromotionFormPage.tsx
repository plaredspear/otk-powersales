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
import type { Account } from '@/api/account';
import { useAccountLookupSearch } from '@/hooks/promotion/useAccountLookupSearch';
import type { Product } from '@/api/product';
import { useProductLookupSearch } from '@/hooks/promotion/useProductLookupSearch';
import ProductLookupOptionLabel from '@/components/product/ProductLookupOptionLabel';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';
import type { PromotionFormData } from '@/api/promotion';
import AccountAdvancedSearchModal from './components/AccountAdvancedSearchModal';
import ProductAdvancedSearchModal from './components/ProductAdvancedSearchModal';
import LookupDropdownFooter from './components/LookupDropdownFooter';

const { TextArea } = Input;

interface AccountOption {
  value: number;
  label: string;
}

interface ProductOption {
  value: number;
  /** 드롭다운은 상태 Tag 를 포함한 ReactNode, 선택값 복원 시에는 문자열이 들어온다. */
  label: React.ReactNode;
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
  const { setDynamicTitle } = useContext(BreadcrumbContext);
  const { data: promotion, isLoading: detailLoading } = usePromotion(sourceId);
  const { data: formMeta, isLoading: formMetaLoading } = usePromotionFormMeta();
  const createMutation = useCreatePromotion();
  const updateMutation = useUpdatePromotion();
  const cloneMutation = useClonePromotion();

  // 거래처 / 대표제품 lookup 검색 — debounce·요청 순번·키워드 보관은 공용 훅이 담당한다.
  // 대표제품은 상세 인라인 편집(PromotionProductSection)과도 로직을 공유한다.
  const {
    items: accountSearchResults,
    total: accountTotal,
    searching: accountSearching,
    keyword: accountKeyword,
    onSearch: handleAccountSearch,
    clearKeyword: clearAccountKeyword,
    selectItem: selectAccount,
  } = useAccountLookupSearch({ size: 20 });
  const {
    items: productSearchResults,
    total: productTotal,
    searching: productSearching,
    keyword: productKeyword,
    onSearch: handleProductSearch,
    clearKeyword: clearProductKeyword,
    selectItem: selectProduct,
  } = useProductLookupSearch({ size: 20 });

  // 드롭다운 옵션 — 거래처는 텍스트 라벨, 대표제품은 상태 Tag 를 포함한 ReactNode 라벨.
  const accountOptions: AccountOption[] = useMemo(
    () =>
      accountSearchResults.map((a) => ({
        value: a.id,
        // 거래처코드가 없으면(수정 진입 시 복원한 옵션) 이름만 표시한다.
        label: a.externalKey ? `${a.name} (${a.externalKey})` : (a.name ?? ''),
      })),
    [accountSearchResults],
  );
  const productOptions: ProductOption[] = useMemo(
    () =>
      productSearchResults.map((p) => ({
        value: p.id,
        label: (
          <ProductLookupOptionLabel
            name={p.name}
            productCode={p.productCode}
            productStatus={p.productStatus}
          />
        ),
      })),
    [productSearchResults],
  );
  const [advancedSearchOpen, setAdvancedSearchOpen] = useState(false);
  const [productAdvancedSearchOpen, setProductAdvancedSearchOpen] = useState(false);
  // 선택된 거래처의 거래상태 — 신규등록 시 거래처 선택 즉시 표시 (수정/복제 초기 로드 제외).
  const [selectedAccountStatus, setSelectedAccountStatus] = useState<string | null>(null);

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
        // 수정 진입 시 재검색 없이 옵션 복원. 상세 응답에는 거래처코드/거래상태가 없어
        // 라벨은 이름만 나오고, 거래상태는 사용자가 다시 선택할 때 표시된다.
        selectAccount({
          id: promotion.accountId,
          name: promotion.accountName,
          externalKey: null,
          accountStatusName: null,
        });
      }
      if (promotion.primaryProductId && promotion.primaryProductName) {
        // 검색 결과와 동일한 표기(제품코드 + 상태 Tag)로 복원 — 수정 진입 시 재검색 불요.
        selectProduct({
          id: promotion.primaryProductId,
          name: promotion.primaryProductName,
          productCode: promotion.primaryProductCode,
          productStatus: promotion.primaryProductStatus,
        });
      }
    }
  }, [isEdit, isClone, promotion, form, selectAccount, selectProduct]);

  const handleAccountChange = (accountId: number) => {
    // Form.Item 이 주입하는 onChange 를 가로챘으므로 폼 값은 직접 반영한다.
    form.setFieldValue('accountId', accountId);
    // 선택 확정 시 고급 검색이 이어받을 키워드를 비운다.
    clearAccountKeyword();
    // 빠른 검색 결과 원본에서 거래상태를 되찾아 표시 (선택 즉시 반영).
    const matched = accountSearchResults.find((a) => a.id === accountId);
    setSelectedAccountStatus(matched?.accountStatusName ?? null);
  };

  const handleAdvancedSearchSelect = (account: Account) => {
    // 고급 검색 그리드에서 고른 거래처를 폼 값 + Select 옵션에 반영 — 빠른 검색 결과와 동일 형식.
    selectAccount({
      id: account.id,
      name: account.name,
      externalKey: account.externalKey,
      accountStatusName: account.accountStatusName,
    });
    form.setFieldValue('accountId', account.id);
    setSelectedAccountStatus(account.accountStatusName);
  };

  const handleProductAdvancedSearchSelect = (product: Product) => {
    // 고급 검색 그리드에서 고른 제품을 폼 값 + Select 옵션에 반영 — 빠른 검색 결과와 동일 형식.
    selectProduct(product);
    form.setFieldValue('primaryProductId', product.id);
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
          값을 복사했습니다. 거래처·기간·대표제품 등을 수정한 뒤 저장하면 신규 행사로 등록됩니다.
        </Card>
      )}
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Card title="정보" style={{ marginBottom: 16 }}>
          <Row gutter={24}>
            <Col xs={24} sm={12}>
              {/*
                Form.Item 의 직접 자식이 Space.Compact 이면 Form 이 value/onChange 를 Select 가
                아닌 Space.Compact 에 주입해 폼 값이 검색어로 오염된다. Form.Item 이 Select 만
                감싸도록 두고, 버튼은 바깥 flex 로 나란히 배치한다(Space.Compact 는 자식 props 를
                건드리므로 쓰지 않는다).
              */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                <Form.Item
                  name="accountId"
                  label="거래처"
                  rules={[{ required: true, message: '거래처를 선택해주세요' }]}
                  // flex item 기본 min-width:auto 를 풀어야 남는 폭을 모두 차지한다.
                  style={{ flex: 1, minWidth: 0, marginBottom: 24 }}
                >
                  <Select
                    showSearch
                    style={{ width: '100%' }}
                    placeholder="거래처 검색 (2자 이상 입력)"
                    filterOption={false}
                    onSearch={handleAccountSearch}
                    loading={accountSearching}
                    options={accountOptions}
                    notFoundContent={accountSearching ? <Spin size="small" /> : null}
                    // 선택 시 거래상태를 함께 표시해야 해 onChange 를 가로챈다 — Form.Item 이
                    // 주입한 onChange 는 덮이므로 handleAccountChange 안에서 폼 값을 직접 세팅한다.
                    onChange={handleAccountChange}
                    // 검색어 입력 상태는 AntD 기본 동작에 맡긴다(blur 시 초기화). searchValue 를
                    // 제어하면 내부 상태와 경합해 입력이 막히므로 쓰지 않는다.
                    // 고급 검색이 이어받을 키워드는 onSearch 콜백에서 accountKeyword 로 따로 보관한다.
                    // 빠른 검색은 첫 페이지 20건만 노출 — 총 건수를 알리고 고급 검색 진입로를
                    // 드롭다운 하단에 상시 제공한다 (동일 키워드를 이어받아 전체 결과를 페이지로 열람).
                    popupRender={(menu) => (
                      <>
                        {menu}
                        <LookupDropdownFooter
                          onMore={() => setAdvancedSearchOpen(true)}
                          total={accountTotal}
                        />
                      </>
                    )}
                  />
                </Form.Item>
                {/* Form.Item 의 label 높이만큼 내려 Select 와 가로 정렬을 맞춘다. */}
                <Button style={{ marginTop: 30 }} onClick={() => setAdvancedSearchOpen(true)}>
                  고급 검색
                </Button>
              </div>
              {selectedAccountStatus && (
                <div style={{ marginTop: -12, marginBottom: 12 }}>
                  <span style={{ color: '#8c8c8c', marginRight: 8 }}>거래상태</span>
                  <span
                    style={{
                      color: ['폐업', '출고중지'].includes(selectedAccountStatus)
                        ? '#cf1322'
                        : undefined,
                      fontWeight: 500,
                    }}
                  >
                    {selectedAccountStatus}
                  </span>
                </div>
              )}
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
              {/*
                Form.Item 의 직접 자식이 Space.Compact 이면 Form 이 value/onChange 를 Select 가
                아닌 Space.Compact 에 주입해 폼 값이 검색어로 오염된다. Form.Item 이 Select 만
                감싸도록 두고, 버튼은 바깥 flex 로 나란히 배치한다(Space.Compact 는 자식 props 를
                건드리므로 쓰지 않는다).
              */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                <Form.Item
                  name="primaryProductId"
                  label="대표제품"
                  rules={[{ required: true, message: '대표제품을 선택해주세요' }]}
                  // flex item 기본 min-width:auto 를 풀어야 남는 폭을 모두 차지한다.
                  style={{ flex: 1, minWidth: 0, marginBottom: 24 }}
                >
                  <Select
                    showSearch
                    allowClear
                    style={{ width: '100%' }}
                    placeholder="제품 검색 (2자 이상 입력)"
                    filterOption={false}
                    onSearch={handleProductSearch}
                    // 사용자가 x 로 비운 경우는 보관 키워드도 함께 정리한다(빈 onSearch 무시와 구분).
                    onClear={clearProductKeyword}
                    loading={productSearching}
                    options={productOptions}
                    notFoundContent={productSearching ? <Spin size="small" /> : null}
                    // 검색어 입력 상태는 AntD 기본 동작에 맡긴다(blur 시 초기화). searchValue 를
                    // 제어하면 내부 상태와 경합해 입력이 막히므로 쓰지 않는다.
                    // 고급 검색이 이어받을 키워드는 onSearch 콜백에서 productKeyword 로 따로 보관한다.
                    // 빠른 검색은 첫 페이지 20건만 노출 — 총 건수를 알리고 고급 검색 진입로를
                    // 드롭다운 하단에 상시 제공한다 (동일 키워드를 이어받아 전체 결과를 페이지로 열람).
                    popupRender={(menu) => (
                      <>
                        {menu}
                        <LookupDropdownFooter
                          onMore={() => setProductAdvancedSearchOpen(true)}
                          total={productTotal}
                        />
                      </>
                    )}
                  />
                </Form.Item>
                {/* Form.Item 의 label 높이만큼 내려 Select 와 가로 정렬을 맞춘다. */}
                <Button
                  style={{ marginTop: 30 }}
                  onClick={() => setProductAdvancedSearchOpen(true)}
                >
                  고급 검색
                </Button>
              </div>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="otherProduct"
                label="기타제품"
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
        initialKeyword={accountKeyword}
      />

      <ProductAdvancedSearchModal
        open={productAdvancedSearchOpen}
        onClose={() => setProductAdvancedSearchOpen(false)}
        onSelect={handleProductAdvancedSearchSelect}
        initialKeyword={productKeyword}
      />
    </div>
  );
}
