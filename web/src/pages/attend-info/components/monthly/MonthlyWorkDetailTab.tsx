import { useCallback, useState } from 'react';
import { Alert, Button, Segmented, Spin, Tag, Typography } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useAttendInfoMembers } from '@/hooks/attend-info/useAttendInfo';
import { useEmployeeMonthlyWorkHistory } from '@/hooks/employee/useEmployeeWorkHistory';
import { MEMBER_STATUS_COLOR, type TeamMember } from '@/api/team-schedule';
import RefreshButton from '@/components/common/RefreshButton';
import { MonthlyMemberSelectPanel } from './MonthlyMemberSelectPanel';
import MonthlyWorkRawTable from './MonthlyWorkRawTable';
import MonthlyWorkInsight from './MonthlyWorkInsight';

const { Text } = Typography;

type MonthlyView = 'month' | 'list';

/**
 * 근무기간 조회 — 월별 개인 근무내역(어디서/어떻게).
 *
 * 좌측에 여사원 일정관리와 동일한 리스트(검색 + 이름(사번))가 페이지 접근 즉시 나열되고,
 * 1명을 클릭하면 우측에서 지정 월의 근무내역을 team_member_schedule 기반으로 조회.
 * 우측 캘린더 상단은 여사원 일정관리와 동일한 [◀ 년월 ▶ 오늘] 네비게이션 + [월간/목록] 토글.
 */
export default function MonthlyWorkDetailTab() {
  const [selected, setSelected] = useState<TeamMember | undefined>(undefined);
  const [period, setPeriod] = useState<Dayjs>(dayjs());
  const [viewType, setViewType] = useState<MonthlyView>('month');

  // 근무기간 조회 전용 여사원 목록 — 본인 지점 스코프 자동 + 퇴사/휴직 포함 (attend_info 권한).
  const membersQuery = useAttendInfoMembers();

  const employeeId = selected?.employeeId;
  const yearMonth = period.format('YYYY-MM');
  const histQuery = useEmployeeMonthlyWorkHistory(employeeId, employeeId ? yearMonth : undefined);

  const items = histQuery.data?.items ?? [];

  const handlePrev = useCallback(() => setPeriod((p) => p.subtract(1, 'month')), []);
  const handleNext = useCallback(() => setPeriod((p) => p.add(1, 'month')), []);
  const handleToday = useCallback(() => setPeriod(dayjs()), []);

  const isHistLoading = employeeId != null && histQuery.isLoading;

  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
      <MonthlyMemberSelectPanel
        members={membersQuery.data ?? []}
        isLoading={membersQuery.isLoading}
        selectedId={employeeId}
        onSelect={setSelected}
      />

      <div style={{ flex: 1, minWidth: 0 }}>
        {/* 여사원 일정관리 캘린더 상단과 동일한 헤더 — 월 네비게이션 + 월간/목록 토글 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 16,
            gap: 12,
            flexWrap: 'wrap',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Button icon={<LeftOutlined />} size="small" onClick={handlePrev} />
            <span style={{ fontSize: 18, fontWeight: 600, minWidth: 140, textAlign: 'center' }}>
              {period.year()}년 {period.month() + 1}월
            </span>
            <Button icon={<RightOutlined />} size="small" onClick={handleNext} />
            <Button size="small" onClick={handleToday} style={{ marginLeft: 8 }}>
              오늘
            </Button>
            {employeeId != null && (
              <RefreshButton onRefresh={histQuery.refetch} refreshing={histQuery.isFetching} />
            )}
          </div>
          <Segmented
            size="small"
            options={[
              { label: '월간', value: 'month' },
              { label: '목록', value: 'list' },
            ]}
            value={viewType}
            onChange={(v) => setViewType(v as MonthlyView)}
          />
        </div>

        {selected ? (
          <div
            style={{
              marginBottom: 12,
              padding: '8px 12px',
              borderLeft: '3px solid #1677ff',
              borderRadius: 4,
              background: '#f0f7ff',
            }}
          >
            <Text strong style={{ fontSize: 15 }}>
              {[
                selected.orgName,
                `${selected.name}(${selected.employeeCode})`,
                selected.jikwee,
              ]
                .filter(Boolean)
                .join(' · ')}
            </Text>
            {selected.status && (
              <Tag
                color={MEMBER_STATUS_COLOR[selected.status] ?? 'default'}
                style={{ marginLeft: 8 }}
              >
                {selected.status}
              </Tag>
            )}
          </div>
        ) : (
          <div style={{ marginBottom: 12 }}>
            <Text type="secondary">
              좌측에서 여사원을 선택하면 해당 월 근무내역이 채워집니다.
            </Text>
          </div>
        )}

        {membersQuery.isError && (
          <Alert
            type="error"
            message={(membersQuery.error as Error)?.message ?? '여사원 목록 조회 실패'}
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

        {/* 캘린더(월간)는 여사원 미선택/내역 0건과 무관하게 항상 노출 — 선택 전에는 빈 달력.
            여사원 변경 시 로딩 중에도 캘린더를 유지하고, 로딩 스피너는 그 위에 오버레이. */}
        <Spin spinning={isHistLoading} size="large">
          {viewType === 'month' ? (
            <MonthlyWorkInsight items={items} year={period.year()} month={period.month() + 1} />
          ) : (
            <MonthlyWorkRawTable items={items} />
          )}
        </Spin>
      </div>
    </div>
  );
}
