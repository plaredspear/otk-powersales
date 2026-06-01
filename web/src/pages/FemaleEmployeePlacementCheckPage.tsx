import { useMemo, useState } from 'react';
import { Alert, Spin, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchPlacementCheck,
  exportPlacementCheck as apiExportPlacementCheck,
  type FemaleEmployeePlacementCheckItem,
} from '@/api/femaleEmployeePlacementCheck';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;

interface QueryParams {
  year: number;
  month: number;
  codes: string[];
}

/**
 * 여사원 배치 점검 현황 (영업지원실용) — SF Report `new_report_4Ic` 이식 (Spec #839).
 *
 * 년·월 + 지점(선택) 으로 여사원/조장의 월간 배치 현황을 조회 (퇴직자 포함). 21컬럼 그리드 + 엑셀 다운로드.
 */
export default function FemaleEmployeePlacementCheckPage() {
  const now = new Date();
  const [year, setYear] = useState<number>(now.getFullYear());
  const [month, setMonth] = useState<number>(now.getMonth() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const query = useQuery({
    queryKey: ['femaleEmployeePlacementCheck', queryParams],
    queryFn: () => {
      const p = queryParams!;
      return fetchPlacementCheck(p.year, p.month, p.codes);
    },
    enabled: queryParams != null,
  });

  const handleSearch = () => {
    if (year == null || month == null) {
      message.warning('년·월은 필수항목입니다.');
      return;
    }
    setQueryParams({ year, month, codes: selectedCodes });
  };

  const handleExport = async () => {
    if (!queryParams) return;
    try {
      await apiExportPlacementCheck(queryParams.year, queryParams.month, queryParams.codes);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<FemaleEmployeePlacementCheckItem> = useMemo(
    () => [
      { title: '근무일자', dataIndex: 'workingDate', width: 110, fixed: 'left', render: (v) => v ?? '-' },
      { title: '소속', dataIndex: 'orgName', width: 100, fixed: 'left', render: (v) => v ?? '-' },
      { title: '사번', dataIndex: 'employeeCode', width: 90, fixed: 'left' },
      { title: '직위', dataIndex: 'jikwee', width: 70, render: (v) => v ?? '-' },
      { title: '성명', dataIndex: 'name', width: 90 },
      { title: '전문행사조', dataIndex: 'professionalPromotionTeam', width: 110, render: (v) => v ?? '-' },
      { title: '재직상태', dataIndex: 'employmentStatus', width: 80, render: (v) => v ?? '-' },
      { title: '거래처유형', dataIndex: 'accountType', width: 100, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 160, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountBranchCode', width: 110, render: (v) => v ?? '-' },
      { title: '거래처지점명', dataIndex: 'accountBranchName', width: 120, render: (v) => v ?? '-' },
      { title: '근무구분1', dataIndex: 'workingCategory1', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분2', dataIndex: 'workingCategory2', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분3', dataIndex: 'workingCategory3', width: 90, render: (v) => v ?? '-' },
      { title: '부근무유형', dataIndex: 'secondWorkType', width: 100, render: (v) => v ?? '-' },
      { title: '근무구분5', dataIndex: 'workingCategory5', width: 90, render: (v) => v ?? '-' },
      { title: '출근일자', dataIndex: 'commuteDate', width: 160, render: (v) => v ?? '-' },
      { title: '근무보고여부', dataIndex: 'isWorkReport', width: 110, render: (v) => v || '-' },
      { title: '입사일', dataIndex: 'startDate', width: 110, render: (v) => v ?? '-' },
      { title: '나이', dataIndex: 'age', width: 70, align: 'right', render: (v) => v ?? '-' },
      { title: '근속연수', dataIndex: 'yearsOfService', width: 80, align: 'right', render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        year={year}
        month={month}
        selectedCodes={selectedCodes}
        onYearChange={setYear}
        onMonthChange={setMonth}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!query.data || query.data.items.length === 0}
        searchLoading={query.isLoading}
        showMonth
      />

      {queryParams != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {queryParams.year}년 {queryParams.month}월 ·{' '}
            {queryParams.codes.length > 0 ? `${queryParams.codes.length}개 지점` : '전체 지점'}
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
          rowKey={(r, idx) => `${r.employeeCode}-${r.accountBranchCode ?? ''}-${r.workingDate ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText:
              queryParams == null ? '조회 조건을 설정하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
