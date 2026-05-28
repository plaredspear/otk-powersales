import type { DailySummary } from '@/api/team-schedule';

interface DaySummaryBannerProps {
  summary: DailySummary | undefined;
}

// SF 레거시 정합 (FullCalendarComponentHelper.js)
const COLOR_MATCH = '#069740';     // 진열/행사 양쪽 정확 일치 시 — 녹색 배경
const COLOR_MISMATCH = '#b2272d';  // 미달 또는 초과 (정확히 일치하지 않음) — 빨강 배경
const COLOR_LEAVE = '#9CAB98';     // 연차 — 회녹색 배경

export function DaySummaryBanner({ summary }: DaySummaryBannerProps) {
  if (!summary) return null;

  const {
    displayExpected,
    displayActual,
    promotionExpected,
    promotionActual,
    annualLeave,
  } = summary;

  // SF 레거시 정합 (FullCalendarComponentController.cls L183-186 + FullCalendarComponentHelper.js L32-47) —
  // backend 가 entry 를 생성한 시점에 진열/행사/연차/대휴 중 적어도 하나는 비어있지 않음.
  // SF 는 entry 가 있으면 (1) 진열/행사 칩 + (2) 연차 칩 (값 0 포함) 을 무조건 push.
  // 대휴 칩은 SF 가 주석 처리하여 미노출.
  const workMatch = displayActual === displayExpected && promotionActual === promotionExpected;
  const workBg = workMatch ? COLOR_MATCH : COLOR_MISMATCH;
  const chipStyle = {
    display: 'block',
    width: '100%',
    boxSizing: 'border-box',
    color: '#fff',
    padding: '1px 4px',
    borderRadius: 2,
    marginBottom: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  } as const;

  return (
    <div style={{ display: 'block', width: '100%', fontSize: 10, lineHeight: '14px', whiteSpace: 'nowrap' }}>
      <div style={{ ...chipStyle, background: workBg }}>
        진열: {displayActual}/{displayExpected} | 행사: {promotionActual}/{promotionExpected}
      </div>
      <div style={{ ...chipStyle, background: COLOR_LEAVE }}>연차 : {annualLeave}</div>
    </div>
  );
}
