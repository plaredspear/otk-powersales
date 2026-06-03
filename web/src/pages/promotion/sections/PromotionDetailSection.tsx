import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { DatePicker, Descriptions, Input, Select, Spin, Tag, Tooltip, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { PromotionDetail, PromotionFormMeta } from '@/api/promotion';
import type { Account } from '@/api/account';
import { fetchAccountsForPromotionLookup } from '@/api/account';
import { usePermission } from '@/hooks/usePermission';

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

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

export interface DetailFormValues {
  accountId: number;
  accountName: string | null;
  startDate: string;
  endDate: string;
  promotionType: string | null;
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

  const { hasEntityPermission } = usePermission();
  // 작성자 → 사용자 상세(/users/:id) 링크는 user READ 권한 보유자(시스템 관리자급)에게만 (목록과 동일).
  const canReadUser = hasEntityPermission('user', 'READ');

  const handleAccountSearch = (keyword: string) => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    if (keyword.length < 2) {
      setAccountOptions([]);
      setAccountSearching(false);
      return;
    }
    setAccountSearching(true);
    searchTimerRef.current = setTimeout(async () => {
      try {
        const result = await fetchAccountsForPromotionLookup({ keyword, size: 10 });
        setAccountOptions(result.content);
      } catch {
        setAccountOptions([]);
      } finally {
        setAccountSearching(false);
      }
    }, 300);
  };

  const handleCopyPromotionNumber = async () => {
    try {
      await navigator.clipboard.writeText(promotion.promotionNumber);
      message.success('행사번호를 복사했습니다');
    } catch {
      message.error('복사에 실패했습니다');
    }
  };

  const typeColor = promotion.promotionType
    ? PROMOTION_TYPE_TAG[promotion.promotionType]
    : undefined;

  const promotionTypeOptions = formMeta?.promotionTypes.map((t) => ({
    value: t.name,
    label: t.name,
  })) ?? [];

  const standLocationOptions = formMeta?.standLocations.map((s) => ({
    value: s.value,
    label: s.name,
  })) ?? [];

  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label="행사번호">
        {promotion.promotionNumber}
        <Tooltip title="행사번호 복사">
          <CopyOutlined
            onClick={handleCopyPromotionNumber}
            style={{ marginLeft: 8, color: '#1677ff', cursor: 'pointer' }}
          />
        </Tooltip>
      </Descriptions.Item>
      <Descriptions.Item label={<RequiredLabel text="시작일" required={editing} />}>
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
        {promotion.promotionName ?? '-'}
        {editing && <CalculatedHint />}
      </Descriptions.Item>
      <Descriptions.Item label={<RequiredLabel text="종료일" required={editing} />}>
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

      <Descriptions.Item label={<RequiredLabel text="거래처" required={editing} />}>
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
        ) : promotion.accountName ? (
          <Link to={`/account/${promotion.accountId}`}>{promotion.accountName}</Link>
        ) : (
          '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label="행사유형">
        {editing ? (
          <Select
            size="small"
            options={promotionTypeOptions}
            value={formValues.promotionType}
            onChange={(v) => onFormChange({ promotionType: v })}
            style={{ width: '100%' }}
          />
        ) : promotion.promotionType ? (
          <Tag color={typeColor}>{promotion.promotionType}</Tag>
        ) : (
          '-'
        )}
      </Descriptions.Item>

      <Descriptions.Item label="거래처코드">
        {promotion.accountCode ?? '-'}
        {editing && <CalculatedHint />}
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

      <Descriptions.Item label="작성자">
        {promotion.createdByName ? (
          canReadUser && promotion.createdById != null ? (
            <Link to={`/users/${promotion.createdById}`}>{promotion.createdByName}</Link>
          ) : (
            promotion.createdByName
          )
        ) : (
          '-'
        )}
      </Descriptions.Item>
      <Descriptions.Item label="작성일">
        {promotion.createdAt ? dayjs(promotion.createdAt).format('YYYY-MM-DD HH:mm') : '-'}
      </Descriptions.Item>
    </Descriptions>
  );
}
