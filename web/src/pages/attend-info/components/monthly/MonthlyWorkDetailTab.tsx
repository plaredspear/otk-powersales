import { useState } from 'react';
import { Alert, DatePicker, Spin, Tabs, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useTeamScheduleForm } from '@/hooks/team-schedule/useTeamScheduleForm';
import { useEmployeeMonthlyWorkHistory } from '@/hooks/employee/useEmployeeWorkHistory';
import type { TeamMember } from '@/api/team-schedule';
import RefreshButton from '@/components/common/RefreshButton';
import { MonthlyMemberSelectPanel } from './MonthlyMemberSelectPanel';
import MonthlyWorkRawTable from './MonthlyWorkRawTable';
import MonthlyWorkInsight from './MonthlyWorkInsight';

const { Text } = Typography;

/**
 * 근무기간 조회 — 월별 개인 근무내역(어디서/어떻게).
 *
 * 좌측에 여사원 일정관리와 동일한 리스트(검색 + 이름(사번))가 페이지 접근 즉시 나열되고,
 * 1명을 클릭하면 우측에서 지정 월의 근무내역을 team_member_schedule 기반으로 조회.
 * 기본 화면은 근무 인사이트(요약+캘린더 2단).
 */
export default function MonthlyWorkDetailTab() {
  const [selected, setSelected] = useState<TeamMember | undefined>(undefined);
  const [period, setPeriod] = useState<Dayjs>(dayjs());

  // 여사원 일정관리와 동일한 form 응답 — members(본인 지점 스코프 자동) 를 즉시 나열.
  const formQuery = useTeamScheduleForm();

  const employeeId = selected?.employeeId;
  const yearMonth = period.format('YYYY-MM');
  const histQuery = useEmployeeMonthlyWorkHistory(employeeId, employeeId ? yearMonth : undefined);

  const items = histQuery.data?.items ?? [];

  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
      <MonthlyMemberSelectPanel
        members={formQuery.data?.members ?? []}
        isLoading={formQuery.isLoading}
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
              {selected.name}({selected.employeeCode})
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

        {formQuery.isError && (
          <Alert
            type="error"
            message={(formQuery.error as Error)?.message ?? '여사원 목록 조회 실패'}
            style={{ marginBottom: 8 }}
          />
        )}
        {histQuery.isError && (
          <Alert
            type="error"
            message={(histQuery.error as Error)?.message ?? '조회 실패'}
            style={{ marginBottom: 8 }}
          />
        )}

        {employeeId == null && (
          <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
            좌측에서 여사원을 선택하면 해당 월 근무내역이 채워집니다.
          </Text>
        )}

        {/* 캘린더는 여사원 미선택/로딩과 무관하게 항상 노출. 선택 전에는 빈 달력. */}
        <Tabs
          defaultActiveKey="insight"
          items={[
            {
              key: 'insight',
              label: '근무 인사이트',
              children:
                employeeId != null && histQuery.isLoading ? (
                  <div style={{ textAlign: 'center', padding: 48 }}>
                    <Spin size="large" />
                  </div>
                ) : (
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
              children:
                employeeId != null && histQuery.isLoading ? (
                  <div style={{ textAlign: 'center', padding: 48 }}>
                    <Spin size="large" />
                  </div>
                ) : (
                  <MonthlyWorkRawTable items={items} />
                ),
            },
          ]}
        />
      </div>
    </div>
  );
}
