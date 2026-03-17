import { useCallback, useMemo, useRef, useState } from 'react';
import { Spin } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useTeamMembers } from '@/hooks/team-schedule/useTeamMembers';
import { useTeamScheduleAccounts } from '@/hooks/team-schedule/useTeamScheduleAccounts';
import { useTeamSchedules } from '@/hooks/team-schedule/useTeamSchedules';
import { useTeamScheduleSummary } from '@/hooks/team-schedule/useTeamScheduleSummary';
import type { TeamSchedule } from '@/api/team-schedule';
import { ScheduleFilterPanel } from './components/ScheduleFilterPanel';
import { ScheduleCalendar } from './components/ScheduleCalendar';
import { ScheduleCreateModal } from './components/ScheduleCreateModal';
import { ScheduleEditModal } from './components/ScheduleEditModal';

type FilterTab = 'member' | 'account';

export default function SchedulePage() {
  const [currentDate, setCurrentDate] = useState<Dayjs>(dayjs());
  const [filterTab, setFilterTab] = useState<FilterTab>('member');
  const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<string[]>([]);
  const [selectedAccountSfids, setSelectedAccountSfids] = useState<string[]>([]);
  const [selectedBranchCode, setSelectedBranchCode] = useState<string>('');

  // Debounce filter values
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const [debouncedEmployeeIds, setDebouncedEmployeeIds] = useState<string[]>([]);
  const [debouncedAccountSfids, setDebouncedAccountSfids] = useState<string[]>([]);

  const handleEmployeeIdsChange = useCallback((ids: string[]) => {
    setSelectedEmployeeIds(ids);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedEmployeeIds(ids), 300);
  }, []);

  const handleAccountSfidsChange = useCallback((ids: string[]) => {
    setSelectedAccountSfids(ids);
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => setDebouncedAccountSfids(ids), 300);
  }, []);

  // Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createModalDate, setCreateModalDate] = useState('');
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editSchedule, setEditSchedule] = useState<TeamSchedule | null>(null);

  const year = currentDate.year();
  const month = currentDate.month() + 1;

  const queryParams = useMemo(
    () => ({
      year,
      month,
      employeeIds: filterTab === 'member' ? debouncedEmployeeIds : [],
      accountSfids: filterTab === 'account' ? debouncedAccountSfids : [],
    }),
    [year, month, filterTab, debouncedEmployeeIds, debouncedAccountSfids],
  );

  const { data: schedules = [], isLoading: schedulesLoading } = useTeamSchedules(queryParams);
  const { data: summaries = [] } = useTeamScheduleSummary(queryParams);
  const { data: members = [] } = useTeamMembers();
  const { data: accounts = [] } = useTeamScheduleAccounts(selectedBranchCode);

  const handleDateClick = useCallback((date: string) => {
    setCreateModalDate(date);
    setCreateModalOpen(true);
  }, []);

  const handleEventClick = useCallback((schedule: TeamSchedule) => {
    setEditSchedule(schedule);
    setEditModalOpen(true);
  }, []);

  return (
    <div style={{ display: 'flex', height: '100%', gap: 16, padding: 16 }}>
      <div style={{ width: 240, flexShrink: 0 }}>
        <ScheduleFilterPanel
          filterTab={filterTab}
          onFilterTabChange={setFilterTab}
          selectedEmployeeIds={selectedEmployeeIds}
          onSelectedEmployeeIdsChange={handleEmployeeIdsChange}
          selectedAccountSfids={selectedAccountSfids}
          onSelectedAccountSfidsChange={handleAccountSfidsChange}
          selectedBranchCode={selectedBranchCode}
          onSelectedBranchCodeChange={setSelectedBranchCode}
        />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Spin spinning={schedulesLoading}>
          <ScheduleCalendar
            currentDate={currentDate}
            onDateChange={setCurrentDate}
            schedules={schedules}
            summaries={summaries}
            onDateClick={handleDateClick}
            onEventClick={handleEventClick}
            isLoading={schedulesLoading}
          />
        </Spin>
      </div>

      <ScheduleCreateModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        date={createModalDate}
        members={members}
        accounts={accounts}
      />

      <ScheduleEditModal
        open={editModalOpen}
        onClose={() => {
          setEditModalOpen(false);
          setEditSchedule(null);
        }}
        schedule={editSchedule}
        accounts={accounts}
      />
    </div>
  );
}
