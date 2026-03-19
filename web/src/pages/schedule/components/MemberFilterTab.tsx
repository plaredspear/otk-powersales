import { useEffect, useRef } from 'react';
import { Checkbox, Spin } from 'antd';
import { useTeamMembers } from '@/hooks/team-schedule/useTeamMembers';

interface MemberFilterTabProps {
  selectedIds: string[];
  onChange: (ids: string[]) => void;
}

export function MemberFilterTab({ selectedIds, onChange }: MemberFilterTabProps) {
  const { data: members = [], isLoading } = useTeamMembers();
  const initializedRef = useRef(false);

  // Select all on first data load
  useEffect(() => {
    if (members.length > 0 && !initializedRef.current) {
      initializedRef.current = true;
      onChange(members.map((m) => m.employeeNumber));
    }
  }, [members, onChange]);

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 24 }}>
        <Spin size="small" />
      </div>
    );
  }

  const allSelected = members.length > 0 && selectedIds.length === members.length;
  const indeterminate = selectedIds.length > 0 && selectedIds.length < members.length;

  const handleSelectAll = (checked: boolean) => {
    onChange(checked ? members.map((m) => m.employeeNumber) : []);
  };

  const handleToggle = (employeeNumber: string, checked: boolean) => {
    if (checked) {
      onChange([...selectedIds, employeeNumber]);
    } else {
      onChange(selectedIds.filter((id) => id !== employeeNumber));
    }
  };

  return (
    <div>
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
      {members.map((member) => (
        <div key={member.employeeNumber} style={{ padding: '3px 0' }}>
          <Checkbox
            checked={selectedIds.includes(member.employeeNumber)}
            onChange={(e) => handleToggle(member.employeeNumber, e.target.checked)}
          >
            {member.name}({member.empCode})
          </Checkbox>
        </div>
      ))}
    </div>
  );
}
