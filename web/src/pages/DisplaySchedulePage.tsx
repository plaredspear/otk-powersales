import { useState } from 'react';
import dayjs from 'dayjs';
import {
  Button,
  Card,
  message,
  Space,
  Table,
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
} from 'antd';
import { DownloadOutlined, UploadOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useScheduleUpload, useScheduleConfirm } from '@/hooks/schedule/useScheduleUpload';
import { useScheduleList } from '@/hooks/schedule/useScheduleList';
import { useScheduleBatchConfirm, useScheduleBatchUnconfirm } from '@/hooks/schedule/useScheduleBatchConfirm';
import { downloadScheduleTemplate } from '@/api/schedule';
import type { ScheduleUploadResult, RowError, RowPreview, ScheduleListItem } from '@/api/schedule';

const { Text } = Typography;
const { RangePicker } = DatePicker;

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

const listColumns: ColumnsType<ScheduleListItem> = [
  { title: '사원번호', dataIndex: 'employeeCode', key: 'employeeCode', width: 100 },
  { title: '사원명', dataIndex: 'employeeName', key: 'employeeName', width: 80 },
  { title: '거래처코드', dataIndex: 'accountCode', key: 'accountCode', width: 110, render: (v) => v ?? '-' },
  { title: '거래처명', dataIndex: 'accountName', key: 'accountName', width: 150, render: (v) => v ?? '-' },
  { title: '근무유형3', dataIndex: 'typeOfWork3', key: 'typeOfWork3', width: 80, align: 'center' },
  { title: '근무유형5', dataIndex: 'typeOfWork5', key: 'typeOfWork5', width: 80, align: 'center' },
  { title: '시작일', dataIndex: 'startDate', key: 'startDate', width: 110, align: 'center' },
  {
    title: '종료일',
    dataIndex: 'endDate',
    key: 'endDate',
    width: 110,
    align: 'center',
    render: (v) => v ?? '-',
  },
  {
    title: '확정',
    dataIndex: 'confirmed',
    key: 'confirmed',
    width: 70,
    align: 'center',
    render: (confirmed: boolean | null) =>
      confirmed ? <Tag color="green">확정</Tag> : <Tag>미확정</Tag>,
  },
  { title: '조직코드', dataIndex: 'costCenterCode', key: 'costCenterCode', width: 80, align: 'center', render: (v) => v ?? '-' },
  {
    title: '전월매출',
    dataIndex: 'lastMonthRevenue',
    key: 'lastMonthRevenue',
    width: 120,
    align: 'right',
    render: (v: number | null) => (v != null ? v.toLocaleString() : '-'),
  },
];

export default function DisplaySchedulePage() {
  const [downloading, setDownloading] = useState(false);
  const [uploadResult, setUploadResult] = useState<ScheduleUploadResult | null>(null);
  const uploadMutation = useScheduleUpload();
  const confirmMutation = useScheduleConfirm();

  // Schedule list state
  const [listPage, setListPage] = useState(0);
  const [filterEmployeeCode, setFilterEmployeeCode] = useState('');
  const [filterAccountName, setFilterAccountName] = useState('');
  const [filterTypeOfWork3, setFilterTypeOfWork3] = useState<string | undefined>(undefined);
  const [filterConfirmed, setFilterConfirmed] = useState<boolean | undefined>(undefined);
  const [filterStartDateRange, setFilterStartDateRange] = useState<[string, string] | null>(null);

  // Applied filters (only update on search click)
  const [appliedFilters, setAppliedFilters] = useState<{
    employeeCode?: string;
    accountName?: string;
    typeOfWork3?: string;
    confirmed?: boolean;
    startDateFrom?: string;
    startDateTo?: string;
  }>({});

  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const scheduleListQuery = useScheduleList({
    page: listPage,
    size: 20,
    ...appliedFilters,
  });

  const batchConfirmMutation = useScheduleBatchConfirm();
  const batchUnconfirmMutation = useScheduleBatchUnconfirm();

  const handleDownload = async () => {
    setDownloading(true);
    try {
      await downloadScheduleTemplate();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '양식 다운로드에 실패했습니다';
      message.error(errorMessage);
    } finally {
      setDownloading(false);
    }
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
      typeOfWork3: filterTypeOfWork3,
      confirmed: filterConfirmed,
      startDateFrom: filterStartDateRange?.[0],
      startDateTo: filterStartDateRange?.[1],
    });
  };

  const handleReset = () => {
    setFilterEmployeeCode('');
    setFilterAccountName('');
    setFilterTypeOfWork3(undefined);
    setFilterConfirmed(undefined);
    setFilterStartDateRange(null);
    setListPage(0);
    setSelectedRowKeys([]);
    setAppliedFilters({});
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

  const handleBatchUnconfirm = () => {
    const ids = selectedRowKeys as number[];
    Modal.confirm({
      title: '확정 해제',
      content: `${ids.length}건의 확정을 해제하시겠습니까?`,
      onOk: () =>
        batchUnconfirmMutation.mutateAsync(ids).then((result) => {
          message.success(`${result.updatedCount}건이 확정 해제되었습니다`);
          setSelectedRowKeys([]);
        }).catch((err) => {
          message.error(err instanceof Error ? err.message : '확정 해제에 실패했습니다');
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
              <Table
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
              <Table
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
          <Input
            placeholder="사원번호"
            value={filterEmployeeCode}
            onChange={(e) => setFilterEmployeeCode(e.target.value)}
            style={{ width: 140 }}
            allowClear
          />
          <Input
            placeholder="거래처명"
            value={filterAccountName}
            onChange={(e) => setFilterAccountName(e.target.value)}
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
            disabled={selectedRowKeys.length === 0}
            loading={batchUnconfirmMutation.isPending}
            onClick={handleBatchUnconfirm}
          >
            {selectedRowKeys.length > 0
              ? `확정 해제 (${selectedRowKeys.length}건 선택)`
              : '확정 해제'}
          </Button>
        </Space>

        <Table
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
          pagination={{
            current: listPage + 1,
            pageSize: 20,
            total: scheduleListQuery.data?.totalElements ?? 0,
            showTotal: (total) => `총 ${total}건`,
            onChange: (page) => {
              setListPage(page - 1);
              setSelectedRowKeys([]);
            },
          }}
        />
      </Card>
    </div>
  );
}
