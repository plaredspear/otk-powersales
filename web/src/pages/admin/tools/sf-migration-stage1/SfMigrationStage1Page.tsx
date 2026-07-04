import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  message,
  Popconfirm,
  Progress,
  Radio,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useResetStage1Copy,
  useStage1CopyProgress,
  useStage1Defaults,
  useStage1Targets,
  useStartStage1Copy,
  useStartStage1CopyAll,
} from '@/hooks/admin/useSfMigrationStage1';
import {
  Stage1AlreadyRunningError,
  type Stage1CopyProgress,
  type Stage1EntityResult,
  type Stage1EntityStatus,
  type Stage1Status,
} from '@/api/admin/sfMigrationStage1';
import ResizableTable from '@/components/common/ResizableTable';

const { Title, Paragraph, Text } = Typography;

const STATUS_TAG: Record<Stage1Status, { color: string; label: string }> = {
  IDLE: { color: 'default', label: '대기' },
  RUNNING: { color: 'processing', label: '실행 중' },
  COMPLETED: { color: 'success', label: '완료' },
  FAILED: { color: 'error', label: '실패' },
};

const ENTITY_STATUS_TAG: Record<Stage1EntityStatus, { color: string; label: string }> = {
  PENDING: { color: 'default', label: '대기' },
  RUNNING: { color: 'processing', label: '실행 중' },
  COMPLETED: { color: 'success', label: '완료' },
  FAILED: { color: 'error', label: '실패' },
  SKIPPED: { color: 'warning', label: '건너뜀' },
};

type RunMode = 'single' | 'batch';
type SampleMode = 'all' | 'sample100k';

const SAMPLE_ROW_LIMIT = 100_000;

/**
 * 클레임 이미지 전용 적재 — 레거시 UploadFile 드롭다운과 분리된 독립 타겟.
 *
 * 클레임 이미지는 SF Files(ContentDocumentLink)에서 추출 → S3 재업로드 후 upload_file 에 적재한다.
 * backend Stage1Targets 의 ClaimImageUploadFile 타겟(csvFileName=claim_upload_files.csv)을 직접
 * 지정해 single copy-from-s3 로 호출한다. 본 타겟은 DEPENDENCY_ORDER 미등록이라 위 드롭다운/일괄에는
 * 나타나지 않는다. bucket 은 defaults 프리필 사용, prefix 만 입력(레거시 upload_files.csv 와 분리).
 */
const CLAIM_IMAGE_TARGET_NAME = 'ClaimImageUploadFile';
const CLAIM_IMAGE_CSV_FILE_NAME = 'claim_upload_files.csv';
const CLAIM_IMAGE_DEFAULT_PREFIX = 'sf-migration/claim-images';

/**
 * 공지 본문 rtaImage 인라인 이미지 전용 적재 — 클레임 이미지 카드와 동일 패턴.
 *
 * 공지 본문 이미지는 SF rich text area 의 인라인 blob 으로 본문 HTML 에 rtaImage URL 로 박혀 있어
 * UploadFile__c / ContentDocumentLink 어디에도 행이 없다. SF 서블릿에서 직접 다운로드 → S3 재업로드 후
 * upload_file 에 적재한다. backend Stage1Targets 의 NoticeImageUploadFile 타겟
 * (csvFileName=notice_image_upload_files.csv)을 직접 지정해 single copy-from-s3 로 호출한다.
 */
const NOTICE_IMAGE_TARGET_NAME = 'NoticeImageUploadFile';
const NOTICE_IMAGE_CSV_FILE_NAME = 'notice_image_upload_files.csv';
const NOTICE_IMAGE_DEFAULT_PREFIX = 'sf-migration/notice-images';

function sampleModeToMaxRows(mode: SampleMode): number | undefined {
  return mode === 'sample100k' ? SAMPLE_ROW_LIMIT : undefined;
}

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
  const defaultsQuery = useStage1Defaults();
  const startSingleMutation = useStartStage1Copy();
  const startBatchMutation = useStartStage1CopyAll();
  const resetMutation = useResetStage1Copy();

  const handleReset = () => {
    resetMutation.mutate(undefined, {
      onSuccess: () => message.success('진행 상태를 초기화했습니다'),
      onError: (err) => message.error(err.message || '상태 초기화에 실패했습니다'),
    });
  };

  const [singleForm] = Form.useForm<{ targetName: string; s3Bucket: string; s3KeyPrefix: string }>();
  const [batchForm] = Form.useForm<{ s3Bucket: string; s3KeyPrefix: string }>();
  const [claimForm] = Form.useForm<{ s3KeyPrefix: string }>();
  const [noticeImageForm] = Form.useForm<{ s3KeyPrefix: string }>();
  const [runMode, setRunMode] = useState<RunMode>('single');
  const [sampleMode, setSampleMode] = useState<SampleMode>('all');

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress ? STATUS_TAG[progress.status] : STATUS_TAG.IDLE;
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [alreadyRunning, setAlreadyRunning] = useState(false);

  // backend 환경값 (S3_BUCKET + 고정 prefix) 으로 두 폼을 프리필 — 사용자 수동 입력 제거.
  // 사용자가 직접 수정한 필드는 덮어쓰지 않도록 최초 1회만 적용.
  const defaults = defaultsQuery.data;
  const [defaultsApplied, setDefaultsApplied] = useState(false);
  useEffect(() => {
    if (!defaults || defaultsApplied) return;
    singleForm.setFieldsValue({
      s3Bucket: defaults.s3Bucket,
      s3KeyPrefix: defaults.s3KeyPrefix,
    });
    batchForm.setFieldsValue({
      s3Bucket: defaults.s3Bucket,
      s3KeyPrefix: defaults.s3KeyPrefix,
    });
    // 클레임 이미지 카드는 prefix 만 입력 — 레거시와 다른 전용 prefix 로 프리필.
    claimForm.setFieldsValue({ s3KeyPrefix: CLAIM_IMAGE_DEFAULT_PREFIX });
    // 공지 본문 이미지 카드도 prefix 만 입력 — 전용 prefix 로 프리필.
    noticeImageForm.setFieldsValue({ s3KeyPrefix: NOTICE_IMAGE_DEFAULT_PREFIX });
    setDefaultsApplied(true);
  }, [defaults, defaultsApplied, singleForm, batchForm, claimForm, noticeImageForm]);

  // 진행 중 row 표시 갱신을 위해 force render 1초 간격 (Statistic 의 elapsed 가 client clock 의존).
  const [, setTick] = useState(0);
  useEffect(() => {
    if (!isRunning) return;
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, [isRunning]);

  const startMutationPending =
    startSingleMutation.isPending || startBatchMutation.isPending;

  // single 모드 최종 S3 key 미리보기 — 선택 target 의 csvFileName 을 prefix 와 조립.
  const watchedTarget = Form.useWatch('targetName', singleForm);
  const watchedPrefix = Form.useWatch('s3KeyPrefix', singleForm);
  const selectedCsvFileName = (targetsQuery.data ?? []).find(
    (t) => t.targetName === watchedTarget,
  )?.csvFileName;
  const previewS3Key =
    watchedPrefix && selectedCsvFileName
      ? `${watchedPrefix.replace(/\/+$/, '')}/${selectedCsvFileName}`
      : null;

  // 클레임 이미지 카드 — 최종 S3 key 미리보기 (prefix + 고정 csvFileName).
  const watchedClaimPrefix = Form.useWatch('s3KeyPrefix', claimForm);
  const claimPreviewS3Key = watchedClaimPrefix
    ? `${watchedClaimPrefix.replace(/\/+$/, '')}/${CLAIM_IMAGE_CSV_FILE_NAME}`
    : null;

  // 공지 본문 이미지 카드 — 최종 S3 key 미리보기 (prefix + 고정 csvFileName).
  const watchedNoticeImagePrefix = Form.useWatch('s3KeyPrefix', noticeImageForm);
  const noticeImagePreviewS3Key = watchedNoticeImagePrefix
    ? `${watchedNoticeImagePrefix.replace(/\/+$/, '')}/${NOTICE_IMAGE_CSV_FILE_NAME}`
    : null;

  const resetSubmitState = () => {
    setSubmitError(null);
    setAlreadyRunning(false);
  };

  const onSubmitSingle = (values: {
    targetName: string;
    s3Bucket: string;
    s3KeyPrefix: string;
  }) => {
    resetSubmitState();
    startSingleMutation.mutate(
      { ...values, maxRows: sampleModeToMaxRows(sampleMode) },
      {
        onError: (err) => {
          if (err instanceof Stage1AlreadyRunningError) {
            setAlreadyRunning(true);
            return;
          }
          setSubmitError(err.message);
        },
      },
    );
  };

  const onSubmitBatch = (values: { s3Bucket: string; s3KeyPrefix: string }) => {
    resetSubmitState();
    startBatchMutation.mutate(
      { ...values, maxRows: sampleModeToMaxRows(sampleMode) },
      {
        onError: (err) => {
          if (err instanceof Stage1AlreadyRunningError) {
            setAlreadyRunning(true);
            return;
          }
          setSubmitError(err.message);
        },
      },
    );
  };

  // 클레임 이미지 적재 — targetName 하드코딩(ClaimImageUploadFile), bucket 은 defaults 프리필 사용.
  const onSubmitClaim = (values: { s3KeyPrefix: string }) => {
    resetSubmitState();
    startSingleMutation.mutate(
      {
        targetName: CLAIM_IMAGE_TARGET_NAME,
        s3Bucket: defaults?.s3Bucket ?? '',
        s3KeyPrefix: values.s3KeyPrefix,
        maxRows: sampleModeToMaxRows(sampleMode),
      },
      {
        onError: (err) => {
          if (err instanceof Stage1AlreadyRunningError) {
            setAlreadyRunning(true);
            return;
          }
          setSubmitError(err.message);
        },
      },
    );
  };

  // 공지 본문 이미지 적재 — targetName 하드코딩(NoticeImageUploadFile), bucket 은 defaults 프리필 사용.
  const onSubmitNoticeImage = (values: { s3KeyPrefix: string }) => {
    resetSubmitState();
    startSingleMutation.mutate(
      {
        targetName: NOTICE_IMAGE_TARGET_NAME,
        s3Bucket: defaults?.s3Bucket ?? '',
        s3KeyPrefix: values.s3KeyPrefix,
        maxRows: sampleModeToMaxRows(sampleMode),
      },
      {
        onError: (err) => {
          if (err instanceof Stage1AlreadyRunningError) {
            setAlreadyRunning(true);
            return;
          }
          setSubmitError(err.message);
        },
      },
    );
  };

  const entityColumns: ColumnsType<Stage1EntityResult> = [
    {
      title: '#',
      key: 'index',
      width: 50,
      render: (_v, _r, idx) => idx + 1,
    },
    {
      title: 'Target',
      dataIndex: 'targetName',
      key: 'targetName',
      render: (v: string) => <Text code>{v}</Text>,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (v: Stage1EntityStatus) => {
        const tag = ENTITY_STATUS_TAG[v];
        return <Tag color={tag.color}>{tag.label}</Tag>;
      },
    },
    {
      title: 'S3 Key',
      dataIndex: 's3Key',
      key: 's3Key',
      ellipsis: true,
      render: (v: string | null) => (v ? <Text code>{v}</Text> : '-'),
    },
    {
      title: '처리 row',
      dataIndex: 'processedRows',
      key: 'processedRows',
      width: 110,
      align: 'right',
      render: (v: number) => v.toLocaleString(),
    },
    {
      title: '제외',
      dataIndex: 'filteredOut',
      key: 'filteredOut',
      width: 90,
      align: 'right',
      render: (v: number) => v.toLocaleString(),
    },
    {
      title: '적재',
      dataIndex: 'insertedRows',
      key: 'insertedRows',
      width: 110,
      align: 'right',
      render: (v: number) => v.toLocaleString(),
    },
    {
      title: '경과',
      key: 'elapsed',
      width: 100,
      render: (_v, r) => formatDuration(r.startedAt, r.finishedAt),
    },
    {
      title: '실패 사유',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      render: (v: string | null) =>
        v ? <Text type="danger" style={{ whiteSpace: 'pre-wrap' }}>{v}</Text> : '-',
    },
  ];

  const isBatch = progress?.mode === 'BATCH';

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>SF Migration — Stage 1 S3 → COPY 적재</Title>
      <Paragraph type="secondary">
        SF 데이터 마이그레이션 Stage 1 (CSV → PostgreSQL COPY) 을 backend 가 같은 VPC 의
        RDS 에 직접 적재한다. 사용자는 사전에 <Text code>extract-csv.sh</Text> 산출 CSV 를 S3
        에 업로드해야 한다. 대용량 entity (예: ErpOrderProduct ≈ 5GB) 는 수 분~수십 분 소요.
      </Paragraph>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="현재 적재 대상 S3 위치"
        description={
          <Descriptions column={1} size="small" colon={false}>
            <Descriptions.Item label="S3 Bucket (환경변수 S3_BUCKET)">
              {defaults?.s3Bucket ? (
                <Text code>{defaults.s3Bucket}</Text>
              ) : (
                <Text type="warning">미설정 (backend S3_BUCKET 환경변수 확인 필요)</Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="CSV 경로 (prefix)">
              <Text code>{defaults?.s3KeyPrefix ?? '-'}/</Text>
            </Descriptions.Item>
          </Descriptions>
        }
      />

      <Card style={{ marginBottom: 16 }} title="실행">
        <Radio.Group
          value={runMode}
          onChange={(e) => {
            setRunMode(e.target.value as RunMode);
            resetSubmitState();
          }}
          optionType="button"
          buttonStyle="solid"
          disabled={isRunning || startMutationPending}
          style={{ marginBottom: 16 }}
        >
          <Radio.Button value="single">개별 실행 (target 1개)</Radio.Button>
          <Radio.Button value="batch">일괄 실행 (전체 entity)</Radio.Button>
        </Radio.Group>

        <div style={{ marginBottom: 16 }}>
          <Text style={{ marginRight: 12 }}>적재 범위:</Text>
          <Radio.Group
            value={sampleMode}
            onChange={(e) => {
              setSampleMode(e.target.value as SampleMode);
              resetSubmitState();
            }}
            optionType="button"
            buttonStyle="solid"
            disabled={isRunning || startMutationPending}
          >
            <Radio.Button value="all">전체</Radio.Button>
            <Radio.Button value="sample100k">
              Sample {SAMPLE_ROW_LIMIT.toLocaleString()} rows
            </Radio.Button>
          </Radio.Group>
          {sampleMode === 'sample100k' && (
            <Text type="secondary" style={{ marginLeft: 12 }}>
              CSV 의 앞 {SAMPLE_ROW_LIMIT.toLocaleString()} row 만 적재 (filterOut 포함). 실제 적재 row 는 그 이하일 수 있음.
            </Text>
          )}
        </div>

        {runMode === 'single' ? (
          <Form
            form={singleForm}
            layout="vertical"
            onFinish={onSubmitSingle}
            disabled={isRunning || startMutationPending}
          >
            <Form.Item
              label="Target"
              name="targetName"
              rules={[{ required: true, message: 'target 을 선택하세요' }]}
            >
              <Select
                placeholder="target 선택 (예: ErpOrderProduct)"
                loading={targetsQuery.isLoading}
                showSearch
                options={[...(targetsQuery.data ?? [])]
                  .sort((a, b) => a.targetName.localeCompare(b.targetName))
                  .map((t) => ({ value: t.targetName, label: t.targetName }))}
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
              label="S3 Key Prefix"
              name="s3KeyPrefix"
              tooltip="파일명은 선택한 target 의 csvFileName 으로 자동 조립됩니다 (<prefix>/<csvFileName>)."
              rules={[{ required: true, message: 'S3 key prefix 를 입력하세요' }]}
              extra={
                previewS3Key ? (
                  <Text type="secondary">
                    최종 S3 key: <Text code>{previewS3Key}</Text>
                  </Text>
                ) : selectedCsvFileName ? (
                  <Text type="secondary">
                    파일명: <Text code>{selectedCsvFileName}</Text> (prefix 입력 시 전체 경로 표시)
                  </Text>
                ) : undefined
              }
            >
              <Input placeholder="sf-migration/input" />
            </Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={startSingleMutation.isPending}
                disabled={isRunning}
              >
                {isRunning ? '진행 중…' : '적재 실행'}
              </Button>
              <Button onClick={() => progressQuery.refetch()} disabled={progressQuery.isFetching}>
                새로 고침
              </Button>
              {isRunning && (
                <Popconfirm
                  title="진행 상태 초기화"
                  description="인스턴스 재시작 등으로 이미 중단된 작업이 '실행 중'으로 남은 경우에만 사용하세요. 다른 인스턴스에서 실제 적재가 진행 중이면 그 진행 표시가 사라집니다."
                  okText="초기화"
                  cancelText="취소"
                  onConfirm={handleReset}
                >
                  <Button danger loading={resetMutation.isPending}>
                    상태 초기화
                  </Button>
                </Popconfirm>
              )}
              <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
                상태: {statusTag.label}
              </Tag>
            </Space>
          </Form>
        ) : (
          <Form
            form={batchForm}
            layout="vertical"
            onFinish={onSubmitBatch}
            disabled={isRunning || startMutationPending}
          >
            <Form.Item
              label="S3 Bucket"
              name="s3Bucket"
              rules={[{ required: true, message: 'S3 bucket 을 입력하세요' }]}
            >
              <Input placeholder="otoki-dev-storage" />
            </Form.Item>
            <Form.Item
              label="S3 Key Prefix"
              name="s3KeyPrefix"
              tooltip="각 entity 의 S3 key 는 <prefix>/<csvFileName> 으로 자동 조립됩니다."
              rules={[{ required: true, message: 'S3 key prefix 를 입력하세요' }]}
            >
              <Input placeholder="sf-migration/input" />
            </Form.Item>
            <Paragraph type="secondary" style={{ marginBottom: 16 }}>
              등록된 {(targetsQuery.data ?? []).length} 개 entity 를 의존성 순서대로 순차 적재합니다.
              1개 entity 가 실패하면 즉시 중단되고 나머지는 SKIPPED 로 표시됩니다.
            </Paragraph>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={startBatchMutation.isPending}
                disabled={isRunning}
              >
                {isRunning ? '진행 중…' : '일괄 실행'}
              </Button>
              <Button onClick={() => progressQuery.refetch()} disabled={progressQuery.isFetching}>
                새로 고침
              </Button>
              {isRunning && (
                <Popconfirm
                  title="진행 상태 초기화"
                  description="인스턴스 재시작 등으로 이미 중단된 작업이 '실행 중'으로 남은 경우에만 사용하세요. 다른 인스턴스에서 실제 적재가 진행 중이면 그 진행 표시가 사라집니다."
                  okText="초기화"
                  cancelText="취소"
                  onConfirm={handleReset}
                >
                  <Button danger loading={resetMutation.isPending}>
                    상태 초기화
                  </Button>
                </Popconfirm>
              )}
              <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
                상태: {statusTag.label}
              </Tag>
            </Space>
          </Form>
        )}

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
              startSingleMutation.reset();
              startBatchMutation.reset();
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
              startSingleMutation.reset();
              startBatchMutation.reset();
            }}
          />
        )}
      </Card>

      <Card style={{ marginBottom: 16 }} title="클레임 이미지 적재">
        <Paragraph type="secondary">
          클레임 첨부 이미지는 레거시 <Text code>UploadFile__c</Text> 추출(위 드롭다운의{' '}
          <Text code>UploadFile</Text>)에 포함되지 않는다. SF Files(ContentDocumentLink)에서
          추출해 S3 업로드한 뒤 <Text code>{CLAIM_IMAGE_CSV_FILE_NAME}</Text> 를 같은{' '}
          <Text code>upload_file</Text> 테이블에 적재한다(전용 타겟{' '}
          <Text code>{CLAIM_IMAGE_TARGET_NAME}</Text>). 적재 후 아래 진행 상태로 결과를 확인하고,
          Stage 2 의 <Text code>UploadFile Parent Resolve</Text> 로 부모(클레임)를 연결한다.
        </Paragraph>
        <Form
          form={claimForm}
          layout="vertical"
          onFinish={onSubmitClaim}
          disabled={isRunning || startMutationPending}
        >
          <Form.Item
            label="S3 Key Prefix"
            name="s3KeyPrefix"
            tooltip={`파일명은 ${CLAIM_IMAGE_CSV_FILE_NAME} 으로 고정됩니다 (<prefix>/${CLAIM_IMAGE_CSV_FILE_NAME}).`}
            rules={[{ required: true, message: 'S3 key prefix 를 입력하세요' }]}
            extra={
              claimPreviewS3Key ? (
                <Text type="secondary">
                  최종 S3 key: <Text code>{claimPreviewS3Key}</Text>
                </Text>
              ) : undefined
            }
          >
            <Input placeholder={CLAIM_IMAGE_DEFAULT_PREFIX} />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              loading={startSingleMutation.isPending}
              disabled={isRunning}
            >
              {isRunning ? '진행 중…' : '클레임 이미지 적재'}
            </Button>
            <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
              상태: {statusTag.label}
            </Tag>
          </Space>
        </Form>
      </Card>

      <Card style={{ marginBottom: 16 }} title="공지 본문 이미지 적재">
        <Paragraph type="secondary">
          공지 본문(rich text) 인라인 이미지는 SF 표준 rtaImage 서블릿 URL 로 본문에 박혀 있어 레거시{' '}
          <Text code>UploadFile__c</Text> 추출(위 드롭다운의 <Text code>UploadFile</Text>)에도, 클레임
          이미지(ContentDocumentLink 경유)에도 포함되지 않는다. SF 서블릿에서 직접 추출해 S3 업로드한 뒤{' '}
          <Text code>{NOTICE_IMAGE_CSV_FILE_NAME}</Text> 를 같은 <Text code>upload_file</Text> 테이블에
          적재한다(전용 타겟 <Text code>{NOTICE_IMAGE_TARGET_NAME}</Text>). 적재 후 Stage 2 의{' '}
          <Text code>UploadFile Parent Resolve</Text> 로 부모(공지)를 연결하고, 본문 HTML 의 rtaImage URL
          치환은 별도 스크립트로 수행한다.
        </Paragraph>
        <Form
          form={noticeImageForm}
          layout="vertical"
          onFinish={onSubmitNoticeImage}
          disabled={isRunning || startMutationPending}
        >
          <Form.Item
            label="S3 Key Prefix"
            name="s3KeyPrefix"
            tooltip={`파일명은 ${NOTICE_IMAGE_CSV_FILE_NAME} 으로 고정됩니다 (<prefix>/${NOTICE_IMAGE_CSV_FILE_NAME}).`}
            rules={[{ required: true, message: 'S3 key prefix 를 입력하세요' }]}
            extra={
              noticeImagePreviewS3Key ? (
                <Text type="secondary">
                  최종 S3 key: <Text code>{noticeImagePreviewS3Key}</Text>
                </Text>
              ) : undefined
            }
          >
            <Input placeholder={NOTICE_IMAGE_DEFAULT_PREFIX} />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              loading={startSingleMutation.isPending}
              disabled={isRunning}
            >
              {isRunning ? '진행 중…' : '공지 본문 이미지 적재'}
            </Button>
            <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
              상태: {statusTag.label}
            </Tag>
          </Space>
        </Form>
      </Card>

      {!progress ? (
        <Empty description="진행 상태 없음 (한 번도 실행되지 않음)" />
      ) : (
        <>
          <Card title={isBatch ? '요약 (일괄)' : '요약'} style={{ marginBottom: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2, md: 3 }} bordered size="small">
              <Descriptions.Item label="모드">
                <Tag>{isBatch ? '일괄 실행 (BATCH)' : '개별 실행 (SINGLE)'}</Tag>
              </Descriptions.Item>
              {isBatch ? (
                <Descriptions.Item label="현재 처리 중">
                  {progress.targetName ? <Text code>{progress.targetName}</Text> : '-'}
                </Descriptions.Item>
              ) : (
                <Descriptions.Item label="Target">
                  <Text code>{progress.targetName ?? '-'}</Text>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="S3">
                {progress.s3Bucket && progress.s3Key ? (
                  <Text code>s3://{progress.s3Bucket}/{progress.s3Key}</Text>
                ) : progress.s3Bucket ? (
                  <Text code>s3://{progress.s3Bucket}/</Text>
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
                title={isBatch ? '처리 row (전체 누적)' : '처리 row (CSV 누적)'}
                value={progress.processedRows.toLocaleString()}
              />
              <Statistic
                title="필수 필드 누락 (제외)"
                value={progress.filteredOut.toLocaleString()}
              />
              <Statistic
                title={isBatch ? '적재된 row (전체 누적)' : '적재된 row (insert 후)'}
                value={progress.insertedRows.toLocaleString()}
              />
              <Statistic title="처리량" value={rowsThroughput(progress)} />
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

          {isBatch && progress.entityResults.length > 0 && (
            <Card title={`Entity 별 결과 (${progress.entityResults.length})`} style={{ marginBottom: 16 }}>
              <ResizableTable<Stage1EntityResult>
                rowKey="targetName"
                dataSource={progress.entityResults}
                columns={entityColumns}
                pagination={false}
                size="small"
                rowClassName={(r) =>
                  r.status === 'FAILED' ? 'ant-table-row-stage1-failed' : ''
                }
              />
            </Card>
          )}

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
