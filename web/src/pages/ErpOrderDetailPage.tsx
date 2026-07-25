import { Alert, Button, Card, Descriptions, Skeleton, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate, useParams } from 'react-router-dom';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import { useErpOrderDetail } from '@/hooks/erpOrder/useErpOrderDetail';
import type { ErpOrderProduct } from '@/api/erpOrder';

/** 빈 값 표시 — null/빈 문자열은 '-'. */
function dash(v: string | number | null | undefined): string {
  if (v === null || v === undefined) return '-';
  const s = String(v).trim();
  return s === '' ? '-' : s;
}

/** BigDecimal 문자열 → 천 단위 콤마 + '원'. 숫자가 아니면 원본 표시. */
function won(v: string | null): string {
  if (v === null || v.trim() === '') return '-';
  const n = Number(v);
  return Number.isFinite(n) ? `${n.toLocaleString()}원` : v;
}

/** BigDecimal 문자열 → 천 단위 콤마 (단위 없음). 숫자가 아니면 원본 표시. */
function num(v: string | null): string {
  if (v === null || v.trim() === '') return '-';
  const n = Number(v);
  return Number.isFinite(n) ? n.toLocaleString() : v;
}

/**
 * 기준정보 > ERP주문 상세 페이지 (`/erp-orders/:id`).
 *
 * ERP주문 헤더 + 주문상품 라인을 조회 노출. 조회 전용 (수정 없음).
 */
export default function ErpOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const orderId = id ? Number(id) : undefined;
  const navigate = useNavigate();

  const { data, isLoading, isError, error, refetch } = useErpOrderDetail(orderId);

  if (isLoading) {
    return (
      <div style={{ padding: 24 }}>
        <Skeleton active />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="ERP주문 상세를 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={
            <Space>
              <Button onClick={() => refetch()}>재시도</Button>
              <Button onClick={() => navigate('/erp-orders')}>목록으로</Button>
            </Space>
          }
        />
      </div>
    );
  }

  const productColumns: ColumnsType<ErpOrderProduct> = [
    { title: '라인', dataIndex: 'lineNumber', width: 70, align: 'center', render: (v: string | null) => dash(v) },
    { title: '제품코드', dataIndex: 'productCode', width: 110, render: (v: string | null) => dash(v) },
    { title: '제품명', dataIndex: 'productName', width: 200, ellipsis: true, render: (v: string | null) => dash(v) },
    { title: '주문수량', dataIndex: 'orderQuantity', width: 100, align: 'right', render: (v: string | null) => num(v) },
    { title: '주문단위', dataIndex: 'unit', width: 80, align: 'center', render: (v: string | null) => dash(v) },
    { title: '납품수량', dataIndex: 'confirmQuantity', width: 100, align: 'right', render: (v: string | null) => num(v) },
    { title: '납품단위', dataIndex: 'confirmUnit', width: 90, align: 'center', render: (v: string | null) => dash(v) },
    { title: '배송수량', dataIndex: 'shippingQuantity', width: 100, align: 'right', render: (v: string | null) => num(v) },
    {
      title: '납품금액',
      dataIndex: 'orderSalesLineAmount',
      width: 120,
      align: 'right',
      render: (v: string | null) => won(v),
    },
    { title: '주문처리상태', dataIndex: 'deliveryStatus', width: 110, align: 'center', render: (v: string | null) => dash(v) },
    { title: 'Item처리상태', dataIndex: 'lineItemStatus', width: 110, align: 'center', render: (v: string | null) => dash(v) },
    { title: '플랜트', dataIndex: 'plantNm', width: 110, render: (v: string | null) => dash(v) },
  ];

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => navigate('/erp-orders')}>← 목록으로</Button>
      </Space>

      <Card
        title={
          <Space>
            <span>주문번호 {data.sapOrderNumber}</span>
            {data.isDeleted && <Tag color="red">삭제됨</Tag>}
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="주문번호">{dash(data.sapOrderNumber)}</Descriptions.Item>
          <Descriptions.Item label="참조주문번호">{dash(data.refSapOrderNumber)}</Descriptions.Item>
          <Descriptions.Item label="거래처코드">{dash(data.sapAccountCode)}</Descriptions.Item>
          <Descriptions.Item label="거래처명">{dash(data.sapAccountName ?? data.accountName)}</Descriptions.Item>
          <Descriptions.Item label="납기일">{dash(data.deliveryRequestDate)}</Descriptions.Item>
          <Descriptions.Item label="주문생성일">{dash(data.orderDate)}</Descriptions.Item>
          <Descriptions.Item label="주문자사번">{dash(data.employeeCode)}</Descriptions.Item>
          <Descriptions.Item label="주문자명">{dash(data.employeeName)}</Descriptions.Item>
          <Descriptions.Item label="접수채널">{dash(data.orderChannelNm ?? data.orderChannel)}</Descriptions.Item>
          <Descriptions.Item label="주문유형">{dash(data.orderTypeNm ?? data.orderType)}</Descriptions.Item>
          <Descriptions.Item label="총주문금액">{won(data.orderSalesAmount)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card size="small" title={`주문상품 (${data.products.length}건)`}>
        <ResizableTable
          rowKey="id"
          columns={productColumns}
          dataSource={data.products}
          locale={listTableLocale()}
          scroll={{ x: 'max-content' }}
          pagination={false}
        />
      </Card>
    </div>
  );
}
