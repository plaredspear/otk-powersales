import { useState } from 'react';
import { useNavigate, type NavigateFunction } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  Button,
  Card,
  message,
  Space,
  Typography,
  Upload,
  Alert,
  Statistic,
  Row,
  Col,
  Divider,
  Input,
  Select,
  DatePicker,
  Tag,
  Modal,
  Tooltip,
} from 'antd';
import { DownloadOutlined, UploadOutlined, SearchOutlined, UndoOutlined, PlusOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useScheduleUpload, useScheduleConfirm } from '@/hooks/schedule/useScheduleUpload';
import { useScheduleList } from '@/hooks/schedule/useScheduleList';
import { useScheduleBatchConfirm, useScheduleBatchUnconfirm, useScheduleBatchDelete } from '@/hooks/schedule/useScheduleBatchConfirm';
import { SCHEDULE_TEMPLATE_PATH, SCHEDULE_EXPORT_PATH, SCHEDULE_EXPORT_ALL_PATH, scheduleExportParams } from '@/api/schedule';
import type { ScheduleUploadResult, RowError, RowPreview, ScheduleListItem, SchedulePreset } from '@/api/schedule';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { PresetFilterSelect, type PresetOption } from '@/components/common/PresetFilterSelect';
import ScheduleCreateModal from './schedule/components/ScheduleCreateModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { useAuthStore } from '@/stores/authStore';

/**
 * 레거시 SF List View 10개 매핑 — `docs/plan/legacy-pages/진열사원스케줄마스터/UC-01.md` 참조.
 * 프리셋 선택 시 backend 가 ValidData 계산식 + 근무형태 / 확정여부 조건을 일괄 적용.
 */
const SCHEDULE_PRESET_OPTIONS: PresetOption<SchedulePreset>[] = [
  { value: 'INPUT_TODAY', label: '0. 당일등록' },
  { value: 'ALL', label: '1. 모두' },
  { value: 'VALID', label: '2-1. 유효사원' },
  { value: 'VALID_CONFIRMED', label: '2-2. 유효사원(확정)' },
  { value: 'VALID_NOT_CONFIRMED', label: '2-3. 유효사원(미확정)' },
  { value: 'FIXED_VALID', label: '3. 고정 유효사원' },
  { value: 'BIFURCATION_VALID', label: '4. 격고 유효사원' },
  { value: 'PATROL_VALID', label: '5. 순회 유효사원' },
  { value: 'VALID_CONFIRMED_TEMP', label: '6. 유효사원(확정)_임시' },
  { value: 'END', label: '7. 종료 사원' },
];

const { Text } = Typography;
const { RangePicker } = DatePicker;

// 검색결과 엑셀 다운로드 최대 건수 — 서버 export 상한(EXPORT_MAX_ROWS) 정합. 초과 시 안내 후 진행.
const EXPORT_MAX_ROWS = 50000;

const errorColumns: ColumnsType<RowError> = [
  { title: '행', dataIndex: 'row', key: 'row', width: 60 },
  { title: '컬럼', dataIndex: 'field', key: 'field', width: 100 },
  { title: '입력값', dataIndex: 'value', key: 'value', width: 120, render: (v) => v ?? '-' },
  { title: '오류 메시지', dataIndex: 'message', key: 'message' },
];

const previewColumns: ColumnsType<RowPreview> = [
  { title: '행', dataIndex: 'row', key: 'row', width: 60 },
  { title: '사원번호', dataIndex: 'employeeCode', key: 'employeeCode', width: 100 },
  { title: '사원명', dataIndex: 'employeeName', key: 'employeeName', width: 90 },
  { title: '거래처코드', dataIndex: 'accountCode', key: 'accountCode', width: 110 },
  { title: '거래처명', dataIndex: 'accountName', key: 'accountName', width: 150 },
  { title: '근무유형3', dataIndex: 'typeOfWork3', key: 'typeOfWork3', width: 90 },
  { title: '근무유형5', dataIndex: 'typeOfWork5', key: 'typeOfWork5', width: 90 },
  { title: '시작일', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '종료일', dataIndex: 'endDate', key: 'endDate', width: 110, render: (v) => v ?? '-' },
];

/** 거래처상태 (SF `Account.AccountStatusName__c`) 중 빨간색으로 강조할 값. */
const ALERT_ACCOUNT_STATUS = new Set(['폐업', '출고중지']);

/**
 * 유효 신호등 색상 (SF formula `Valid__c` 의 IMAGE() Greenlight/Yellowlight/Redlight 대체).
 * GREEN=유효 / YELLOW=예정 / RED=종료.
 */
const VALID_LIGHT_COLOR: Record<string, string> = {
  GREEN: '#52c41a',
  YELLOW: '#faad14',
  RED: '#ff4d4f',
};

/** 유효 신호등 circle 렌더 — 색상 dot + 유효데이터 텍스트 tooltip. */
function renderValidLight(valid: string | null, validData: string | null) {
  if (!valid) return <span>-</span>;
  const color = VALID_LIGHT_COLOR[valid] ?? '#d9d9d9';
  return (
    <Tooltip title={validData ?? ''}>
      <span
        style={{
          display: 'inline-block',
          width: 14,
          height: 14,
          borderRadius: '50%',
          backgroundColor: color,
        }}
      />
    </Tooltip>
  );
}

function buildListColumns(
  onEdit: (row: ScheduleListItem) => void,
  onConfirm: (row: ScheduleListItem) => void,
  onUnconfirm: (row: ScheduleListItem) => void,
  confirming: boolean,
  unconfirming: boolean,
  canUnconfirm: boolean,
  navigate: NavigateFunction,
): ColumnsType<ScheduleListItem> {
  // SF 「진열사원 스케줄 마스터」 List View 컬럼 순서 정합:
  // 유효 · 지점명 · 사번 · 성명 · 재직상태 · 거래처코드 · 거래명 · 거래처유형 · 근무형태3 · 근무형태5
  // · 시작일 · 종료일 · 거래처상태 · 전월매출 · 확정.
  return [
    {
      title: '유효',
      dataIndex: 'valid',
      key: 'valid',
      width: 60,
      align: 'center',
      render: (_: string | null, row) => renderValidLight(row.valid, row.validData),
    },
    { title: '지점명', dataIndex: 'branchName', key: 'branchName', width: 110, render: (v) => v ?? '-' },
    { title: '사번', dataIndex: 'employeeCode', key: 'employeeCode', width: 100 },
    {
      title: '성명',
      dataIndex: 'employeeName',
      key: 'employeeName',
      width: 90,
      render: (val: string, row) =>
        row.employeeId != null ? (
          <a
            onClick={(e) => {
              e.preventDefault();
              navigate(`/female-employee/${row.employeeId}`);
            }}
            href={`/female-employee/${row.employeeId}`}
          >
            {val}
          </a>
        ) : (
          val
        ),
    },
    {
      title: '재직상태',
      dataIndex: 'employmentStatus',
      key: 'employmentStatus',
      width: 100,
      align: 'center',
      render: (v) => v ?? '-',
    },
    { title: '거래처코드', dataIndex: 'accountCode', key: 'accountCode', width: 110, render: (v) => v ?? '-' },
    {
      title: '거래명',
      dataIndex: 'accountName',
      key: 'accountName',
      width: 150,
      ellipsis: true,
      render: (val: string | null, row) =>
        row.accountId != null && val ? (
          <Button
            type="link"
            style={{ padding: 0, height: 'auto' }}
            onClick={() => navigate(`/account/${row.accountId}`)}
          >
            {val}
          </Button>
        ) : (
          val ?? '-'
        ),
    },
    { title: '거래처유형', dataIndex: 'accountType', key: 'accountType', width: 100, align: 'center', render: (v) => v ?? '-' },
    { title: '근무형태3', dataIndex: 'typeOfWork3', key: 'typeOfWork3', width: 80, align: 'center' },
    { title: '근무형태5', dataIndex: 'typeOfWork5', key: 'typeOfWork5', width: 80, align: 'center' },
    { title: '시작일', dataIndex: 'startDate', key: 'startDate', width: 110, align: 'center', sorter: true },
    {
      title: '종료일',
      dataIndex: 'endDate',
      key: 'endDate',
      width: 110,
      align: 'center',
      sorter: true,
      render: (v) => v ?? '-',
    },
    {
      title: '거래처상태',
      dataIndex: 'accountStatus',
      key: 'accountStatus',
      width: 100,
      align: 'center',
      render: (v: string | null) =>
        v == null ? '-' : <span style={ALERT_ACCOUNT_STATUS.has(v) ? { color: '#ff4d4f' } : undefined}>{v}</span>,
    },
    {
      title: '전월매출',
      dataIndex: 'lastMonthRevenue',
      key: 'lastMonthRevenue',
      width: 120,
      align: 'right',
      sorter: true,
      render: (v: number | null) => (v != null ? v.toLocaleString() : '-'),
    },
    {
      title: '확정',
      dataIndex: 'confirmed',
      key: 'confirmed',
      width: 70,
      align: 'center',
      sorter: true,
      render: (confirmed: boolean | null) =>
        confirmed ? <Tag color="green">확정</Tag> : <Tag color="red">미확정</Tag>,
    },
    {
      title: '액션',
      key: 'action',
      width: 140,
      align: 'center',
      fixed: 'right',
      render: (_, row) => (
        <Space size={4}>
          {row.attendanceCount > 0 && row.endDate != null ? (
            <Tooltip placement="left" title={`출근 등록(${row.attendanceCount}건)된 스케줄은 수정할 수 없습니다`}>
              <Button size="small" disabled>
                수정
              </Button>
            </Tooltip>
          ) : row.attendanceCount > 0 ? (
            // 출근 등록되어도 종료일이 없으면 종료일 입력을 위해 수정 허용 (모달에서 종료일만 편집 가능)
            <Tooltip placement="left" title={`출근 등록 ${row.attendanceCount}건, 종료일만 수정 가능합니다`}>
              <Button size="small" onClick={() => onEdit(row)}>
                수정
              </Button>
            </Tooltip>
          ) : row.confirmed ? (
            // 확정된 스케줄은 출근 없어도 종료일만 수정 가능 (모달 잠금 정합)
            <Tooltip placement="left" title="종료일만 수정 가능합니다">
              <Button size="small" onClick={() => onEdit(row)}>
                수정
              </Button>
            </Tooltip>
          ) : (
            <Button size="small" onClick={() => onEdit(row)}>
              수정
            </Button>
          )}
          {row.confirmed
            ? canUnconfirm && (
                <Button
                  size="small"
                  danger
                  loading={unconfirming}
                  onClick={() => onUnconfirm(row)}
                >
                  확정 해제
                </Button>
              )
            : (
                <Button
                  size="small"
                  type="primary"
                  loading={confirming}
                  onClick={() => onConfirm(row)}
                >
                  확정
                </Button>
              )}
        </Space>
      ),
    },
  ];
}

export default function DisplaySchedulePage() {
  const navigate = useNavigate();
  // 확정 해제 권한 — backend isAdminGrade(시스템 관리자 OR 영업지원실) 정합. 비관리자는 버튼 미노출.
  const user = useAuthStore((s) => s.user);
  const canUnconfirm = user?.profileName === '시스템 관리자' || user?.isSalesSupport === true;
  const { run: runTemplate, downloading } = useExcelDownload();
  const { run: runExport, downloading: exporting } = useExcelDownload();
  const { run: runExportAll, downloading: exportingAll } = useExcelDownload();
  const [uploadResult, setUploadResult] = useState<ScheduleUploadResult | null>(null);
  const uploadMutation = useScheduleUpload();
  const confirmMutation = useScheduleConfirm();

  // Schedule list state
  const [listPage, setListPage] = useState(0);
  const [listSize, setListSize] = useState(50);
  const [filterEmployeeCode, setFilterEmployeeCode] = useState('');
  const [filterAccountName, setFilterAccountName] = useState('');
  const [filterAccountType, setFilterAccountType] = useState('');
  const [filterTypeOfWork3, setFilterTypeOfWork3] = useState<string | undefined>(undefined);
  const [filterConfirmed, setFilterConfirmed] = useState<boolean | undefined>(undefined);
  const [filterStartDateRange, setFilterStartDateRange] = useState<[string, string] | null>(null);
  const [filterPreset, setFilterPreset] = useState<SchedulePreset | undefined>(undefined);
  const [sortBy, setSortBy] = useState<string | undefined>(undefined);
  const [sortDir, setSortDir] = useState<'asc' | 'desc' | undefined>(undefined);

  // Applied filters (only update on search click)
  const [appliedFilters, setAppliedFilters] = useState<{
    employeeCode?: string;
    accountName?: string;
    accountType?: string;
    typeOfWork3?: string;
    confirmed?: boolean;
    startDateFrom?: string;
    startDateTo?: string;
    preset?: SchedulePreset;
  }>({});

  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ScheduleListItem | null>(null);
  const modalOpen = createModalOpen || editTarget != null;

  const scheduleListQuery = useScheduleList({
    page: listPage,
    size: listSize,
    ...appliedFilters,
    sortBy,
    sortDir,
  });

  const batchConfirmMutation = useScheduleBatchConfirm();
  const batchUnconfirmMutation = useScheduleBatchUnconfirm();
  const batchDeleteMutation = useScheduleBatchDelete();

  const listColumns = buildListColumns(
    (row) => setEditTarget(row),
    (row) => handleRowConfirm(row),
    (row) => handleRowUnconfirm(row),
    batchConfirmMutation.isPending,
    batchUnconfirmMutation.isPending,
    canUnconfirm,
    navigate,
  );

  const handleDownload = () => {
    runTemplate(SCHEDULE_TEMPLATE_PATH, '진열스케줄_양식.xlsx');
  };

  const handleUpload: UploadProps['beforeUpload'] = (file) => {
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      message.error('xlsx 파일만 업로드 가능합니다');
      return Upload.LIST_IGNORE;
    }

    setUploadResult(null);
    uploadMutation.mutate(file, {
      onSuccess: (result) => {
        setUploadResult(result);
      },
      onError: (err) => {
        const errorMessage = err instanceof Error ? err.message : '업로드에 실패했습니다';
        message.error(errorMessage);
      },
    });

    return false;
  };

  const handleConfirmUpload = () => {
    if (!uploadResult) return;

    confirmMutation.mutate(uploadResult.uploadId, {
      onSuccess: (result) => {
        message.success(`${result.insertedCount}건이 등록되었습니다`);
        setUploadResult(null);
        scheduleListQuery.refetch();
      },
      onError: (err) => {
        const errorMessage = err instanceof Error ? err.message : '등록 확정에 실패했습니다';
        message.error(errorMessage);
      },
    });
  };

  const handleSearch = () => {
    setListPage(0);
    setSelectedRowKeys([]);
    setAppliedFilters({
      employeeCode: filterEmployeeCode || undefined,
      accountName: filterAccountName || undefined,
      accountType: filterAccountType || undefined,
      typeOfWork3: filterTypeOfWork3,
      confirmed: filterConfirmed,
      startDateFrom: filterStartDateRange?.[0],
      startDateTo: filterStartDateRange?.[1],
      preset: filterPreset,
    });
  };

  const handleReset = () => {
    setFilterEmployeeCode('');
    setFilterAccountName('');
    setFilterAccountType('');
    setFilterTypeOfWork3(undefined);
    setFilterConfirmed(undefined);
    setFilterStartDateRange(null);
    setFilterPreset(undefined);
    setSortBy(undefined);
    setSortDir(undefined);
    setListPage(0);
    setSelectedRowKeys([]);
    setAppliedFilters({});
  };

  /**
   * 프리셋 선택 시 자유 필터 폼을 자동 채움 + 즉시 적용 (검색 버튼 클릭 불필요).
   * 레거시 SF List View 드롭다운 UX 동등.
   */
  const handlePresetChange = (preset: SchedulePreset | undefined) => {
    setFilterPreset(preset);
    setListPage(0);
    setSelectedRowKeys([]);
    setAppliedFilters({
      employeeCode: filterEmployeeCode || undefined,
      accountName: filterAccountName || undefined,
      accountType: filterAccountType || undefined,
      typeOfWork3: filterTypeOfWork3,
      confirmed: filterConfirmed,
      startDateFrom: filterStartDateRange?.[0],
      startDateTo: filterStartDateRange?.[1],
      preset,
    });
  };

  const handleBatchConfirm = () => {
    const ids = selectedRowKeys as number[];
    Modal.confirm({
      title: '일괄 확정',
      content: `${ids.length}건을 확정하시겠습니까?`,
      onOk: () =>
        batchConfirmMutation.mutateAsync(ids).then((result) => {
          message.success(`${result.updatedCount}건이 확정되었습니다`);
          setSelectedRowKeys([]);
        }).catch((err) => {
          message.error(err instanceof Error ? err.message : '일괄 확정에 실패했습니다');
        }),
    });
  };

  const handleRowConfirm = (row: ScheduleListItem) => {
    Modal.confirm({
      title: '확정',
      content: `${row.employeeName} · ${row.accountName ?? '-'} 1건을 확정하시겠습니까?`,
      onOk: () =>
        batchConfirmMutation.mutateAsync([row.id]).then((result) => {
          message.success(`${result.updatedCount}건이 확정되었습니다`);
          setSelectedRowKeys((keys) => keys.filter((k) => k !== row.id));
        }).catch((err) => {
          message.error(err instanceof Error ? err.message : '확정에 실패했습니다');
        }),
    });
  };

  const handleRowUnconfirm = (row: ScheduleListItem) => {
    Modal.confirm({
      title: '확정 해제',
      content: `${row.employeeName} · ${row.accountName ?? '-'} 1건의 확정을 해제하시겠습니까? (권한·사업소 범위 외 또는 출근 등록 건은 차단됩니다)`,
      onOk: () =>
        batchUnconfirmMutation.mutateAsync([row.id]).then((result) => {
          if (result.failedCount === 0) {
            message.success('확정 해제되었습니다');
            return;
          }
          // 차단됨 — 사유 표시
          message.error(result.failures[0]?.message ?? '확정 해제할 수 없습니다');
        }).catch((err) => {
          message.error(err instanceof Error ? err.message : '확정 해제에 실패했습니다');
        }),
    });
  };

  const handleExportSelected = () => {
    const ids = selectedRowKeys as number[];
    if (ids.length === 0) return;
    runExport(SCHEDULE_EXPORT_PATH, '진열스케줄.xlsx', { method: 'post', data: { ids } });
  };

  // 현재 적용된 검색/프리셋 필터 + 정렬을 그대로 전량 export (목록과 동일 가시 범위).
  const handleExportAll = () => {
    runExportAll(SCHEDULE_EXPORT_ALL_PATH, '진열스케줄.xlsx', {
      params: scheduleExportParams({ ...appliedFilters, sortBy, sortDir }),
      totalCount: scheduleListQuery.data?.totalElements ?? 0,
      maxRows: EXPORT_MAX_ROWS,
    });
  };

  const handleBatchDelete = () => {
    const ids = selectedRowKeys as number[];
    Modal.confirm({
      title: '선택 삭제',
      content: `${ids.length}건을 삭제하시겠습니까? (확정 + 여사원일정 연결 건은 차단됩니다)`,
      okType: 'danger',
      onOk: () =>
        batchDeleteMutation.mutateAsync(ids).then((result) => {
          setSelectedRowKeys([]);
          if (result.failedCount === 0) {
            message.success(`${result.deletedCount}건이 삭제되었습니다`);
            return;
          }
          // partial success — 실패 사유 표시
          Modal.info({
            title: `삭제 결과: 성공 ${result.deletedCount}건 / 실패 ${result.failedCount}건`,
            width: 560,
            content: (
              <div>
                <p>다음 항목은 차단되어 삭제되지 않았습니다:</p>
                <ul style={{ marginTop: 8 }}>
                  {result.failures.map((f) => (
                    <li key={f.id}>ID {f.id} — {f.message}</li>
                  ))}
                </ul>
              </div>
            ),
          });
        }).catch((err) => {
          message.error(err instanceof Error ? err.message : '일괄 삭제에 실패했습니다');
        }),
    });
  };

  const hasErrors = uploadResult != null && uploadResult.errorRows > 0;
  const canConfirmUpload = uploadResult != null && uploadResult.errorRows === 0 && uploadResult.successRows > 0;

  return (
    <div>
      <Card title="진열스케줄마스터">
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalOpen(true)}
            >
              신규 등록
            </Button>
            <Button
              icon={<DownloadOutlined />}
              loading={downloading}
              onClick={handleDownload}
            >
              양식 다운로드
            </Button>
            <Upload
              accept=".xlsx"
              showUploadList={false}
              beforeUpload={handleUpload}
            >
              <Button
                icon={<UploadOutlined />}
                loading={uploadMutation.isPending}
              >
                Excel 업로드
              </Button>
            </Upload>
          </Space>
          <Text type="secondary">
            ※ xlsx 파일만 업로드 가능합니다 (최대 500행, 5MB)
          </Text>
        </Space>
      </Card>

      {uploadResult && (
        <Card title="업로드 결과" style={{ marginTop: 16 }}>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col>
              <Statistic title="전체" value={uploadResult.totalRows} suffix="건" />
            </Col>
            <Col>
              <Statistic
                title="성공"
                value={uploadResult.successRows}
                suffix="건"
                valueStyle={{ color: '#3f8600' }}
              />
            </Col>
            <Col>
              <Statistic
                title="실패"
                value={uploadResult.errorRows}
                suffix="건"
                valueStyle={uploadResult.errorRows > 0 ? { color: '#cf1322' } : undefined}
              />
            </Col>
          </Row>

          {hasErrors && (
            <>
              <Alert
                type="warning"
                message={`오류 목록 (${uploadResult.errorRows}건)`}
                style={{ marginBottom: 8 }}
              />
              <ResizableTable
                columns={errorColumns}
                dataSource={uploadResult.errors}
                rowKey={(r) => `${r.row}-${r.column}`}
                size="small"
                pagination={false}
                scroll={{ x: 600 }}
                style={{ marginBottom: 16 }}
              />
              <Divider />
            </>
          )}

          {uploadResult.previews.length > 0 && (
            <>
              <Alert
                type="success"
                message={`성공 목록 (${uploadResult.successRows}건)`}
                style={{ marginBottom: 8 }}
              />
              <ResizableTable
                columns={previewColumns}
                dataSource={uploadResult.previews}
                rowKey="row"
                size="small"
                pagination={false}
                scroll={{ x: 900 }}
                style={{ marginBottom: 16 }}
              />
            </>
          )}

          <div style={{ textAlign: 'right', marginTop: 16 }}>
            <Button
              type="primary"
              disabled={!canConfirmUpload}
              loading={confirmMutation.isPending}
              onClick={handleConfirmUpload}
            >
              등록 확정
            </Button>
          </div>

          {hasErrors && (
            <Alert
              type="info"
              message="오류를 수정한 후 다시 업로드해 주세요"
              style={{ marginTop: 8 }}
            />
          )}
        </Card>
      )}

      <Card title="스케줄 목록" style={{ marginTop: 16 }}>
        <Space wrap size="middle" style={{ marginBottom: 16 }}>
          <PresetFilterSelect<SchedulePreset>
            options={SCHEDULE_PRESET_OPTIONS}
            value={filterPreset}
            onChange={handlePresetChange}
            placeholder="뷰 선택"
            style={{ width: 220 }}
          />
          <Input
            placeholder="사원번호"
            value={filterEmployeeCode}
            onChange={(e) => setFilterEmployeeCode(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 140 }}
            allowClear
          />
          <Input
            placeholder="거래처명/거래처코드"
            value={filterAccountName}
            onChange={(e) => setFilterAccountName(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 180 }}
            allowClear
          />
          <Input
            placeholder="거래처유형"
            value={filterAccountType}
            onChange={(e) => setFilterAccountType(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 140 }}
            allowClear
          />
          <Select
            placeholder="근무유형3"
            value={filterTypeOfWork3}
            onChange={(v) => setFilterTypeOfWork3(v)}
            allowClear
            style={{ width: 120 }}
            options={[
              { label: '고정', value: '고정' },
              { label: '격고', value: '격고' },
              { label: '순회', value: '순회' },
            ]}
          />
          <Select
            placeholder="확정상태"
            value={filterConfirmed}
            onChange={(v) => setFilterConfirmed(v)}
            allowClear
            style={{ width: 120 }}
            options={[
              { label: '확정', value: true },
              { label: '미확정', value: false },
            ]}
          />
          <RangePicker
            value={
              filterStartDateRange
                ? [
                    filterStartDateRange[0] ? dayjs(filterStartDateRange[0]) : null,
                    filterStartDateRange[1] ? dayjs(filterStartDateRange[1]) : null,
                  ]
                : null
            }
            onChange={(_dates, dateStrings) => {
              if (dateStrings[0] && dateStrings[1]) {
                setFilterStartDateRange([dateStrings[0], dateStrings[1]]);
              } else {
                setFilterStartDateRange(null);
              }
            }}
            placeholder={['시작일 from', '시작일 to']}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            검색
          </Button>
          <Button icon={<UndoOutlined />} onClick={handleReset}>
            초기화
          </Button>
          <RefreshButton
            onRefresh={scheduleListQuery.refetch}
            refreshing={scheduleListQuery.isFetching}
          />
        </Space>

        <Space style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            disabled={selectedRowKeys.length === 0}
            loading={batchConfirmMutation.isPending}
            onClick={handleBatchConfirm}
          >
            {selectedRowKeys.length > 0
              ? `일괄 확정 (${selectedRowKeys.length}건 선택)`
              : '일괄 확정'}
          </Button>
          <Button
            danger
            disabled={selectedRowKeys.length === 0}
            loading={batchDeleteMutation.isPending}
            onClick={handleBatchDelete}
          >
            {selectedRowKeys.length > 0
              ? `선택 삭제 (${selectedRowKeys.length}건 선택)`
              : '선택 삭제'}
          </Button>
          <Button
            icon={<DownloadOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={exporting}
            onClick={handleExportSelected}
          >
            {selectedRowKeys.length > 0
              ? `선택 다운로드 (${selectedRowKeys.length}건 선택)`
              : '선택 다운로드'}
          </Button>
          <Button
            icon={<DownloadOutlined />}
            loading={exportingAll}
            onClick={handleExportAll}
          >
            검색결과 다운로드
          </Button>
        </Space>

        <ResizableTable
          columns={listColumns}
          dataSource={scheduleListQuery.data?.content}
          rowKey="id"
          size="small"
          loading={scheduleListQuery.isLoading}
          scroll={{ x: 1200 }}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
          }}
          pagination={buildListPagination({
            page: listPage,
            pageSize: listSize,
            total: scheduleListQuery.data?.totalElements ?? 0,
            onPageChange: (page) => {
              setListPage(page);
              setSelectedRowKeys([]);
            },
            onSizeChange: (size) => {
              // 페이지 크기 변경 시 1페이지로 이동 (antd 가 page 를 보정하나 명시적으로 리셋)
              setListSize(size);
              setListPage(0);
              setSelectedRowKeys([]);
            },
          })}
          onChange={(_pagination, _filters, sorter, extra) => {
            // 페이지 이동(paginate)은 pagination.onChange 가 처리. 여기서 listPage 를 0 으로
            // 리셋하면 페이지 클릭이 항상 1페이지로 되돌아가므로, 정렬 변경일 때만 처리한다.
            if (extra.action !== 'sort') return;
            const single = Array.isArray(sorter) ? sorter[0] : sorter;
            if (single && single.order && single.field) {
              setSortBy(String(single.field));
              setSortDir(single.order === 'ascend' ? 'asc' : 'desc');
            } else {
              setSortBy(undefined);
              setSortDir(undefined);
            }
            setListPage(0);
          }}
        />
      </Card>

      <ScheduleCreateModal
        open={modalOpen}
        editTarget={editTarget}
        onClose={() => {
          setCreateModalOpen(false);
          setEditTarget(null);
        }}
        onSuccess={() => {
          scheduleListQuery.refetch();
        }}
      />
    </div>
  );
}
