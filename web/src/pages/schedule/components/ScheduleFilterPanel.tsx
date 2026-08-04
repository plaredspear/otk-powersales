import { Button, Segmented, Select, Typography } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { MemberFilterTab } from './MemberFilterTab';
import { AccountFilterTab } from './AccountFilterTab';
import { AccountBranchSearchSelect } from './AccountBranchSearchSelect';
import BranchScopeEmptyNotice from '@/components/common/BranchScopeEmptyNotice';
import type { Branch, TeamMember, TeamScheduleAccount } from '@/api/team-schedule';

type FilterTab = 'member' | 'account';

interface ScheduleFilterPanelProps {
  filterTab: FilterTab;
  onFilterTabChange: (tab: FilterTab) => void;
  branches: Branch[];
  members: TeamMember[];
  accounts: TeamScheduleAccount[];
  promotionTeams: string[];
  isFormLoading: boolean;
  selectedEmployeeIds: number[];
  onSelectedEmployeeIdsChange: (ids: number[]) => void;
  selectedAccountIds: number[];
  onSelectedAccountIdsChange: (ids: number[]) => void;
  selectedBranchCode: string;
  onSelectedBranchCodeChange: (code: string) => void;
  /** 거래처 먼저 찾기 — 검색 결과에서 거래처를 고른 순간 (지점 전환/차단은 호출부가 판단). */
  onAccountPicked: (account: TeamScheduleAccount) => void;
  selectedPromotionTeams: string[];
  onSelectedPromotionTeamsChange: (teams: string[]) => void;
  onApply: () => void;
  isFilterDirty: boolean;
  isCoolingDown: boolean;
}

const TAB_OPTIONS = [
  { label: '거래처', value: 'account' as const },
  { label: '여사원', value: 'member' as const },
];

export function ScheduleFilterPanel({
  filterTab,
  onFilterTabChange,
  branches,
  members,
  accounts,
  promotionTeams,
  isFormLoading,
  selectedEmployeeIds,
  onSelectedEmployeeIdsChange,
  selectedAccountIds,
  onSelectedAccountIdsChange,
  selectedBranchCode,
  onSelectedBranchCodeChange,
  onAccountPicked,
  selectedPromotionTeams,
  onSelectedPromotionTeamsChange,
  onApply,
  isFilterDirty,
  isCoolingDown,
}: ScheduleFilterPanelProps) {
  const isSingleBranch = branches.length === 1;
  const branchOptions = branches
    .map((b) => ({ value: b.branchCode, label: b.branchName }))
    .sort((a, b) => a.label.localeCompare(b.label, 'ko'));

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        background: '#fff',
        borderRadius: 8,
        padding: 12,
        border: '1px solid #f0f0f0',
      }}
    >
      {/* 옵션 0건 = 거래처/여사원이 통째로 0건. 종전에는 지점 UI 가 사라져 원인 단서가 없었다. */}
      {!isFormLoading && branches.length === 0 && (
        <div style={{ marginBottom: 12 }}>
          <BranchScopeEmptyNotice />
        </div>
      )}

      {/* 지점 선택 — 거래처/여사원 탭 공통. 다중지점 사용자만 노출(단일지점은 본인 지점 자동 스코프). */}
      {!isSingleBranch && branches.length > 0 && (
        <Select
          style={{ width: '100%', marginBottom: 12 }}
          placeholder="지점 (전체)"
          options={branchOptions}
          value={selectedBranchCode || undefined}
          onChange={onSelectedBranchCodeChange}
          allowClear
          showSearch
          optionFilterProp="label"
        />
      )}

      {/*
        거래처 먼저 찾기 — 지점 선택이 선행 조건이 되지 않도록, 거래처명으로 전 지점을 검색한다.
        고른 거래처의 지점으로 위 셀렉터가 전환된다 (다중지점 사용자만 의미가 있어 단일지점은 숨김).
      */}
      {!isSingleBranch && branches.length > 0 && (
        <AccountBranchSearchSelect
          effectiveBranchCode={selectedBranchCode}
          effectiveBranchName={
            branches.find((b) => b.branchCode === selectedBranchCode)?.branchName
          }
          onPick={onAccountPicked}
        />
      )}

      <Segmented
        block
        options={TAB_OPTIONS}
        value={filterTab}
        onChange={(val) => onFilterTabChange(val as FilterTab)}
        style={{ marginBottom: 12 }}
      />

      <Button
        type="primary"
        icon={<SearchOutlined />}
        block
        onClick={onApply}
        disabled={isCoolingDown}
        style={{ marginBottom: 12 }}
      >
        {isCoolingDown ? '조회 (잠시 후 가능)' : `조회${isFilterDirty ? ' (변경됨)' : ''}`}
      </Button>

      <div style={{ marginBottom: 12 }}>
        <Typography.Text strong style={{ fontSize: 13, display: 'block', marginBottom: 4 }}>
          전문행사조
        </Typography.Text>
        <Select
          mode="multiple"
          allowClear
          placeholder="전체"
          style={{ width: '100%' }}
          value={selectedPromotionTeams}
          onChange={onSelectedPromotionTeamsChange}
          loading={isFormLoading}
          options={promotionTeams.map((team) => ({ label: team, value: team }))}
          maxTagCount="responsive"
        />
      </div>

      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        {filterTab === 'member' ? (
          <MemberFilterTab
            members={members}
            isLoading={isFormLoading}
            selectedIds={selectedEmployeeIds}
            onChange={onSelectedEmployeeIdsChange}
          />
        ) : (
          <AccountFilterTab
            accounts={accounts}
            isAccountsLoading={isFormLoading}
            selectedIds={selectedAccountIds}
            onChange={onSelectedAccountIdsChange}
          />
        )}
      </div>
    </div>
  );
}
