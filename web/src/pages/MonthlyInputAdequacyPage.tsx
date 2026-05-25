import { useMemo, useState } from 'react';
import { Alert, Radio, Space, Spin, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchMatrix,
  exportMatrix as apiExportMatrix,
  type MonthlyInputAdequacyItem,
} from '@/api/monthlyInputAdequacy';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';

const { Text } = Typography;

const WORKING_CATEGORY_3_OPTIONS = ['전체', '고정', '격고', '순회'];

function suitabilityCellStyle(suitability: string): React.CSSProperties {
  switch (suitability) {
    case '적합':
      return { backgroundColor: '#C8E6C9' };
    case '경계':
      return { backgroundColor: '#FFF59D' };
    case '재검토':
      return { backgroundColor: '#FFCDD2' };
    default:
      return {};
  }
}

interface QueryParams {
  year: number;
  codes: string[];
  workingCategory3: string;
}

export default function MonthlyInputAdequacyPage() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState<number>(currentYear);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [workingCategory3, setWorkingCategory3] = useState<string>('전체');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const matrixQuery = useQuery({
    queryKey: ['monthlyInputAdequacy', 'matrix', queryParams],
    queryFn: () => {
      const p = queryParams!;
      const filter = p.workingCategory3 !== '전체' ? p.workingCategory3 : undefined;
      return fetchMatrix(p.year, p.codes, filter);
    },
    enabled: queryParams != null,
  });

  const handleSearch = () => {
    if (year == null) {
      message.warning('년도는 필수항목입니다.');
      return;
    }
    if (selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    setQueryParams({ year, codes: selectedCodes, workingCategory3 });
  };

  const handleExport = async () => {
    if (!queryParams) return;
    try {
      const filter = queryParams.workingCategory3 !== '전체' ? queryParams.workingCategory3 : undefined;
      await apiExportMatrix(queryParams.year, queryParams.codes, filter);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<MonthlyInputAdequacyItem> = useMemo(() => {
    const base: ColumnsType<MonthlyInputAdequacyItem> = [
      { title: '소속', dataIndex: 'branchName', width: 100, fixed: 'left' },
      { title: '근무형태3', dataIndex: 'workingCategory3', width: 90, fixed: 'left', render: (v) => v ?? '-' },
      { title: '이름', dataIndex: 'employeeName', width: 90, fixed: 'left' },
      { title: '사번', dataIndex: 'employeeCode', width: 90 },
      { title: '직위', dataIndex: 'title', width: 70, render: (v) => v ?? '-' },
      { title: '거래처유형', dataIndex: 'accountCategory', width: 100 },
      { title: '거래처명', dataIndex: 'accountName', width: 140 },
      { title: '거래처코드', dataIndex: 'accountCode', width: 110 },
    ];
    const monthCols: ColumnsType<MonthlyInputAdequacyItem> = (Array.from({ length: 12 }, (_, i) => i + 1)).map((m) => ({
      title: `${m}월`,
      dataIndex: ['monthlySuitability', m - 1],
      width: 80,
      align: 'center' as const,
      render: (_: unknown, row) => {
        const label = row.monthlySuitability[m - 1] ?? '';
        return label;
      },
      onCell: (row) => ({ style: suitabilityCellStyle(row.monthlySuitability[m - 1] ?? '') }),
    }));
    return [...base, ...monthCols];
  }, []);

  const workingCategory3Filter = (
    <Space direction="vertical" size={4}>
      <span>근무형태3:</span>
      <Radio.Group
        value={workingCategory3}
        onChange={(e) => setWorkingCategory3(e.target.value)}
      >
        {WORKING_CATEGORY_3_OPTIONS.map((v) => (
          <Radio key={v} value={v}>
            {v}
          </Radio>
        ))}
      </Radio.Group>
    </Space>
  );

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        year={year}
        selectedCodes={selectedCodes}
        onYearChange={setYear}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!matrixQuery.data || matrixQuery.data.items.length === 0}
        searchLoading={matrixQuery.isLoading}
        showMonth={false}
        extraFilters={workingCategory3Filter}
      />

      {queryParams != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {queryParams.year}년 · {queryParams.codes.length}개 지점
            {queryParams.workingCategory3 !== '전체' && ` · 근무형태3: ${queryParams.workingCategory3}`}
          </Text>
        </div>
      )}

      {matrixQuery.isError && (
        <Alert
          type="error"
          message={(matrixQuery.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {matrixQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <Table
          rowKey={(r) => `${r.employeeCode}-${r.accountCode}-${r.workingCategory3 ?? ''}`}
          size="small"
          columns={columns}
          dataSource={matrixQuery.data?.items ?? []}
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
