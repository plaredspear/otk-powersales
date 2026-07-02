import { useState } from 'react';
import { Button, Card, DatePicker, Select, Space, Tag, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useSapInboundAudits,
  useSapInboundCatalog,
} from '@/hooks/admin/useSapInbound';
import type { SapInboundAuditRow } from '@/api/admin/sapIntegration';
import SapInboundAuditDetailModal from './SapInboundAuditDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

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

/**
 * SAP Inbound 호출 이력 탭.
 *
 * SAP 에서 backend 로 들어오는 인바운드 호출의 감사 로그(인증/스코프/변환 결과)를
 * 필터링 + 페이지네이션으로 조회한다. 행 클릭 시 상세 모달을 띄운다.
 *
 * `lockedEndpoint` 를 주면 해당 Endpoint 로 조회를 고정하고 Endpoint 필터 셀렉터를
 * 숨긴다. API 상세 탭에서 그 API 의 호출 이력만 인라인으로 보여줄 때 사용한다.
 */
export default function SapInboundAuditsTab({
  lockedEndpoint,
}: {
  lockedEndpoint?: string;
} = {}) {
  // 조회 조건 버퍼 — "조회" 버튼 시점에만 applied 로 반영 (필터 변경만으로 조회하지 않음)
  const [clientIdInput, setClientIdInput] = useState<string | undefined>(undefined);
  const [eventTypeInput, setEventTypeInput] = useState<string | undefined>(undefined);
  const [endpointInput, setEndpointInput] = useState<string | undefined>(undefined);
  const [rangeInput, setRangeInput] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [applied, setApplied] = useState<{
    clientId?: string;
    eventType?: string;
    endpoint?: string;
    from?: string;
    to?: string;
  }>({});
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedRow, setSelectedRow] = useState<SapInboundAuditRow | null>(null);

  const handleSearch = () => {
    setPage(1);
    setApplied({
      clientId: clientIdInput,
      eventType: eventTypeInput,
      endpoint: endpointInput,
      from: rangeInput?.[0]?.toISOString() ?? undefined,
      to: rangeInput?.[1]?.toISOString() ?? undefined,
    });
  };

  const catalogQuery = useSapInboundCatalog();
  const auditsQuery = useSapInboundAudits({
    clientId: applied.clientId,
    eventType: applied.eventType,
    endpoint: lockedEndpoint ?? applied.endpoint,
    from: applied.from,
    to: applied.to,
    page,
    size,
  });

  const endpointOptions =
    catalogQuery.data?.map((item) => ({
      label: item.endpointPath,
      value: item.endpointPath,
    })) ?? [];

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
    <>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
          <Space wrap>
          <Select
            allowClear
            placeholder="Client ID"
            style={{ width: 180 }}
            value={clientIdInput}
            onChange={(value) => setClientIdInput(value || undefined)}
            mode="tags"
            maxCount={1}
          />
          <Select
            allowClear
            placeholder="Event Type"
            style={{ width: 240 }}
            value={eventTypeInput}
            onChange={(value) => setEventTypeInput(value ?? undefined)}
            options={EVENT_TYPE_OPTIONS}
          />
          {lockedEndpoint === undefined && (
            <Select
              allowClear
              placeholder="Endpoint"
              style={{ width: 280 }}
              value={endpointInput}
              onChange={(value) => setEndpointInput(value ?? undefined)}
              options={endpointOptions}
              showSearch
              optionFilterProp="label"
            />
          )}
          <RangePicker
            showTime
            value={rangeInput as [Dayjs, Dayjs] | null}
            onChange={(value) => setRangeInput(value as [Dayjs | null, Dayjs | null] | null)}
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
          <Button type="primary" onClick={handleSearch}>
            조회
          </Button>
          </Space>
          <RefreshButton onRefresh={auditsQuery.refetch} refreshing={auditsQuery.isFetching} />
        </Space>
      </Card>

      <ResizableTable<SapInboundAuditRow>
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

      <SapInboundAuditDetailModal
        row={selectedRow}
        open={selectedRow !== null}
        onClose={() => setSelectedRow(null)}
      />
    </>
  );
}
