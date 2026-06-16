import type { TeamSchedule } from '@/api/team-schedule';

/**
 * 일정 이벤트 칩의 배경/테두리 색을 결정한다.
 *
 * SF 레거시(FullCalendarComponentHelper.js) 정합 — 연차/대휴는 진회색, 행사는 분홍,
 * 진열(및 그 외)은 파랑. 캘린더 셀 이벤트, "+N 개" 일정 목록 모달이 동일 색을 공유한다.
 */
export function getEventColor(schedule: TeamSchedule): string {
  if (schedule.workingType === '연차' || schedule.workingType === '대휴') return '#495E62';
  if (schedule.workingCategory1 === '행사') return '#F392BC';
  if (schedule.workingCategory1 === '진열') return '#4E8BBF';
  return '#4E8BBF';
}
