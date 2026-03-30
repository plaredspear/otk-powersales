import { Segmented } from 'antd';
import { MemberFilterTab } from './MemberFilterTab';
import { AccountFilterTab } from './AccountFilterTab';

type FilterTab = 'member' | 'account';

interface ScheduleFilterPanelProps {
  filterTab: FilterTab;
  onFilterTabChange: (tab: FilterTab) => void;
  selectedEmployeeIds: number[];
  onSelectedEmployeeIdsChange: (ids: number[]) => void;
  selectedAccountIds: number[];
  onSelectedAccountIdsChange: (ids: number[]) => void;
  selectedBranchCode: string;
  onSelectedBranchCodeChange: (code: string) => void;
}

const TAB_OPTIONS = [
  { label: '여사원', value: 'member' as const },
  { label: '거래처', value: 'account' as const },
];

export function ScheduleFilterPanel({
  filterTab,
  onFilterTabChange,
  selectedEmployeeIds,
  onSelectedEmployeeIdsChange,
  selectedAccountIds,
  onSelectedAccountIdsChange,
  selectedBranchCode,
  onSelectedBranchCodeChange,
}: ScheduleFilterPanelProps) {
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
      <Segmented
        block
        options={TAB_OPTIONS}
        value={filterTab}
        onChange={(val) => onFilterTabChange(val as FilterTab)}
        style={{ marginBottom: 12 }}
      />

      <div style={{ flex: 1, overflow: 'auto' }}>
        {filterTab === 'member' ? (
          <MemberFilterTab
            selectedIds={selectedEmployeeIds}
            onChange={onSelectedEmployeeIdsChange}
          />
        ) : (
          <AccountFilterTab
            selectedIds={selectedAccountIds}
            onChange={onSelectedAccountIdsChange}
            branchCode={selectedBranchCode}
            onBranchCodeChange={onSelectedBranchCodeChange}
          />
        )}
      </div>
    </div>
  );
}
