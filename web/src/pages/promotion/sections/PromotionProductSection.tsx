import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Descriptions, Input, Select, Space, Spin } from 'antd';
import type { PromotionDetail } from '@/api/promotion';
import type { Product } from '@/api/product';
import { useProductLookupSearch } from '@/hooks/promotion/useProductLookupSearch';
import ProductLookupOptionLabel from '@/components/product/ProductLookupOptionLabel';
import ProductAdvancedSearchModal from '../components/ProductAdvancedSearchModal';
import LookupDropdownFooter from '../components/LookupDropdownFooter';

/** 필수 입력 필드 라벨 — 편집 모드에서 빨간 * 표시 (SF 레거시 편집 화면 동등). */
function RequiredLabel({ text, required }: { text: string; required: boolean }) {
  if (!required) return <>{text}</>;
  return (
    <>
      <span style={{ color: '#ff4d4f', marginRight: 2 }}>*</span>
      {text}
    </>
  );
}

/** 저장 시 다른 값으로부터 계산되는 읽기 전용 필드 안내 (SF "저장 시 이 필드가 계산됨" 동등). */
function CalculatedHint() {
  return (
    <div style={{ color: '#999', fontSize: 12, marginTop: 2 }}>저장 시 이 필드가 계산됨</div>
  );
}

export interface ProductFormValues {
  primaryProductId: number | null;
  primaryProductName: string | null;
  otherProduct: string | null;
  remark: string | null;
}

interface Props {
  promotion: PromotionDetail;
  editing: boolean;
  formValues: ProductFormValues;
  onFormChange: (values: Partial<ProductFormValues>) => void;
}

export default function PromotionProductSection({
  promotion,
  editing,
  formValues,
  onFormChange,
}: Props) {
  const [advancedSearchOpen, setAdvancedSearchOpen] = useState(false);
  // 드롭다운은 첫 10건만 노출 — 검색/키워드 보관 로직은 등록·수정 폼과 공유한다.
  const {
    items: productOptions,
    total: productTotal,
    searching: productSearching,
    keyword: productKeyword,
    onSearch: handleProductSearch,
    clearKeyword,
    selectItem: selectProduct,
  } = useProductLookupSearch({ size: 10 });

  const handleAdvancedSearchSelect = (product: Product) => {
    // 고급 검색 그리드에서 고른 제품을 폼 값 + Select 옵션에 반영 — 빠른 검색 결과와 동일 형식.
    selectProduct(product);
    onFormChange({
      primaryProductId: product.id,
      primaryProductName: product.name,
    });
  };

  return (
    <>
      <Descriptions column={2} bordered size="small">
        <Descriptions.Item label={<RequiredLabel text="대표제품" required={editing} />}>
          {editing ? (
            <Space.Compact style={{ width: '100%' }}>
              <Select
                size="small"
                showSearch
                filterOption={false}
                placeholder="제품 검색..."
                value={
                  formValues.primaryProductId
                    ? {
                        value: formValues.primaryProductId,
                        label: formValues.primaryProductName ?? String(formValues.primaryProductId),
                      }
                    : undefined
                }
                labelInValue
                onSearch={handleProductSearch}
                // 검색어 입력 상태는 AntD 기본 동작에 맡긴다(blur 시 초기화).
                // 고급 검색이 이어받을 키워드는 onSearch 콜백에서 productKeyword 로 따로 보관한다.
                onChange={(option) => {
                  if (option) {
                    // 라벨이 ReactNode(상태 Tag 포함) 라 label 폴백을 쓸 수 없다 — 검색 결과
                    // 원본에서 제품명을 되찾는다(옵션은 항상 검색 결과에서 생성되므로 매칭된다).
                    const selected = productOptions.find((p) => p.id === option.value);
                    onFormChange({
                      primaryProductId: option.value as number,
                      primaryProductName: selected?.name ?? formValues.primaryProductName,
                    });
                    // 선택 확정 시 고급 검색이 이어받을 키워드를 비운다.
                    clearKeyword();
                  }
                }}
                notFoundContent={productSearching ? <Spin size="small" /> : '검색 결과 없음'}
                options={productOptions.map((p) => ({
                  value: p.id,
                  label: (
                    <ProductLookupOptionLabel
                      name={p.name}
                      productCode={p.productCode}
                      productStatus={p.productStatus}
                    />
                  ),
                }))}
                // 빠른 검색은 첫 페이지 10건만 노출 — 총 건수를 알리고 고급 검색 진입로를
                // 드롭다운 하단에 상시 제공한다 (동일 키워드를 이어받아 전체 결과를 페이지로 열람).
                popupRender={(menu) => (
                  <>
                    {menu}
                    <LookupDropdownFooter
                      onMore={() => setAdvancedSearchOpen(true)}
                      total={productTotal}
                    />
                  </>
                )}
                style={{ width: '100%' }}
              />
              <Button size="small" onClick={() => setAdvancedSearchOpen(true)}>
                고급 검색
              </Button>
            </Space.Compact>
          ) : promotion.primaryProductName && promotion.primaryProductCode ? (
            <Link to={`/product/${promotion.primaryProductCode}`}>
              {promotion.primaryProductName}
            </Link>
          ) : (
            promotion.primaryProductName ?? '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="기타제품">
          {editing ? (
            <Input
              size="small"
              maxLength={200}
              value={formValues.otherProduct ?? ''}
              onChange={(e) => onFormChange({ otherProduct: e.target.value || null })}
            />
          ) : (
            promotion.otherProduct ?? '-'
          )}
        </Descriptions.Item>

        <Descriptions.Item label="제품코드">
          {promotion.primaryProductCode ?? '-'}
          {editing && <CalculatedHint />}
        </Descriptions.Item>
        <Descriptions.Item label="비고">
          {editing ? (
            <Input
              size="small"
              maxLength={200}
              value={formValues.remark ?? ''}
              onChange={(e) => onFormChange({ remark: e.target.value || null })}
            />
          ) : (
            promotion.remark ?? '-'
          )}
        </Descriptions.Item>

        <Descriptions.Item label="제품유형">
          {promotion.category1 ?? '-'}
          {editing && <CalculatedHint />}
        </Descriptions.Item>
        <Descriptions.Item label=" ">{''}</Descriptions.Item>
      </Descriptions>

      <ProductAdvancedSearchModal
        open={advancedSearchOpen}
        onClose={() => setAdvancedSearchOpen(false)}
        onSelect={handleAdvancedSearchSelect}
        initialKeyword={productKeyword}
      />
    </>
  );
}
