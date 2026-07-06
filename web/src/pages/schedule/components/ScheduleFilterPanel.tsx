import { Button, Segmented, Select, Typography } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { MemberFilterTab } from './MemberFilterTab';
import { AccountFilterTab } from './AccountFilterTab';
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
      {/* 지점 선택 — 거래처/여사원 탭 공통. 다중지점 사용자만 노출(단일지점은 본인 지점 자동 스코프). */}
      {!isSingleBranch && (
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
