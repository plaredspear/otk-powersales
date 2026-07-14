import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Button, Card, DatePicker, Space, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import { useScheduledJobDailyStatus } from '@/hooks/admin/useScheduledJobs';
import type {
  ScheduledJobDailyStatusItem,
  ScheduledJobStatus,
} from '@/api/admin/scheduledJob';

const { Text } = Typography;

const STATUS_TAG_COLOR: Record<ScheduledJobStatus, string> = {
  SUCCESS: 'green',
  FAILURE: 'red',
  RUNNING: 'blue',
  SKIPPED: 'default',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('MM-DD HH:mm:ss');
}

/**
 * 실행여부 셀 — 예상 횟수 대비 실제 실행을 한눈에 판정한다.
 *
 * - 비활성(enabled=false): 회색 "비활성" (자동 실행 대상 아님).
 * - 예상 0회: "해당없음" (예: ORORA 월매출을 3일이 아닌 날 조회).
 * - 실제 >= 예상: 초록 "정상".
 * - 실제 0회 & 예상 >= 1: 빨강 "미실행".
 * - 그 외(실제 < 예상, 일부 실행): 주황 "부분실행".
 */
function ExecutionCell({ item }: { item: ScheduledJobDailyStatusItem }) {
  if (!item.enabled) {
    return <Tag color="default">비활성</Tag>;
  }
  const expected = item.expectedCount;
  if (expected === 0) {
    return (
      <Tooltip title="이 시간 윈도우에는 예정된 실행이 없습니다.">
        <Tag color="default">해당없음</Tag>
      </Tooltip>
    );
  }
  if (expected == null) {
    // cron 파싱 실패/미등록 — 예상 횟수를 못 구한 상태.
    if (item.actualCount > 0) return <Tag color="green">실행됨</Tag>;
    // 활성인데 실행도 예상횟수도 없는 잡은 "조용한 실패" 로 감춰지지 않도록 주의 색으로 노출.
    return (
      <Tooltip title="예상 실행 횟수를 계산하지 못했고(cron 미해석/미등록) 실제 실행 이력도 없습니다.">
        <Tag color="orange">확인필요</Tag>
      </Tooltip>
    );
  }
  if (item.actualCount >= expected) {
    return <Tag color="green">정상</Tag>;
  }
  if (item.actualCount === 0) {
    return <Tag color="red">미실행</Tag>;
  }
  return <Tag color="orange">부분실행</Tag>;
}

/**
 * 개발자 도구 - 대시보드 상단 "일별 스케줄 실행현황" 카드.
 *
 * 하루 단위지만 시간 윈도우는 `(선택일 - 1일) 22:00 ~ 선택일 22:00` (KST). 날짜를 바꿔가며
 * 과거·미래 어느 날짜든 조회할 수 있다 (당일 22시~자정 사이 조회 대응). 각 스케줄의 예상 발화
 * 횟수(실제 적용 cron 기준) 대비 실제 실행 횟수를 함께 보여 준다. `VIEW_ALL_DATA` 권한 필요.
 */
export default function DailyScheduleStatusCard() {
  const [date, setDate] = useState<Dayjs>(() => dayjs());
  const dateStr = date.format('YYYY-MM-DD');
  const { data, isLoading, isError, isFetching, refetch } = useScheduledJobDailyStatus(dateStr);

  const columns: ColumnsType<ScheduledJobDailyStatusItem> = [
    {
      title: '스케줄',
      dataIndex: 'label',
      key: 'label',
      width: 200,
      render: (label: string, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{label}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.scheduleText}
          </Text>
        </Space>
      ),
    },
    {
      title: '실행여부',
      key: 'execution',
      width: 110,
      align: 'center',
      render: (_: unknown, record) => <ExecutionCell item={record} />,
    },
    {
      title: '실행 횟수',
      key: 'count',
      width: 130,
      align: 'center',
      render: (_: unknown, record) => {
        // "실제 / 예상" 형태. 예상이 없으면 실제만.
        const expectedText = record.expectedCount == null ? '-' : String(record.expectedCount);
        return (
          <Space size={4}>
            <Text strong>{record.actualCount}</Text>
            <Text type="secondary">/ {expectedText}</Text>
          </Space>
        );
      },
    },
    {
      title: '성공',
      dataIndex: 'successCount',
      key: 'successCount',
      width: 70,
      align: 'center',
      render: (v: number) => (v > 0 ? <Text type="success">{v}</Text> : <Text type="secondary">0</Text>),
    },
    {
      title: '실패',
      dataIndex: 'failureCount',
      key: 'failureCount',
      width: 70,
      align: 'center',
      render: (v: number) => (v > 0 ? <Text type="danger">{v}</Text> : <Text type="secondary">0</Text>),
    },
    {
      title: '스킵',
      dataIndex: 'skippedCount',
      key: 'skippedCount',
      width: 70,
      align: 'center',
      render: (v: number) => (v > 0 ? <Text type="warning">{v}</Text> : <Text type="secondary">0</Text>),
    },
    {
      title: '마지막 실행',
      key: 'last',
      width: 180,
      render: (_: unknown, record) =>
        record.lastStartedAt ? (
          <Space size={6}>
            <Text>{formatDateTime(record.lastStartedAt)}</Text>
            {record.lastStatus && (
              <Tag color={STATUS_TAG_COLOR[record.lastStatus]} style={{ marginInlineEnd: 0 }}>
                {record.lastStatus}
              </Tag>
            )}
          </Space>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
    {
      title: '비고',
      dataIndex: 'note',
      key: 'note',
      render: (note: string | null) => (note ? <Text type="secondary" style={{ fontSize: 12 }}>{note}</Text> : null),
    },
  ];

  const windowText = data
    ? `${dayjs(data.windowFrom).format('MM-DD HH:mm')} ~ ${dayjs(data.windowTo).format('MM-DD HH:mm')}`
    : '-';

  return (
    <Card
      size="small"
      style={{ marginBottom: 24 }}
      title="일별 스케줄 실행현황"
      extra={
        <Link to="/admin/tools/scheduled-jobs">스케줄 잡 상세 →</Link>
      }
    >
      <Space wrap align="center" style={{ marginBottom: 12 }}>
        <Text type="secondary">기준일</Text>
        <DatePicker
          value={date}
          onChange={(value) => value && setDate(value)}
          allowClear={false}
        />
        <Button onClick={() => refetch()} loading={isFetching}>
          새로고침
        </Button>
        <Text type="secondary" style={{ fontSize: 12 }}>
          조회 윈도우: {windowText}
        </Text>
      </Space>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="시간 윈도우 안내"
        description={
          <>
            일별 현황이지만 시간 윈도우는 <Text strong>기준일 전날 22시 ~ 기준일 22시</Text>(KST)
            입니다. 예: 7/15 조회 시 7/14 22:00 ~ 7/15 22:00 구간을 집계합니다. 미래 날짜도 조회
            가능합니다(당일 22시 이후 조회 대응). "실행 횟수"는 <Text strong>실제 / 예상</Text>이며,
            예상 횟수는 실제 적용된 cron 기준으로 계산됩니다. ORORA 월매출은 매월 3일에만 실행되어
            그 외 날짜에는 예상 0회로 표기됩니다.
          </>
        }
      />

      {isError && (
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message="일별 스케줄 실행현황 조회에 실패했습니다. 시스템 관리자 권한이 필요합니다."
        />
      )}

      <ResizableTable<ScheduledJobDailyStatusItem>
        rowKey="jobName"
        columns={columns}
        dataSource={data?.items ?? []}
        loading={isLoading}
        pagination={false}
        locale={listTableLocale()}
      />
    </Card>
  );
}
