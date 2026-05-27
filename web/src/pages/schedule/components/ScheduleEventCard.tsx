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
  const checkColor = isList ? '#52c41a' : '#a0ffb0';
  const crossColor = isList ? '#ff4d4f' : '#ffaaaa';

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
        <span style={{ fontWeight: 600 }}>
          {employeeName}({employeeCode})
        </span>
        {isWork && (
          <span style={{ fontSize: 10 }}>
            {isClockIn ? (
              <span style={{ color: checkColor }}>&#10003;</span>
            ) : (
              <span style={{ color: crossColor }}>&#10007;</span>
            )}
          </span>
        )}
      </div>
      {categories && (
        <div style={{ fontSize: 10, opacity: subTextOpacity }}>{categories}</div>
      )}
      {accountName && (
        <div
          style={{
            fontSize: 10,
            opacity: isList ? 0.7 : 0.85,
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
