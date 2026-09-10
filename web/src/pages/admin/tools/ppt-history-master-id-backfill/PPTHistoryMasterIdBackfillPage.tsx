import { useState } from 'react';
import {
  Alert,
  Button,
  Descriptions,
  InputNumber,
  Modal,
  Space,
  Statistic,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  executePPTHistoryBackfill,
  previewPPTHistoryBackfill,
  type PPTHistoryBackfillPreview,
  type PPTHistoryBackfillResult,
} from '@/api/admin/pptHistoryMasterIdBackfill';

const { Title, Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'ppt-history', 'master-id-backfill', 'preview'] as const;

const DEFAULT_LIMIT = 1000;
const MAX_LIMIT = 5000;

/**
 * 개발자 도구 — 전문행사조 이력 원인 마스터 FK(master_id) 백필.
 *
 * SF 이관 이력에는 마스터 참조 필드가 없어 FK 가 비어 있고, 그대로 두면 마스터 삭제 / 핵심필드 수정
 * 가드가 이관분 마스터를 보호하지 못한다. preview 로 대상(단일 매칭 / 후보 다중) 건수를 확인하고,
 * execute 로 이력 id 오래된 순 최대 limit 건씩 채운다. 잔여가 0 이 될 때까지 반복 실행한다.
 */
export default function PPTHistoryMasterIdBackfillPage() {
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState<number>(DEFAULT_LIMIT);
  const [lastResult, setLastResult] = useState<PPTHistoryBackfillResult | null>(null);

  const {
    data: preview,
    isLoading,
    refetch,
  } = useQuery<PPTHistoryBackfillPreview>({
    queryKey: QUERY_KEY,
    queryFn: previewPPTHistoryBackfill,
  });

  const executeMutation = useMutation({
    mutationFn: () => executePPTHistoryBackfill(limit),
    onSuccess: (result: PPTHistoryBackfillResult) => {
      setLastResult(result);
      message.success(`백필 완료 — ${result.updated}건 연결, 잔여 ${result.remaining}건`);
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: Error) => {
      message.error(err.message || '백필 실행에 실패했습니다');
    },
  });

  const backfillable = preview?.backfillable ?? 0;
  const ambiguous = preview?.ambiguous ?? 0;

  const handleExecuteClick = () => {
    Modal.confirm({
      title: '전문행사조 이력 master_id 백필 실행',
      content: (
        <div>
          <Paragraph style={{ marginBottom: 8 }}>
            마스터 참조가 비어있는 이관 이력에 원인 마스터를{' '}
            <Text strong>이력 오래된 순으로 최대 {limit.toLocaleString()}건</Text> 연결합니다.
          </Paragraph>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            후보가 둘 이상인 이력은 오연결 방지를 위해 건드리지 않으며, 이미 연결된 이력도 그대로
            둡니다. 대상이 이보다 많으면 잔여가 0 이 될 때까지 다시 실행하세요.
          </Paragraph>
        </div>
      ),
      okText: '백필 실행',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () => executeMutation.mutateAsync(),
    });
  };

  return (
    <div style={{ padding: 24, maxWidth: 720 }}>
      <Title level={3} style={{ marginBottom: 8 }}>
        전문행사조 이력 master_id 백필
      </Title>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        SF 이력 오브젝트에는 마스터 참조 필드가 없어 이관된 전문행사조 이력은 원인 마스터가 비어
        있습니다. 마스터 <Text strong>삭제 / 사원·전문행사조·시작일 수정 차단</Text> 가드가 이 값으로
        &ldquo;사원에 반영된 마스터인가&rdquo; 를 판정하므로, 백필 전에는 이관분 마스터가 보호되지
        않습니다. SYSTEM_ADMIN 권한이 필요합니다.
      </Paragraph>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="매칭 규칙: 같은 사원 + 같은 전문행사조 + 변경 시각(KST)이 마스터 기간 안. 후보가 둘 이상이면 건드리지 않습니다. 이미 채워진 이력은 대상에서 빠지므로 여러 번 실행해도 결과가 같습니다."
      />

      <Descriptions bordered column={1} size="middle" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="연결 가능 (단일 매칭)">
          <Statistic
            value={backfillable}
            valueStyle={{ fontSize: 20 }}
            suffix="건"
            loading={isLoading}
          />
        </Descriptions.Item>
        <Descriptions.Item label="후보 다중 (제외)">
          <Statistic
            value={ambiguous}
            valueStyle={{ fontSize: 20, color: ambiguous > 0 ? '#d46b08' : undefined }}
            suffix="건"
            loading={isLoading}
          />
        </Descriptions.Item>
      </Descriptions>

      {ambiguous > 0 && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="후보가 둘 이상인 이력이 있습니다"
          description="같은 사원에게 같은 조가 거래처만 달리 배정된 경우 등입니다. 이 이력들은 연결되지 않으므로 해당 마스터는 계속 가드 밖에 남습니다. 건수가 크면 매칭 규칙 조정을 검토하세요."
        />
      )}

      <Space style={{ marginBottom: 16 }} wrap>
        <Button onClick={() => refetch()} loading={isLoading}>
          새로고침
        </Button>
        <Space>
          <Text>1회 처리 상한</Text>
          <InputNumber
            min={1}
            max={MAX_LIMIT}
            value={limit}
            onChange={(v) => setLimit(v ?? DEFAULT_LIMIT)}
            style={{ width: 120 }}
          />
        </Space>
        <Button
          type="primary"
          danger
          onClick={handleExecuteClick}
          loading={executeMutation.isPending}
          disabled={backfillable === 0}
        >
          백필 실행
        </Button>
      </Space>

      {lastResult && (
        <>
          <Title level={5} style={{ marginTop: 8 }}>
            최근 실행 결과
          </Title>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="연결 완료">
              <Tag color="green">{lastResult.updated}건</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="잔여 (연결 가능)">
              {lastResult.remaining === 0 ? (
                <Tag color="blue">0건 (완료)</Tag>
              ) : (
                <Tag color="orange">{lastResult.remaining}건 (재실행 필요)</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="후보 다중 (제외)">{lastResult.ambiguous}건</Descriptions.Item>
          </Descriptions>
        </>
      )}
    </div>
  );
}
