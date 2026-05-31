import { useCallback, useMemo, useRef } from 'react';
import { Button, Segmented } from 'antd';
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
  schedules: TeamSchedule[];
  summaries: DailySummary[];
  // 요약(summaries) fetch 가 한 번이라도 완료됐는지. false 면 셀에 요약 배지를 그리지 않는다.
  summariesReady: boolean;
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
  schedules,
  summaries,
  summariesReady,
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
      // 월간/목록 모두 currentDate 의 월 단위로 동작 — 탭 전환 시 조회 범위(from/to)가 동일해
      // 재fetch 없이 같은 데이터를 공유한다.
      calendarRef.current?.getApi().changeView(view);
      const calDate = currentDate.format('YYYY-MM-DD');
      calendarRef.current?.getApi().gotoDate(calDate);
    },
    [currentDate, onViewTypeChange],
  );

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
          <DaySummaryBanner summary={summary} ready={summariesReady} />
        </div>
      );
    },
    [summaryMap, summariesReady, onDateClick],
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
        flex: 1,
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
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
          {/* 월간/목록 공통 월 네비게이션 — 탭 전환 시에도 동일 헤더 유지 */}
          <Button icon={<LeftOutlined />} size="small" onClick={handlePrev} />
          <span style={{ fontSize: 18, fontWeight: 600, minWidth: 140, textAlign: 'center' }}>
            {currentDate.year()}년 {currentDate.month() + 1}월
          </span>
          <Button icon={<RightOutlined />} size="small" onClick={handleNext} />
          <Button size="small" onClick={handleToday} style={{ marginLeft: 8 }}>
            오늘
          </Button>
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

      {/*
        FullCalendar daygrid 셀 상단 영역은 기본적으로 day-number 만 우측 정렬되는 inline-flex 라
        dayCellContent 에 넣은 요약 칩이 셀 전폭을 차지하지 못한다. SF 정합 (셀 전체 폭 chip) 을 위해
        .fc-daygrid-day-top 의 레이아웃을 column + stretch 로 override + 자식 div 도 width 100%.
      */}
      <style>{`
        .fc-daygrid-day-top {
          display: flex !important;
          flex-direction: column !important;
          align-items: stretch !important;
          width: 100% !important;
        }
        .fc-daygrid-day-top > * {
          width: 100% !important;
        }
      `}</style>
      {/* FullCalendar — SF 레거시 정합: 캘린더 전체가 한 화면에 보이도록 height=100% 컨테이너 채움 + 셀별 +N 개 popover */}
      <div style={{ flex: 1, minHeight: 0 }}>
        <FullCalendar
          ref={calendarRef}
          plugins={[dayGridPlugin, listPlugin]}
          initialView={viewType}
          initialDate={currentDate.format('YYYY-MM-DD')}
          headerToolbar={false}
          locale="ko"
          allDayText="종일"
          noEventsText="일정이 없습니다"
          height={isListView ? 'auto' : '100%'}
          dayMaxEventRows={true}
          moreLinkText={(num) => `+${num} 개`}
          events={events}
          dayCellContent={renderDayCellContent}
          eventContent={renderEventContent}
          eventClick={handleEventClick}
          fixedWeekCount={false}
        />
      </div>
    </div>
  );
}
