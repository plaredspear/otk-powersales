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

export default function ScheduledJobsPage() {
  const [jobName, setJobName] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<ScheduledJobStatus | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const [selectedRun, setSelectedRun] = useState<ScheduledJobRun | null>(null);

  const from = range?.[0]?.toISOString() ?? undefined;
  const to = range?.[1]?.toISOString() ?? undefined;

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

  const jobNameOptions = useMemo(() => {
    const fromSummary = summaryQuery.data?.distinctJobNames ?? [];
    const fromCatalog = catalogQuery.data?.map((entry) => entry.jobName) ?? [];
    const merged = Array.from(new Set([...fromCatalog, ...fromSummary])).sort();
    return merged.map((name) => ({ label: name, value: name }));
  }, [summaryQuery.data, catalogQuery.data]);

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
        style={{ marginTop: 24 }}
        defaultActiveKey="runs"
        items={[
          {
            key: 'runs',
            label: '실행 이력',
            children: (
              <>
                <Space wrap style={{ marginBottom: 16 }}>
                  <Select
                    allowClear
                    placeholder="잡 이름"
                    style={{ width: 240 }}
                    value={jobName}
                    onChange={(value) => {
                      setJobName(value ?? undefined);
                      setPage(1);
                    }}
                    options={jobNameOptions}
                    showSearch
                    optionFilterProp="label"
                  />
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
                  columns={runColumns}
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
            ),
          },
          {
            key: 'catalog',
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
        ]}
      />

      <JobRunDetailModal
        run={selectedRun}
        open={selectedRun !== null}
        onClose={() => setSelectedRun(null)}
      />
    </div>
  );
}
