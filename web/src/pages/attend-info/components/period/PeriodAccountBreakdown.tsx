import { Alert, Button, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useWorkHistoryEmployeeAccounts } from '@/hooks/attend-info/useAttendInfo';
import type { WorkHistoryAccountStat } from '@/api/attendInfo';
import { MEMBER_STATUS_COLOR, type TeamMember } from '@/api/team-schedule';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';

const { Text } = Typography;

interface Props {
  member: TeamMember;
  /** 시작년월 (yyyy-MM) */
  fromYearMonth: string;
  /** 종료년월 (yyyy-MM) */
  toYearMonth: string;
  /** 기간 입력 오류 (역순/최대 초과) — true 면 조회하지 않음 */
  rangeInvalid: boolean;
  /** 여사원 선택 해제 → 전체 집계 목록으로 복귀 */
  onClear: () => void;
}

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

function numericColumn(
  title: string,
  dataIndex: keyof WorkHistoryAccountStat,
  width: number,
): ColumnsType<WorkHistoryAccountStat>[number] {
  return {
    title,
    dataIndex,
    width,
    align: 'right',
    render: (v: number) => formatNumber(v),
  };
}

const columns: ColumnsType<WorkHistoryAccountStat> = [
  {
    title: '거래처명',
    dataIndex: 'accountName',
    width: 220,
    ellipsis: true,
    render: (v: string | null) => v ?? <Text type="secondary">(거래처 미지정)</Text>,
  },
  {
    title: '거래처코드',
    dataIndex: 'accountExternalKey',
    width: 120,
    ellipsis: true,
    render: (v: string | null) => v ?? '-',
  },
  numericColumn('총 근무일수', 'totalWorkingDays', 110),
  numericColumn('진열', 'displayDays', 80),
  numericColumn('행사', 'eventDays', 80),
  numericColumn('근무', 'workDays', 80),
  numericColumn('연차', 'annualLeaveDays', 80),
  numericColumn('대휴', 'altHolidayDays', 80),
];

/**
 * 기간별 근무기간 — 선택 여사원의 거래처별 근무 집계 뷰.
 *
 * 좌측 패널에서 여사원을 선택하면 선택한 기간(시작~종료 년월) 내 근무 행을 거래처 단위로
 * 집계해 표시한다. 기간 입력이 유효하지 않으면 조회하지 않는다 (상단 경고가 안내).
 */
export default function PeriodAccountBreakdown({
  member,
  fromYearMonth,
  toYearMonth,
  rangeInvalid,
  onClear,
}: Props) {
  const { data, isLoading, isError, error } = useWorkHistoryEmployeeAccounts(
    rangeInvalid
      ? null
      : { employeeCode: member.employeeCode, fromYearMonth, toYearMonth },
  );

  return (
    <div>
      {/* 월별 근무내역(개인) 탭과 동일한 선택 여사원 강조 박스 + 목록 복귀 버튼 */}
      <div
        style={{
          marginBottom: 12,
          padding: '8px 12px',
          borderLeft: '3px solid #1677ff',
          borderRadius: 4,
          background: '#f0f7ff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <Text strong style={{ fontSize: 15 }}>
            {[member.orgName, `${member.name}(${member.employeeCode})`, member.jikwee]
              .filter(Boolean)
              .join(' · ')}
          </Text>
          {member.status && (
            <Tag color={MEMBER_STATUS_COLOR[member.status] ?? 'default'} style={{ marginLeft: 8 }}>
              {member.status}
            </Tag>
          )}
          <Text type="secondary" style={{ marginLeft: 8 }}>
            {fromYearMonth} ~ {toYearMonth} 거래처별 근무내역
          </Text>
        </div>
        <Button size="small" onClick={onClear}>
          전체 목록으로
        </Button>
      </div>

      {isError && (
        <Alert
          type="error"
          message="거래처별 근무내역 조회 실패"
          description={error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다'}
          style={{ marginBottom: 16 }}
        />
      )}

      {data && (
        <Text style={{ marginBottom: 8, display: 'block' }}>
          총 {formatNumber(data.totalCount)}개 거래처
        </Text>
      )}
      <ResizableTable
        rowKey={(r: WorkHistoryAccountStat) => `${r.accountExternalKey ?? ''}|${r.accountName ?? ''}`}
        columns={columns}
        dataSource={data?.items ?? []}
        loading={isLoading}
        pagination={false}
        sticky
        scroll={{ x: 'max-content' }}
        locale={listTableLocale({
          searched: !rangeInvalid,
          emptyText: '선택한 기간에 근무내역이 없습니다.',
        })}
      />
    </div>
  );
}
