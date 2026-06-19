import { useMemo, useState } from 'react';
import { Alert, DatePicker, Empty, Input, Spin, Tabs, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useFemaleEmployees } from '@/hooks/employee/useEmployees';
import { useEmployeeMonthlyWorkHistory } from '@/hooks/employee/useEmployeeWorkHistory';
import type { Employee } from '@/api/employee';
import RefreshButton from '@/components/common/RefreshButton';
import MonthlyWorkRawTable from './MonthlyWorkRawTable';
import MonthlyWorkInsight from './MonthlyWorkInsight';

const { Text } = Typography;

/** 여사원 리스트를 한 번에 받아 클라이언트에서 그룹핑/필터한다. 지점 수 × 인원 규모상 1페이지로 충분. */
const FEMALE_LIST_SIZE = 500;

const UNGROUPED = '__미지정__';

/** 좌측 인원 선택 사이드바 — 지점별로 묶인 여사원 리스트를 클릭으로 빠르게 전환. */
function EmployeeSidebar({
  employees,
  loading,
  selectedId,
  onSelect,
}: {
  employees: Employee[];
  loading: boolean;
  selectedId: number | undefined;
  onSelect: (e: Employee) => void;
}) {
  const [keyword, setKeyword] = useState('');

  const groups = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    const filtered = kw
      ? employees.filter(
          (e) =>
            e.name.toLowerCase().includes(kw) ||
            e.employeeCode.toLowerCase().includes(kw),
        )
      : employees;

    const byBranch = new Map<string, { label: string; rows: Employee[] }>();
    for (const e of filtered) {
      const key = e.costCenterCode ?? UNGROUPED;
      if (!byBranch.has(key)) {
        byBranch.set(key, {
          label:
            e.orgName ?? (e.costCenterCode ? e.costCenterCode : '지점 미지정'),
          rows: [],
        });
      }
      byBranch.get(key)!.rows.push(e);
    }
    // 지점명 오름차순, 미지정은 맨 뒤
    return Array.from(byBranch.entries())
      .map(([key, v]) => ({
        key,
        label: v.label,
        rows: v.rows.sort((a, b) => a.name.localeCompare(b.name, 'ko')),
      }))
      .sort((a, b) => {
        if (a.key === UNGROUPED) return 1;
        if (b.key === UNGROUPED) return -1;
        return a.label.localeCompare(b.label, 'ko');
      });
  }, [employees, keyword]);

  const totalCount = useMemo(
    () => groups.reduce((sum, g) => sum + g.rows.length, 0),
    [groups],
  );

  return (
    <div
      style={{
        width: 240,
        flexShrink: 0,
        border: '1px solid #f0f0f0',
        borderRadius: 8,
        display: 'flex',
        flexDirection: 'column',
        maxHeight: 640,
      }}
    >
      <div style={{ padding: 8, borderBottom: '1px solid #f0f0f0' }}>
        <Input.Search
          allowClear
          placeholder="이름/사번 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: 4 }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin />
          </div>
        ) : totalCount === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="여사원 없음"
            style={{ marginTop: 24 }}
          />
        ) : (
          groups.map((g) => (
            <div key={g.key} style={{ marginBottom: 8 }}>
              <div
                style={{
                  fontSize: 11,
                  fontWeight: 600,
                  color: '#888',
                  padding: '4px 8px',
                  position: 'sticky',
                  top: 0,
                  background: '#fff',
                }}
              >
                {g.label} ({g.rows.length})
              </div>
              {g.rows.map((e) => {
                const active = e.id === selectedId;
                return (
                  <div
                    key={e.id}
                    role="button"
                    tabIndex={0}
                    onClick={() => onSelect(e)}
                    onKeyDown={(ev) => {
                      if (ev.key === 'Enter' || ev.key === ' ') onSelect(e);
                    }}
                    style={{
                      cursor: 'pointer',
                      padding: '6px 8px',
                      borderRadius: 6,
                      background: active ? '#e6f4ff' : undefined,
                      color: active ? '#1677ff' : undefined,
                      fontWeight: active ? 600 : undefined,
                      display: 'flex',
                      justifyContent: 'space-between',
                      gap: 8,
                    }}
                  >
                    <span
                      style={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {e.name}
                    </span>
                    <Text type="secondary" style={{ flexShrink: 0, fontSize: 11 }}>
                      {e.employeeCode}
                    </Text>
                  </div>
                );
              })}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

/**
 * 근무기간 조회 — 월별 개인 근무내역(어디서/어떻게).
 *
 * 좌측 지점별 여사원 리스트에서 1명을 클릭하면, 우측에서 지정 월의 근무내역을
 * team_member_schedule 기반으로 조회. 기본 화면은 근무 인사이트(요약+캘린더 2단).
 */
export default function MonthlyWorkDetailTab() {
  const [selected, setSelected] = useState<Employee | undefined>(undefined);
  const [period, setPeriod] = useState<Dayjs>(dayjs());

  const empQuery = useFemaleEmployees({ status: 'A', page: 0, size: FEMALE_LIST_SIZE });

  const employeeId = selected?.id;
  const yearMonth = period.format('YYYY-MM');
  const histQuery = useEmployeeMonthlyWorkHistory(employeeId, employeeId ? yearMonth : undefined);

  const items = histQuery.data?.items ?? [];

  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
      <EmployeeSidebar
        employees={empQuery.data?.content ?? []}
        loading={empQuery.isLoading}
        selectedId={employeeId}
        onSelect={setSelected}
      />

      <div style={{ flex: 1, minWidth: 0 }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            marginBottom: 12,
            flexWrap: 'wrap',
          }}
        >
          {selected ? (
            <Text strong>
              {selected.name} ({selected.employeeCode})
              {selected.orgName ? ` · ${selected.orgName}` : ''}
            </Text>
          ) : (
            <Text type="secondary">좌측에서 여사원을 선택하세요</Text>
          )}
          <span style={{ marginLeft: 'auto' }}>년·월:</span>
          <DatePicker
            picker="month"
            value={period}
            onChange={(v) => v && setPeriod(v)}
            allowClear={false}
          />
          {employeeId != null && (
            <RefreshButton onRefresh={histQuery.refetch} refreshing={histQuery.isFetching} />
          )}
        </div>

        {employeeId != null && (
          <div style={{ marginBottom: 8 }}>
            <Text type="secondary">
              {period.year()}년 {period.month() + 1}월 — 총 {items.length}건
            </Text>
          </div>
        )}

        {histQuery.isError && (
          <Alert
            type="error"
            message={(histQuery.error as Error)?.message ?? '조회 실패'}
            style={{ marginBottom: 8 }}
          />
        )}

        {employeeId == null ? (
          <Empty description="여사원을 선택하면 해당 월 근무내역을 조회합니다" />
        ) : histQuery.isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Spin size="large" />
          </div>
        ) : (
          <Tabs
            defaultActiveKey="insight"
            items={[
              {
                key: 'insight',
                label: '근무 인사이트',
                children: (
                  <MonthlyWorkInsight
                    items={items}
                    year={period.year()}
                    month={period.month() + 1}
                  />
                ),
              },
              {
                key: 'raw',
                label: '일자별 내역',
                children: <MonthlyWorkRawTable items={items} />,
              },
            ]}
          />
        )}
      </div>
    </div>
  );
}
