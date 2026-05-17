import { Checkbox, Empty, Spin } from 'antd';
import { useTeamMembers } from '@/hooks/team-schedule/useTeamMembers';

interface MemberFilterTabProps {
  selectedIds: number[];
  onChange: (ids: number[]) => void;
}

export function MemberFilterTab({ selectedIds, onChange }: MemberFilterTabProps) {
  const { data: members = [], isLoading } = useTeamMembers();

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

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 24 }}>
        <Spin size="small" />
      </div>
    );
  }

  if (members.length === 0) {
    return <Empty description="여사원이 없습니다" imageStyle={{ height: 48 }} style={{ marginTop: 24 }} />;
  }

  return (
    <div>
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
    </div>
  );
}
