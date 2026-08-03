import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { message, Spin } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useTeamScheduleForm } from '@/hooks/team-schedule/useTeamScheduleForm';
import { useTeamSchedules } from '@/hooks/team-schedule/useTeamSchedules';
import type { TeamSchedule, TeamScheduleAccount } from '@/api/team-schedule';
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

  // 거래처를 먼저 골라 지점을 전환한 경우, 새 지점의 거래처 목록이 도착하면 자동 체크할 대상.
  const [pendingAccountId, setPendingAccountId] = useState<number | null>(null);

  const handleBranchCodeChange = useCallback((code: string) => {
    setSelectedBranchCode(code);
    // 지점이 바뀌면 거래처 목록 자체가 달라지므로 staging + applied 선택 모두 초기화.
    // applied 를 비우지 않으면 이전 지점의 accountId 로 schedules fetch 가 트리거됨.
    setSelectedAccountIds([]);
    setAppliedAccountIds([]);
    // 사용자가 지점을 직접 바꿨다면 거래처 검색으로 예약된 자동 체크는 무효.
    setPendingAccountId(null);
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

  // 월간/목록 뷰 모두 currentDate 의 월 단위 범위를 조회 — viewType 변경(탭 전환)이 from/to 에
  // 영향을 주지 않아 queryKey 가 동일하게 유지되고, 캘린더와 목록이 동일 데이터를 공유한다.
  const queryParams = useMemo(() => {
    return {
      from: currentDate.startOf('month').format(DATE_FMT),
      to: currentDate.endOf('month').format(DATE_FMT),
      employeeIds: filterTab === 'member' ? appliedEmployeeIds : [],
      accountIds: filterTab === 'account' ? appliedAccountIds : [],
      promotionTeams: appliedPromotionTeams,
      // 무필터(거래처/여사원 미선택) 호출 시 backend 가 지점 거래처 전체 기준 요약을 산출하도록 전달.
      // 단일지점 사용자는 빈 문자열이라 backend 가 본인 지점을 자동 사용.
      branchCode: selectedBranchCode || undefined,
    };
  }, [
    currentDate,
    filterTab,
    appliedEmployeeIds,
    appliedAccountIds,
    appliedPromotionTeams,
    selectedBranchCode,
  ]);

  // enabled 가드 제거로 필터 없이도 항상 조회 — 월/지점 변경 시 queryKey 변화로 자동 refetch.
  const { data, isLoading: schedulesLoading, isFetching: schedulesFetching, refetch: refetchSchedules } =
    useTeamSchedules(queryParams);
  const schedules = data?.schedules ?? [];

  // 화면 초기 로드 — branches/members/professional-promotion-teams/accounts/dailySummary 통합 fetch.
  // selectedBranchCode 변화 시 queryKey 가 달라져 자동 재요청 → 다중지점 사용자의 지점 선택 시점에
  // 해당 지점 거래처 + dailySummary 가 즉시 갱신된다. 단일지점 사용자는 selectedBranchCode 가
  // 빈 문자열이라 backend 가 본인 지점을 자동 사용.
  const { data: form, isLoading: isFormLoading } = useTeamScheduleForm(selectedBranchCode || undefined);
  // branches / accounts 는 아래 콜백·effect 의 deps 에 들어가므로 identity 를 고정한다
  // (`?? []` 를 그대로 쓰면 매 렌더 새 배열이라 거래처 자동 체크 effect 가 무한 재실행된다).
  const branches = useMemo(() => form?.branches ?? [], [form]);
  const members = form?.members ?? [];
  const promotionTeams = form?.professionalPromotionTeams ?? [];
  const accounts = useMemo(() => form?.accounts ?? [], [form]);

  /**
   * 거래처 먼저 찾기 — 검색 결과에서 고른 거래처의 지점으로 셀렉터를 전환한다.
   *
   * 이 화면의 지점 셀렉터는 **단일 선택**이라 여러 지점의 거래처가 섞일 수 없다. 따라서 이미 다른
   * 지점이 적용된 상태면 전환하지 않고 차단 안내만 한다 (지점을 바꾸려면 사용자가 셀렉터를 직접
   * 바꿔 기존 선택을 비우는 흐름 — [handleBranchCodeChange]). 같은 지점이면 그 자리에서 체크만 추가.
   */
  const handleAccountPicked = useCallback(
    (account: TeamScheduleAccount) => {
      const targetCode = account.selectorBranchCode;
      if (account.selectorBranchStatus !== 'RESOLVED' || !targetCode) {
        message.warning(
          account.selectorBranchStatus === 'AMBIGUOUS'
            ? `${account.name}의 지점을 자동으로 특정할 수 없습니다. 지점을 직접 선택해주세요.`
            : `${account.name}의 지점이 조회 권한 범위 밖입니다.`,
        );
        return;
      }
      if (!selectedBranchCode) {
        // 지점 미선택 → 이 거래처의 지점으로 전환하고, 목록이 도착하면 자동 체크한다.
        setSelectedBranchCode(targetCode);
        setPendingAccountId(account.accountId);
        return;
      }
      if (selectedBranchCode !== targetCode) {
        const currentName =
          branches.find((b) => b.branchCode === selectedBranchCode)?.branchName ?? '선택된';
        message.warning(
          `현재 ${currentName} 거래처만 선택할 수 있습니다. 다른 지점의 거래처를 고르려면 지점 선택을 먼저 바꿔주세요.`,
        );
        return;
      }
      markUserTouched();
      setSelectedAccountIds((prev) =>
        prev.includes(account.accountId) ? prev : [...prev, account.accountId],
      );
    },
    [selectedBranchCode, branches, markUserTouched],
  );

  // 지점 전환으로 새 거래처 목록이 도착하면, 검색으로 고른 거래처를 체크한 뒤 예약을 해제한다.
  useEffect(() => {
    if (pendingAccountId == null) return;
    if (!accounts.some((a) => a.accountId === pendingAccountId)) return;
    markUserTouched();
    setSelectedAccountIds((prev) =>
      prev.includes(pendingAccountId) ? prev : [...prev, pendingAccountId],
    );
    setPendingAccountId(null);
  }, [pendingAccountId, accounts, markUserTouched]);

  // 캘린더 요약은 월/지점 단위 조회 결과 (data.dailySummary) 를 우선 사용.
  // 첫 응답 도착 전(data 없음)에는 form 의 현재 월 요약을 fallback 으로 표시.
  const summaries = data?.dailySummary ?? form?.dailySummary ?? [];

  // 요약 배지 표시 가능 여부 — 현재 조회(월/지점)의 fetch 가 진행 중이면 false.
  // 월 변경 시 새 fetch 가 시작되는 동안 이전 달 배지를 모두 숨겼다가, 완료 후 다시 표시.
  const summariesReady = !schedulesFetching && data !== undefined;

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
    <div style={{ display: 'flex', height: 'calc(100vh - 160px)', minHeight: 0, gap: 16, padding: 16, boxSizing: 'border-box' }}>
      <div style={{ width: 240, flexShrink: 0, minHeight: 0, height: '100%' }}>
        <ScheduleFilterPanel
          filterTab={filterTab}
          onFilterTabChange={handleFilterTabChange}
          branches={branches}
          members={members}
          accounts={accounts}
          promotionTeams={promotionTeams}
          isFormLoading={isFormLoading}
          selectedEmployeeIds={selectedEmployeeIds}
          onSelectedEmployeeIdsChange={handleEmployeeIdsChange}
          selectedAccountIds={selectedAccountIds}
          onSelectedAccountIdsChange={handleAccountIdsChange}
          selectedBranchCode={selectedBranchCode}
          onSelectedBranchCodeChange={handleBranchCodeChange}
          onAccountPicked={handleAccountPicked}
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
            schedules={schedules}
            summaries={summaries}
            summariesReady={summariesReady}
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
        summary={summaries.find((s) => s.date === dayListModalDate)}
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
