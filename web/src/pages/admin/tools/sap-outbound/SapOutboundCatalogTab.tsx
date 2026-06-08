import { Tag, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSapOutboundCatalog } from '@/hooks/admin/useSapOutbound';
import type {
  OutboundTriggerType,
  SapOutboundCatalogItem,
} from '@/api/admin/sapIntegration';
import ResizableTable from '@/components/common/ResizableTable';

const TRIGGER_TAG_COLOR: Record<OutboundTriggerType, string> = {
  BATCH: 'blue',
  REALTIME: 'green',
  OUTBOX: 'purple',
};

function shortSenderClass(fqn: string): string {
  const segments = fqn.split('.');
  return segments[segments.length - 1] ?? fqn;
}

/**
 * SAP Outbound API 목록(카탈로그) 탭.
 *
 * backend 가 SAP 으로 송신할 수 있는 아웃바운드 인터페이스의 정적 카탈로그(인터페이스 ID/
 * 한글명/트리거 유형/sender 클래스/설명)를 표로 노출한다. 페이지네이션 없이 전량 표시.
 */
export default function SapOutboundCatalogTab() {
  const catalogQuery = useSapOutboundCatalog();

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

  return (
    <ResizableTable<SapOutboundCatalogItem>
      rowKey="interfaceId"
      loading={catalogQuery.isLoading}
      dataSource={catalogQuery.data ?? []}
      columns={catalogColumns}
      pagination={false}
    />
  );
}
