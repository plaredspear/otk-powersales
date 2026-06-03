import { Alert, Empty, Modal, Spin } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { fetchDetail } from '@/api/monthlySalesDashboard';
import MonthlySalesDetailBody from './MonthlySalesDetailBody';

interface MonthlySalesDashboardDetailModalProps {
  open: boolean;
  onClose: () => void;
  customerId: number | null;
  customerName: string | null;
  year: number;
  month: number;
}

/**
 * 거래처 단건 상세 모달 — 모바일 동등 6 영역.
 *
 * 진도율 바 (UC-05) + 목표/실적 카드 (UC-06) + 카테고리 4종 (UC-07, 과거월 한정) + 전년 동월 차트 (UC-08) + 전년 평균 차트 (UC-09).
 */
export default function MonthlySalesDashboardDetailModal({
  open,
  onClose,
  customerId,
  customerName,
  year,
  month,
}: MonthlySalesDashboardDetailModalProps) {
  const detailQuery = useQuery({
    queryKey: ['monthlySalesDashboard', 'detail', customerId, year, month],
    queryFn: () => fetchDetail(customerId!, year, month),
    enabled: open && customerId != null,
  });

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={`월매출 상세 — ${customerName ?? ''} (${year}-${String(month).padStart(2, '0')})`}
      width={760}
      footer={null}
      destroyOnClose
    >
      {detailQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : detailQuery.isError ? (
        <Alert
          type="error"
          message={(detailQuery.error as Error)?.message ?? '상세 조회 실패'}
        />
      ) : detailQuery.data ? (
        <MonthlySalesDetailBody detail={detailQuery.data} />
      ) : (
        <Empty />
      )}
    </Modal>
  );
}
