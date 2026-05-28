import type { TeamSchedule } from '@/api/team-schedule';

interface ScheduleEventCardProps {
  schedule: TeamSchedule;
  variant?: 'month' | 'list';
}

export function ScheduleEventCard({ schedule, variant = 'month' }: ScheduleEventCardProps) {
  const {
    employeeName,
    employeeCode,
    workingType,
    workingCategory1,
    workingCategory2,
    workingCategory3,
    accountName,
    isClockIn,
  } = schedule;

  const categories = [workingCategory1, workingCategory2, workingCategory3]
    .filter(Boolean)
    .join(' | ');

  const isWork = workingType === '근무';
  const isList = variant === 'list';
  const textColor = isList ? '#262626' : '#fff';
  const subTextOpacity = isList ? 0.75 : 0.9;
  // SF 레거시 정합 (FullCalendarComponentHelper.js L62-73) — 근무 이벤트만 출근 여부에 따라
  // ✅ (clock-in) / ❌ (no clock-in) emoji prefix. 연차/대휴 는 prefix 없음.
  const statusEmoji = isWork ? (isClockIn ? '✅' : '❌') : '';

  // 월간 뷰 (SF 정합): 한 줄에 status + 이름(코드) | 카테고리 | 거래처 ellipsis.
  // 목록 뷰: 정보 밀도 위해 기존 multi-line 유지.
  if (!isList) {
    const segments = [
      `${employeeName}(${employeeCode})`,
      categories || undefined,
      accountName || undefined,
      !isWork ? workingType : undefined,
    ].filter(Boolean) as string[];
    const lineText = statusEmoji ? `${statusEmoji} ${segments.join(' | ')}` : segments.join(' | ');
    return (
      <div
        title={lineText}
        style={{
          fontSize: 11,
          lineHeight: '16px',
          padding: '1px 4px',
          color: textColor,
          overflow: 'hidden',
          whiteSpace: 'nowrap',
          textOverflow: 'ellipsis',
          cursor: isWork ? 'pointer' : 'default',
        }}
      >
        {lineText}
      </div>
    );
  }

  return (
    <div
      style={{
        fontSize: 11,
        lineHeight: '15px',
        padding: '1px 4px',
        color: textColor,
        overflow: 'hidden',
        cursor: isWork ? 'pointer' : 'default',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
        {statusEmoji && <span style={{ fontSize: 11 }}>{statusEmoji}</span>}
        <span style={{ fontWeight: 600 }}>
          {employeeName}({employeeCode})
        </span>
      </div>
      {categories && (
        <div style={{ fontSize: 10, opacity: subTextOpacity }}>{categories}</div>
      )}
      {accountName && (
        <div
          style={{
            fontSize: 10,
            opacity: 0.7,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {accountName}
        </div>
      )}
      {!isWork && (
        <div style={{ fontSize: 10, opacity: subTextOpacity }}>{workingType}</div>
      )}
    </div>
  );
}
