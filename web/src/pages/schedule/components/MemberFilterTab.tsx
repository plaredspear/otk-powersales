import { Checkbox, Empty, Select, Spin } from 'antd';
import { useTeamMembers } from '@/hooks/team-schedule/useTeamMembers';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

interface MemberFilterTabProps {
  selectedIds: number[];
  onChange: (ids: number[]) => void;
  branchCode: string;
  onBranchCodeChange: (code: string) => void;
}

export function MemberFilterTab({
  selectedIds,
  onChange,
  branchCode,
  onBranchCodeChange,
}: MemberFilterTabProps) {
  const { data: branches = [] } = useTeamScheduleBranches();
  const isSingleBranch = branches.length === 1;
  const effectiveBranchCode = isSingleBranch ? branches[0].branchCode : branchCode;
  const { data: members = [], isLoading } = useTeamMembers(effectiveBranchCode || undefined);

  const branchOptions = branches.map((b) => ({
    value: b.branchCode,
    label: b.branchName,
  }));

  const allSelected = members.length > 0 && selectedIds.length === members.length;
  const indeterminate = selectedIds.length > 0 && selectedIds.length < members.length;

  const handleSelectAll = (checked: boolean) => {
    onChange(checked ? members.map((m) => m.employeeId) : []);
  };

  const handleToggle = (employeeId: number, checked: boolean) => {
    if (checked) {
      onChange([...selectedIds, employeeId]);
    } else {
      onChange(selectedIds.filter((id) => id !== employeeId));
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
          imageStyle={{ height: 48 }}
          style={{ marginTop: 24 }}
        />
      ) : isLoading ? (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin size="small" />
        </div>
      ) : members.length === 0 ? (
        <Empty description="여사원이 없습니다" imageStyle={{ height: 48 }} style={{ marginTop: 24 }} />
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
              {selectedIds.length} / {members.length}
            </span>
          </div>
          {members.map((member) => (
            <div key={member.employeeId} style={{ padding: '3px 0' }}>
              <Checkbox
                checked={selectedIds.includes(member.employeeId)}
                onChange={(e) => handleToggle(member.employeeId, e.target.checked)}
              >
                {member.name}({member.employeeCode})
              </Checkbox>
            </div>
          ))}
        </>
      )}
    </div>
  );
}
