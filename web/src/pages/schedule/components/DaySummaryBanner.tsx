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
    compensatoryLeave,
  } = summary;

  const hasDisplay = displayExpected > 0 || displayActual > 0;
  const hasPromotion = promotionExpected > 0 || promotionActual > 0;
  const hasLeave = annualLeave > 0;
  const hasCompLeave = compensatoryLeave > 0;

  if (!hasDisplay && !hasPromotion && !hasLeave && !hasCompLeave) return null;

  // SF 레거시 정합 (FullCalendarComponentHelper.js L22-29) —
  // 진열/행사 한 행 결합 + 양쪽 모두 actual == expected (정확 일치) 일 때만 match (녹색).
  // 초과 달성 (actual > expected) 도 mismatch 처리 — SF 와 동일.
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
      {(hasDisplay || hasPromotion) && (
        <div style={{ ...chipStyle, background: workBg }}>
          진열: {displayActual}/{displayExpected} | 행사: {promotionActual}/{promotionExpected}
        </div>
      )}
      {hasLeave && (
        <div style={{ ...chipStyle, background: COLOR_LEAVE }}>연차 : {annualLeave}</div>
      )}
      {hasCompLeave && (
        <div style={{ ...chipStyle, background: COLOR_LEAVE }}>대휴 : {compensatoryLeave}</div>
      )}
    </div>
  );
}
