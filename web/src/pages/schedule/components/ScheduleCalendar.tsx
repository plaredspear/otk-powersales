import { useCallback, useEffect, useMemo, useRef } from 'react';
import { Button, DatePicker, Segmented } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import type { DayCellContentArg, EventContentArg, EventInput } from '@fullcalendar/core';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import type { DailySummary, TeamSchedule } from '@/api/team-schedule';
import { DaySummaryBanner } from './DaySummaryBanner';
import { ScheduleEventCard } from './ScheduleEventCard';

export type CalendarView = 'dayGridMonth' | 'listMonth';

interface ScheduleCalendarProps {
  currentDate: Dayjs;
  onDateChange: (d: Dayjs) => void;
  viewType: CalendarView;
  onViewTypeChange: (v: CalendarView) => void;
  listRange: [Dayjs, Dayjs];
  onListRangeChange: (range: [Dayjs, Dayjs]) => void;
  schedules: TeamSchedule[];
  summaries: DailySummary[];
  onDateClick: (date: string) => void;
  onEventClick: (schedule: TeamSchedule) => void;
  isLoading: boolean;
}

function getEventColor(schedule: TeamSchedule): string {
  if (schedule.workingType === '연차' || schedule.workingType === '대휴') return '#495E62';
  if (schedule.workingCategory1 === '행사') return '#F392BC';
  if (schedule.workingCategory1 === '진열') return '#4E8BBF';
  return '#4E8BBF';
}

export function ScheduleCalendar({
  currentDate,
  onDateChange,
  viewType,
  onViewTypeChange,
  listRange,
  onListRangeChange,
  schedules,
  summaries,
  onDateClick,
  onEventClick,
}: ScheduleCalendarProps) {
  const calendarRef = useRef<FullCalendar>(null);

  const summaryMap = useMemo(() => {
    const map = new Map<string, DailySummary>();
    summaries.forEach((s) => map.set(s.date, s));
    return map;
  }, [summaries]);

  const scheduleMap = useMemo(() => {
    const map = new Map<string, TeamSchedule>();
    schedules.forEach((s) => map.set(String(s.id), s));
    return map;
  }, [schedules]);

  const events: EventInput[] = useMemo(
    () =>
      schedules.map((s) => {
        const color = getEventColor(s);
        return {
          id: String(s.id),
          title: s.employeeName,
          date: s.workingDate,
          backgroundColor: color,
          borderColor: color,
          textColor: '#fff',
          extendedProps: { dotColor: color },
        };
      }),
    [schedules],
  );

  const handlePrev = useCallback(() => {
    const newDate = currentDate.subtract(1, 'month');
    onDateChange(newDate);
    calendarRef.current?.getApi().prev();
  }, [currentDate, onDateChange]);

  const handleNext = useCallback(() => {
    const newDate = currentDate.add(1, 'month');
    onDateChange(newDate);
    calendarRef.current?.getApi().next();
  }, [currentDate, onDateChange]);

  const handleToday = useCallback(() => {
    const today = dayjs();
    onDateChange(today);
    calendarRef.current?.getApi().today();
  }, [onDateChange]);

  const handleViewChange = useCallback(
    (value: string | number) => {
      const view = value as CalendarView;
      onViewTypeChange(view);
      calendarRef.current?.getApi().changeView(view);
      const calDate = currentDate.format('YYYY-MM-DD');
      calendarRef.current?.getApi().gotoDate(calDate);
    },
    [currentDate, onViewTypeChange],
  );

  // 목록 뷰의 임의 기간 표시 — FullCalendar visibleRange 동적 적용
  useEffect(() => {
    if (viewType !== 'listMonth') return;
    const api = calendarRef.current?.getApi();
    if (!api) return;
    // visibleRange.end 는 exclusive 이므로 +1일
    api.changeView('listMonth', {
      start: listRange[0].format('YYYY-MM-DD'),
      end: listRange[1].add(1, 'day').format('YYYY-MM-DD'),
    });
  }, [viewType, listRange]);

  const renderDayCellContent = useCallback(
    (arg: DayCellContentArg) => {
      const dateStr = dayjs(arg.date).format('YYYY-MM-DD');
      const summary = summaryMap.get(dateStr);
      return (
        <div
          style={{ width: '100%', cursor: 'pointer' }}
          onClick={(e) => {
            e.stopPropagation();
            onDateClick(dateStr);
          }}
        >
          <div style={{ fontWeight: 500, marginBottom: 2 }}>{arg.dayNumberText}</div>
          <DaySummaryBanner summary={summary} />
        </div>
      );
    },
    [summaryMap, onDateClick],
  );

  const renderEventContent = useCallback(
    (arg: EventContentArg) => {
      const schedule = scheduleMap.get(arg.event.id);
      if (!schedule) return null;
      const variant = arg.view.type === 'listMonth' ? 'list' : 'month';
      return <ScheduleEventCard schedule={schedule} variant={variant} />;
    },
    [scheduleMap],
  );

  const handleEventClick = useCallback(
    (info: { event: { id: string } }) => {
      const schedule = scheduleMap.get(info.event.id);
      if (!schedule) return;
      if (schedule.workingType !== '근무') return;
      onEventClick(schedule);
    },
    [scheduleMap, onEventClick],
  );

  const isListView = viewType === 'listMonth';

  return (
    <div
      style={{
        background: '#fff',
        borderRadius: 8,
        padding: 16,
        border: '1px solid #f0f0f0',
      }}
    >
      {/* Custom Header */}
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
          {isListView ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <DatePicker.RangePicker
                size="small"
                value={listRange}
                onChange={(range) => {
                  if (range && range[0] && range[1]) {
                    onListRangeChange([range[0], range[1]]);
                  }
                }}
                allowClear={false}
                disabledDate={(current, info) => {
                  if (!info.from) return false;
                  // info.from 기준 ±91일 초과면 disable → 최대 92일 (시작일 포함)
                  return Math.abs(current.diff(info.from, 'day')) > 91;
                }}
              />
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>최대 92일</span>
            </div>
          ) : (
            <>
              <Button icon={<LeftOutlined />} size="small" onClick={handlePrev} />
              <span style={{ fontSize: 18, fontWeight: 600, minWidth: 140, textAlign: 'center' }}>
                {currentDate.year()}년 {currentDate.month() + 1}월
              </span>
              <Button icon={<RightOutlined />} size="small" onClick={handleNext} />
              <Button size="small" onClick={handleToday} style={{ marginLeft: 8 }}>
                오늘
              </Button>
            </>
          )}
        </div>
        <Segmented
          size="small"
          options={[
            { label: '월간', value: 'dayGridMonth' },
            { label: '목록', value: 'listMonth' },
          ]}
          value={viewType}
          onChange={handleViewChange}
        />
      </div>

      {/* FullCalendar */}
      <FullCalendar
        ref={calendarRef}
        plugins={[dayGridPlugin, listPlugin]}
        initialView={viewType}
        initialDate={currentDate.format('YYYY-MM-DD')}
        headerToolbar={false}
        locale="ko"
        allDayText="종일"
        noEventsText="일정이 없습니다"
        height="auto"
        dayMaxEvents={4}
        events={events}
        dayCellContent={renderDayCellContent}
        eventContent={renderEventContent}
        eventClick={handleEventClick}
        fixedWeekCount={false}
      />
    </div>
  );
}
