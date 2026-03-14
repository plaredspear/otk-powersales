import { useState } from 'react';
import { Button, Card, message, Select, Space, Table, Typography, Upload, Alert, Statistic, Row, Col, Divider } from 'antd';
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useScheduleBranches } from '@/hooks/schedule/useScheduleBranches';
import { useScheduleUpload, useScheduleConfirm } from '@/hooks/schedule/useScheduleUpload';
import { downloadScheduleTemplate } from '@/api/schedule';
import type { ScheduleUploadResult, RowError, RowPreview } from '@/api/schedule';

const { Text } = Typography;

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

export default function DisplaySchedulePage() {
  const [selectedBranch, setSelectedBranch] = useState<string | undefined>(undefined);
  const [downloading, setDownloading] = useState(false);
  const [uploadResult, setUploadResult] = useState<ScheduleUploadResult | null>(null);
  const { data: branches, isLoading: branchesLoading } = useScheduleBranches();
  const uploadMutation = useScheduleUpload();
  const confirmMutation = useScheduleConfirm();

  const handleDownload = async () => {
    if (!selectedBranch) {
      message.warning('지점을 선택해주세요');
      return;
    }

    setDownloading(true);
    try {
      await downloadScheduleTemplate(selectedBranch);
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

    return false; // prevent auto upload
  };

  const handleConfirm = () => {
    if (!uploadResult) return;

    confirmMutation.mutate(uploadResult.uploadId, {
      onSuccess: (result) => {
        message.success(`${result.insertedCount}건이 등록되었습니다`);
        setUploadResult(null);
      },
      onError: (err) => {
        const errorMessage = err instanceof Error ? err.message : '등록 확정에 실패했습니다';
        message.error(errorMessage);
      },
    });
  };

  const branchOptions = (branches ?? []).map((b) => ({
    label: b.branchName,
    value: b.costCenterCode,
  }));

  const hasErrors = uploadResult != null && uploadResult.errorRows > 0;
  const canConfirm = uploadResult != null && uploadResult.errorRows === 0 && uploadResult.successRows > 0;

  return (
    <div>
      <Card title="진열스케줄마스터">
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <div>
            <Text strong>지점 선택: </Text>
            <Select
              showSearch
              placeholder="지점을 선택해주세요"
              style={{ width: 250 }}
              value={selectedBranch}
              onChange={setSelectedBranch}
              options={branchOptions}
              loading={branchesLoading}
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </div>
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
              disabled={!canConfirm}
              loading={confirmMutation.isPending}
              onClick={handleConfirm}
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
    </div>
  );
}
