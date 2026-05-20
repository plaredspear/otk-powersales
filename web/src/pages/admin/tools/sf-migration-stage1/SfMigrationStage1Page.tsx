import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  Progress,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import {
  useStage1CopyProgress,
  useStage1Targets,
  useStartStage1Copy,
} from '@/hooks/admin/useSfMigrationStage1';
import {
  Stage1AlreadyRunningError,
  type Stage1CopyProgress,
  type Stage1Status,
} from '@/api/admin/sfMigrationStage1';

const { Title, Paragraph, Text } = Typography;

const STATUS_TAG: Record<Stage1Status, { color: string; label: string }> = {
  IDLE: { color: 'default', label: '대기' },
  RUNNING: { color: 'processing', label: '실행 중' },
  COMPLETED: { color: 'success', label: '완료' },
  FAILED: { color: 'error', label: '실패' },
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function formatDuration(start: string | null, end: string | null): string {
  if (!start) return '-';
  const endMs = end ? dayjs(end).valueOf() : Date.now();
  const seconds = Math.max(0, Math.floor((endMs - dayjs(start).valueOf()) / 1000));
  if (seconds < 60) return `${seconds}초`;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}분 ${s}초`;
}

function rowsThroughput(p: Stage1CopyProgress): string {
  if (!p.startedAt) return '-';
  const endMs = p.finishedAt ? dayjs(p.finishedAt).valueOf() : Date.now();
  const seconds = Math.max(1, Math.floor((endMs - dayjs(p.startedAt).valueOf()) / 1000));
  const rps = Math.round(p.processedRows / seconds);
  return `${rps.toLocaleString()} rows/s`;
}

export default function SfMigrationStage1Page() {
  const progressQuery = useStage1CopyProgress();
  const targetsQuery = useStage1Targets();
  const startMutation = useStartStage1Copy();
  const [form] = Form.useForm<{ targetName: string; s3Bucket: string; s3Key: string }>();

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress ? STATUS_TAG[progress.status] : STATUS_TAG.IDLE;
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [alreadyRunning, setAlreadyRunning] = useState(false);

  // 진행 중 row 표시 갱신을 위해 force render 1초 간격 (Statistic 의 elapsed 가 client clock 의존).
  const [, setTick] = useState(0);
  useEffect(() => {
    if (!isRunning) return;
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, [isRunning]);

  const onSubmit = (values: { targetName: string; s3Bucket: string; s3Key: string }) => {
    setSubmitError(null);
    setAlreadyRunning(false);
    startMutation.mutate(values, {
      onError: (err) => {
        if (err instanceof Stage1AlreadyRunningError) {
          setAlreadyRunning(true);
          return;
        }
        setSubmitError(err.message);
      },
    });
  };

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>SF Migration — Stage 1 S3 → COPY 적재</Title>
      <Paragraph type="secondary">
        SF 데이터 마이그레이션 Stage 1 (CSV → PostgreSQL COPY) 을 backend 가 같은 VPC 의
        RDS 에 직접 적재한다. 사용자는 사전에 <Text code>extract-csv.sh</Text> 산출 CSV 를 S3
        에 업로드해야 한다. 대용량 entity (예: ErpOrderProduct ≈ 5GB) 는 수 분~수십 분 소요.
      </Paragraph>

      <Card style={{ marginBottom: 16 }} title="실행">
        <Form
          form={form}
          layout="vertical"
          onFinish={onSubmit}
          disabled={isRunning || startMutation.isPending}
        >
          <Form.Item
            label="Target"
            name="targetName"
            rules={[{ required: true, message: 'target 을 선택하세요' }]}
          >
            <Select
              placeholder="target 선택 (예: ErpOrderProduct)"
              loading={targetsQuery.isLoading}
              options={(targetsQuery.data ?? []).map((t) => ({ value: t, label: t }))}
            />
          </Form.Item>
          <Form.Item
            label="S3 Bucket"
            name="s3Bucket"
            rules={[{ required: true, message: 'S3 bucket 을 입력하세요' }]}
          >
            <Input placeholder="otoki-dev-storage" />
          </Form.Item>
          <Form.Item
            label="S3 Key"
            name="s3Key"
            rules={[{ required: true, message: 'S3 key 를 입력하세요' }]}
          >
            <Input placeholder="sf-migration/input/erp_order_products.csv" />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              loading={startMutation.isPending}
              disabled={isRunning}
            >
              {isRunning ? '진행 중…' : '적재 실행'}
            </Button>
            <Button onClick={() => progressQuery.refetch()} disabled={progressQuery.isFetching}>
              새로 고침
            </Button>
            <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
              상태: {statusTag.label}
            </Tag>
          </Space>
          {alreadyRunning && (
            <Alert
              style={{ marginTop: 12 }}
              type="info"
              showIcon
              message="이미 실행 중입니다"
              description="아래 진행 상태를 확인하세요. 완료 후 다시 실행할 수 있습니다."
              closable
              onClose={() => {
                setAlreadyRunning(false);
                startMutation.reset();
              }}
            />
          )}
          {submitError && (
            <Alert
              style={{ marginTop: 12 }}
              type="error"
              showIcon
              message="실행 요청 실패"
              description={submitError}
              closable
              onClose={() => {
                setSubmitError(null);
                startMutation.reset();
              }}
            />
          )}
        </Form>
      </Card>

      {!progress ? (
        <Empty description="진행 상태 없음 (한 번도 실행되지 않음)" />
      ) : (
        <>
          <Card title="요약" style={{ marginBottom: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2, md: 3 }} bordered size="small">
              <Descriptions.Item label="Target">
                <Text code>{progress.targetName ?? '-'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="S3">
                {progress.s3Bucket && progress.s3Key ? (
                  <Text code>s3://{progress.s3Bucket}/{progress.s3Key}</Text>
                ) : (
                  '-'
                )}
              </Descriptions.Item>
              <Descriptions.Item label="상태">
                <Tag color={statusTag.color}>{statusTag.label}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="시작">
                {formatDateTime(progress.startedAt)}
              </Descriptions.Item>
              <Descriptions.Item label="종료">
                {formatDateTime(progress.finishedAt)}
              </Descriptions.Item>
              <Descriptions.Item label="경과">
                {formatDuration(progress.startedAt, progress.finishedAt)}
              </Descriptions.Item>
            </Descriptions>

            <Space size="large" style={{ marginTop: 16 }} wrap>
              <Statistic
                title="처리 row (CSV 누적)"
                value={progress.processedRows.toLocaleString()}
              />
              <Statistic
                title="필수 필드 누락 (제외)"
                value={progress.filteredOut.toLocaleString()}
              />
              <Statistic
                title="적재된 row (insert 후)"
                value={progress.insertedRows.toLocaleString()}
              />
              <Statistic
                title="처리량"
                value={rowsThroughput(progress)}
              />
            </Space>

            {isRunning && (
              <div style={{ marginTop: 16 }}>
                <Text>진행 (총 row 수 사전 미상 — 누적 표시):</Text>
                <Progress
                  percent={progress.processedRows > 0 ? 50 : 5}
                  status="active"
                  showInfo={false}
                />
              </div>
            )}
            {progress.status === 'COMPLETED' && (
              <div style={{ marginTop: 16 }}>
                <Progress percent={100} status="success" />
              </div>
            )}
            {progress.status === 'FAILED' && (
              <div style={{ marginTop: 16 }}>
                <Progress percent={100} status="exception" />
              </div>
            )}
          </Card>

          {progress.errors.length > 0 && (
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message={`오류 ${progress.errors.length}건`}
              description={
                <ul style={{ paddingLeft: 16, margin: 0 }}>
                  {progress.errors.map((e, idx) => (
                    <li key={idx}>
                      <Text type="secondary">{e}</Text>
                    </li>
                  ))}
                </ul>
              }
            />
          )}
        </>
      )}
    </div>
  );
}
