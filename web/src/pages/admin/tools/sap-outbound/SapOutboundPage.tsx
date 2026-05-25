import { useState } from 'react';
import {
  Alert,
  Card,
  DatePicker,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useSapOutboundCatalog,
  useSapOutboundLogs,
  useSapOutboundOutboxPending,
} from '@/hooks/admin/useSapOutbound';
import type {
  OutboundTriggerType,
  SapOutboundCatalogItem,
  SapOutboundLogRow,
  SapOutboundResultCode,
  SapOutboxPendingRow,
} from '@/api/admin/sapIntegration';
import SapOutboundLogDetailModal from './SapOutboundLogDetailModal';
import SapOutboundTestTab from './SapOutboundTestTab';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const RESULT_CODE_OPTIONS: { label: string; value: SapOutboundResultCode }[] = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'FAIL', value: 'FAIL' },
  { label: 'INVALID_RESPONSE', value: 'INVALID_RESPONSE' },
];

const PAGE_SIZE_OPTIONS = [
  { label: '20', value: 20 },
  { label: '50', value: 50 },
  { label: '100', value: 100 },
];

const TRIGGER_TAG_COLOR: Record<OutboundTriggerType, string> = {
  BATCH: 'blue',
  REALTIME: 'green',
  OUTBOX: 'purple',
};

const RESULT_TAG_COLOR: Record<SapOutboundResultCode, string> = {
  SUCCESS: 'green',
  FAIL: 'red',
  INVALID_RESPONSE: 'orange',
};

const OUTBOX_STATUS_TAG_COLOR: Record<string, string> = {
  PENDING: 'blue',
  RETRY: 'orange',
};

const MAX_RETRY_COUNT = 5;

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function shortSenderClass(fqn: string): string {
  const segments = fqn.split('.');
  return segments[segments.length - 1] ?? fqn;
}

export default function SapOutboundPage() {
  const [interfaceId, setInterfaceId] = useState<string | undefined>(undefined);
  const [resultCode, setResultCode] = useState<SapOutboundResultCode | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [outboxPage, setOutboxPage] = useState(1);
  const [outboxSize] = useState(20);
  const [selectedRow, setSelectedRow] = useState<SapOutboundLogRow | null>(null);

  const catalogQuery = useSapOutboundCatalog();
  const logsQuery = useSapOutboundLogs({
    interfaceId,
    resultCode,
    from: range?.[0]?.toISOString() ?? undefined,
    to: range?.[1]?.toISOString() ?? undefined,
    page,
    size,
  });
  const outboxQuery = useSapOutboundOutboxPending(outboxPage, outboxSize);

  const interfaceOptions =
    catalogQuery.data?.map((item) => ({
      label: `${item.interfaceId} (${item.koreanName})`,
      value: item.interfaceId,
    })) ?? [];

  const catalogColumns: ColumnsType<SapOutboundCatalogItem> = [
    {
      title: 'Interface ID',
      dataIndex: 'interfaceId',
      key: 'interfaceId',
      width: 280,
      render: (value: string) => <code>{value}</code>,
    },
    { title: '한글명', dataIndex: 'koreanName', key: 'koreanName', width: 220 },
    {
      title: '트리거',
      dataIndex: 'triggerType',
      key: 'triggerType',
      width: 100,
      render: (value: OutboundTriggerType) => (
        <Tag color={TRIGGER_TAG_COLOR[value]}>{value}</Tag>
      ),
    },
    {
      title: 'Sender Class',
      dataIndex: 'senderClass',
      key: 'senderClass',
      width: 240,
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Tooltip title={value} placement="topLeft">
          <code>{shortSenderClass(value)}</code>
        </Tooltip>
      ),
    },
    {
      title: '설명',
      dataIndex: 'description',
      key: 'description',
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Tooltip title={value} placement="topLeft">
          {value}
        </Tooltip>
      ),
    },
  ];

  const logColumns: ColumnsType<SapOutboundLogRow> = [
    {
      title: '요청 시각',
      dataIndex: 'requestedAt',
      key: 'requestedAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'Interface',
      dataIndex: 'interfaceId',
      key: 'interfaceId',
      width: 240,
      render: (value: string) => <code>{value}</code>,
    },
    {
      title: 'Endpoint Path',
      dataIndex: 'endpointPath',
      key: 'endpointPath',
      width: 220,
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Tooltip title={value} placement="topLeft">
          <code>{value}</code>
        </Tooltip>
      ),
    },
    {
      title: 'Req',
      dataIndex: 'requestCount',
      key: 'requestCount',
      width: 70,
      align: 'right',
    },
    {
      title: 'Result',
      dataIndex: 'resultCode',
      key: 'resultCode',
      width: 140,
      render: (value: SapOutboundResultCode | null) =>
        value ? <Tag color={RESULT_TAG_COLOR[value] ?? 'default'}>{value}</Tag> : '-',
    },
    {
      title: 'HTTP',
      dataIndex: 'httpStatus',
      key: 'httpStatus',
      width: 80,
      align: 'right',
      render: (value: number | null) => value ?? '-',
    },
    {
      title: '시도',
      dataIndex: 'attemptCount',
      key: 'attemptCount',
      width: 70,
      align: 'right',
      render: (value: number) => (value > 1 ? <Text type="warning">{value}</Text> : value),
    },
    {
      title: '시간(ms)',
      dataIndex: 'durationMs',
      key: 'durationMs',
      width: 100,
      align: 'right',
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: '완료 시각',
      dataIndex: 'completedAt',
      key: 'completedAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'Msg',
      dataIndex: 'resultMsg',
      key: 'resultMsg',
      ellipsis: { showTitle: false },
      render: (value: string | null) =>
        value ? (
          <Tooltip title={value} placement="topLeft">
            {value}
          </Tooltip>
        ) : (
          '-'
        ),
    },
  ];

  const outboxColumns: ColumnsType<SapOutboxPendingRow> = [
    {
      title: '적재 시각',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: 'Domain Type',
      dataIndex: 'domainType',
      key: 'domainType',
      width: 220,
      render: (value: string) => <Tag>{value}</Tag>,
    },
    {
      title: 'Aggregate ID',
      dataIndex: 'aggregateId',
      key: 'aggregateId',
      width: 120,
      align: 'right',
    },
    {
      title: 'Interface ID',
      dataIndex: 'interfaceId',
      key: 'interfaceId',
      width: 240,
      render: (value: string) => <code>{value}</code>,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: string) => (
        <Tag color={OUTBOX_STATUS_TAG_COLOR[value] ?? 'default'}>{value}</Tag>
      ),
    },
    {
      title: '재시도',
      dataIndex: 'retryCount',
      key: 'retryCount',
      width: 80,
      align: 'right',
      render: (value: number) =>
        value >= MAX_RETRY_COUNT ? <Text type="danger">{value}</Text> : value,
    },
    {
      title: '마지막 오류',
      dataIndex: 'lastError',
      key: 'lastError',
      ellipsis: { showTitle: false },
      render: (value: string | null) =>
        value ? (
          <Tooltip title={value} placement="topLeft">
            {value}
          </Tooltip>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>SAP Outbound</Title>
      <Text type="secondary">
        backend 에서 SAP REST Adapter 로 송신되는 아웃바운드 호출 카탈로그 + 호출 이력 + 대기 큐를 조회합니다.
      </Text>

      <Tabs
        style={{ marginTop: 24 }}
        defaultActiveKey="logs"
        items={[
          {
            key: 'logs',
            label: '호출 이력',
            children: (
              <>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Space wrap>
                    <Select
                      allowClear
                      placeholder="Interface"
                      style={{ width: 360 }}
                      value={interfaceId}
                      onChange={(value) => {
                        setInterfaceId(value ?? undefined);
                        setPage(1);
                      }}
                      options={interfaceOptions}
                      showSearch
                      optionFilterProp="label"
                    />
                    <Select
                      allowClear
                      placeholder="Result"
                      style={{ width: 180 }}
                      value={resultCode}
                      onChange={(value) => {
                        setResultCode(value ?? undefined);
                        setPage(1);
                      }}
                      options={RESULT_CODE_OPTIONS}
                    />
                    <RangePicker
                      showTime
                      value={range as [Dayjs, Dayjs] | null}
                      onChange={(value) => {
                        setRange(value as [Dayjs | null, Dayjs | null] | null);
                        setPage(1);
                      }}
                    />
                    <Select
                      style={{ width: 100 }}
                      value={size}
                      onChange={(value) => {
                        setSize(value);
                        setPage(1);
                      }}
                      options={PAGE_SIZE_OPTIONS}
                    />
                  </Space>
                </Card>

                <Table<SapOutboundLogRow>
                  rowKey="id"
                  loading={logsQuery.isLoading}
                  dataSource={logsQuery.data?.items ?? []}
                  columns={logColumns}
                  pagination={{
                    current: page,
                    pageSize: size,
                    total: logsQuery.data?.totalCount ?? 0,
                    onChange: setPage,
                    showSizeChanger: false,
                  }}
                  onRow={(record) => ({
                    onClick: () => setSelectedRow(record),
                    style: { cursor: 'pointer' },
                  })}
                  scroll={{ x: 1700 }}
                />
              </>
            ),
          },
          {
            key: 'outbox',
            label: `대기 중 (Outbox)${
              outboxQuery.data?.totalCount ? ` · ${outboxQuery.data.totalCount}` : ''
            }`,
            children: (
              <>
                <Alert
                  type="info"
                  showIcon
                  style={{ marginBottom: 16 }}
                  message="본 탭은 큐에 남아있는 항목입니다 (PENDING + RETRY)."
                  description="정상 발송된 항목은 호출 이력 탭에 표시됩니다. 30초마다 자동 새로고침됩니다."
                />
                <Table<SapOutboxPendingRow>
                  rowKey="id"
                  loading={outboxQuery.isLoading}
                  dataSource={outboxQuery.data?.items ?? []}
                  columns={outboxColumns}
                  pagination={{
                    current: outboxPage,
                    pageSize: outboxSize,
                    total: outboxQuery.data?.totalCount ?? 0,
                    onChange: setOutboxPage,
                    showSizeChanger: false,
                  }}
                  scroll={{ x: 1400 }}
                />
              </>
            ),
          },
          {
            key: 'catalog',
            label: 'API 목록',
            children: (
              <Table<SapOutboundCatalogItem>
                rowKey="interfaceId"
                loading={catalogQuery.isLoading}
                dataSource={catalogQuery.data ?? []}
                columns={catalogColumns}
                pagination={false}
              />
            ),
          },
          {
            key: 'test',
            label: '테스트',
            children: <SapOutboundTestTab />,
          },
        ]}
      />

      <SapOutboundLogDetailModal
        row={selectedRow}
        open={selectedRow !== null}
        onClose={() => setSelectedRow(null)}
      />
    </div>
  );
}
