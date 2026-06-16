import { useCallback, useMemo, useRef } from 'react';
import { Button, Segmented } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import type { DayCellContentArg, EventContentArg, EventInput, MoreLinkArg } from '@fullcalendar/core';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import type { DailySummary, TeamSchedule } from '@/api/team-schedule';
import { DaySummaryBanner } from './DaySummaryBanner';
import { ScheduleEventCard } from './ScheduleEventCard';
import { getEventColor } from './scheduleEventColor';

export type CalendarView = 'dayGridMonth' | 'listMonth';

// SF 레거시 정합 (FullCalendarComponentHelper.js) — 목록 뷰 요약 칩 색상
const SUMMARY_COLOR_MATCH = '#069740'; // 진열/행사 양쪽 정확 일치 — 녹색
const SUMMARY_COLOR_MISMATCH = '#b2272d'; // 미달/초과 — 빨강
const SUMMARY_COLOR_LEAVE = '#9CAB98'; // 연차 — 회녹색

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

  const isListView = viewType === 'listMonth';

  const events: EventInput[] = useMemo(() => {
    const scheduleEvents: EventInput[] = schedules.map((s) => {
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
    });

    // 목록(listMonth) 뷰에서는 SF 레거시(FullCalendarComponentHelper.js drawSchedule) 정합으로
    // 일별 요약을 종일 이벤트 2건(진열/행사 칩 + 연차 칩)으로 추가한다. 월간 뷰는 셀 배지
    // (dayCellContent) 가 동일 정보를 그리므로 요약 이벤트를 넣지 않아 중복을 피한다.
    if (!isListView) return scheduleEvents;

    const summaryEvents: EventInput[] = [];
    summaries.forEach((s) => {
      const workMatch =
        s.displayActual === s.displayExpected && s.promotionActual === s.promotionExpected;
      const workColor = workMatch ? SUMMARY_COLOR_MATCH : SUMMARY_COLOR_MISMATCH;
      summaryEvents.push({
        id: `summary-work-${s.date}`,
        title: `진열: ${s.displayActual}/${s.displayExpected} | 행사: ${s.promotionActual}/${s.promotionExpected}`,
        date: s.date,
        allDay: true,
        backgroundColor: workColor,
        borderColor: workColor,
        textColor: '#fff',
        extendedProps: { sortOrder: 1 },
      });
      summaryEvents.push({
        id: `summary-leave-${s.date}`,
        title: `연차 : ${s.annualLeave}`,
        date: s.date,
        allDay: true,
        backgroundColor: SUMMARY_COLOR_LEAVE,
        borderColor: SUMMARY_COLOR_LEAVE,
        textColor: '#fff',
        extendedProps: { sortOrder: 2 },
      });
    });
    // 일반 일정은 요약(1,2) 뒤에 오도록 sortOrder=3 (SF order 1,2 → 3 정합). eventOrder 로 정렬.
    scheduleEvents.forEach((e) => {
      e.extendedProps = { ...(e.extendedProps ?? {}), sortOrder: 3 };
    });
    return [...summaryEvents, ...scheduleEvents];
  }, [schedules, summaries, isListView]);

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
      // 요약 이벤트(summary-*)는 scheduleMap 에 없음 — title("진열: a/b | 행사: c/d", "연차 : n")을
      // 직접 렌더한다. (undefined 반환 시 list 뷰에서 title 셀이 비어 수치가 사라짐)
      if (!schedule) {
        return <span style={{ fontSize: 13 }}>{arg.event.title}</span>;
      }
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

  // "+N 개" 링크 클릭 시 FullCalendar 기본 popover(z-index 9999) 대신 날짜 클릭과 동일하게
  // DayScheduleListModal 을 띄운다. popover 는 z-index 가 antd Modal 보다 높아, popover 안에서
  // 일정 상세 모달을 열면 상세가 popover 뒤로 가려지는 문제가 있었다.
  // FullCalendar 내부 분기(MoreLinkContainer.handleClick)는
  //   if (!moreLinkClick || moreLinkClick === 'popover') { popover 열기 }
  //   else if (typeof moreLinkClick === 'string')        { zoomTo(date, moreLinkClick) }
  // 빈 문자열('')은 falsy 라 `!moreLinkClick` 이 참이 되어 popover 가 그대로 열린다(모달 2개).
  // 등록되지 않은 view 이름('none')을 반환하면 zoomTo 가 spec 을 못 찾아 같은 달 내 날짜만
  // 이동(화면 변화 없음)하고 popover 는 열지 않는다 — FullCalendar 6.x 에서 popover 차단 패턴.
  const handleMoreLinkClick = useCallback(
    (arg: MoreLinkArg) => {
      onDateClick(dayjs(arg.date).format('YYYY-MM-DD'));
      return 'none';
    },
    [onDateClick],
  );

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
        /*
          화면(브라우저 높이)이 작아질 때 셀 행 높이가 (날짜 + 요약 배지 2줄 + 이벤트 +
          "+N 개" 링크) 콘텐츠보다 작아지면, 셀 내용이 셀 경계를 넘어 다음 주 행의 날짜와
          겹쳤다. 해결: 셀마다 콘텐츠를 다 담을 최소 높이를 보장한다 (overflow:hidden 으로
          개별 셀을 자르거나 셀별 스크롤을 만들지 않는다). 컨테이너가 (6주 × 최소높이) 보다
          작아지면 부모 div(아래 overflowY:auto)의 세로 스크롤로 캘린더 전체가 스크롤된다.
          화면이 충분히 크면 FullCalendar 가 셀을 균등 분배해 한 화면에 다 보인다.
          월간 뷰(.fc-daygrid) 한정 — 목록 뷰(.fc-list)는 이 셀렉터에 매칭되지 않아 무관.
        */
        .fc-daygrid-body .fc-daygrid-day {
          height: auto !important;
        }
        .fc-scrollgrid-sync-table .fc-daygrid-day-frame {
          min-height: 96px !important;
        }
      `}</style>
      {/* FullCalendar — 셀별 +N 개 popover. 컨테이너 높이가 충분하면 한 화면에 다 보이고,
          화면이 작아 (6주 × 셀 최소높이) 를 넘기면 부모 컨테이너에 세로 스크롤을 부여해
          셀 내용이 다음 주 행과 겹치지 않게 한다. 목록 뷰도 동일하게 세로 스크롤. */}
      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto' }}>
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
          dayMaxEventRows={3}
          moreLinkText={(num) => `+${num} 개`}
          moreLinkClick={handleMoreLinkClick}
          events={events}
          eventOrder="sortOrder"
          dayCellContent={renderDayCellContent}
          eventContent={renderEventContent}
          eventClick={handleEventClick}
          fixedWeekCount={false}
        />
      </div>
    </div>
  );
}
