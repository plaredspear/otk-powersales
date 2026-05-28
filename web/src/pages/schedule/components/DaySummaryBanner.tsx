import type { DailySummary } from '@/api/team-schedule';

interface DaySummaryBannerProps {
  summary: DailySummary | undefined;
}

const COLOR_MATCH = '#069740';
const COLOR_MISMATCH = '#b2272d';

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

  // SF 레거시 정합 — 진열/행사 는 한 행에 ` | ` 로 결합 + 양쪽 비교 모두 일치할 때만 match 색.
  const workMatch = displayActual >= displayExpected && promotionActual >= promotionExpected;
  const workColor = workMatch ? COLOR_MATCH : COLOR_MISMATCH;

  return (
    <div style={{ fontSize: 10, lineHeight: '14px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
      {(hasDisplay || hasPromotion) && (
        <div style={{ color: workColor }}>
          진열: {displayActual}/{displayExpected} | 행사: {promotionActual}/{promotionExpected}
        </div>
      )}
      {hasLeave && (
        <div style={{ color: '#666' }}>연차: {annualLeave}</div>
      )}
      {hasCompLeave && (
        <div style={{ color: '#666' }}>대휴: {compensatoryLeave}</div>
      )}
    </div>
  );
}
