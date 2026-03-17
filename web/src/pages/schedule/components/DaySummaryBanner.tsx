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

  const displayColor = displayActual >= displayExpected ? COLOR_MATCH : COLOR_MISMATCH;
  const promotionColor = promotionActual >= promotionExpected ? COLOR_MATCH : COLOR_MISMATCH;

  const hasDisplay = displayExpected > 0 || displayActual > 0;
  const hasPromotion = promotionExpected > 0 || promotionActual > 0;
  const hasLeave = annualLeave > 0;
  const hasCompLeave = compensatoryLeave > 0;

  if (!hasDisplay && !hasPromotion && !hasLeave && !hasCompLeave) return null;

  return (
    <div style={{ fontSize: 10, lineHeight: '14px', whiteSpace: 'nowrap' }}>
      {hasDisplay && (
        <div style={{ color: displayColor }}>
          진열: {displayActual}/{displayExpected}
        </div>
      )}
      {hasPromotion && (
        <div style={{ color: promotionColor }}>
          행사: {promotionActual}/{promotionExpected}
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
