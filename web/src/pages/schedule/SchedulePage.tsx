import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { message, Spin } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useTeamScheduleAccounts } from '@/hooks/team-schedule/useTeamScheduleAccounts';
import { useTeamScheduleForm } from '@/hooks/team-schedule/useTeamScheduleForm';
import { useTeamSchedules } from '@/hooks/team-schedule/useTeamSchedules';
import type { TeamSchedule } from '@/api/team-schedule';
import { ScheduleFilterPanel } from './components/ScheduleFilterPanel';
import { ScheduleCalendar, type CalendarView } from './components/ScheduleCalendar';
import { DayScheduleListModal } from './components/DayScheduleListModal';
import { ScheduleEditModal } from './components/ScheduleEditModal';
import { usePermission } from '@/hooks/usePermission';

type FilterTab = 'member' | 'account';

const DATE_FMT = 'YYYY-MM-DD';
const COOLDOWN_MS = 1500;

export default function SchedulePage() {
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('team_member_schedule', 'EDIT');
  const [currentDate, setCurrentDate] = useState<Dayjs>(dayjs());
  const [viewType, setViewType] = useState<CalendarView>('dayGridMonth');
  const [listRange, setListRange] = useState<[Dayjs, Dayjs]>(() => [
    dayjs().startOf('month'),
    dayjs().endOf('month'),
  ]);
  const [filterTab, setFilterTab] = useState<FilterTab>('account');

  // staging: 사용자 체크박스 선택 (UI 즉시 반영). applied: 조회 버튼 클릭 시 commit (fetch trigger).
  // 다수 여사원/거래처 multi-select 중 매 체크마다 자동 조회되던 동작 제거.
  const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<number[]>([]);
  const [selectedAccountIds, setSelectedAccountIds] = useState<number[]>([]);
  const [selectedPromotionTeams, setSelectedPromotionTeams] = useState<string[]>([]);
  const [appliedEmployeeIds, setAppliedEmployeeIds] = useState<number[]>([]);
  const [appliedAccountIds, setAppliedAccountIds] = useState<number[]>([]);
  const [appliedPromotionTeams, setAppliedPromotionTeams] = useState<string[]>([]);

  const [selectedBranchCode, setSelectedBranchCode] = useState<string>('');

  // 사용자가 staging 필터를 한 번이라도 수정했는지 여부 — dirty 판정 가드.
  // 초기 mount 시 staging=빈/applied=전체거래처 자연 mismatch 가 dirty 로 잘못 표시되는 것 회피.
  const userTouchedFilterRef = useRef(false);
  const markUserTouched = useCallback(() => { userTouchedFilterRef.current = true; }, []);

  const handleEmployeeIdsChange = useCallback((ids: number[]) => {
    markUserTouched();
    setSelectedEmployeeIds(ids);
  }, [markUserTouched]);

  const handleAccountIdsChange = useCallback((ids: number[]) => {
    markUserTouched();
    setSelectedAccountIds(ids);
  }, [markUserTouched]);

  const handlePromotionTeamsChange = useCallback((teams: string[]) => {
    markUserTouched();
    setSelectedPromotionTeams(teams);
  }, [markUserTouched]);

  const handleBranchCodeChange = useCallback((code: string) => {
    setSelectedBranchCode(code);
    // 지점이 바뀌면 거래처 목록 자체가 달라지므로 staging 선택 초기화.
    setSelectedAccountIds([]);
  }, []);

  const handleFilterTabChange = useCallback((tab: FilterTab) => {
    setFilterTab(tab);
    // XOR — 다른 탭의 applied 도 비워 backend 가 빈 결과 분기 (fetch hook enabled=false).
    setAppliedEmployeeIds([]);
    setAppliedAccountIds([]);
  }, []);

  // Modal state
  const [dayListModalOpen, setDayListModalOpen] = useState(false);
  const [dayListModalDate, setDayListModalDate] = useState('');
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editSchedule, setEditSchedule] = useState<TeamSchedule | null>(null);

  const queryParams = useMemo(() => {
    const from =
      viewType === 'listMonth'
        ? listRange[0].format(DATE_FMT)
        : currentDate.startOf('month').format(DATE_FMT);
    const to =
      viewType === 'listMonth'
        ? listRange[1].format(DATE_FMT)
        : currentDate.endOf('month').format(DATE_FMT);
    return {
      from,
      to,
      employeeIds: filterTab === 'member' ? appliedEmployeeIds : [],
      accountIds: filterTab === 'account' ? appliedAccountIds : [],
      promotionTeams: appliedPromotionTeams,
    };
  }, [
    currentDate,
    viewType,
    listRange,
    filterTab,
    appliedEmployeeIds,
    appliedAccountIds,
    appliedPromotionTeams,
  ]);

  const { data, isLoading: schedulesLoading, refetch: refetchSchedules } = useTeamSchedules(queryParams);
  const schedules = data?.schedules ?? [];
  const summaries = data?.dailySummary ?? [];

  // 화면 초기 로드 — branches/members/professional-promotion-teams + (단일지점 시) accounts 통합 fetch.
  const { data: form, isLoading: isFormLoading } = useTeamScheduleForm();
  const branches = form?.branches ?? [];
  const members = form?.members ?? [];
  const promotionTeams = form?.professionalPromotionTeams ?? [];

  // 다중지점 사용자가 지점 드롭다운에서 선택했을 때만 거래처를 별도 fetch.
  // 단일지점 사용자는 form 응답 안에 accounts 가 이미 들어 있으므로 추가 호출 없음 (enabled=false).
  const isSingleBranch = branches.length === 1;
  const accountsFetchBranchCode = isSingleBranch ? '' : selectedBranchCode;
  const { data: fetchedAccounts = [], isLoading: isAccountsLoading } = useTeamScheduleAccounts(
    accountsFetchBranchCode,
    { enabled: !isSingleBranch && accountsFetchBranchCode.length > 0 },
  );
  const accounts = isSingleBranch ? form?.accounts ?? [] : fetchedAccounts;

  // SF 레거시 정합 — 마운트 시 캘린더 요약은 항상 즉시 노출 (조건 선택 무관).
  // staging (체크박스 표시) 은 비워 두고 applied 에만 거래처 전체를 채워 fetch 트리거.
  // 사용자가 직접 거래처를 선택/조회하기 전까지 "내 거래처 전체" 가 backend 호출 기준.
  // accounts 가 로드된 첫 시점에 한해 1회 적용 (이후 사용자 입력 우선).
  const autoFilledRef = useRef(false);
  useEffect(() => {
    if (autoFilledRef.current) return;
    if (filterTab !== 'account') return;
    if (accounts.length === 0) return;
    setAppliedAccountIds(accounts.map((a) => a.accountId));
    autoFilledRef.current = true;
  }, [accounts, filterTab]);

  // staging 이 applied 와 동일해도 "조회" 클릭 시 항상 강제 재요청 — react-query 가 동일 queryKey 일 때
  // cache 즉시 반환만 하고 background refetch 안 하므로 명시 refetch 필요.
  // 잦은 클릭으로 인한 서버 부하 방지 — 1.5초 cooldown.
  const [isCoolingDown, setIsCoolingDown] = useState(false);
  const cooldownTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    return () => {
      if (cooldownTimerRef.current) clearTimeout(cooldownTimerRef.current);
    };
  }, []);

  const handleApplyFilter = useCallback(() => {
    if (isCoolingDown) return;
    setAppliedEmployeeIds(selectedEmployeeIds);
    setAppliedAccountIds(selectedAccountIds);
    setAppliedPromotionTeams(selectedPromotionTeams);
    refetchSchedules();
    setIsCoolingDown(true);
    if (cooldownTimerRef.current) clearTimeout(cooldownTimerRef.current);
    cooldownTimerRef.current = setTimeout(() => setIsCoolingDown(false), COOLDOWN_MS);
  }, [
    isCoolingDown,
    selectedEmployeeIds,
    selectedAccountIds,
    selectedPromotionTeams,
    refetchSchedules,
  ]);

  const handleDateClick = useCallback((date: string) => {
    setDayListModalDate(date);
    setDayListModalOpen(true);
  }, []);

  const openScheduleDetail = useCallback((schedule: TeamSchedule) => {
    if (schedule.workingCategory1 === '행사') {
      if (schedule.promotionId == null) {
        message.warning('연결된 행사를 찾을 수 없습니다.');
        return;
      }
      message.info('행사페이지가 안 뜰 경우 다시 클릭해주세요.');
      window.open(`/promotions/${schedule.promotionId}`, '_blank', 'noopener');
      return;
    }
    setEditSchedule(schedule);
    setEditModalOpen(true);
  }, []);

  const handleEventClick = openScheduleDetail;
  const handleScheduleClickFromList = openScheduleDetail;

  // staging != applied 인 경우 "조회" 버튼 강조용 dirty flag.
  // 초기 mount 시점에는 staging 비어 있고 applied 가 auto-fill (전체 거래처) 로 채워져
  // 자연 mismatch 가 발생하므로, 사용자가 한번이라도 staging 을 수정했을 때만 dirty 판정.
  const isFilterDirty = useMemo(() => {
    if (!userTouchedFilterRef.current) return false;
    const stagingIds = filterTab === 'member' ? selectedEmployeeIds : selectedAccountIds;
    const appliedIds = filterTab === 'member' ? appliedEmployeeIds : appliedAccountIds;
    return (
      !isSameSet(stagingIds, appliedIds) ||
      !isSameSet(selectedPromotionTeams, appliedPromotionTeams)
    );
  }, [
    filterTab,
    selectedEmployeeIds,
    selectedAccountIds,
    appliedEmployeeIds,
    appliedAccountIds,
    selectedPromotionTeams,
    appliedPromotionTeams,
  ]);

  return (
    <div style={{ display: 'flex', height: '100%', minHeight: 0, gap: 16, padding: 16 }}>
      <div style={{ width: 240, flexShrink: 0, minHeight: 0, height: '100%' }}>
        <ScheduleFilterPanel
          filterTab={filterTab}
          onFilterTabChange={handleFilterTabChange}
          branches={branches}
          members={members}
          accounts={accounts}
          promotionTeams={promotionTeams}
          isFormLoading={isFormLoading}
          isAccountsLoading={isAccountsLoading}
          selectedEmployeeIds={selectedEmployeeIds}
          onSelectedEmployeeIdsChange={handleEmployeeIdsChange}
          selectedAccountIds={selectedAccountIds}
          onSelectedAccountIdsChange={handleAccountIdsChange}
          selectedBranchCode={selectedBranchCode}
          onSelectedBranchCodeChange={handleBranchCodeChange}
          selectedPromotionTeams={selectedPromotionTeams}
          onSelectedPromotionTeamsChange={handlePromotionTeamsChange}
          onApply={handleApplyFilter}
          isFilterDirty={isFilterDirty}
          isCoolingDown={isCoolingDown}
        />
      </div>
      <div style={{ flex: 1, minWidth: 0, minHeight: 0, height: '100%', overflow: 'hidden', display: 'flex', flexDirection: 'column', position: 'relative' }}>
        {schedulesLoading && (
          <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.5)', zIndex: 10 }}>
            <Spin />
          </div>
        )}
        <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <ScheduleCalendar
            currentDate={currentDate}
            onDateChange={setCurrentDate}
            viewType={viewType}
            onViewTypeChange={setViewType}
            listRange={listRange}
            onListRangeChange={setListRange}
            schedules={schedules}
            summaries={summaries}
            onDateClick={handleDateClick}
            onEventClick={handleEventClick}
            isLoading={schedulesLoading}
          />
        </div>
      </div>

      <DayScheduleListModal
        open={dayListModalOpen}
        onClose={() => setDayListModalOpen(false)}
        date={dayListModalDate}
        schedules={schedules}
        onScheduleClick={handleScheduleClickFromList}
      />

      <ScheduleEditModal
        open={editModalOpen}
        onClose={() => {
          setEditModalOpen(false);
          setEditSchedule(null);
        }}
        schedule={editSchedule}
        accounts={accounts}
        readOnly={!canWrite}
      />
    </div>
  );
}

function isSameSet<T extends string | number>(a: T[], b: T[]): boolean {
  if (a.length !== b.length) return false;
  const setA = new Set(a);
  for (const v of b) {
    if (!setA.has(v)) return false;
  }
  return true;
}
