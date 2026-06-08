import { useState } from 'react';
import { Alert, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useSapOutboundOutboxPending } from '@/hooks/admin/useSapOutbound';
import type { SapOutboxPendingRow } from '@/api/admin/sapIntegration';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;

const OUTBOX_STATUS_TAG_COLOR: Record<string, string> = {
  PENDING: 'blue',
  RETRY: 'orange',
};

const MAX_RETRY_COUNT = 5;

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

/**
 * SAP Outbound 대기 큐(Outbox) 탭.
 *
 * 트랜잭셔널 아웃박스 큐에 남아 있는 PENDING/RETRY 행을 조회한다. 정상 발송된 항목은
 * '호출 이력' 탭에 표시되며, 본 탭은 30초마다 자동 새로고침된다(hook 내부 설정).
 */
export default function SapOutboundOutboxTab() {
  const [page, setPage] = useState(1);
  const [size] = useState(20);
  const outboxQuery = useSapOutboundOutboxPending(page, size);

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
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="본 탭은 큐에 남아있는 항목입니다 (PENDING + RETRY)."
        description="정상 발송된 항목은 호출 이력 탭에 표시됩니다. 30초마다 자동 새로고침됩니다."
      />
      <ResizableTable<SapOutboxPendingRow>
        rowKey="id"
        loading={outboxQuery.isLoading}
        dataSource={outboxQuery.data?.items ?? []}
        columns={outboxColumns}
        pagination={{
          current: page,
          pageSize: size,
          total: outboxQuery.data?.totalCount ?? 0,
          onChange: setPage,
          showSizeChanger: false,
        }}
        scroll={{ x: 1400 }}
      />
    </>
  );
}
