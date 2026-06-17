import { useState } from 'react';
import { Modal, Upload, Button, Alert, message, Tag } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import * as XLSX from 'xlsx';
import { useValidatePPTMasterBulk, useConfirmPPTMasterBulk } from '@/hooks/promotion/usePPTMasterBulk';
import type { PPTMasterBulkItem, BulkValidationRow } from '@/api/pptMaster';
import { PPT_MASTER_TEMPLATE_PATH } from '@/api/pptMaster';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import dayjs from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';

const { Dragger } = Upload;
const MAX_ROWS = 450;

interface Props {
  open: boolean;
  onClose: () => void;
}

interface ParsedRow extends PPTMasterBulkItem {
  _rowIndex: number;
}

export default function PPTMasterUploadModal({ open, onClose }: Props) {
  const [parsedItems, setParsedItems] = useState<ParsedRow[]>([]);
  const [validationResults, setValidationResults] = useState<BulkValidationRow[]>([]);
  const [isAllValid, setIsAllValid] = useState(false);
  const [parseError, setParseError] = useState<string | null>(null);

  const validateMutation = useValidatePPTMasterBulk();
  const confirmMutation = useConfirmPPTMasterBulk();

  const reset = () => {
    setParsedItems([]);
    setValidationResults([]);
    setIsAllValid(false);
    setParseError(null);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const formatExcelDate = (value: unknown): string => {
    if (typeof value === 'number') {
      // Excel serial date number
      const date = XLSX.SSF.parseDateCode(value);
      return `${date.y}-${String(date.m).padStart(2, '0')}-${String(date.d).padStart(2, '0')}`;
    }
    if (typeof value === 'string') {
      return value.trim();
    }
    return '';
  };

  const handleFile = async (file: File) => {
    reset();

    if (!file.name.endsWith('.xlsx')) {
      setParseError('.xlsx 파일만 업로드 가능합니다');
      return false;
    }

    try {
      const buffer = await file.arrayBuffer();
      const workbook = XLSX.read(buffer, { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json<Record<string, unknown>>(sheet);

      if (rows.length === 0) {
        setParseError('데이터가 없습니다');
        return false;
      }

      if (rows.length > MAX_ROWS) {
        setParseError(`최대 ${MAX_ROWS}건까지 업로드 가능합니다 (현재 ${rows.length}건)`);
        return false;
      }

      const items: ParsedRow[] = rows.map((row, index) => ({
        _rowIndex: index + 1,
        employeeCode: String(row['사번'] ?? ''),
        accountCode: String(row['거래처코드'] ?? ''),
        teamType: String(row['전문행사조'] ?? ''),
        startDate: formatExcelDate(row['시작일']),
        endDate: row['종료일'] ? formatExcelDate(row['종료일']) : null,
      }));

      setParsedItems(items);

      // Server-side validation
      const bulkItems = items.map(({ _rowIndex: _, ...rest }) => rest);
      const result = await validateMutation.mutateAsync(bulkItems);
      setValidationResults(result.results);
      setIsAllValid(result.isAllValid);
    } catch {
      setParseError('파일 파싱에 실패했습니다');
    }

    return false; // prevent default upload
  };

  const handleConfirm = async () => {
    try {
      const bulkItems = parsedItems.map(({ _rowIndex: _, ...rest }) => rest);
      const result = await confirmMutation.mutateAsync(bulkItems);
      message.success(`${result.createdCount}건이 등록되었습니다`);
      handleClose();
    } catch (err) {
      if (
        err &&
        typeof err === 'object' &&
        'message' in err &&
        typeof (err as { message: unknown }).message === 'string'
      ) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const { run: runTemplate, downloading: templateDownloading } = useExcelDownload();

  const handleDownloadTemplate = () => {
    runTemplate(PPT_MASTER_TEMPLATE_PATH, `전문행사조마스터_템플릿_${dayjs().format('YYYYMMDD')}.xlsx`);
  };

  const validationMap = new Map(validationResults.map((r) => [r.row, r]));

  const columns: ColumnsType<ParsedRow> = [
    { title: '#', dataIndex: '_rowIndex', width: 50, align: 'center' },
    { title: '사번', dataIndex: 'employee_code', width: 100, align: 'center' },
    { title: '거래처코드', dataIndex: 'account_code', width: 120, align: 'center' },
    { title: '전문행사조', dataIndex: 'team_type', width: 130, align: 'center' },
    { title: '시작일', dataIndex: 'start_date', width: 120, align: 'center' },
    {
      title: '종료일',
      dataIndex: 'end_date',
      width: 120,
      align: 'center',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '결과',
      width: 200,
      align: 'center',
      render: (_, record) => {
        const result = validationMap.get(record._rowIndex);
        if (!result) return null;
        return result.valid ? (
          <Tag color="green">성공</Tag>
        ) : (
          <Tag color="red">{result.errorMessage}</Tag>
        );
      },
    },
  ];

  return (
    <Modal
      title="엑셀 일괄 업로드"
      open={open}
      onCancel={handleClose}
      width={800}
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button onClick={handleDownloadTemplate} loading={templateDownloading}>엑셀 템플릿 다운로드</Button>
          <div style={{ display: 'flex', gap: 8 }}>
            <Button onClick={handleClose}>취소</Button>
            <Button
              type="primary"
              onClick={handleConfirm}
              disabled={!isAllValid || parsedItems.length === 0}
              loading={confirmMutation.isPending}
            >
              업로드
            </Button>
          </div>
        </div>
      }
    >
      {parsedItems.length === 0 && (
        <Dragger
          accept=".xlsx"
          showUploadList={false}
          beforeUpload={handleFile}
          style={{ marginBottom: 16 }}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">파일을 여기에 드래그하거나 클릭하여 선택</p>
          <p className="ant-upload-hint">.xlsx 파일만 지원, 최대 {MAX_ROWS}건</p>
        </Dragger>
      )}

      {parseError && (
        <Alert type="error" message={parseError} showIcon style={{ marginBottom: 16 }} />
      )}

      {parsedItems.length > 0 && (
        <>
          {!isAllValid && validationResults.length > 0 && (
            <Alert
              type="warning"
              message="에러 항목이 있어 업로드할 수 없습니다"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}
          {isAllValid && (
            <Alert
              type="success"
              message={`전체 ${parsedItems.length}건 검증 성공`}
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}
          <ResizableTable
            rowKey="_rowIndex"
            columns={columns}
            dataSource={parsedItems}
            size="small"
            pagination={false}
            scroll={{ y: 400 }}
            loading={validateMutation.isPending}
          />
        </>
      )}
    </Modal>
  );
}
