import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Space, Spin, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  fetchSafetyCheckReportRpa,
  exportSafetyCheckReportRpa as apiExport,
  type FemaleEmployeeSafetyCheckRpaItem,
} from '@/api/femaleEmployeeSafetyCheckReportRpa';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;

/**
 * 판매여사원 일일 안전점검 현황 (RPA용) — SF Report new_report_xdB 이식 (Spec #842).
 *
 * 조회 일자(기본 어제)의 안전점검 완료 건을 전사 조회 (지점 스코프 없음 — SF scope=organization).
 * #841 영업지원실/지점용과 데이터 소스 동일, 마지막 컬럼이 출근일자 대신 소유자명(CUST_NAME).
 * 24컬럼 그리드 + 엑셀 다운로드.
 */
export default function FemaleEmployeeSafetyCheckReportRpaPage() {
  const [date, setDate] = useState<Dayjs>(dayjs().subtract(1, 'day'));
  const [queryDate, setQueryDate] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['femaleEmployeeSafetyCheckReportRpa', queryDate],
    queryFn: () => fetchSafetyCheckReportRpa(queryDate!),
    enabled: queryDate != null,
  });

  const handleSearch = () => {
    if (!date) {
      message.warning('조회일자는 필수항목입니다.');
      return;
    }
    setQueryDate(date.format('YYYY-MM-DD'));
  };

  const handleExport = async () => {
    if (!queryDate) return;
    try {
      await apiExport(queryDate);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<FemaleEmployeeSafetyCheckRpaItem> = useMemo(
    () => [
      { title: '사번', dataIndex: 'employeeCode', width: 100, fixed: 'left' },
      { title: '여사원명', dataIndex: 'ladyName', width: 90, fixed: 'left' },
      { title: '소속', dataIndex: 'employeeOrgName', width: 100, render: (v) => v ?? '-' },
      { title: '거래처유형', dataIndex: 'accountType', width: 100, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountBranchCode', width: 110, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 160, render: (v) => v ?? '-' },
      { title: '근무구분1', dataIndex: 'workingCategory1', width: 90, render: (v) => v ?? '-' },
      { title: '점검시간', dataIndex: 'checkTime', width: 150, render: (v) => v ?? '-' },
      { title: '근무보고여부', dataIndex: 'isWorkReport', width: 110, render: (v) => v || '-' },
      { title: 'HR코드', dataIndex: 'hrCode', width: 90, render: (v) => v ?? '-' },
      { title: '점검1', dataIndex: 'equipment1', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검2', dataIndex: 'equipment2', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검3', dataIndex: 'equipment3', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검4', dataIndex: 'equipment4', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검5', dataIndex: 'equipment5', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검6', dataIndex: 'equipment6', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검7', dataIndex: 'equipment7', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검8', dataIndex: 'equipment8', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '점검9', dataIndex: 'equipment9', width: 70, align: 'center', render: (v) => v ?? '-' },
      { title: '주의사항', dataIndex: 'precaution', width: 180, render: (v) => v ?? '-' },
      { title: '주의확인', dataIndex: 'precautionChk', width: 80, align: 'right', render: (v) => v ?? '-' },
      { title: '근무구분2', dataIndex: 'workingCategory2', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분3', dataIndex: 'workingCategory3', width: 90, render: (v) => v ?? '-' },
      { title: '부근무유형', dataIndex: 'secondWorkType', width: 100, render: (v) => v ?? '-' },
      { title: '소유자명', dataIndex: 'custName', width: 120, render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>조회일자:</span>
        <DatePicker value={date} onChange={(v) => v && setDate(v)} allowClear={false} />
        <Button type="primary" onClick={handleSearch} loading={query.isLoading}>
          조회
        </Button>
        {queryDate != null && (
          <RefreshButton onRefresh={query.refetch} refreshing={query.isFetching} />
        )}
        <Button onClick={handleExport} disabled={!query.data || query.data.items.length === 0}>
          엑셀 다운로드
        </Button>
      </Space>

      {queryDate != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">{queryDate} 안전점검 현황 (RPA)</Text>
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
          rowKey={(r, idx) => `${r.employeeCode}-${r.accountBranchCode ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText:
              queryDate == null ? '조회일자를 선택하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
