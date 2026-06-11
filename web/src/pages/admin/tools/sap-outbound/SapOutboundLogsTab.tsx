import { useState } from 'react';
import { Card, DatePicker, Select, Space, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useSapOutboundCatalog,
  useSapOutboundLogs,
} from '@/hooks/admin/useSapOutbound';
import type {
  SapOutboundLogRow,
  SapOutboundResultCode,
} from '@/api/admin/sapIntegration';
import SapOutboundLogDetailModal from './SapOutboundLogDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;
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

const RESULT_TAG_COLOR: Record<SapOutboundResultCode, string> = {
  SUCCESS: 'green',
  FAIL: 'red',
  INVALID_RESPONSE: 'orange',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

/**
 * SAP Outbound 호출 이력 탭.
 *
 * backend 가 SAP REST Adapter 로 송신한 아웃바운드 호출의 결과 로그(결과 코드/HTTP/시도/
 * 소요시간)를 인터페이스·결과·기간 필터로 조회한다. 행 클릭 시 상세 모달.
 *
 * `lockedInterfaceId` 를 주면 해당 Interface 로 조회를 고정하고 Interface 필터 셀렉터를
 * 숨긴다. API 상세 탭에서 그 API 의 호출 이력만 인라인으로 보여줄 때 사용한다.
 */
export default function SapOutboundLogsTab({
  lockedInterfaceId,
}: {
  lockedInterfaceId?: string;
} = {}) {
  const [interfaceId, setInterfaceId] = useState<string | undefined>(undefined);
  const [resultCode, setResultCode] = useState<SapOutboundResultCode | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedRow, setSelectedRow] = useState<SapOutboundLogRow | null>(null);

  const catalogQuery = useSapOutboundCatalog();
  const logsQuery = useSapOutboundLogs({
    interfaceId: lockedInterfaceId ?? interfaceId,
    resultCode,
    from: range?.[0]?.toISOString() ?? undefined,
    to: range?.[1]?.toISOString() ?? undefined,
    page,
    size,
  });

  const interfaceOptions =
    catalogQuery.data?.map((item) => ({
      label: `${item.interfaceId} (${item.koreanName})`,
      value: item.interfaceId,
    })) ?? [];

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

  return (
    <>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
          <Space wrap>
          {lockedInterfaceId === undefined && (
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
          )}
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
          <RefreshButton onRefresh={logsQuery.refetch} refreshing={logsQuery.isFetching} />
        </Space>
      </Card>

      <ResizableTable<SapOutboundLogRow>
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

      <SapOutboundLogDetailModal
        row={selectedRow}
        open={selectedRow !== null}
        onClose={() => setSelectedRow(null)}
      />
    </>
  );
}
