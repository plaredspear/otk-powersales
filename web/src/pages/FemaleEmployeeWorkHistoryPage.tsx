import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Input, Space, Spin, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  fetchWorkHistory,
  exportWorkHistory as apiExportWorkHistory,
  type FemaleEmployeeWorkHistoryItem,
} from '@/api/femaleEmployeeWorkHistory';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;

interface QueryParams {
  employeeCode: string;
  year: number;
  month: number;
}

/**
 * 여사원 근무내역 (개인별 조회) — SF Report `new_report_nEX` 이식 (Spec #840).
 *
 * 사번 + 년·월로 특정 여사원의 월간 근무내역을 조회. 지점 사용자는 본인 소속 지점(costCenterCode) 사번만
 * 조회 가능 (backend DataScope 가드, SF 여사원 일정관리 정합). 15컬럼 그리드 + 엑셀 다운로드.
 */
export default function FemaleEmployeeWorkHistoryPage() {
  const [employeeCode, setEmployeeCode] = useState<string>('');
  const [period, setPeriod] = useState<Dayjs>(dayjs());
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const query = useQuery({
    queryKey: ['femaleEmployeeWorkHistory', queryParams],
    queryFn: () => {
      const p = queryParams!;
      return fetchWorkHistory(p.employeeCode, p.year, p.month);
    },
    enabled: queryParams != null,
  });

  const handleSearch = () => {
    const code = employeeCode.trim();
    if (!code) {
      message.warning('사번은 필수항목입니다.');
      return;
    }
    if (!period) {
      message.warning('년·월은 필수항목입니다.');
      return;
    }
    setQueryParams({ employeeCode: code, year: period.year(), month: period.month() + 1 });
  };

  const handleExport = async () => {
    if (!queryParams) return;
    try {
      await apiExportWorkHistory(queryParams.employeeCode, queryParams.year, queryParams.month);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<FemaleEmployeeWorkHistoryItem> = useMemo(
    () => [
      { title: '일정명', dataIndex: 'scheduleName', width: 160, fixed: 'left', render: (v) => v ?? '-' },
      { title: '성명', dataIndex: 'name', width: 90, fixed: 'left' },
      { title: '사번', dataIndex: 'employeeCode', width: 100 },
      { title: '나이', dataIndex: 'age', width: 70, align: 'right', render: (v) => v ?? '-' },
      { title: '근무일자', dataIndex: 'workingDate', width: 110, render: (v) => v ?? '-' },
      { title: '거래처지점명', dataIndex: 'accountBranchName', width: 120, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountBranchCode', width: 110, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 160, render: (v) => v ?? '-' },
      { title: '근무유형', dataIndex: 'workingType', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분1', dataIndex: 'workingCategory1', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분2', dataIndex: 'workingCategory2', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분3', dataIndex: 'workingCategory3', width: 90, render: (v) => v ?? '-' },
      { title: '부근무유형', dataIndex: 'secondWorkType', width: 100, render: (v) => v ?? '-' },
      { title: '근무보고여부', dataIndex: 'isWorkReport', width: 110, render: (v) => v || '-' },
      { title: '출근일자', dataIndex: 'commuteDate', width: 160, render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>사번:</span>
        <Input
          value={employeeCode}
          onChange={(e) => setEmployeeCode(e.target.value)}
          onPressEnter={handleSearch}
          placeholder="사번 입력"
          style={{ width: 160 }}
          allowClear
        />
        <span>년·월:</span>
        <DatePicker
          picker="month"
          value={period}
          onChange={(v) => v && setPeriod(v)}
          allowClear={false}
        />
        <Button type="primary" onClick={handleSearch} loading={query.isLoading}>
          조회
        </Button>
        <Button
          onClick={handleExport}
          disabled={!query.data || query.data.items.length === 0}
        >
          엑셀 다운로드
        </Button>
      </Space>

      {queryParams != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            사번 {queryParams.employeeCode} · {queryParams.year}년 {queryParams.month}월
          </Text>
        </div>
      )}

      {query.isError && (
        <Alert
          type="error"
          message={(query.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {query.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <ResizableTable
          rowKey={(r, idx) => `${r.employeeCode}-${r.workingDate ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText:
              queryParams == null ? '사번과 년·월을 입력하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
