import { Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSapInboundCatalog } from '@/hooks/admin/useSapInbound';
import type { SapInboundCatalogItem } from '@/api/admin/sapIntegration';
import ResizableTable from '@/components/common/ResizableTable';

/**
 * SAP Inbound API 목록(카탈로그) 탭.
 *
 * SAP 가 호출할 수 있는 인바운드 엔드포인트의 정적 카탈로그(경로/한글명/스코프/적재 대상/
 * 컨트롤러/설명)를 표로 노출한다. 호출 이력과 무관한 정의 메타이므로 페이지네이션 없이 전량 표시.
 */
export default function SapInboundCatalogTab() {
  const catalogQuery = useSapInboundCatalog();

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

  return (
    <ResizableTable<SapInboundCatalogItem>
      rowKey="endpointPath"
      loading={catalogQuery.isLoading}
      dataSource={catalogQuery.data ?? []}
      columns={catalogColumns}
      pagination={false}
    />
  );
}
