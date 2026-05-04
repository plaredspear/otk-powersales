import { useEffect, useRef, useState } from 'react';
import { Checkbox, DatePicker, Input, Modal, Select, Spin, message } from 'antd';
import dayjs from 'dayjs';
import type {
  PromotionScheduleBulkUpdateItem,
  PromotionScheduleItem,
  PromotionScheduleMember,
  WorkingCategory1,
  WorkingCategory3,
} from '@/api/promotionSchedule';
import { fetchAccounts, type Account } from '@/api/account';
import { useBulkUpdatePromotionSchedules } from '@/hooks/promotion/usePromotionSchedules';

interface SelectedRow {
  member: PromotionScheduleMember;
  schedule: PromotionScheduleItem;
}

interface Props {
  open: boolean;
  promotionId: number;
  selected: SelectedRow[];
  onClose: () => void;
  onSuccess: () => void;
}

interface FormState {
  changeAccount: boolean;
  accountId: number | null;
  accountName: string | null;
  changeWorkingDate: boolean;
  workingDate: string | null;
  changeCategory1: boolean;
  workingCategory1: WorkingCategory1;
  changeCategory3: boolean;
  workingCategory3: WorkingCategory3;
  changeCategory4: boolean;
  workingCategory4: string;
}

const CATEGORY1_OPTIONS: { label: string; value: WorkingCategory1 }[] = [
  { label: '행사', value: '행사' },
  { label: '진열', value: '진열' },
];

const CATEGORY3_OPTIONS: { label: string; value: WorkingCategory3 }[] = [
  { label: '고정', value: '고정' },
  { label: '순회', value: '순회' },
  { label: '격고', value: '격고' },
];

function isWorkingCategory1(v: string | null): v is WorkingCategory1 {
  return v === '행사' || v === '진열';
}

function isWorkingCategory3(v: string | null): v is WorkingCategory3 {
  return v === '고정' || v === '순회' || v === '격고';
}

const initialState: FormState = {
  changeAccount: false,
  accountId: null,
  accountName: null,
  changeWorkingDate: false,
  workingDate: null,
  changeCategory1: false,
  workingCategory1: '행사',
  changeCategory3: false,
  workingCategory3: '고정',
  changeCategory4: false,
  workingCategory4: '',
};

export default function PromotionScheduleBulkUpdateModal({
  open,
  promotionId,
  selected,
  onClose,
  onSuccess,
}: Props) {
  const [state, setState] = useState<FormState>(initialState);
  const [accountOptions, setAccountOptions] = useState<Account[]>([]);
  const [accountSearchLoading, setAccountSearchLoading] = useState(false);
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const bulkUpdate = useBulkUpdatePromotionSchedules();

  useEffect(() => {
    if (open) {
      setState(initialState);
      setAccountOptions([]);
    }
  }, [open]);

  const handleAccountSearch = (keyword: string) => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    if (keyword.length < 2) {
      setAccountOptions([]);
      return;
    }
    setAccountSearchLoading(true);
    searchTimerRef.current = setTimeout(async () => {
      try {
        const result = await fetchAccounts({ keyword, size: 10 });
        setAccountOptions(result.content);
      } catch {
        setAccountOptions([]);
      } finally {
        setAccountSearchLoading(false);
      }
    }, 300);
  };

  const noChange =
    !state.changeAccount &&
    !state.changeWorkingDate &&
    !state.changeCategory1 &&
    !state.changeCategory3 &&
    !state.changeCategory4;

  const handleOk = async () => {
    if (noChange) {
      message.error('변경할 필드를 최소 1개 이상 체크하세요');
      return;
    }
    if (state.changeAccount && state.accountId == null) {
      message.error('새 거래처를 선택하세요');
      return;
    }
    if (state.changeWorkingDate && !state.workingDate) {
      message.error('새 작업일자를 선택하세요');
      return;
    }

    const items: PromotionScheduleBulkUpdateItem[] = selected.map(({ schedule }) => {
      const cat1 = state.changeCategory1
        ? state.workingCategory1
        : isWorkingCategory1(schedule.workingCategory1)
          ? schedule.workingCategory1
          : '행사';
      const cat3 = state.changeCategory3
        ? state.workingCategory3
        : isWorkingCategory3(schedule.workingCategory3)
          ? schedule.workingCategory3
          : '고정';

      return {
        scheduleId: schedule.scheduleId,
        accountId: state.changeAccount && state.accountId != null ? state.accountId : schedule.accountId,
        workingDate: state.changeWorkingDate && state.workingDate ? state.workingDate : schedule.workingDate,
        workingCategory1: cat1,
        workingCategory3: cat3,
        workingCategory4: state.changeCategory4 ? (state.workingCategory4 || null) : schedule.workingCategory4,
      };
    });

    try {
      const result = await bulkUpdate.mutateAsync({
        promotionId,
        data: { items },
      });
      message.success(`${result.updatedCount}건 변경 완료`);
      onSuccess();
      onClose();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '일괄 변경에 실패했습니다');
    }
  };

  return (
    <Modal
      title={`선택한 ${selected.length}건의 일정 변경`}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={bulkUpdate.isPending}
      okText="변경 적용"
      cancelText="취소"
      width={520}
    >
      <div style={{ marginBottom: 16, color: '#666', fontSize: 13 }}>
        ※ 체크된 필드만 변경되며, 미체크는 기존 값이 유지됩니다.
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Checkbox
            checked={state.changeAccount}
            onChange={(e) => setState((prev) => ({ ...prev, changeAccount: e.target.checked }))}
            style={{ width: 90, flexShrink: 0 }}
          >
            거래처
          </Checkbox>
          <Select
            disabled={!state.changeAccount}
            showSearch
            filterOption={false}
            placeholder="거래처 검색 (2자 이상)"
            value={
              state.accountId != null
                ? { value: state.accountId, label: state.accountName ?? String(state.accountId) }
                : undefined
            }
            labelInValue
            loading={accountSearchLoading}
            onSearch={handleAccountSearch}
            onChange={(opt: { value: number; label: string }) => {
              const found = accountOptions.find((a) => a.id === opt.value);
              setState((prev) => ({
                ...prev,
                accountId: opt.value,
                accountName: found?.name ?? opt.label,
              }));
            }}
            notFoundContent={accountSearchLoading ? <Spin size="small" /> : '검색 결과 없음'}
            allowClear
            onClear={() => setState((prev) => ({ ...prev, accountId: null, accountName: null }))}
            style={{ flex: 1 }}
          >
            {accountOptions.map((acc) => (
              <Select.Option key={acc.id} value={acc.id}>
                {acc.name} ({acc.externalKey ?? '-'})
              </Select.Option>
            ))}
          </Select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Checkbox
            checked={state.changeWorkingDate}
            onChange={(e) => setState((prev) => ({ ...prev, changeWorkingDate: e.target.checked }))}
            style={{ width: 90, flexShrink: 0 }}
          >
            작업일자
          </Checkbox>
          <DatePicker
            disabled={!state.changeWorkingDate}
            format="YYYY-MM-DD"
            value={state.workingDate ? dayjs(state.workingDate) : null}
            onChange={(d) =>
              setState((prev) => ({ ...prev, workingDate: d ? d.format('YYYY-MM-DD') : null }))
            }
            style={{ flex: 1 }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Checkbox
            checked={state.changeCategory1}
            onChange={(e) => setState((prev) => ({ ...prev, changeCategory1: e.target.checked }))}
            style={{ width: 90, flexShrink: 0 }}
          >
            카테고리1
          </Checkbox>
          <Select
            disabled={!state.changeCategory1}
            options={CATEGORY1_OPTIONS}
            value={state.workingCategory1}
            onChange={(v) => setState((prev) => ({ ...prev, workingCategory1: v }))}
            style={{ flex: 1 }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Checkbox
            checked={state.changeCategory3}
            onChange={(e) => setState((prev) => ({ ...prev, changeCategory3: e.target.checked }))}
            style={{ width: 90, flexShrink: 0 }}
          >
            카테고리3
          </Checkbox>
          <Select
            disabled={!state.changeCategory3}
            options={CATEGORY3_OPTIONS}
            value={state.workingCategory3}
            onChange={(v) => setState((prev) => ({ ...prev, workingCategory3: v }))}
            style={{ flex: 1 }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Checkbox
            checked={state.changeCategory4}
            onChange={(e) => setState((prev) => ({ ...prev, changeCategory4: e.target.checked }))}
            style={{ width: 90, flexShrink: 0 }}
          >
            카테고리4
          </Checkbox>
          <Input
            disabled={!state.changeCategory4}
            value={state.workingCategory4}
            onChange={(e) => setState((prev) => ({ ...prev, workingCategory4: e.target.value }))}
            placeholder="(선택) 추가 분류"
            style={{ flex: 1 }}
            allowClear
          />
        </div>
      </div>
    </Modal>
  );
}
