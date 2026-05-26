import { useMemo, useState } from 'react';
import { Checkbox, Empty, Input, Select, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
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
  const [search, setSearch] = useState('');

  const branchOptions = branches.map((b) => ({
    value: b.branchCode,
    label: b.branchName,
  }));

  // 이름 또는 externalKey (SAP code) 에 검색어를 포함하는 row 만 표시.
  const filteredAccounts = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return accounts;
    return accounts.filter((a) => {
      const name = (a.name ?? '').toLowerCase();
      const ext = (a.externalKey ?? '').toLowerCase();
      return name.includes(q) || ext.includes(q);
    });
  }, [accounts, search]);

  const filteredIds = useMemo(() => filteredAccounts.map((a) => a.accountId), [filteredAccounts]);
  const selectedInFiltered = useMemo(
    () => filteredIds.filter((id) => selectedIds.includes(id)).length,
    [filteredIds, selectedIds],
  );
  const allSelected = filteredAccounts.length > 0 && selectedInFiltered === filteredAccounts.length;
  const indeterminate = selectedInFiltered > 0 && selectedInFiltered < filteredAccounts.length;

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      onChange(Array.from(new Set([...selectedIds, ...filteredIds])));
    } else {
      const filteredSet = new Set(filteredIds);
      onChange(selectedIds.filter((id) => !filteredSet.has(id)));
    }
  };

  const handleToggle = (accountId: number, checked: boolean) => {
    if (checked) {
      onChange([...selectedIds, accountId]);
    } else {
      onChange(selectedIds.filter((id) => id !== accountId));
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: 0, flex: 1 }}>
      {!isSingleBranch && (
        <Select
          style={{ width: '100%', marginBottom: 8, flexShrink: 0 }}
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
          <Input
            size="small"
            allowClear
            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            placeholder="이름 또는 코드 검색"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ marginBottom: 6, flexShrink: 0 }}
          />
          <div
            style={{
              padding: '4px 0',
              borderBottom: '1px solid #f0f0f0',
              marginBottom: 4,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexShrink: 0,
            }}
          >
            <Checkbox
              checked={allSelected}
              indeterminate={indeterminate}
              onChange={(e) => handleSelectAll(e.target.checked)}
              disabled={filteredAccounts.length === 0}
            >
              <span style={{ fontWeight: 600 }}>전체선택</span>
            </Checkbox>
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>
              {selectedIds.length} / {accounts.length}
              {search.trim() && ` (검색 ${filteredAccounts.length})`}
            </span>
          </div>
          <div style={{ flex: 1, overflowY: 'auto', minHeight: 0 }}>
            {filteredAccounts.length === 0 ? (
              <Empty
                description="일치하는 거래처가 없습니다"
                styles={{ image: { height: 36 } }}
                style={{ marginTop: 16 }}
              />
            ) : (
              filteredAccounts.map((account) => (
                <div key={account.accountId} style={{ padding: '3px 0' }}>
                  <Checkbox
                    checked={selectedIds.includes(account.accountId)}
                    onChange={(e) => handleToggle(account.accountId, e.target.checked)}
                  >
                    {account.name}
                  </Checkbox>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}
