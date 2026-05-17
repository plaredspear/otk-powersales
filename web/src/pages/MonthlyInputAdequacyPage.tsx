import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Empty,
  InputNumber,
  Radio,
  Spin,
  Table,
  Typography,
  message,
} from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchMatrix,
  exportMatrix as apiExportMatrix,
  type MonthlyInputAdequacyItem,
} from '@/api/monthlyInputAdequacy';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

const { Title, Text } = Typography;

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
  allBranches: boolean;
}

export default function MonthlyInputAdequacyPage() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState<number>(currentYear);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [allBranchesChecked, setAllBranchesChecked] = useState(false);
  const [workingCategory3, setWorkingCategory3] = useState<string>('전체');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const { data: branches = [] } = useTeamScheduleBranches();

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
    if (!allBranchesChecked && selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    const codes = allBranchesChecked ? branches.map((b) => b.branchCode) : selectedCodes;
    setQueryParams({ year, codes, workingCategory3, allBranches: allBranchesChecked });
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

  const handleAllBranchesToggle = (checked: boolean) => {
    setAllBranchesChecked(checked);
    if (checked) setSelectedCodes([]);
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

  const showEmpty = matrixQuery.data != null && matrixQuery.data.items.length === 0;

  return (
    <div style={{ display: 'flex', height: '100%', minHeight: 600 }}>
      <div style={{ width: 280, padding: 16, borderRight: '1px solid #f0f0f0', overflowY: 'auto' }}>
        <Title level={5}>검색 조건</Title>

        <div style={{ marginBottom: 12 }}>
          <Text strong>년도</Text>
          <div style={{ marginTop: 4 }}>
            <InputNumber
              value={year}
              min={2020}
              max={2099}
              onChange={(v) => v != null && setYear(v)}
              style={{ width: 100 }}
            />
          </div>
        </div>

        <div style={{ marginBottom: 12 }}>
          <Text strong>근무형태3</Text>
          <Radio.Group
            value={workingCategory3}
            onChange={(e) => setWorkingCategory3(e.target.value)}
            style={{ marginTop: 4, display: 'block' }}
          >
            {WORKING_CATEGORY_3_OPTIONS.map((v) => (
              <Radio key={v} value={v}>
                {v}
              </Radio>
            ))}
          </Radio.Group>
        </div>

        <div style={{ marginBottom: 12 }}>
          <Text strong>지점</Text>
          <div style={{ marginTop: 4 }}>
            <Checkbox
              checked={allBranchesChecked}
              onChange={(e) => handleAllBranchesToggle(e.target.checked)}
            >
              전체선택
            </Checkbox>
          </div>
          <div
            style={{
              marginTop: 4,
              maxHeight: 200,
              overflowY: 'auto',
              border: '1px solid #f0f0f0',
              padding: 4,
              opacity: allBranchesChecked ? 0.5 : 1,
            }}
          >
            <Checkbox.Group
              value={selectedCodes}
              onChange={(vals) => setSelectedCodes(vals as string[])}
              disabled={allBranchesChecked}
              style={{ display: 'flex', flexDirection: 'column' }}
            >
              {branches.map((b) => (
                <Checkbox key={b.branchCode} value={b.branchCode}>
                  {b.branchName}
                </Checkbox>
              ))}
            </Checkbox.Group>
          </div>
        </div>

        <Button type="primary" icon={<SearchOutlined />} block onClick={handleSearch}>
          조회하기
        </Button>
      </div>

      <div style={{ flex: 1, padding: 16, overflowX: 'auto', minWidth: 0 }}>
        <Title level={4}>월별 진열사원 투입적합성 현황</Title>

        {queryParams == null && <Empty description="좌측에서 조건을 선택한 뒤 조회하기를 눌러주세요." />}

        {queryParams != null && (
          <>
            <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text type="secondary">
                {queryParams.year}년 · {queryParams.allBranches ? '전체지점' : `${queryParams.codes.length}개 지점`}
                {queryParams.workingCategory3 !== '전체' && ` · 근무형태3: ${queryParams.workingCategory3}`}
              </Text>
              <Button
                icon={<DownloadOutlined />}
                onClick={handleExport}
                disabled={!matrixQuery.data || matrixQuery.data.items.length === 0}
              >
                엑셀다운로드
              </Button>
            </div>
            {matrixQuery.isLoading && <Spin />}
            {matrixQuery.isError && (
              <Alert type="error" message={(matrixQuery.error as Error)?.message ?? '조회 실패'} />
            )}
            {showEmpty && <Alert type="warning" message="검색결과가 없습니다." showIcon />}
            {matrixQuery.data && matrixQuery.data.items.length > 0 && (
              <Table
                rowKey={(r) => `${r.employeeCode}-${r.accountCode}`}
                size="small"
                columns={columns}
                dataSource={matrixQuery.data.items}
                pagination={false}
                scroll={{ x: 'max-content' }}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}
