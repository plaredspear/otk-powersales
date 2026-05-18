import { useState } from 'react';
import {
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
  useSapInboundAudits,
  useSapInboundCatalog,
} from '@/hooks/admin/useSapInbound';
import type {
  SapInboundAuditRow,
  SapInboundCatalogItem,
} from '@/api/admin/sapIntegration';
import SapInboundAuditDetailModal from './SapInboundAuditDetailModal';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const EVENT_TYPE_OPTIONS = [
  { label: 'REQUEST_ACCEPTED', value: 'REQUEST_ACCEPTED' },
  { label: 'REQUEST_REJECTED_AUTH', value: 'REQUEST_REJECTED_AUTH' },
  { label: 'REQUEST_REJECTED_SCOPE', value: 'REQUEST_REJECTED_SCOPE' },
  { label: 'REQUEST_REJECTED_IP', value: 'REQUEST_REJECTED_IP' },
  { label: 'REQUEST_REJECTED_SANITY', value: 'REQUEST_REJECTED_SANITY' },
  { label: 'TOKEN_ISSUED', value: 'TOKEN_ISSUED' },
  { label: 'TOKEN_REJECTED', value: 'TOKEN_REJECTED' },
  { label: 'SCHEDULE_CONVERSION', value: 'SCHEDULE_CONVERSION' },
  { label: 'SCHEDULE_CONVERSION_FAILED', value: 'SCHEDULE_CONVERSION_FAILED' },
  { label: 'MANUAL_ORIGIN_PROTECTED', value: 'MANUAL_ORIGIN_PROTECTED' },
];

const PAGE_SIZE_OPTIONS = [
  { label: '20', value: 20 },
  { label: '50', value: 50 },
  { label: '100', value: 100 },
];

const EVENT_TAG_COLOR: Record<string, string> = {
  REQUEST_ACCEPTED: 'green',
  REQUEST_REJECTED_AUTH: 'red',
  REQUEST_REJECTED_SCOPE: 'red',
  REQUEST_REJECTED_IP: 'red',
  REQUEST_REJECTED_SANITY: 'red',
  TOKEN_ISSUED: 'blue',
  TOKEN_REJECTED: 'red',
  SCHEDULE_CONVERSION: 'cyan',
  SCHEDULE_CONVERSION_FAILED: 'orange',
  MANUAL_ORIGIN_PROTECTED: 'gold',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

export default function SapInboundPage() {
  const [clientId, setClientId] = useState<string | undefined>(undefined);
  const [eventType, setEventType] = useState<string | undefined>(undefined);
  const [endpoint, setEndpoint] = useState<string | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedRow, setSelectedRow] = useState<SapInboundAuditRow | null>(null);

  const catalogQuery = useSapInboundCatalog();
  const auditsQuery = useSapInboundAudits({
    clientId,
    eventType,
    endpoint,
    from: range?.[0]?.toISOString() ?? undefined,
    to: range?.[1]?.toISOString() ?? undefined,
    page,
    size,
  });

  const endpointOptions =
    catalogQuery.data?.map((item) => ({
      label: item.endpointPath,
      value: item.endpointPath,
    })) ?? [];

  const catalogColumns: ColumnsType<SapInboundCatalogItem> = [
    {
      title: 'Endpoint',
      dataIndex: 'endpointPath',
      key: 'endpointPath',
      width: 260,
      render: (value: string) => <code>{value}</code>,
    },
    { title: '한글명', dataIndex: 'koreanName', key: 'koreanName', width: 200 },
    {
      title: 'Scope',
      dataIndex: 'requiredScope',
      key: 'requiredScope',
      width: 180,
      render: (value: string) => <code>{value}</code>,
    },
    { title: '적재 대상', dataIndex: 'targetEntity', key: 'targetEntity', width: 180 },
    {
      title: '컨트롤러',
      dataIndex: 'controllerClass',
      key: 'controllerClass',
      width: 240,
      render: (value: string) => <code>{value}</code>,
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

  const auditColumns: ColumnsType<SapInboundAuditRow> = [
    {
      title: '시간',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'Event Type',
      dataIndex: 'eventType',
      key: 'eventType',
      width: 200,
      render: (value: string) => (
        <Tag color={EVENT_TAG_COLOR[value] ?? 'default'}>{value}</Tag>
      ),
    },
    { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', width: 140 },
    {
      title: 'Endpoint',
      dataIndex: 'endpoint',
      key: 'endpoint',
      width: 220,
      ellipsis: true,
      render: (value: string | null) => (value ? <code>{value}</code> : '-'),
    },
    {
      title: 'HTTP Method',
      dataIndex: 'httpMethod',
      key: 'httpMethod',
      width: 100,
      render: (value: string | null) => (value ? <Tag>{value}</Tag> : '-'),
    },
    { title: 'IP', dataIndex: 'clientIp', key: 'clientIp', width: 120 },
    {
      title: 'Scope',
      dataIndex: 'scope',
      key: 'scope',
      width: 200,
      ellipsis: { showTitle: false },
      render: (value: string | null) =>
        value ? (
          <Tooltip title={value} placement="topLeft">
            <code>{value}</code>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: '수신',
      dataIndex: 'receivedCount',
      key: 'receivedCount',
      width: 80,
      align: 'right',
      render: (value: number | null) => value ?? '-',
    },
    {
      title: '이전',
      dataIndex: 'previousCount',
      key: 'previousCount',
      width: 80,
      align: 'right',
      render: (value: number | null) => value ?? '-',
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
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
      <Title level={3}>SAP Inbound</Title>
      <Text type="secondary">
        SAP 에서 backend 로 송신되는 인바운드 호출의 카탈로그와 호출 이력을 조회합니다.
      </Text>

      <Tabs
        style={{ marginTop: 24 }}
        defaultActiveKey="audits"
        items={[
          {
            key: 'audits',
            label: '호출 이력',
            children: (
              <>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Space wrap>
                    <Select
                      allowClear
                      placeholder="Client ID"
                      style={{ width: 180 }}
                      value={clientId}
                      onChange={(value) => {
                        setClientId(value || undefined);
                        setPage(1);
                      }}
                      mode="tags"
                      maxCount={1}
                    />
                    <Select
                      allowClear
                      placeholder="Event Type"
                      style={{ width: 240 }}
                      value={eventType}
                      onChange={(value) => {
                        setEventType(value ?? undefined);
                        setPage(1);
                      }}
                      options={EVENT_TYPE_OPTIONS}
                    />
                    <Select
                      allowClear
                      placeholder="Endpoint"
                      style={{ width: 280 }}
                      value={endpoint}
                      onChange={(value) => {
                        setEndpoint(value ?? undefined);
                        setPage(1);
                      }}
                      options={endpointOptions}
                      showSearch
                      optionFilterProp="label"
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

                <Table<SapInboundAuditRow>
                  rowKey="id"
                  loading={auditsQuery.isLoading}
                  dataSource={auditsQuery.data?.items ?? []}
                  columns={auditColumns}
                  pagination={{
                    current: page,
                    pageSize: size,
                    total: auditsQuery.data?.totalCount ?? 0,
                    onChange: setPage,
                    showSizeChanger: false,
                  }}
                  onRow={(record) => ({
                    onClick: () => setSelectedRow(record),
                    style: { cursor: 'pointer' },
                  })}
                  scroll={{ x: 1600 }}
                />
              </>
            ),
          },
          {
            key: 'catalog',
            label: 'API 목록',
            children: (
              <Table<SapInboundCatalogItem>
                rowKey="endpointPath"
                loading={catalogQuery.isLoading}
                dataSource={catalogQuery.data ?? []}
                columns={catalogColumns}
                pagination={false}
              />
            ),
          },
        ]}
      />

      <SapInboundAuditDetailModal
        row={selectedRow}
        open={selectedRow !== null}
        onClose={() => setSelectedRow(null)}
      />
    </div>
  );
}
