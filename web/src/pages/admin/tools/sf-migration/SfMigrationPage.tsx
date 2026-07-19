import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
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
  useFkResolvableTables,
  useFkResolveProgress,
  useRunLeaderProfileFlags,
  useRunNaturalKeyFkResolve,
  useRunNoticeRtaPlaceholder,
  useRunPasswordHash,
  useRunPicklistAll,
  useRunPicklistColumn,
  useRunSharingRecalcAll,
  useRunUploadFilePolymorphicParent,
  useRunUserProfileSfidReconcile,
  useRunUserRoleHierarchyRecalc,
  useSharingRecalcStatus,
  useStartFkResolve,
} from '@/hooks/admin/useSfMigration';
import type {
  FkResolveProgress,
  FkResolveStatus,
  FkResolveTableResult,
  LeaderProfileFlagsSubstepResult,
  NaturalKeyFkSubstepResult,
  NoticeRtaPlaceholderSubstepResult,
  PasswordHashSubstepResult,
  PicklistSubstepResult,
  UploadFileParentSubstepResult,
  UserProfileReconcileSubstepResult,
} from '@/api/admin/sfMigration';
import ResizableTable from '@/components/common/ResizableTable';

const { Title, Paragraph, Text } = Typography;

const STATUS_TAG: Record<FkResolveStatus, { color: string; label: string }> = {
  IDLE: { color: 'default', label: '대기' },
  RUNNING: { color: 'processing', label: '실행 중' },
  COMPLETED: { color: 'success', label: '완료' },
  COMPLETED_WITH_WARNINGS: { color: 'warning', label: '완료 (경고 있음)' },
  FAILED: { color: 'error', label: '실패' },
};

const UNKNOWN_STATUS_TAG = { color: 'default', label: '알 수 없음' };

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

function overallPercent(p: FkResolveProgress): number {
  if (p.totalTables === 0) return 0;
  return Math.min(100, Math.round((p.completedTables / p.totalTables) * 100));
}

function currentTablePercent(p: FkResolveProgress): number {
  if (p.currentTableTotalChunks === 0) return 0;
  return Math.min(
    100,
    Math.round((p.currentTableChunk / p.currentTableTotalChunks) * 100),
  );
}

export default function SfMigrationPage() {
  const progressQuery = useFkResolveProgress();
  const tablesQuery = useFkResolvableTables();
  const startMutation = useStartFkResolve();

  // 단일 테이블 실행용 선택값 (미선택 시 undefined → '전체 실행' 만 가능).
  const [selectedTable, setSelectedTable] = useState<string | undefined>(undefined);
  const runPicklistAllMutation = useRunPicklistAll();
  const runPicklistColumnMutation = useRunPicklistColumn();
  const runNaturalKeyFkMutation = useRunNaturalKeyFkResolve();
  const runUploadFileParentMutation = useRunUploadFilePolymorphicParent();
  const runHierarchyRecalcMutation = useRunUserRoleHierarchyRecalc();
  const runProfileReconcileMutation = useRunUserProfileSfidReconcile();
  const runLeaderProfileFlagsMutation = useRunLeaderProfileFlags();
  const runPasswordHashMutation = useRunPasswordHash();
  const runSharingRecalcMutation = useRunSharingRecalcAll();
  const sharingRecalcStatusQuery = useSharingRecalcStatus();
  const runNoticeRtaMutation = useRunNoticeRtaPlaceholder();

  // 공지 본문 placeholder 치환은 비가역 UPDATE — 기본 dry-run, apply 명시 선택 시에만 실제 변경.
  const [noticeRtaApply, setNoticeRtaApply] = useState(false);

  const progress = progressQuery.data;
  const isRunning = progress?.status === 'RUNNING';
  const statusTag = progress
    ? (STATUS_TAG[progress.status] ?? UNKNOWN_STATUS_TAG)
    : STATUS_TAG.IDLE;

  // Stage 2-B 는 전체 실행(picklistAll) + 개별 컬럼 실행(picklistColumn) 두 경로를 공유한다.
  // 가장 최근에 끝난 mutation 의 결과를 표시 (submittedAt 비교 — 0 이면 미실행).
  const lastAllAt = runPicklistAllMutation.submittedAt ?? 0;
  const lastColumnAt = runPicklistColumnMutation.submittedAt ?? 0;
  const latestPicklist = lastAllAt >= lastColumnAt ? runPicklistAllMutation : runPicklistColumnMutation;
  const picklistResult = latestPicklist.data;
  const picklistError = latestPicklist.error as Error | null;
  const picklistPending = runPicklistAllMutation.isPending || runPicklistColumnMutation.isPending;

  const naturalKeyResult = runNaturalKeyFkMutation.data;
  const naturalKeyError = runNaturalKeyFkMutation.error as Error | null;
  const naturalKeyPending = runNaturalKeyFkMutation.isPending;

  const uploadFileParentResult = runUploadFileParentMutation.data;
  const uploadFileParentError = runUploadFileParentMutation.error as Error | null;
  const uploadFileParentPending = runUploadFileParentMutation.isPending;

  const hierarchyResult = runHierarchyRecalcMutation.data;
  const hierarchyError = runHierarchyRecalcMutation.error as Error | null;
  const hierarchyPending = runHierarchyRecalcMutation.isPending;

  const profileReconcileResult = runProfileReconcileMutation.data;
  const profileReconcileError = runProfileReconcileMutation.error as Error | null;
  const profileReconcilePending = runProfileReconcileMutation.isPending;

  const leaderProfileFlagsResult = runLeaderProfileFlagsMutation.data;
  const leaderProfileFlagsError = runLeaderProfileFlagsMutation.error as Error | null;
  const leaderProfileFlagsPending = runLeaderProfileFlagsMutation.isPending;

  const passwordHashResult = runPasswordHashMutation.data;
  const passwordHashError = runPasswordHashMutation.error as Error | null;
  const passwordHashPending = runPasswordHashMutation.isPending;

  const sharingRecalcResult = runSharingRecalcMutation.data;
  const sharingRecalcError = runSharingRecalcMutation.error as Error | null;
  const sharingRecalcPending = runSharingRecalcMutation.isPending;
  const sharingRecalcStatus = sharingRecalcStatusQuery.data;
  const sharingRecalcStatusError = sharingRecalcStatusQuery.error as Error | null;

  const noticeRtaResult = runNoticeRtaMutation.data;
  const noticeRtaError = runNoticeRtaMutation.error as Error | null;
  const noticeRtaPending = runNoticeRtaMutation.isPending;

  const tableColumns: ColumnsType<FkResolveTableResult> = useMemo(
    () => [
      {
        title: '테이블',
        dataIndex: 'label',
        key: 'label',
        ellipsis: true,
      },
      {
        title: '적용 row 수',
        dataIndex: 'rowsAffected',
        key: 'rowsAffected',
        width: 160,
        align: 'right',
        render: (v: number) => v.toLocaleString(),
      },
    ],
    [],
  );

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>SF Migration — Stage 2 FK Resolve</Title>
      <Paragraph type="secondary">
        SF 데이터 마이그레이션 Stage 2-A (FK Resolve) 를 실행하고 진행 상태를 1초 간격으로 확인한다.
        본 작업은 1회성 cut-over 도구이며, ErpOrderProduct 등 대용량 테이블 처리 시간이 길어질 수 있다.
        전체 실행 외에, 특정 테이블만 골라 단일 실행할 수 있다 (대용량 테이블 재처리 / 부분 재시도용).
        단일/전체 실행 모두 동일한 진행 상태 추적을 공유하며, 실행 중에는 중복 실행이 차단된다.
      </Paragraph>

      <Card style={{ marginBottom: 16 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space size="middle" wrap>
            <Tag color={statusTag.color} style={{ fontSize: 14, padding: '4px 12px' }}>
              상태: {statusTag.label}
            </Tag>
            <Button
              type="primary"
              loading={startMutation.isPending && !selectedTable}
              disabled={isRunning}
              onClick={() => startMutation.mutate(undefined)}
            >
              {isRunning ? '진행 중…' : '전체 실행'}
            </Button>
            <Select
              style={{ minWidth: 280 }}
              placeholder="테이블 선택 (단일 실행)"
              allowClear
              showSearch
              value={selectedTable}
              onChange={(v) => setSelectedTable(v)}
              loading={tablesQuery.isLoading}
              options={(tablesQuery.data ?? []).map((t) => ({ label: t, value: t }))}
              disabled={isRunning}
            />
            <Button
              loading={startMutation.isPending && !!selectedTable}
              disabled={isRunning || !selectedTable}
              onClick={() => startMutation.mutate(selectedTable)}
            >
              선택 테이블만 실행
            </Button>
            <Button
              onClick={() => progressQuery.refetch()}
              disabled={progressQuery.isFetching}
            >
              새로 고침
            </Button>
          </Space>

          {startMutation.isError && (
            <Alert
              type="error"
              showIcon
              message="실행 요청 실패"
              description={(startMutation.error as Error)?.message ?? 'unknown'}
              closable
              onClose={() => startMutation.reset()}
            />
          )}
        </Space>
      </Card>

      {!progress ? (
        <Empty description="진행 상태 없음 (한 번도 실행되지 않음)" />
      ) : (
        <>
          <Card title="요약" style={{ marginBottom: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2, md: 3 }} bordered size="small">
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
                title="처리 테이블"
                value={`${progress.completedTables} / ${progress.totalTables}`}
              />
              <Statistic
                title="누적 적용 row"
                value={progress.totalRowsAffected.toLocaleString()}
              />
            </Space>

            <div style={{ marginTop: 16 }}>
              <Text>전체 진행률 ({progress.completedTables}/{progress.totalTables} 테이블)</Text>
              <Progress
                percent={overallPercent(progress)}
                status={
                  progress.status === 'FAILED'
                    ? 'exception'
                    : progress.status === 'COMPLETED'
                    ? 'success'
                    : progress.status === 'COMPLETED_WITH_WARNINGS'
                    ? 'normal'
                    : 'active'
                }
              />
            </div>

            {progress.currentTable && (
              <div style={{ marginTop: 8 }}>
                <Text>
                  현재 테이블: <Text code>{progress.currentTable}</Text>
                  {' '}
                  ({progress.currentTableChunk}/{progress.currentTableTotalChunks} chunks)
                </Text>
                <Progress
                  percent={currentTablePercent(progress)}
                  status={isRunning ? 'active' : 'normal'}
                />
              </div>
            )}
          </Card>

          {progress.errors.length > 0 && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={`경고 / 오류 ${progress.errors.length}건`}
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

          <Card title="테이블별 결과">
            <ResizableTable<FkResolveTableResult>
              size="small"
              rowKey="label"
              columns={tableColumns}
              dataSource={progress.tableResults}
              pagination={false}
              locale={{ emptyText: '아직 완료된 테이블 없음' }}
            />
          </Card>
        </>
      )}

      <Card title="Natural Key FK 해소" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          sfid prefix 가 아닌 자연 키 (developer_name / name / 외부 sfid 컬럼) 기반으로
          FK id 컬럼을 채운다. <Text code>permission_set_flags</Text> /{' '}
          <Text code>permission_set_assignment</Text> / <Text code>profile_flags</Text> /{' '}
          <Text code>sharing_rule_target</Text> 등의 권한 평탄화에 필요한 FK 가 여기서
          연결된다. <Text strong>위 FK Resolve 가 완료된 후</Text> 1회 실행. 동기 실행 —
          보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={naturalKeyPending}
            disabled={naturalKeyPending}
            onClick={() => {
              runNaturalKeyFkMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {naturalKeyError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="Natural Key FK 해소 실패"
            description={naturalKeyError.message}
            closable
            onClose={() => {
              runNaturalKeyFkMutation.reset();
            }}
          />
        )}

        {naturalKeyResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{naturalKeyResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="누적 적용 row">
                {naturalKeyResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<NaturalKeyFkSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '대상', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={naturalKeyResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="UploadFile Parent Resolve" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          Stage1 이 적재한 <Text code>object_type</Text> (SF Object__c 원본) 기준으로{' '}
          <Text code>parent_type</Text> 을 파생하고, <Text code>(parent_type, record_sfid)</Text>{' '}
          → <Text code>parent_id</Text> (Long FK) 를 채운다.{' '}
          <Text code>claim</Text> / <Text code>notice</Text> / <Text code>proposal</Text> /{' '}
          <Text code>site_activity</Text> 4종 polymorphic. <Text code>record_sfid</Text> 는 일반 FK
          Resolve 가 건드리지 않으므로, <Text strong>Stage1 적재 완료 후</Text> 본 substep 을 1회
          실행해야 첨부 이미지 조회가 연결된다. 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={uploadFileParentPending}
            disabled={uploadFileParentPending}
            onClick={() => {
              runUploadFileParentMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {uploadFileParentError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="UploadFile Parent Resolve 실패"
            description={uploadFileParentError.message}
            closable
            onClose={() => {
              runUploadFileParentMutation.reset();
            }}
          />
        )}

        {uploadFileParentResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{uploadFileParentResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="누적 적용 row">
                {uploadFileParentResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<UploadFileParentSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '대상', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={uploadFileParentResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="공지 본문 이미지 placeholder 치환" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          공지 본문(<Text code>notice.contents</Text>)에 박힌 SF rtaImage 서블릿 URL{' '}
          <Text code>&lt;img&gt;</Text> 태그를 만료 없는 placeholder{' '}
          <Text code>&lt;img src="notice-image://{'{refid}'}" data-refid="{'{refid}'}"&gt;</Text> 로
          치환한다. 조회 시점에 백엔드가 <Text code>data-refid</Text> 로 presigned URL 을 rewrite
          하므로 본문엔 placeholder 만 영구 저장된다. <Text strong>공지 본문 이미지 적재(Stage1
          NoticeImageUploadFile) + UploadFile Parent Resolve 완료 후</Text> 1회 실행. 멱등 — 이미
          치환된 본문은 skip. <Text strong>비가역 UPDATE</Text> 이므로 먼저 dry-run 으로 변경 규모를
          확인한 뒤 apply 한다.
        </Paragraph>

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Radio.Group
            optionType="button"
            buttonStyle="solid"
            value={noticeRtaApply ? 'apply' : 'dry-run'}
            onChange={(e) => setNoticeRtaApply(e.target.value === 'apply')}
            disabled={noticeRtaPending}
          >
            <Radio.Button value="dry-run">Dry-run (변경 대상만 집계)</Radio.Button>
            <Radio.Button value="apply">Apply (실제 UPDATE)</Radio.Button>
          </Radio.Group>

          <Space>
            <Button
              type="primary"
              danger={noticeRtaApply}
              loading={noticeRtaPending}
              disabled={noticeRtaPending}
              onClick={() => {
                runNoticeRtaMutation.mutate(!noticeRtaApply);
              }}
            >
              {noticeRtaApply ? '실행 (Apply)' : '실행 (Dry-run)'}
            </Button>
          </Space>
        </Space>

        {noticeRtaError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="공지 본문 placeholder 치환 실패"
            description={noticeRtaError.message}
            closable
            onClose={() => {
              runNoticeRtaMutation.reset();
            }}
          />
        )}

        {noticeRtaResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{noticeRtaResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="치환 이미지 수">
                {noticeRtaResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<NoticeRtaPlaceholderSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '항목', dataIndex: 'label', key: 'label' },
                {
                  title: '건수',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={noticeRtaResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="UserRole Hierarchy 재계산" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          <Text code>user_role_hierarchy_snapshot</Text> 의{' '}
          <Text code>all_subordinate_ids</Text> / <Text code>depth</Text> /{' '}
          <Text code>ancestor_path</Text> / <Text code>snapshot_at</Text> 을{' '}
          <Text code>user_role.parent_user_role_id</Text> 트리 기반으로 재계산.{' '}
          <Text strong>위 Natural Key FK 해소가 완료된 후</Text> 1회 실행 — 미실행 시{' '}
          <Text code>depth</Text> NULL 인 row 가 권한 평가 path 에서 Hibernate primitive
          setter 호출 시점에 NPE 유발 (예: <Text code>/api/v1/admin/accounts</Text>{' '}
          500). 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={hierarchyPending}
            disabled={hierarchyPending}
            onClick={() => {
              runHierarchyRecalcMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {hierarchyError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="UserRole Hierarchy 재계산 실패"
            description={hierarchyError.message}
            closable
            onClose={() => {
              runHierarchyRecalcMutation.reset();
            }}
          />
        )}

        {hierarchyResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{hierarchyResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="결과">
                <Text type="success">snapshot 재계산 완료 (상세 row 수는 서버 logger 참조)</Text>
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Card>

      <Card title="User Profile 정합 (SF profile_sfid override)" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          <Text code>user.profile_id</Text> 를 SF <Text code>profile_sfid</Text> 기준으로 최종
          정합한다. 일반 FK Resolve 는 <Text code>profile_id = COALESCE(...)</Text> +{' '}
          <Text code>WHERE profile_id IS NULL</Text> 가드라, provisioning 이 이미 채운 값(fallback{' '}
          <Text code>5.영업사원</Text> / 옛 <Text code>9. Staff</Text>)을 덮어쓰지 못한다. 본
          substep 은 <Text code>profile_sfid → profile.sfid</Text> 조인으로 SF 정답 profile_id 를
          산출해 <Text strong>무조건 override</Text> 한다 — SF User.Profile 이 최종 권위. 멱등 —
          이미 일치하는 row 는 skip. 단 <Text code>시스템 관리자</Text> 로 격상된 계정은 override
          대상에서 제외(관리자 권한 보존). <Text strong>위 FK Resolve 완료 후</Text> 1회 실행. 동기
          실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={profileReconcilePending}
            disabled={profileReconcilePending}
            onClick={() => {
              runProfileReconcileMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {profileReconcileError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="User Profile 정합 실패"
            description={profileReconcileError.message}
            closable
            onClose={() => {
              runProfileReconcileMutation.reset();
            }}
          />
        )}

        {profileReconcileResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{profileReconcileResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="override 적용 row">
                {profileReconcileResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<UserProfileReconcileSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '대상', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={profileReconcileResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="조장 ProfileFlags 권한 적용 (6.조장)" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          SF frozen snapshot 에 없는 <Text strong>신규 조장 권한</Text>을 backend 코드 SoT
          (<Text code>LeaderProfileFlagsSeed</Text>) 기준으로 <Text code>profile_flags</Text> 에
          적용한다. 적용 대상은 <Text code>6.조장</Text> 단건 —{' '}
          <Text code>7.영업사원 + 조장</Text> 은 web admin 권한 편집으로 수동 처리한다.
          <br />
          <Text strong>위 FK Resolve → Natural Key FK 해소 완료 후</Text> 1회 실행한다. 그 전에는{' '}
          <Text code>profile_flags.profile_id</Text> 가 NULL 이라 전건 skip 으로 보고된다. row 를 새로
          만들지 않고 <Text strong>기존 row 만 갱신</Text>하며(과거 부팅 Runner 의 UNIQUE 충돌 회피),{' '}
          <Text code>is_locally_modified=TRUE</Text> 인 web admin 편집분은 보존한다. 멱등 — 동일 값
          재적용은 0 row. 적용 시 권한/데이터스코프 캐시가 자동 무효화된다. 동기 실행 — 보통 수 초 내
          완료.
        </Paragraph>

        <Space>
          <Button
            type="primary"
            loading={leaderProfileFlagsPending}
            disabled={leaderProfileFlagsPending}
            onClick={() => {
              runLeaderProfileFlagsMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {leaderProfileFlagsError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="조장 ProfileFlags 적용 실패"
            description={leaderProfileFlagsError.message}
            closable
            onClose={() => {
              runLeaderProfileFlagsMutation.reset();
            }}
          />
        )}

        {leaderProfileFlagsResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{leaderProfileFlagsResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="적용 row">
                {leaderProfileFlagsResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            {leaderProfileFlagsResult.totalRowsAffected === 0 && (
              <Alert
                type="warning"
                showIcon
                style={{ marginTop: 12 }}
                message="적용된 row 가 없습니다"
                description="아래 사유를 확인하세요 — row 부재(Stage 1 적재 / Natural Key FK 해소 선행 필요), web admin 편집분 보존(is_locally_modified=TRUE), 또는 이미 동일 값이 적용된 상태(멱등)."
              />
            )}
            <ResizableTable<LeaderProfileFlagsSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '대상', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={leaderProfileFlagsResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="Stage 2-C — 초기 비밀번호 적재 (BCrypt)" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          SF 는 비밀번호를 <Text strong>단방향 hash 로만 보관</Text>해 추출 자체가 불가능하므로,
          마이그레이션 대상 user 의 비밀번호는 이전되지 않고 <Text strong>고정 초기 평문</Text>{' '}
          (<Text code>pwrs1234!</Text>) 의 BCrypt hash 로 새로 발급된다. 대상은{' '}
          <Text code>sfid IS NOT NULL AND (password IS NULL OR password = &apos;&apos;)</Text> 인 row —
          Stage 1 이 NOT NULL 제약 회피용으로 넣어둔 <Text code>&apos;&apos;</Text> placeholder 를 덮어쓴다.
          이미 채워진 row 는 skip 이라 <Text strong>멱등</Text>이다.
          <br />
          <Text code>password_change_required = TRUE</Text> 를 함께 설정해 최초 로그인 시 비밀번호
          변경을 강제한다. BCrypt salt 가 매 encode 마다 랜덤이라 사용자별 hash 는 서로 다르지만
          평문은 모두 동일하다. 동기 실행 — row 별로 개별 encode 하므로{' '}
          <Text strong>사원 수에 비례</Text>해 시간이 걸린다 (수천 건이면 수십 초).
        </Paragraph>

        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="런칭 공지 필요"
          description="초기 비밀번호가 전 사용자 공통 고정값이므로, 사용자에게 사번 + pwrs1234! 로 로그인 후 즉시 변경하도록 안내해야 한다."
        />

        <Space>
          <Button
            type="primary"
            loading={passwordHashPending}
            disabled={passwordHashPending}
            onClick={() => {
              runPasswordHashMutation.mutate();
            }}
          >
            실행
          </Button>
        </Space>

        {passwordHashError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="초기 비밀번호 적재 실패"
            description={passwordHashError.message}
            closable
            onClose={() => {
              runPasswordHashMutation.reset();
            }}
          />
        )}

        {passwordHashResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{passwordHashResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="적용 row">
                {passwordHashResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            {passwordHashResult.totalRowsAffected === 0 && (
              <Alert
                type="warning"
                showIcon
                style={{ marginTop: 12 }}
                message="적용된 row 가 없습니다"
                description="아래 사유를 확인하세요 — Stage 1 user 적재 미완료(sfid 보유 row 부재), 또는 이미 비밀번호가 채워진 상태(멱등)."
              />
            )}
            <ResizableTable<PasswordHashSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '대상', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={passwordHashResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="Stage 2-B — Derived 캐시 동기화" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          Employee.cost_center_code 를 기준으로 derived 캐시를 동기화한다 — <Text code>User.cost_center_code</Text>
          (employee_code 조인) + <Text code>ProfessionalPromotionTeamMaster.branch_code</Text> (employee_id 조인).
          후자는 SF <Text code>CostCenterCode__c</Text>(라벨 "조직유형") 가 운영 dead field 라 사원 소속 지점으로 백필하며,
          멱등(branch_code IS NULL 한정)이라 신규 등록분은 건드리지 않는다.
          한글 picklist → enum 변환 substep (Employee.role / Employee.professional_promotion_team /
          User.profile_type) 은 폐기 — SF picklist value 가 곧 저장값이라 변환 자체가 불요.
          본 작업은 동기 실행 — 보통 수 초 내 완료.
        </Paragraph>

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            대상: <Text code>User.cost_center_code</Text> + <Text code>ProfessionalPromotionTeamMaster.branch_code</Text>
          </Paragraph>
          <Space wrap>
            <Button
              type="primary"
              loading={picklistPending}
              disabled={picklistPending}
              onClick={() => {
                runPicklistColumnMutation.reset();
                runPicklistAllMutation.mutate();
              }}
            >
              전체 실행
            </Button>
            <Button
              loading={picklistPending}
              disabled={picklistPending}
              onClick={() => {
                runPicklistAllMutation.reset();
                runPicklistColumnMutation.mutate('user_cost_center_code');
              }}
            >
              User.cost_center_code 만
            </Button>
            <Button
              loading={picklistPending}
              disabled={picklistPending}
              onClick={() => {
                runPicklistAllMutation.reset();
                runPicklistColumnMutation.mutate('ppt_master_branch_code');
              }}
            >
              전문행사조 branch_code 만
            </Button>
          </Space>
        </Space>

        {picklistError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="Derived 캐시 동기화 실패"
            description={picklistError.message}
            closable
            onClose={() => {
              runPicklistAllMutation.reset();
              runPicklistColumnMutation.reset();
            }}
          />
        )}

        {picklistResult && (
          <div style={{ marginTop: 16 }}>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label="substep">
                <Text code>{picklistResult.substep}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="누적 적용 row">
                {picklistResult.totalRowsAffected.toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
            <ResizableTable<PicklistSubstepResult>
              style={{ marginTop: 12 }}
              size="small"
              rowKey="label"
              pagination={false}
              columns={[
                { title: '대상', dataIndex: 'label', key: 'label' },
                {
                  title: '적용 row',
                  dataIndex: 'rowsAffected',
                  key: 'rowsAffected',
                  width: 160,
                  align: 'right',
                  render: (v: number) => v.toLocaleString(),
                },
              ]}
              dataSource={picklistResult.results}
            />
          </div>
        )}
      </Card>

      <Card title="Sharing Recalc (cut-over 최종 단계)" style={{ marginTop: 24 }}>
        <Paragraph type="secondary">
          OWD / RecordType / FLS / SharingRule 관련 cache 를 일괄 무효화한다. 데이터 재계산이 아니라{' '}
          <Text strong>cache evict 만</Text> 수행하며 (재계산 자체는 evaluator 가 매 read 시점 runtime
          처리), 멱등이라 여러 번 실행해도 안전하다.
          <br />
          <Text strong>위 Stage 2 substep 을 모두 마친 뒤 마지막에 1회</Text> 실행한다 — 메타 적재가
          끝나기 전에 돌리면 evict 후 다시 옛 값이 캐시된다.
        </Paragraph>

        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="권한 주의 — 본 기능만 MODIFY_ALL_DATA 가드가 있습니다"
          description={
            <>
              Stage 1·2 substep 은 로그인만 요구하지만, Sharing Recalc 는{' '}
              <Text code>MODIFY_ALL_DATA</Text> 권한이 필요하다. 시스템 관리자 Profile 이 연결된
              계정으로 실행해야 하며, <Text code>user.profile_id</Text> 가 NULL 인 계정(예: SF Profile
              적재 전에 생성된 부트스트랩 계정)은 <Text code>403</Text> 이 난다. 403 이 발생하면 먼저
              해당 계정의 Profile 연결을 확인한다.
            </>
          }
        />

        {sharingRecalcStatus && (
          <Descriptions
            column={{ xs: 1, sm: 2 }}
            bordered
            size="small"
            style={{ marginBottom: 12 }}
            title="최근 실행 이력"
          >
            <Descriptions.Item label="최종 실행 시각">
              {sharingRecalcStatus.lastRecalcAt ?? '실행 이력 없음'}
            </Descriptions.Item>
            <Descriptions.Item label="범위">
              {sharingRecalcStatus.lastRecalcScope ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="evict 캐시 수">
              {sharingRecalcStatus.lastEvictedCount?.toLocaleString() ?? '-'}
            </Descriptions.Item>
            <Descriptions.Item label="소요 시간">
              {sharingRecalcStatus.lastDurationMs != null
                ? `${sharingRecalcStatus.lastDurationMs.toLocaleString()} ms`
                : '-'}
            </Descriptions.Item>
          </Descriptions>
        )}

        {sharingRecalcStatusError && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message="실행 이력 조회 실패"
            description={`${sharingRecalcStatusError.message} — 권한(MODIFY_ALL_DATA) 부족일 수 있습니다. 이력 조회 실패와 무관하게 아래 실행은 시도할 수 있습니다.`}
          />
        )}

        <Space>
          <Button
            type="primary"
            loading={sharingRecalcPending}
            disabled={sharingRecalcPending}
            onClick={() => {
              runSharingRecalcMutation.mutate();
            }}
          >
            전체 Recalc 실행
          </Button>
        </Space>

        {sharingRecalcError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message="Sharing Recalc 실패"
            description={sharingRecalcError.message}
            closable
            onClose={() => {
              runSharingRecalcMutation.reset();
            }}
          />
        )}

        {sharingRecalcResult && (
          <Descriptions
            column={{ xs: 1, sm: 2 }}
            bordered
            size="small"
            style={{ marginTop: 16 }}
          >
            <Descriptions.Item label="evict 된 캐시 수">
              {sharingRecalcResult.evictedCount.toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="소요 시간">
              {sharingRecalcResult.durationMs.toLocaleString()} ms
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>
    </div>
  );
}
