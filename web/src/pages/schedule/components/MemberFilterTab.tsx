import { useMemo, useState } from 'react';
import { Checkbox, Empty, Input, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { TeamMember } from '@/api/team-schedule';

interface MemberFilterTabProps {
  members: TeamMember[];
  isLoading: boolean;
  selectedIds: number[];
  onChange: (ids: number[]) => void;
}

export function MemberFilterTab({ members, isLoading, selectedIds, onChange }: MemberFilterTabProps) {
  const [search, setSearch] = useState('');

  // 이름 또는 사번에 검색어를 포함하는 row 만 표시. 공백/대소문자 정규화.
  const filteredMembers = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return members;
    return members.filter((m) => {
      const name = (m.name ?? '').toLowerCase();
      const code = (m.employeeCode ?? '').toLowerCase();
      return name.includes(q) || code.includes(q);
    });
  }, [members, search]);

  // 전체선택 의미: "현재 보이는(filtered) 여사원 모두 선택". staging 의 다른 row 는 유지.
  const filteredIds = useMemo(() => filteredMembers.map((m) => m.employeeId), [filteredMembers]);
  const selectedInFiltered = useMemo(
    () => filteredIds.filter((id) => selectedIds.includes(id)).length,
    [filteredIds, selectedIds],
  );
  const allSelected = filteredMembers.length > 0 && selectedInFiltered === filteredMembers.length;
  const indeterminate = selectedInFiltered > 0 && selectedInFiltered < filteredMembers.length;

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      const union = Array.from(new Set([...selectedIds, ...filteredIds]));
      onChange(union);
    } else {
      const filteredSet = new Set(filteredIds);
      onChange(selectedIds.filter((id) => !filteredSet.has(id)));
    }
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
    return <Empty description="여사원이 없습니다" styles={{ image: { height: 48 } }} style={{ marginTop: 24 }} />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: 0, flex: 1 }}>
      <Input
        size="small"
        allowClear
        prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
        placeholder="이름 또는 사번 검색"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{ marginBottom: 6 }}
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
          disabled={filteredMembers.length === 0}
        >
          <span style={{ fontWeight: 600 }}>전체선택</span>
        </Checkbox>
        <span style={{ fontSize: 12, color: '#8c8c8c' }}>
          {selectedIds.length} / {members.length}
          {search.trim() && ` (검색 ${filteredMembers.length})`}
        </span>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', minHeight: 0 }}>
        {filteredMembers.length === 0 ? (
          <Empty
            description="일치하는 여사원이 없습니다"
            styles={{ image: { height: 36 } }}
            style={{ marginTop: 16 }}
          />
        ) : (
          filteredMembers.map((member) => (
            <div key={member.employeeId} style={{ padding: '3px 0' }}>
              <Checkbox
                checked={selectedIds.includes(member.employeeId)}
                onChange={(e) => handleToggle(member.employeeId, e.target.checked)}
              >
                {member.name}({member.employeeCode})
              </Checkbox>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
