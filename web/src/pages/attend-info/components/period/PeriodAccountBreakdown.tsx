import { Alert, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useWorkHistoryEmployeeAccounts } from '@/hooks/attend-info/useAttendInfo';
import type { WorkHistoryAccountMonthlyStat, WorkHistoryAccountStat } from '@/api/attendInfo';
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
}

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

/** 환산 계열(BigDecimal 문자열/숫자) → 소수 표시. 값 없으면 '-'. */
function formatDecimal(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '-';
  const num = typeof value === 'string' ? Number(value) : value;
  if (Number.isNaN(num)) return '-';
  return num.toLocaleString('ko-KR', { minimumFractionDigits: 0, maximumFractionDigits: 4 });
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
  {
    title: '거래처 지점명',
    dataIndex: 'accountBranchName',
    width: 120,
    ellipsis: true,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '유통형태',
    dataIndex: 'distributionChannelLabel',
    width: 120,
    ellipsis: true,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '거래처유형',
    dataIndex: 'abcTypeLabel',
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
  {
    title: '총 투입횟수',
    dataIndex: 'totalInputCount',
    width: 110,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '총 환산근무일수',
    dataIndex: 'equivalentWorkingDays',
    width: 130,
    align: 'right',
    render: (v: string | number) => formatDecimal(v),
  },
];

/** 월별 분해(펼침) 컬럼 — 환산인원 + 근무형태 대표값은 월 단위로만 정의되어 여기에만 표시. */
const monthlyColumns: ColumnsType<WorkHistoryAccountMonthlyStat> = [
  { title: '년월', dataIndex: 'yearMonth', width: 100 },
  {
    title: '근무일수',
    dataIndex: 'totalWorkingDays',
    width: 90,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '투입횟수',
    dataIndex: 'totalInputCount',
    width: 90,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '환산근무일수',
    dataIndex: 'equivalentWorkingDays',
    width: 120,
    align: 'right',
    render: (v: string | number) => formatDecimal(v),
  },
  {
    title: '환산인원',
    dataIndex: 'convertedHeadcount',
    width: 100,
    align: 'right',
    render: (v: string | number) => formatDecimal(v),
  },
  { title: '근무형태1', dataIndex: 'workingCategory1', width: 100, render: (v: string | null) => v ?? '-' },
  { title: '근무형태3', dataIndex: 'workingCategory3', width: 100, render: (v: string | null) => v ?? '-' },
  { title: '근무형태4', dataIndex: 'workingCategory4', width: 100, render: (v: string | null) => v ?? '-' },
  { title: '근무형태5', dataIndex: 'workingCategory5', width: 100, render: (v: string | null) => v ?? '-' },
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
}: Props) {
  const { data, isLoading, isError, error } = useWorkHistoryEmployeeAccounts(
    rangeInvalid
      ? null
      : { employeeCode: member.employeeCode, fromYearMonth, toYearMonth },
  );

  return (
    <div>
      {/* 월별 근무내역(개인) 탭과 동일한 선택 여사원 강조 박스 */}
      <div
        style={{
          marginBottom: 12,
          padding: '8px 12px',
          borderLeft: '3px solid #1677ff',
          borderRadius: 4,
          background: '#f0f7ff',
        }}
      >
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
        expandable={{
          // 월별 분해(환산인원·근무형태)는 다월 조회일 때만 채워진다. 있는 행만 펼침 가능.
          rowExpandable: (r: WorkHistoryAccountStat) => r.monthlyStats.length > 0,
          expandedRowRender: (r: WorkHistoryAccountStat) => (
            <Table<WorkHistoryAccountMonthlyStat>
              rowKey={(m) => m.yearMonth}
              columns={monthlyColumns}
              dataSource={r.monthlyStats}
              pagination={false}
              size="small"
              scroll={{ x: 'max-content' }}
            />
          ),
        }}
        locale={listTableLocale({
          searched: !rangeInvalid,
          emptyText: '선택한 기간에 근무내역이 없습니다.',
        })}
      />
    </div>
  );
}
