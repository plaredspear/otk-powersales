import { useCallback, useRef, useState } from 'react';
import { DatePicker, Descriptions, Input, Select, Spin, Tag } from 'antd';
import dayjs from 'dayjs';
import type { PromotionDetail, PromotionFormMeta } from '@/api/promotion';
import type { Account } from '@/api/account';
import { fetchAccounts } from '@/api/account';

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

export interface DetailFormValues {
  accountId: number;
  accountName: string | null;
  startDate: string;
  endDate: string;
  promotionTypeId: number | null;
  standLocation: string | null;
  message: string | null;
}

interface Props {
  promotion: PromotionDetail;
  editing: boolean;
  formValues: DetailFormValues;
  onFormChange: (values: Partial<DetailFormValues>) => void;
  formMeta?: PromotionFormMeta;
}

export default function PromotionDetailSection({
  promotion,
  editing,
  formValues,
  onFormChange,
  formMeta,
}: Props) {
  const [accountOptions, setAccountOptions] = useState<Account[]>([]);
  const [accountSearching, setAccountSearching] = useState(false);
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleAccountSearch = useCallback((keyword: string) => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    if (keyword.length < 2) {
      setAccountOptions([]);
      setAccountSearching(false);
      return;
    }
    setAccountSearching(true);
    searchTimerRef.current = setTimeout(async () => {
      try {
        const result = await fetchAccounts({ keyword, size: 10 });
        setAccountOptions(result.content);
      } catch {
        setAccountOptions([]);
      } finally {
        setAccountSearching(false);
      }
    }, 300);
  }, []);

  const typeColor = promotion.promotionTypeName
    ? PROMOTION_TYPE_TAG[promotion.promotionTypeName]
    : undefined;

  const promotionTypeOptions = formMeta?.promotionTypes.map((t) => ({
    value: t.id,
    label: t.name,
  })) ?? [];

  const standLocationOptions = formMeta?.standLocations.map((s) => ({
    value: s.value,
    label: s.name,
  })) ?? [];

  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label="행사번호">{promotion.promotionNumber}</Descriptions.Item>
      <Descriptions.Item label="시작일">
        {editing ? (
          <DatePicker
            size="small"
            format="YYYY-MM-DD"
            value={formValues.startDate ? dayjs(formValues.startDate) : null}
            onChange={(d) => onFormChange({ startDate: d ? d.format('YYYY-MM-DD') : '' })}
            style={{ width: '100%' }}
          />
        ) : (
          promotion.startDate ?? '-'
        )}
      </Descriptions.Item>

      <Descriptions.Item label="행사명">
        {promotion.promotionName ? (
          <>{promotion.promotionName}</>
        ) : (
          '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label="종료일">
        {editing ? (
          <DatePicker
            size="small"
            format="YYYY-MM-DD"
            value={formValues.endDate ? dayjs(formValues.endDate) : null}
            onChange={(d) => onFormChange({ endDate: d ? d.format('YYYY-MM-DD') : '' })}
            style={{ width: '100%' }}
          />
        ) : (
          promotion.endDate ?? '-'
        )}
      </Descriptions.Item>

      <Descriptions.Item label="거래처">
        {editing ? (
          <Select
            size="small"
            showSearch
            filterOption={false}
            placeholder="거래처 검색..."
            value={
              formValues.accountId
                ? {
                    value: formValues.accountId,
                    label: formValues.accountName ?? String(formValues.accountId),
                  }
                : undefined
            }
            labelInValue
            onSearch={handleAccountSearch}
            onChange={(option) => {
              if (option) {
                const selected = accountOptions.find((a) => a.id === option.value);
                onFormChange({
                  accountId: option.value as number,
                  accountName: selected?.name ?? (option.label as string),
                });
              }
            }}
            notFoundContent={accountSearching ? <Spin size="small" /> : '검색 결과 없음'}
            options={accountOptions.map((a) => ({
              value: a.id,
              label: `${a.name} (${a.externalKey})`,
            }))}
            style={{ width: '100%' }}
          />
        ) : (
          promotion.accountName ?? '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label="행사유형">
        {editing ? (
          <Select
            size="small"
            options={promotionTypeOptions}
            value={formValues.promotionTypeId}
            onChange={(v) => onFormChange({ promotionTypeId: v })}
            style={{ width: '100%' }}
          />
        ) : promotion.promotionTypeName ? (
          <Tag color={typeColor}>{promotion.promotionTypeName}</Tag>
        ) : (
          '-'
        )}
      </Descriptions.Item>

      <Descriptions.Item label="거래처코드">
        {promotion.accountId ?? '-'}
      </Descriptions.Item>
      <Descriptions.Item label="매대위치">
        {editing ? (
          <Select
            size="small"
            options={standLocationOptions}
            value={formValues.standLocation ?? undefined}
            onChange={(v) => onFormChange({ standLocation: v || null })}
            allowClear
            style={{ width: '100%' }}
          />
        ) : (
          promotion.standLocation ?? '-'
        )}
      </Descriptions.Item>

      <Descriptions.Item label="메시지">
        {editing ? (
          <Input.TextArea
            size="small"
            maxLength={255}
            value={formValues.message ?? ''}
            onChange={(e) => onFormChange({ message: e.target.value || null })}
            autoSize={{ minRows: 1, maxRows: 3 }}
          />
        ) : (
          promotion.message ?? '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label="CC코드">
        {promotion.costCenterCode ?? '-'}
      </Descriptions.Item>

      <Descriptions.Item label="마감여부">
        {promotion.isClosed ? <Tag color="red">마감</Tag> : '미마감'}
      </Descriptions.Item>
      <Descriptions.Item label=" ">{''}</Descriptions.Item>
    </Descriptions>
  );
}
