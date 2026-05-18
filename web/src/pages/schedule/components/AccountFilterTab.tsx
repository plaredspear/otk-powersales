import { Checkbox, Empty, Select, Spin } from 'antd';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';
import { useTeamScheduleAccounts } from '@/hooks/team-schedule/useTeamScheduleAccounts';

interface AccountFilterTabProps {
  selectedIds: number[];
  onChange: (ids: number[]) => void;
  branchCode: string;
  onBranchCodeChange: (code: string) => void;
}

export function AccountFilterTab({
  selectedIds,
  onChange,
  branchCode,
  onBranchCodeChange,
}: AccountFilterTabProps) {
  const { data: branches = [] } = useTeamScheduleBranches();
  const isSingleBranch = branches.length === 1;
  const effectiveBranchCode = isSingleBranch ? branches[0].branchCode : branchCode;
  const { data: accounts = [], isLoading } = useTeamScheduleAccounts(effectiveBranchCode);

  const branchOptions = branches.map((b) => ({
    value: b.branchCode,
    label: b.branchName,
  }));

  const allSelected = accounts.length > 0 && selectedIds.length === accounts.length;
  const indeterminate = selectedIds.length > 0 && selectedIds.length < accounts.length;

  const handleSelectAll = (checked: boolean) => {
    onChange(checked ? accounts.map((a) => a.accountId) : []);
  };

  const handleToggle = (accountId: number, checked: boolean) => {
    if (checked) {
      onChange([...selectedIds, accountId]);
    } else {
      onChange(selectedIds.filter((id) => id !== accountId));
    }
  };

  return (
    <div>
      {!isSingleBranch && (
        <Select
          style={{ width: '100%', marginBottom: 8 }}
          placeholder="지점 선택"
          options={branchOptions}
          value={branchCode || undefined}
          onChange={onBranchCodeChange}
          allowClear
          showSearch
          optionFilterProp="label"
        />
      )}

      {!effectiveBranchCode ? (
        <Empty
          description="지점을 먼저 선택해주세요"
          styles={{ image: { height: 48 } }}
          style={{ marginTop: 24 }}
        />
      ) : isLoading ? (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin size="small" />
        </div>
      ) : accounts.length === 0 ? (
        <Empty description="거래처가 없습니다" styles={{ image: { height: 48 } }} style={{ marginTop: 24 }} />
      ) : (
        <>
          <div
            style={{
              padding: '4px 0',
              borderBottom: '1px solid #f0f0f0',
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <Checkbox
              checked={allSelected}
              indeterminate={indeterminate}
              onChange={(e) => handleSelectAll(e.target.checked)}
            >
              <span style={{ fontWeight: 600 }}>전체선택</span>
            </Checkbox>
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>
              {selectedIds.length} / {accounts.length}
            </span>
          </div>
          {accounts.map((account) => (
            <div key={account.accountId} style={{ padding: '3px 0' }}>
              <Checkbox
                checked={selectedIds.includes(account.accountId)}
                onChange={(e) => handleToggle(account.accountId, e.target.checked)}
              >
                {account.name}
              </Checkbox>
            </div>
          ))}
        </>
      )}
    </div>
  );
}
