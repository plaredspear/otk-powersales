import { useEffect, useRef } from 'react';
import { Checkbox, Select, Spin } from 'antd';
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
  const { data: accounts = [], isLoading } = useTeamScheduleAccounts(branchCode);
  const initializedRef = useRef(false);
  const prevBranchCode = useRef(branchCode);

  // Select all on first data load or when branch changes
  useEffect(() => {
    if (accounts.length > 0) {
      if (!initializedRef.current || prevBranchCode.current !== branchCode) {
        initializedRef.current = true;
        prevBranchCode.current = branchCode;
        onChange(accounts.map((a) => a.accountId));
      }
    }
  }, [accounts, branchCode, onChange]);

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
      <Select
        style={{ width: '100%', marginBottom: 8 }}
        placeholder="지점 선택"
        options={branchOptions}
        value={branchCode || undefined}
        onChange={onBranchCodeChange}
        allowClear
      />

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin size="small" />
        </div>
      ) : (
        <>
          <div
            style={{
              padding: '4px 0',
              borderBottom: '1px solid #f0f0f0',
              marginBottom: 4,
            }}
          >
            <Checkbox
              checked={allSelected}
              indeterminate={indeterminate}
              onChange={(e) => handleSelectAll(e.target.checked)}
            >
              <span style={{ fontWeight: 600 }}>전체선택</span>
            </Checkbox>
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
