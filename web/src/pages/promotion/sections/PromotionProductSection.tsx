import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Descriptions, Input, Select, Spin } from 'antd';
import type { PromotionDetail } from '@/api/promotion';
import type { Product } from '@/api/product';
import { fetchProductsForPromotionLookup } from '@/api/product';

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
  const [productOptions, setProductOptions] = useState<Product[]>([]);
  const [productSearching, setProductSearching] = useState(false);
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleProductSearch = (keyword: string) => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    if (keyword.length < 2) {
      setProductOptions([]);
      setProductSearching(false);
      return;
    }
    setProductSearching(true);
    searchTimerRef.current = setTimeout(async () => {
      try {
        const result = await fetchProductsForPromotionLookup({ keyword, size: 10 });
        setProductOptions(result.content);
      } catch {
        setProductOptions([]);
      } finally {
        setProductSearching(false);
      }
    }, 300);
  };

  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label={<RequiredLabel text="대표제품" required={editing} />}>
        {editing ? (
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
            onChange={(option) => {
              if (option) {
                const selected = productOptions.find((p) => p.id === option.value);
                onFormChange({
                  primaryProductId: option.value as number,
                  primaryProductName: selected?.name ?? (option.label as string),
                });
              }
            }}
            notFoundContent={productSearching ? <Spin size="small" /> : '검색 결과 없음'}
            options={productOptions.map((p) => ({
              value: p.id,
              label: `${p.name} (${p.productCode})`,
            }))}
            style={{ width: '100%' }}
          />
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
  );
}
