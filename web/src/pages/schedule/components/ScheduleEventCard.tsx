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
    accountExternalKey,
    accountType,
    accountBranchName,
    isClockIn,
  } = schedule;

  const categories = [workingCategory1, workingCategory2, workingCategory3]
    .filter(Boolean)
    .join(' | ');

  const isWork = workingType === '근무';
  const isList = variant === 'list';
  const textColor = isList ? '#262626' : '#fff';
  // SF 레거시 정합 (FullCalendarComponentHelper.js L62-73) — 근무 이벤트만 출근 여부에 따라
  // ✅ (clock-in) / ❌ (no clock-in) emoji prefix. 연차/대휴 는 prefix 없음.
  const statusEmoji = isWork ? (isClockIn ? '✅' : '❌') : '';

  // SF 레거시 거래처 표기 (FullCalendarComponentController.fetchAllShcedule L90):
  // {accountName}({externalKey},{accountType},{branchName}) — 비어있는 부가정보는 괄호에서 생략.
  const accountText = (() => {
    if (!accountName) return undefined;
    const detail = [accountExternalKey, accountType, accountBranchName].filter(Boolean).join(',');
    return detail ? `${accountName}(${detail})` : accountName;
  })();

  // SF title 조립 (cls L88-96):
  // - 근무: 이름(코드) | cat1 | cat2 | cat3 | 근무 | 거래처(...)
  // - 비근무(연차/대휴): 이름(코드) | workingType
  const segments = isWork
    ? [`${employeeName}(${employeeCode})`, categories || undefined, workingType, accountText]
    : [`${employeeName}(${employeeCode})`, workingType];
  const lineText = segments.filter(Boolean).join(' | ');
  const fullText = statusEmoji ? `${statusEmoji} ${lineText}` : lineText;

  // 월간/목록 모두 SF 정합으로 한 줄 표시. 월간은 흰 글씨 ellipsis, 목록은 검정 글씨 ellipsis.
  return (
    <div
      title={fullText}
      style={{
        fontSize: isList ? 13 : 11,
        lineHeight: '16px',
        padding: '1px 4px',
        color: textColor,
        overflow: 'hidden',
        whiteSpace: 'nowrap',
        textOverflow: 'ellipsis',
        cursor: isWork ? 'pointer' : 'default',
      }}
    >
      {fullText}
    </div>
  );
}
