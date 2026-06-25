import { useMemo, useState } from 'react';
import { Empty, Input, Radio, Spin, Tag } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { MEMBER_STATUS_COLOR, type TeamMember } from '@/api/team-schedule';

interface Props {
  members: TeamMember[];
  isLoading: boolean;
  selectedId: number | undefined;
  onSelect: (member: TeamMember) => void;
}

/**
 * 월별 근무내역 — 좌측 여사원 선택 패널.
 *
 * 여사원 일정관리(MemberFilterTab)와 동일한 리스트 UI(검색 + 이름(사번) 나열)이되,
 * 1명만 보는 화면이므로 체크박스 다중선택 대신 클릭 단일선택. 페이지 접근 즉시 본인 지점
 * 여사원이 나열된다(useTeamScheduleForm 의 members — SF 지점 스코프 자동 적용).
 */
export function MonthlyMemberSelectPanel({ members, isLoading, selectedId, onSelect }: Props) {
  const [search, setSearch] = useState('');
  // 'ALL' = 전체, 그 외에는 status 값. 데이터에 실제 존재하는 status 만 옵션화.
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // 데이터에 존재하는 재직상태별 건수 (필터 옵션 + 라벨 카운트). 정의된 색상 순서를 우선 적용.
  const statusOptions = useMemo(() => {
    const counts = new Map<string, number>();
    for (const m of members) {
      if (m.status) counts.set(m.status, (counts.get(m.status) ?? 0) + 1);
    }
    const known = Object.keys(MEMBER_STATUS_COLOR).filter((s) => counts.has(s));
    const extra = [...counts.keys()].filter((s) => !(s in MEMBER_STATUS_COLOR));
    return [...known, ...extra].map((s) => ({ status: s, count: counts.get(s) ?? 0 }));
  }, [members]);

  // 검색어(이름/사번) + 재직상태 필터를 AND 조합. 공백/대소문자 정규화.
  const filteredMembers = useMemo(() => {
    const q = search.trim().toLowerCase();
    return members.filter((m) => {
      if (statusFilter !== 'ALL' && (m.status ?? '') !== statusFilter) return false;
      if (!q) return true;
      const name = (m.name ?? '').toLowerCase();
      const code = (m.employeeCode ?? '').toLowerCase();
      return name.includes(q) || code.includes(q);
    });
  }, [members, search, statusFilter]);

  return (
    <div
      style={{
        width: 240,
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        background: '#fff',
        borderRadius: 8,
        padding: 12,
        border: '1px solid #f0f0f0',
        maxHeight: 680,
      }}
    >
      <Input
        size="small"
        allowClear
        prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
        placeholder="이름 또는 사번 검색"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{ marginBottom: 6 }}
      />
      {statusOptions.length > 0 && (
        <Radio.Group
          size="small"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          optionType="button"
          buttonStyle="solid"
          style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginBottom: 6 }}
        >
          <Radio.Button value="ALL">전체 {members.length}</Radio.Button>
          {statusOptions.map(({ status, count }) => (
            <Radio.Button key={status} value={status}>
              {status} {count}
            </Radio.Button>
          ))}
        </Radio.Group>
      )}
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
        <span style={{ fontWeight: 600 }}>여사원</span>
        <span style={{ fontSize: 12, color: '#8c8c8c' }}>
          {members.length}명{search.trim() && ` (검색 ${filteredMembers.length})`}
        </span>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', minHeight: 0 }}>
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin size="small" />
          </div>
        ) : members.length === 0 ? (
          <Empty
            description="여사원이 없습니다"
            styles={{ image: { height: 48 } }}
            style={{ marginTop: 24 }}
          />
        ) : filteredMembers.length === 0 ? (
          <Empty
            description="일치하는 여사원이 없습니다"
            styles={{ image: { height: 36 } }}
            style={{ marginTop: 16 }}
          />
        ) : (
          filteredMembers.map((member) => {
            const active = member.employeeId === selectedId;
            return (
              <div
                key={member.employeeId}
                role="button"
                tabIndex={0}
                onClick={() => onSelect(member)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') onSelect(member);
                }}
                style={{
                  cursor: 'pointer',
                  padding: '5px 8px',
                  borderRadius: 6,
                  background: active ? '#e6f4ff' : undefined,
                  color: active ? '#1677ff' : undefined,
                  fontWeight: active ? 600 : undefined,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 6,
                }}
              >
                <span
                  style={{
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {member.name}({member.employeeCode})
                </span>
                {member.status && (
                  <Tag
                    color={MEMBER_STATUS_COLOR[member.status] ?? 'default'}
                    style={{ marginInlineEnd: 0, flexShrink: 0 }}
                  >
                    {member.status}
                  </Tag>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
