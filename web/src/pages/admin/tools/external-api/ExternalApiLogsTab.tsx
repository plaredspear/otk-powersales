import { useState } from 'react';
import { Card, DatePicker, Select, Space, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useExternalApiLogKeys,
  useExternalApiLogs,
} from '@/hooks/admin/useExternalApiLog';
import type {
  ExternalApiLogRow,
  ExternalApiTargetSystem,
} from '@/api/admin/externalApiLog';
import ExternalApiLogDetailModal from './ExternalApiLogDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;
const { RangePicker } = DatePicker;

const TARGET_OPTIONS: { label: string; value: ExternalApiTargetSystem }[] = [
  { label: 'SAP', value: 'SAP' },
  { label: 'SF', value: 'SF' },
  { label: 'NAVER', value: 'NAVER' },
];

const SUCCESS_OPTIONS: { label: string; value: 'true' | 'false' }[] = [
  { label: '성공', value: 'true' },
  { label: '실패', value: 'false' },
];

const PAGE_SIZE_OPTIONS = [
  { label: '20', value: 20 },
  { label: '50', value: 50 },
  { label: '100', value: 100 },
];

const TARGET_TAG_COLOR: Record<ExternalApiTargetSystem, string> = {
  SAP: 'blue',
  SF: 'cyan',
  NAVER: 'geekblue',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

/**
 * 외부 API 호출 이력 탭.
 *
 * backend 가 SAP / SF / Naver 로 송신한 outbound HTTP 호출 공통 로그(`external_api_log`)를
 * 시스템 / endpoint key / 성공 여부 / 기간 필터로 조회한다. 행 클릭 시 상세 모달.
 *
 * `lockedEndpointKey` 를 주면 해당 endpoint key 로 조회를 고정하고 시스템/key 필터 셀렉터를
 * 숨긴다. 외부 API 테스트 각 탭에서 그 API 의 호출 이력만 인라인으로 보여줄 때 사용한다.
 */
export default function ExternalApiLogsTab({
  lockedEndpointKey,
}: {
  lockedEndpointKey?: string;
} = {}) {
  const [targetSystem, setTargetSystem] = useState<ExternalApiTargetSystem | undefined>(undefined);
  const [endpointKey, setEndpointKey] = useState<string | undefined>(undefined);
  const [success, setSuccess] = useState<'true' | 'false' | undefined>(undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedRow, setSelectedRow] = useState<ExternalApiLogRow | null>(null);

  const keysQuery = useExternalApiLogKeys();
  const logsQuery = useExternalApiLogs({
    targetSystem: lockedEndpointKey === undefined ? targetSystem : undefined,
    endpointKey: lockedEndpointKey ?? endpointKey,
    success: success === undefined ? undefined : success === 'true',
    from: range?.[0]?.toISOString() ?? undefined,
    to: range?.[1]?.toISOString() ?? undefined,
    page,
    size,
  });

  const keyOptions =
    keysQuery.data?.map((key) => ({ label: key, value: key })) ?? [];

  const logColumns: ColumnsType<ExternalApiLogRow> = [
    {
      title: '요청 시각',
      dataIndex: 'requestedAt',
      key: 'requestedAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'System',
      dataIndex: 'targetSystem',
      key: 'targetSystem',
      width: 90,
      render: (value: ExternalApiTargetSystem) => (
        <Tag color={TARGET_TAG_COLOR[value] ?? 'default'}>{value}</Tag>
      ),
    },
    {
      title: 'Endpoint Key',
      dataIndex: 'endpointKey',
      key: 'endpointKey',
      width: 220,
      render: (value: string | null) => (value ? <code>{value}</code> : '-'),
    },
    {
      title: 'Method',
      dataIndex: 'httpMethod',
      key: 'httpMethod',
      width: 80,
      render: (value: string) => <code>{value}</code>,
    },
    {
      title: 'URI',
      dataIndex: 'uri',
      key: 'uri',
      width: 320,
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Tooltip title={value} placement="topLeft">
          <code>{value}</code>
        </Tooltip>
      ),
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
      title: '결과',
      dataIndex: 'success',
      key: 'success',
      width: 90,
      render: (value: boolean) => (
        <Tag color={value ? 'green' : 'red'}>{value ? 'SUCCESS' : 'FAIL'}</Tag>
      ),
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
      render: (value: string) => <Text type="secondary">{formatDateTime(value)}</Text>,
    },
  ];

  return (
    <>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
          <Space wrap>
            {lockedEndpointKey === undefined && (
              <>
                <Select
                  allowClear
                  placeholder="System"
                  style={{ width: 140 }}
                  value={targetSystem}
                  onChange={(value) => {
                    setTargetSystem(value ?? undefined);
                    setPage(1);
                  }}
                  options={TARGET_OPTIONS}
                />
                <Select
                  allowClear
                  placeholder="Endpoint Key"
                  style={{ width: 320 }}
                  value={endpointKey}
                  onChange={(value) => {
                    setEndpointKey(value ?? undefined);
                    setPage(1);
                  }}
                  options={keyOptions}
                  showSearch
                  optionFilterProp="label"
                />
              </>
            )}
            <Select
              allowClear
              placeholder="결과"
              style={{ width: 120 }}
              value={success}
              onChange={(value) => {
                setSuccess(value ?? undefined);
                setPage(1);
              }}
              options={SUCCESS_OPTIONS}
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

      <ResizableTable<ExternalApiLogRow>
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
        scroll={{ x: 1400 }}
      />

      <ExternalApiLogDetailModal
        row={selectedRow}
        open={selectedRow !== null}
        onClose={() => setSelectedRow(null)}
      />
    </>
  );
}
