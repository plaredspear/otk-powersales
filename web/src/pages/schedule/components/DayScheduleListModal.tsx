import { Modal, Empty } from 'antd';
import dayjs from 'dayjs';
import 'dayjs/locale/ko';
import type { DailySummary, TeamSchedule } from '@/api/team-schedule';
import { DaySummaryBanner } from './DaySummaryBanner';
import { ScheduleEventCard } from './ScheduleEventCard';
import { getEventColor } from './scheduleEventColor';

interface DayScheduleListModalProps {
  open: boolean;
  onClose: () => void;
  date: string;
  schedules: TeamSchedule[];
  summary: DailySummary | undefined;
  onScheduleClick: (schedule: TeamSchedule) => void;
}

/**
 * "+N 개" / 날짜 셀 클릭 시 뜨는 해당일 전체 일정 목록 모달.
 *
 * FullCalendar 내장 popover 와 동일한 디자인(상단 요약 배너 + 색깔 칩 리스트)을 antd Modal 로
 * 재현한다. popover 는 z-index(9999)가 antd Modal 보다 높아 상세 모달을 가려, popover 대신
 * 이 모달로 통일했다(ScheduleCalendar.moreLinkClick). 근무 일정 클릭 시 상세 모달을 연다.
 */
export function DayScheduleListModal({
  open,
  onClose,
  date,
  schedules,
  summary,
  onScheduleClick,
}: DayScheduleListModalProps) {
  const title = date
    ? `${dayjs(date).locale('ko').format('YYYY년 M월 D일 (ddd)')} 일정`
    : '';

  const daySchedules = schedules.filter((s) => s.workingDate === date);

  return (
    <Modal
      title={title}
      open={open}
      onCancel={onClose}
      footer={null}
      width={800}
      // 일정 상세 모달(ScheduleEditModal, zIndex=1200)이 이 목록 위에서 열리므로
      // 목록 모달은 더 낮은 z-index 로 고정해 상세 모달이 항상 앞에 오도록 한다.
      // (명시하지 않으면 antd 가 나중에 열린 이 모달에 더 높은 자동 z-index 를 부여해
      //  상세 모달을 가려버린다.)
      zIndex={1050}
      destroyOnHidden
    >
      {/* 상단 요약 배너 — 캘린더 popover 와 동일하게 진열/행사 + 연차 칩 표시 */}
      <DaySummaryBanner summary={summary} ready />
      {daySchedules.length === 0 ? (
        <Empty description="등록된 일정이 없습니다" style={{ marginTop: 12 }} />
      ) : (
        <div style={{ marginTop: 4, maxHeight: '60vh', overflowY: 'auto' }}>
          {daySchedules.map((schedule) => {
            const color = getEventColor(schedule);
            const isWork = schedule.workingType === '근무';
            return (
              <div
                key={schedule.id}
                onClick={() => {
                  if (isWork) onScheduleClick(schedule);
                }}
                style={{
                  background: color,
                  borderRadius: 2,
                  marginBottom: 2,
                  cursor: isWork ? 'pointer' : 'default',
                }}
              >
                <ScheduleEventCard schedule={schedule} variant="month" showCommuteTime />
              </div>
            );
          })}
        </div>
      )}
    </Modal>
  );
}
