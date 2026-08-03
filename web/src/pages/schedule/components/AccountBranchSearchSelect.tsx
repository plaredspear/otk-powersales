import { useMemo, useState } from 'react';
import { Select, Typography } from 'antd';
import { useLookupSearch } from '@/hooks/common/useLookupSearch';
import { searchTeamScheduleAccounts, type TeamScheduleAccount } from '@/api/team-schedule';

interface AccountBranchSearchSelectProps {
  /** 현재 적용 중인 지점 코드 (단일지점 사용자는 본인 지점). 비어 있으면 아직 지점 미선택. */
  effectiveBranchCode: string;
  /** 현재 지점명 — 타 지점 거래처 차단 안내에 쓴다. */
  effectiveBranchName?: string;
  /** 검색 결과에서 거래처를 고른 순간. 지점 전환/차단 판단은 호출부(SchedulePage) 가 한다. */
  onPick: (account: TeamScheduleAccount) => void;
}

/**
 * 거래처 먼저 찾기 — 지점을 고르지 않아도 거래처명/코드로 전 지점을 검색한다.
 *
 * 이 화면의 지점 셀렉터는 **단일 선택**이라, 이미 어떤 지점이 적용된 상태에서 다른 지점의 거래처를
 * 고르면 여러 지점이 섞이게 된다. 그래서 타 지점 거래처 옵션은 `disabled` 로 막고 지점명을 함께
 * 보여준다 (왜 못 고르는지 드러나게). 지점이 아직 없으면 모든 결과를 고를 수 있고, 고른 거래처의
 * 지점으로 셀렉터가 전환된다.
 */
export function AccountBranchSearchSelect({
  effectiveBranchCode,
  effectiveBranchName,
  onPick,
}: AccountBranchSearchSelectProps) {
  const { items, searching, onSearch, clearKeyword, selectItem } = useLookupSearch<
    TeamScheduleAccount,
    TeamScheduleAccount
  >({
    fetchPage: async ({ keyword }) => {
      const content = await searchTeamScheduleAccounts(keyword);
      return { content, totalElements: content.length };
    },
    toItem: (row) => row,
    size: 50,
  });

  const options = useMemo(
    () =>
      items.map((account) => {
        const branchLabel = account.selectorBranchName ?? account.branchName ?? '지점 미상';
        // 역산 실패(AMBIGUOUS/OUT_OF_SCOPE)도 지점 전환이 불가하므로 함께 막는다.
        const pickable =
          account.selectorBranchStatus === 'RESOLVED' &&
          (!effectiveBranchCode || account.selectorBranchCode === effectiveBranchCode);
        return {
          value: account.accountId,
          label: `${account.name} (${account.externalKey}) · ${branchLabel}`,
          disabled: !pickable,
          account,
        };
      }),
    [items, effectiveBranchCode],
  );

  // 선택 즉시 비워 다음 검색을 바로 받는다 (선택 결과는 지점 전환/체크로 드러나므로 잔상 불필요).
  const [pickedValue, setPickedValue] = useState<number | undefined>(undefined);

  const handleSelect = (accountId: number) => {
    const picked = items.find((a) => a.accountId === accountId);
    if (!picked) return;
    selectItem(picked);
    setPickedValue(undefined);
    onPick(picked);
  };

  return (
    <div style={{ marginBottom: 12 }}>
      <Select
        showSearch
        allowClear
        style={{ width: '100%' }}
        placeholder="거래처 검색 (지점 무관)"
        filterOption={false}
        value={pickedValue}
        options={options}
        loading={searching}
        onSearch={onSearch}
        onClear={() => {
          setPickedValue(undefined);
          clearKeyword();
        }}
        onSelect={handleSelect}
        notFoundContent={searching ? '검색 중...' : '2자 이상 입력하세요'}
      />
      {effectiveBranchCode && (
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {effectiveBranchName ?? '현재 지점'} 거래처만 선택할 수 있습니다.
        </Typography.Text>
      )}
    </div>
  );
}
