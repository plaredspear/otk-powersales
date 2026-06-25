import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Segmented, Select, Spin, Tag, Typography } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useAttendInfoBranches, useAttendInfoMembers } from '@/hooks/attend-info/useAttendInfo';
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
  // 다중/전사 권한자가 선택한 지점. 단일지점 사용자는 본인 지점이 자동 채워진다.
  const [branchCode, setBranchCode] = useState<string | undefined>(undefined);

  // 권한별 조회 허용 지점. 단일지점이면 1건 → 고정 표시, 다중/전사면 드롭다운.
  const branchesQuery = useAttendInfoBranches();
  const branches = useMemo(() => branchesQuery.data ?? [], [branchesQuery.data]);
  const singleBranch = branches.length === 1;

  // 단일지점 사용자는 본인 지점을 자동 선택. stale 코드(권한 주체 변경)는 정리.
  useEffect(() => {
    if (branches.length === 0) return;
    if (branchCode && !branches.some((b) => b.branchCode === branchCode)) {
      setBranchCode(undefined);
      setSelected(undefined);
      return;
    }
    if (singleBranch && !branchCode) {
      setBranchCode(branches[0].branchCode);
    }
  }, [branches, singleBranch, branchCode]);

  // 근무기간 조회 전용 여사원 목록 — 선택 지점(또는 본인 지점) + 퇴사/휴직 포함 (attend_info 권한).
  const membersQuery = useAttendInfoMembers(branchCode);

  // 지점 변경 시 이전 지점에서 고른 여사원은 무효 — 선택 해제.
  const handleBranchChange = useCallback((code: string) => {
    setBranchCode(code);
    setSelected(undefined);
  }, []);

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
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, flexShrink: 0 }}>
        {/* 지점명 — 단일지점은 고정 Tag, 다중/전사 권한자는 선택 드롭다운. */}
        <div style={{ width: 240 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            지점명
          </Text>
          {singleBranch ? (
            <div style={{ marginTop: 4 }}>
              <Tag color="geekblue" style={{ fontSize: 13, padding: '3px 10px', marginInlineEnd: 0 }}>
                {branches[0].branchName}
              </Tag>
            </div>
          ) : (
            <Select
              size="small"
              style={{ width: '100%', marginTop: 4 }}
              placeholder="지점 선택"
              value={branchCode}
              onChange={handleBranchChange}
              options={branches.map((b) => ({ value: b.branchCode, label: b.branchName }))}
              showSearch
              optionFilterProp="label"
              loading={branchesQuery.isLoading}
              notFoundContent="지점 없음"
            />
          )}
        </div>

        <MonthlyMemberSelectPanel
          members={membersQuery.data ?? []}
          isLoading={membersQuery.isLoading}
          selectedId={employeeId}
          onSelect={setSelected}
        />
      </div>

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
