import { useCallback, useMemo, useRef, useState } from 'react';
import { Spin } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useTeamScheduleAccounts } from '@/hooks/team-schedule/useTeamScheduleAccounts';
import { useTeamSchedules } from '@/hooks/team-schedule/useTeamSchedules';
import type { TeamSchedule } from '@/api/team-schedule';
import { ScheduleFilterPanel } from './components/ScheduleFilterPanel';
import { ScheduleCalendar, type CalendarView } from './components/ScheduleCalendar';
import { DayScheduleListModal } from './components/DayScheduleListModal';
import { ScheduleEditModal } from './components/ScheduleEditModal';
import { usePermission } from '@/hooks/usePermission';

type FilterTab = 'member' | 'account';

const DATE_FMT = 'YYYY-MM-DD';

export default function SchedulePage() {
  const { hasPermission } = usePermission();
  const canWrite = hasPermission('SCHEDULE_WRITE');
  const [currentDate, setCurrentDate] = useState<Dayjs>(dayjs());
  const [viewType, setViewType] = useState<CalendarView>('dayGridMonth');
  const [listRange, setListRange] = useState<[Dayjs, Dayjs]>(() => [
    dayjs().startOf('month'),
    dayjs().endOf('month'),
  ]);
  const [filterTab, setFilterTab] = useState<FilterTab>('account');
  const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<number[]>([]);
  const [selectedAccountIds, setSelectedAccountIds] = useState<number[]>([]);
  const [selectedBranchCode, setSelectedBranchCode] = useState<string>('');
  const [selectedPromotionTeams, setSelectedPromotionTeams] = useState<string[]>([]);

  // Debounce filter values
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const [debouncedEmployeeIds, setDebouncedEmployeeIds] = useState<number[]>([]);
  const [debouncedAccountIds, setDebouncedAccountIds] = useState<number[]>([]);
  const [debouncedPromotionTeams, setDebouncedPromotionTeams] = useState<string[]>([]);

  const handleEmployeeIdsChange = useCallback((ids: number[]) => {
    setSelectedEmployeeIds(ids);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedEmployeeIds(ids), 300);
  }, []);

  const handleAccountIdsChange = useCallback((ids: number[]) => {
    setSelectedAccountIds(ids);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedAccountIds(ids), 300);
  }, []);

  const handlePromotionTeamsChange = useCallback((teams: string[]) => {
    setSelectedPromotionTeams(teams);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedPromotionTeams(teams), 300);
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
      employeeIds: filterTab === 'member' ? debouncedEmployeeIds : [],
      accountIds: filterTab === 'account' ? debouncedAccountIds : [],
      promotionTeams: debouncedPromotionTeams,
    };
  }, [
    currentDate,
    viewType,
    listRange,
    filterTab,
    debouncedEmployeeIds,
    debouncedAccountIds,
    debouncedPromotionTeams,
  ]);

  const { data, isLoading: schedulesLoading } = useTeamSchedules(queryParams);
  const schedules = data?.schedules ?? [];
  const summaries = data?.dailySummary ?? [];
  const { data: accounts = [] } = useTeamScheduleAccounts(selectedBranchCode);

  const handleDateClick = useCallback((date: string) => {
    setDayListModalDate(date);
    setDayListModalOpen(true);
  }, []);

  const openScheduleDetail = useCallback((schedule: TeamSchedule) => {
    if (schedule.workingCategory1 === '행사' && schedule.promotionId != null) {
      window.open(`/promotions/${schedule.promotionId}`, '_blank', 'noopener');
      return;
    }
    setEditSchedule(schedule);
    setEditModalOpen(true);
  }, []);

  const handleEventClick = openScheduleDetail;
  const handleScheduleClickFromList = openScheduleDetail;

  return (
    <div style={{ display: 'flex', height: '100%', gap: 16, padding: 16 }}>
      <div style={{ width: 240, flexShrink: 0 }}>
        <ScheduleFilterPanel
          filterTab={filterTab}
          onFilterTabChange={setFilterTab}
          selectedEmployeeIds={selectedEmployeeIds}
          onSelectedEmployeeIdsChange={handleEmployeeIdsChange}
          selectedAccountIds={selectedAccountIds}
          onSelectedAccountIdsChange={handleAccountIdsChange}
          selectedBranchCode={selectedBranchCode}
          onSelectedBranchCodeChange={setSelectedBranchCode}
          selectedPromotionTeams={selectedPromotionTeams}
          onSelectedPromotionTeamsChange={handlePromotionTeamsChange}
        />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Spin spinning={schedulesLoading}>
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
        </Spin>
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
