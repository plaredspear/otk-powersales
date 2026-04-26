import { useRef, useState } from 'react';
import { Descriptions, Input, Select, Spin, Tag } from 'antd';
import type { PromotionDetail } from '@/api/promotion';
import type { Product } from '@/api/product';
import { fetchProducts } from '@/api/product';

const CATEGORY_TAG: Record<string, string> = {
  라면: 'red',
  냉장: 'blue',
  냉동: 'cyan',
  만두: 'orange',
};

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
        const result = await fetchProducts({ keyword, size: 10 });
        setProductOptions(result.content);
      } catch {
        setProductOptions([]);
      } finally {
        setProductSearching(false);
      }
    }, 300);
  };

  const categoryColor = promotion.category ? CATEGORY_TAG[promotion.category] : undefined;

  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label="대표제품">
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
        {promotion.primaryProductId ?? '-'}
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
        {promotion.productType ?? '-'}
      </Descriptions.Item>
      <Descriptions.Item label=" ">{''}</Descriptions.Item>

      <Descriptions.Item label="카테고리">
        {promotion.category ? (
          <Tag color={categoryColor}>{promotion.category}</Tag>
        ) : (
          '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label=" ">{''}</Descriptions.Item>
    </Descriptions>
  );
}
