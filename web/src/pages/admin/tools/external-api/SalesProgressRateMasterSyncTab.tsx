import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  Row,
  Space,
  Statistic,
  Typography,
  notification,
} from 'antd';
import { SyncOutlined } from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import {
  triggerSalesProgressRateMasterSync,
  type SalesProgressRateMasterSyncResult,
} from '@/api/salesProgressRateMaster';

const { Text } = Typography;

/**
 * SF 거래처목표등록마스터 동기화 수동 실행 탭 (외부 API 테스트 통합 페이지).
 *
 * 주기 배치(salesProgressRateMaster.sync, 매시 정각)와 동일한 SF fetch → ExternalKey upsert 경로를
 * 백엔드 `POST /api/v1/admin/sales-progress-rate-master/sync/test` 로 즉시 1회 실행하고, upsert 통계
 * (fetched/inserted/updated/skipped)를 표시한다. 운영 배치를 기다리지 않고 SF→DB 동기화를 검증하기
 * 위한 용도이며, 실행 즉시 실제 DB 에 upsert 가 반영된다. SF fetch 통신부가 미구현(TODO)인 동안에는
 * fetched=0 의 no-op 으로 동작한다. SYSTEM(MODIFY_ALL_DATA) 권한 필요.
 */
export default function SalesProgressRateMasterSyncTab() {
  const [result, setResult] = useState<SalesProgressRateMasterSyncResult | null>(
    null,
  );

  const mutation = useMutation<SalesProgressRateMasterSyncResult, Error, void>({
    mutationFn: triggerSalesProgressRateMasterSync,
  });

  const handleSync = async () => {
    try {
      const response = await mutation.mutateAsync();
      setResult(response);
      notification.success({
        key: 'sales-progress-sync',
        message: '거래처목표등록마스터 동기화 완료',
        description: `조회 ${response.fetched}건 · 추가 ${response.inserted}건 · 갱신 ${response.updated}건 · 건너뜀 ${response.skipped}건`,
      });
    } catch (err) {
      setResult(null);
      notification.error({
        key: 'sales-progress-sync-error',
        message: '거래처목표등록마스터 동기화 실패',
        description:
          err instanceof Error ? err.message : '잠시 후 다시 시도해주세요.',
      });
    }
  };

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="거래처목표등록마스터 동기화 (POST /api/v1/admin/sales-progress-rate-master/sync/test)">
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Text type="secondary">
            주기 배치(매시 정각)와 동일한 SF 조회 → ExternalKey 기준 upsert 경로를 즉시 1회
            실행합니다. 실행 즉시 실제 DB 에 반영되며, SF fetch 통신부가 아직 미구현(TODO)인 동안에는
            조회 0건의 no-op 으로 동작합니다.
          </Text>
          <Button
            type="primary"
            icon={<SyncOutlined />}
            loading={mutation.isPending}
            onClick={handleSync}
          >
            지금 동기화 실행
          </Button>
        </Space>
      </Card>

      {result && (
        <Card title="동기화 결과">
          <Row gutter={16}>
            <Col span={6}>
              <Statistic title="조회 (fetched)" value={result.fetched} />
            </Col>
            <Col span={6}>
              <Statistic title="추가 (inserted)" value={result.inserted} />
            </Col>
            <Col span={6}>
              <Statistic title="갱신 (updated)" value={result.updated} />
            </Col>
            <Col span={6}>
              <Statistic title="건너뜀 (skipped)" value={result.skipped} />
            </Col>
          </Row>
        </Card>
      )}
    </Space>
  );
}
