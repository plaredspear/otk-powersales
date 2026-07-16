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
  executeNameBackfill,
  previewNameBackfill,
  type NameBackfillPreview,
  type NameBackfillResult,
} from '@/api/admin/teamMemberScheduleNameBackfill';

const { Title, Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'team-member-schedule', 'name-backfill', 'preview'] as const;

const DEFAULT_LIMIT = 1000;
const MAX_LIMIT = 5000;

/**
 * 개발자 도구 — 여사원일정 name 백필.
 *
 * 자동 채번(TS{00000000})은 신규 INSERT 부터 적용되므로, 그 이전에 name 이 비어 저장된 기존
 * 일정에 SF AutoNumber 재현값을 소급 부여한다. preview 로 대상 건수를 확인하고, execute 로
 * id 오래된 순 최대 limit 건씩 채번한다. 대상이 많으면 잔여가 0 이 될 때까지 반복 실행한다.
 */
export default function ScheduleNameBackfillPage() {
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState<number>(DEFAULT_LIMIT);
  const [lastResult, setLastResult] = useState<NameBackfillResult | null>(null);

  const {
    data: preview,
    isLoading,
    refetch,
  } = useQuery<NameBackfillPreview>({ queryKey: QUERY_KEY, queryFn: previewNameBackfill });

  const executeMutation = useMutation({
    mutationFn: () => executeNameBackfill(limit),
    onSuccess: (result: NameBackfillResult) => {
      setLastResult(result);
      message.success(`백필 완료 — ${result.updated}건 채번, 잔여 ${result.remaining}건`);
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: Error) => {
      message.error(err.message || '백필 실행에 실패했습니다');
    },
  });

  const missing = preview?.missing ?? 0;

  const handleExecuteClick = () => {
    Modal.confirm({
      title: '여사원일정 name 백필 실행',
      content: (
        <div>
          <Paragraph style={{ marginBottom: 8 }}>
            name 이 비어있는 여사원일정에 <Text strong>TS{'{'}00000000{'}'}</Text> 형식의 채번값을{' '}
            <Text strong>id 오래된 순으로 최대 {limit.toLocaleString()}건</Text> 소급 부여합니다.
          </Paragraph>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            신규 INSERT 채번과 동일한 시퀀스를 공유하므로 번호가 겹치지 않습니다. 대상이 이보다 많으면
            잔여가 0 이 될 때까지 다시 실행하세요.
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
        여사원일정 name 백필
      </Title>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        SF 레거시 <code>DKRetail__TeamMemberSchedule__c.Name</code> AutoNumber(
        <code>TS{'{'}00000000{'}'}</code>) 를 재현하는 자동 채번은 <Text strong>신규 등록부터</Text>{' '}
        적용됩니다. 그 이전에 <code>name</code> 이 비어 저장된 기존 일정에 채번값을 소급 부여하는
        도구입니다. SYSTEM_ADMIN 권한이 필요합니다.
      </Paragraph>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="채번은 신규 INSERT 와 동일한 시퀀스(setval 보정)를 공유합니다. 여러 번 실행해도 번호가 겹치지 않으며, 이미 채워진 일정은 건드리지 않습니다."
      />

      <Descriptions bordered column={1} size="middle" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="채번 필요 건수 (name 비어있음)">
          <Statistic
            value={missing}
            valueStyle={{ fontSize: 20 }}
            suffix="건"
            loading={isLoading}
          />
        </Descriptions.Item>
      </Descriptions>

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
          disabled={missing === 0}
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
            <Descriptions.Item label="조회 대상">{lastResult.processed}건</Descriptions.Item>
            <Descriptions.Item label="채번 완료">
              <Tag color="green">{lastResult.updated}건</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="잔여">
              {lastResult.remaining === 0 ? (
                <Tag color="blue">0건 (완료)</Tag>
              ) : (
                <Tag color="orange">{lastResult.remaining}건 (재실행 필요)</Tag>
              )}
            </Descriptions.Item>
          </Descriptions>
        </>
      )}
    </div>
  );
}
