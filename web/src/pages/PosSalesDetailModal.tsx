import { Alert, Empty, Modal, Spin, Statistic, Row, Col, Card } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchPosSalesDetail,
  type PosSalesDetail,
  type PosSalesProduct,
} from '@/api/posSales';
import ResizableTable from '@/components/common/ResizableTable';

interface PosSalesDetailModalProps {
  open: boolean;
  onClose: () => void;
  customerId: number | null;
  customerName: string | null;
  /** 조회 시작일 (YYYY-MM-DD) */
  startDate: string;
  /** 조회 종료일 (YYYY-MM-DD) */
  endDate: string;
  /** 목록과 동일한 제품 필터 — 상세 합계가 목록 행 합계와 정합하도록 반영 */
  productIds?: number[];
  category2?: string;
  category3?: string;
}

/**
 * 거래처 단건 POS매출 상세 모달 — 제품별 명세 (바코드 포함).
 *
 * 레거시 `posmain.jsp` 의 품목별 명세 (`selectPosData` — `GROUP BY ITEM_CD`) 동등.
 * 목록 조회의 기간/제품/분류 필터를 그대로 반영한다.
 */
export default function PosSalesDetailModal({
  open,
  onClose,
  customerId,
  customerName,
  startDate,
  endDate,
  productIds = [],
  category2,
  category3,
}: PosSalesDetailModalProps) {
  const detailQuery = useQuery({
    queryKey: [
      'posSalesDashboard',
      'detail',
      customerId,
      startDate,
      endDate,
      productIds,
      category2,
      category3,
    ],
    queryFn: () =>
      fetchPosSalesDetail(customerId!, startDate, endDate, { productIds, category2, category3 }),
    enabled: open && customerId != null,
  });

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={`POS매출 상세 — ${customerName ?? ''} (${startDate} ~ ${endDate})`}
      width={860}
      footer={null}
      destroyOnClose
    >
      {detailQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : detailQuery.isError ? (
        <Alert type="error" message={(detailQuery.error as Error)?.message ?? '상세 조회 실패'} />
      ) : detailQuery.data ? (
        <DetailBody detail={detailQuery.data} />
      ) : (
        <Empty />
      )}
    </Modal>
  );
}

function DetailBody({ detail }: { detail: PosSalesDetail }) {
  const formatWon = (v: number) => `${v.toLocaleString()}원`;
  const formatQty = (v: number) => v.toLocaleString();

  const columns: ColumnsType<PosSalesProduct> = [
    { title: '제품코드', dataIndex: 'productCode', width: 120 },
    { title: '제품명', dataIndex: 'productName', width: 260, ellipsis: true, render: (v) => v || '-' },
    { title: '바코드', dataIndex: 'barcode', width: 150, render: (v: string | null) => v ?? '-' },
    {
      title: '금액',
      dataIndex: 'amount',
      width: 140,
      align: 'right',
      render: (v: number) => formatWon(v),
    },
    {
      title: '수량',
      dataIndex: 'quantity',
      width: 110,
      align: 'right',
      render: (v: number) => formatQty(v),
    },
  ];

  return (
    <div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Row gutter={16}>
          <Col span={12}>
            <Statistic title="POS매출 금액 합계" value={detail.totalAmount.toLocaleString()} suffix="원" />
          </Col>
          <Col span={12}>
            <Statistic title="POS매출 수량 합계" value={detail.totalQuantity.toLocaleString()} />
          </Col>
        </Row>
      </Card>

      <ResizableTable
        rowKey={(r) => r.productCode}
        size="small"
        columns={columns}
        dataSource={detail.items}
        pagination={false}
        // 세로 스크롤(y) 유지 + 컬럼 width 합(x) 명시 → 가로는 고정 폭으로 ellipsis/리사이즈 동작.
        scroll={{ x: 780, y: 360 }}
        locale={{ emptyText: '제품별 POS매출 내역이 없습니다' }}
      />
    </div>
  );
}
