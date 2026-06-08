import { useMemo, useState } from 'react';
import {
  Card,
  Col,
  DatePicker,
  Row,
  Select,
  Space,
  Statistic,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useScheduledJobCatalog,
  useScheduledJobRuns,
  useScheduledJobSummary,
} from '@/hooks/admin/useScheduledJobs';
import type {
  RegisteredScheduledJob,
  ScheduledJobRun,
  ScheduledJobStatus,
} from '@/api/admin/scheduledJob';
import JobRunDetailModal from './JobRunDetailModal';
import ResizableTable from '@/components/common/ResizableTable';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const STATUS_TAG_COLOR: Record<ScheduledJobStatus, string> = {
  SUCCESS: 'green',
  FAILURE: 'red',
  RUNNING: 'blue',
};

const STATUS_OPTIONS: { label: string; value: ScheduledJobStatus }[] = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'FAILURE', value: 'FAILURE' },
  { label: 'RUNNING', value: 'RUNNING' },
];

/** 잡 이름 → 탭에 표시할 10자 이내 한글 라벨. 미매핑 잡은 원본 jobName 으로 폴백. */
const JOB_LABELS: Record<string, string> = {
  'agreement-word-cycle-batch': '약관단어 리셋',
  'attendance-sap-batch': '근태 SAP전송',
  'display-master-sap-batch': '진열 SAP전송',
  'display-master-last-month-revenue-batch': '진열 전월매출',
  'mfeis-this-month-revenue-batch': '일정 전월매출',
  'account-naver-geocode-batch': '거래처 좌표변환',
  'pptMaster.expire': '행사조 만료',
  'pptMaster.syncValid': '행사조 sync',
  'sap.processPostponedAppointments': '연기예약 처리',
  'salesProgressRateMaster.sync': '목표마스터 sync',
  'sap-outbox-worker': 'SAP outbox',
  'scheduledJobRun.cleanup': '이력 정리',
};

function jobLabel(jobName: string): string {
  return JOB_LABELS[jobName] ?? jobName;
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return '-';
  if (ms < 1000) return `${ms}ms`;
  const seconds = ms / 1000;
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  const minutes = Math.floor(seconds / 60);
  const remainSec = Math.round(seconds - minutes * 60);
  return `${minutes}m ${remainSec}s`;
}

const ALL_JOBS_KEY = '__all__';
const CATALOG_KEY = '__catalog__';

export default function ScheduledJobsPage() {
  const [activeTab, setActiveTab] = useState<string>(ALL_JOBS_KEY);
  const [status, setStatus] = useState<ScheduledJobStatus | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [selectedRun, setSelectedRun] = useState<ScheduledJobRun | null>(null);

  const from = range?.[0]?.toISOString() ?? undefined;
  const to = range?.[1]?.toISOString() ?? undefined;

  // 잡 이름별 탭: '전체' 탭이면 jobName 필터 없음, 잡 탭이면 해당 잡 이름으로 필터.
  const jobName =
    activeTab === ALL_JOBS_KEY || activeTab === CATALOG_KEY ? undefined : activeTab;

  const summaryQuery = useScheduledJobSummary(24);
  const catalogQuery = useScheduledJobCatalog();
  const runsQuery = useScheduledJobRuns({
    jobName,
    status,
    from,
    to,
    page,
    size,
  });

  // 등록된 작업(catalog) 기준으로 잡 이름 탭 목록 구성.
  const jobNames = useMemo(
    () => (catalogQuery.data ?? []).map((entry) => entry.jobName),
    [catalogQuery.data],
  );

  const runColumns: ColumnsType<ScheduledJobRun> = [
    {
      title: '시작 시각',
      dataIndex: 'startedAt',
      key: 'startedAt',
      width: 180,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '잡 이름',
      dataIndex: 'jobName',
      key: 'jobName',
      width: 220,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: ScheduledJobStatus) => (
        <Tag color={STATUS_TAG_COLOR[value]}>{value}</Tag>
      ),
    },
    {
      title: '소요시간',
      dataIndex: 'durationMs',
      key: 'durationMs',
      width: 100,
      render: (value: number | null) => formatDuration(value),
    },
    {
      title: '오류 메시지',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      ellipsis: true,
      render: (value: string | null) => value || '-',
    },
  ];

  const catalogColumns: ColumnsType<RegisteredScheduledJob> = [
    { title: '잡 이름', dataIndex: 'jobName', key: 'jobName', width: 240 },
    { title: 'cron 표현식', dataIndex: 'cron', key: 'cron', width: 280 },
    { title: '설명', dataIndex: 'description', key: 'description' },
  ];

  // 잡 탭에서는 '잡 이름' 컬럼이 중복이므로 제외하고, '전체' 탭에서만 노출.
  const visibleRunColumns =
    jobName === undefined
      ? runColumns
      : runColumns.filter((col) => col.key !== 'jobName');

  const runsHistoryNode = (
    <>
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="상태"
          style={{ width: 140 }}
          value={status}
          onChange={(value) => {
            setStatus(value ?? undefined);
            setPage(1);
          }}
          options={STATUS_OPTIONS}
        />
        <RangePicker
          showTime
          value={range as [Dayjs, Dayjs] | null}
          onChange={(value) => {
            setRange(value as [Dayjs | null, Dayjs | null] | null);
            setPage(1);
          }}
        />
      </Space>
      <ResizableTable<ScheduledJobRun>
        rowKey="id"
        loading={runsQuery.isLoading}
        dataSource={runsQuery.data?.items ?? []}
        columns={visibleRunColumns}
        pagination={{
          current: page,
          pageSize: size,
          total: runsQuery.data?.totalCount ?? 0,
          onChange: setPage,
          showSizeChanger: false,
        }}
        onRow={(record) => ({
          onClick: () => setSelectedRun(record),
          style: { cursor: 'pointer' },
        })}
      />
    </>
  );

  const tabItems = [
    {
      key: ALL_JOBS_KEY,
      label: '전체',
      children: runsHistoryNode,
    },
    ...jobNames.map((name) => ({
      key: name,
      label: <span title={name}>{jobLabel(name)}</span>,
      children: runsHistoryNode,
    })),
    {
      key: CATALOG_KEY,
      label: '등록된 작업',
      children: (
        <ResizableTable<RegisteredScheduledJob>
          rowKey="jobName"
          loading={catalogQuery.isLoading}
          dataSource={catalogQuery.data ?? []}
          columns={catalogColumns}
          pagination={false}
        />
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>스케줄 잡 실행 이력</Title>
      <Text type="secondary">
        backend `@Scheduled` 배치의 실행 상태를 조회합니다. 요약 카드는 최근 24시간 윈도우 기준입니다.
      </Text>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="총 실행"
              value={summaryQuery.data?.totalCount ?? 0}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="SUCCESS"
              value={summaryQuery.data?.successCount ?? 0}
              valueStyle={{ color: '#52c41a' }}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="FAILURE"
              value={summaryQuery.data?.failureCount ?? 0}
              valueStyle={{ color: '#ff4d4f' }}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="RUNNING"
              value={summaryQuery.data?.runningCount ?? 0}
              valueStyle={{ color: '#1677ff' }}
              loading={summaryQuery.isLoading}
            />
          </Card>
        </Col>
      </Row>
      {summaryQuery.data && (
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          윈도우: {formatDateTime(summaryQuery.data.windowFrom)} ~ {formatDateTime(summaryQuery.data.windowTo)}
        </Text>
      )}

      <Tabs
        tabPosition="left"
        style={{ marginTop: 24 }}
        activeKey={activeTab}
        onChange={(key) => {
          setActiveTab(key);
          setPage(1);
        }}
        items={tabItems}
      />

      <JobRunDetailModal
        run={selectedRun}
        open={selectedRun !== null}
        onClose={() => setSelectedRun(null)}
      />
    </div>
  );
}
