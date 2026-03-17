import type { TeamSchedule } from '@/api/team-schedule';

interface ScheduleEventCardProps {
  schedule: TeamSchedule;
}

export function ScheduleEventCard({ schedule }: ScheduleEventCardProps) {
  const {
    employeeName,
    empCode,
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

  return (
    <div
      style={{
        fontSize: 11,
        lineHeight: '15px',
        padding: '1px 4px',
        color: '#fff',
        overflow: 'hidden',
        cursor: isWork ? 'pointer' : 'default',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
        <span style={{ fontWeight: 600 }}>
          {employeeName}({empCode})
        </span>
        {isWork && (
          <span style={{ fontSize: 10 }}>
            {isClockIn ? (
              <span style={{ color: '#a0ffb0' }}>&#10003;</span>
            ) : (
              <span style={{ color: '#ffaaaa' }}>&#10007;</span>
            )}
          </span>
        )}
      </div>
      {categories && (
        <div style={{ fontSize: 10, opacity: 0.9 }}>{categories}</div>
      )}
      {accountName && (
        <div
          style={{
            fontSize: 10,
            opacity: 0.85,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {accountName}
        </div>
      )}
      {!isWork && (
        <div style={{ fontSize: 10, opacity: 0.9 }}>{workingType}</div>
      )}
    </div>
  );
}
