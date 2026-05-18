import { Alert, Card, Col, Empty, Modal, Progress, Row, Spin, Statistic, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { fetchDetail, type MonthlySalesDashboardDetail } from '@/api/monthlySalesDashboard';
import YearComparisonBarChart from '@/components/charts/YearComparisonBarChart';

const { Text } = Typography;

const CATEGORY_LABELS: Record<string, string> = {
  AMBIENT: '상온',
  NOODLE: '라면',
  FROZEN_REFRIGERATED: '냉동·냉장',
  OIL_FAT: '유지류',
};

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
        <DetailBody detail={detailQuery.data} />
      ) : (
        <Empty />
      )}
    </Modal>
  );
}

function DetailBody({ detail }: { detail: MonthlySalesDashboardDetail }) {
  const isPastMonth = detail.categorySales.length > 0;
  const progressColor =
    detail.achievementRate >= detail.referenceAchievementRate ? '#1677ff' : '#ff4d4f';
  const formatWon = (v: number) => `${v.toLocaleString()}원`;

  return (
    <div>
      <Card size="small" title="당월 매출 진도율" style={{ marginBottom: 12 }}>
        <Progress
          percent={Math.min(100, Math.max(0, Math.round(detail.achievementRate * 10) / 10))}
          strokeColor={progressColor}
          format={(p) => `${p?.toFixed(1) ?? 0}%`}
        />
        <Text type="secondary">
          기준 진도율: {detail.referenceAchievementRate.toFixed(1)}%
        </Text>
      </Card>

      <Card size="small" title="목표 / 실적" style={{ marginBottom: 12 }}>
        <Row gutter={16}>
          <Col span={12}>
            <Statistic title="목표 금액" value={formatWon(detail.targetAmount)} />
          </Col>
          <Col span={12}>
            <Statistic
              title="마감 합계 실적"
              value={formatWon(detail.achievedAmount)}
              suffix={`(${detail.achievementRate.toFixed(1)}% 달성)`}
            />
          </Col>
        </Row>
      </Card>

      {isPastMonth && (
        <Card size="small" title="카테고리별 실적 (과거 월)" style={{ marginBottom: 12 }}>
          <Row gutter={[12, 12]}>
            {detail.categorySales.map((c) => (
              <Col span={12} key={c.category}>
                <Card size="small">
                  <div>
                    <Text strong>{CATEGORY_LABELS[c.category] ?? c.category}</Text>
                  </div>
                  <Text type="secondary">목표 {formatWon(c.targetAmount)}</Text>
                  <div>
                    <Text>
                      실적 {formatWon(c.achievedAmount)} ({c.achievementRate.toFixed(1)}%)
                    </Text>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>
        </Card>
      )}

      <Card size="small" title="전년 대비 동월 실적 (백만원)" style={{ marginBottom: 12 }}>
        <YearComparisonBarChart
          leftLabel={`${detail.salesYear}-${String(detail.salesMonth).padStart(2, '0')}`}
          leftValue={detail.yearComparison.currentYear}
          rightLabel={`${detail.salesYear - 1}-${String(detail.salesMonth).padStart(2, '0')}`}
          rightValue={detail.yearComparison.previousYear}
          unitSuffix="백만"
        />
      </Card>

      <Card
        size="small"
        title={`전년 대비 월 평균 실적 (${detail.monthlyAverage.startMonth}월~${detail.monthlyAverage.endMonth}월, 백만원)`}
      >
        <YearComparisonBarChart
          leftLabel={`${detail.salesYear}`}
          leftValue={detail.monthlyAverage.currentYearAverage}
          rightLabel={`${detail.salesYear - 1}`}
          rightValue={detail.monthlyAverage.previousYearAverage}
          unitSuffix="백만"
        />
      </Card>
    </div>
  );
}
