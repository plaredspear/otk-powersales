import { useMemo, useState } from 'react';
import { Alert, DatePicker, Space, Typography, message } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchPlacementCheck,
  exportPlacementCheck as apiExportPlacementCheck,
  type FemaleEmployeePlacementCheckItem,
} from '@/api/femaleEmployeePlacementCheck';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useReportBranches } from '@/hooks/female-employee/useReportBranches';
import RefreshButton from '@/components/common/RefreshButton';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';

const { Text } = Typography;

interface QueryParams {
  /** YYYY-MM-DD */
  startDate: string;
  /** YYYY-MM-DD */
  endDate: string;
  codes: string[];
}

/** 서버(`AdminFemaleEmployeePlacementCheckService.MAX_RANGE_DAYS`) 와 동일한 조회기간 상한 (일). */
const MAX_RANGE_DAYS = 92;

/**
 * 여사원 배치 점검 현황 (영업지원실용) — SF Report `new_report_4Ic` 이식 (Spec #839).
 *
 * 조회기간(시작일~종료일, 최대 92일) + 지점(선택) 으로 여사원/조장의 배치 현황을 조회 (퇴직자 포함).
 * 21컬럼 그리드 + 엑셀 다운로드. 기본 기간은 당월 1일~말일.
 * 나이/근속연수는 SF formula 정합상 조회기간이 아닌 **오늘** 기준이며, 여사원이 아닌 행(조장) 은 공백이다.
 */
export default function FemaleEmployeePlacementCheckPage() {
  // 지점 옵션 — 보고서 공용 /report-branches (안전점검·환산인원과 동일 소스로 통일)
  const { data: reportBranches = [], isLoading: branchesLoading } = useReportBranches();
  // 기본 기간 = 당월 1일~말일 (종전 "조회월" 기본값과 동일 범위).
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().startOf('month'),
    dayjs().endOf('month'),
  ]);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const query = useQuery({
    queryKey: ['femaleEmployeePlacementCheck', queryParams],
    queryFn: () => {
      const p = queryParams!;
      return fetchPlacementCheck(p.startDate, p.endDate, p.codes);
    },
    enabled: queryParams != null,
  });

  // 기간 검증은 서버(400) 도 하지만, 조회 버튼을 미리 막아 왕복 없이 사유를 보여준다.
  const rangeDays = dateRange[1].diff(dateRange[0], 'day');
  const rangeInvalid = rangeDays < 0 || rangeDays > MAX_RANGE_DAYS;

  const handleSearch = () => {
    if (rangeDays < 0) {
      message.warning('시작일은 종료일보다 이후일 수 없습니다.');
      return;
    }
    if (rangeDays > MAX_RANGE_DAYS) {
      message.warning(`조회 기간은 최대 ${MAX_RANGE_DAYS}일까지 가능합니다.`);
      return;
    }
    setQueryParams({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      codes: selectedCodes,
    });
  };

  const handleExport = async () => {
    if (!queryParams) return;
    try {
      await apiExportPlacementCheck(queryParams.startDate, queryParams.endDate, queryParams.codes);
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
      { title: 'SAP거래처코드', dataIndex: 'accountSapCode', width: 120, render: (v) => v ?? '-' },
      { title: '거래처지점명', dataIndex: 'accountBranchName', width: 120, render: (v) => v ?? '-' },
      { title: '근무구분1', dataIndex: 'workingCategory1', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분2', dataIndex: 'workingCategory2', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분3', dataIndex: 'workingCategory3', width: 90, render: (v) => v ?? '-' },
      { title: '부근무유형', dataIndex: 'secondWorkType', width: 100, render: (v) => v ?? '-' },
      { title: '근무구분5', dataIndex: 'workingCategory5', width: 90, render: (v) => v ?? '-' },
      { title: '출근일자', dataIndex: 'commuteDate', width: 160, render: (v) => v ?? '-' },
      { title: '근무보고여부', dataIndex: 'isWorkReport', width: 110, render: (v) => v || '-' },
      { title: '입사일', dataIndex: 'startDate', width: 110, render: (v) => v ?? '-' },
      // 나이/근속연수는 SF formula 정합 문자열("41살"/"7년") — 여사원이 아닌 행(조장) 은 SF 와 동일하게 공백.
      { title: '나이', dataIndex: 'age', width: 70, render: (v) => v ?? '-' },
      { title: '근속연수', dataIndex: 'yearsOfService', width: 80, render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        branches={reportBranches}
        branchesLoading={branchesLoading}
        selectedCodes={selectedCodes}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!query.data || query.data.items.length === 0}
        searchLoading={query.isLoading}
        searchDisabled={rangeInvalid}
        periodFilter={
          <Space direction="vertical" size={4}>
            <span>조회기간:</span>
            <DatePicker.RangePicker
              value={dateRange}
              onChange={(range) => {
                if (range?.[0] && range?.[1]) setDateRange([range[0], range[1]]);
              }}
              allowClear={false}
              format="YYYY-MM-DD"
              style={{ width: 260 }}
            />
          </Space>
        }
        extraActions={
          queryParams != null ? (
            <RefreshButton onRefresh={query.refetch} refreshing={query.isFetching} />
          ) : undefined
        }
      />

      {queryParams != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {queryParams.startDate} ~ {queryParams.endDate} ·{' '}
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

      <ResizableTable
        rowKey={(r, idx) => `${r.employeeCode}-${r.accountSapCode ?? ''}-${r.workingDate ?? ''}-${idx}`}
        size="small"
        columns={columns}
        dataSource={query.data?.items ?? []}
        loading={query.isLoading}
        pagination={false}
        scroll={{ x: 'max-content', y: 'calc(100vh - 320px)' }}
        locale={listTableLocale({ searched: queryParams != null })}
        summary={() =>
          query.data && query.data.items.length > 0 ? (
            <ResizableTable.Summary fixed>
              <ResizableTable.Summary.Row>
                <ResizableTable.Summary.Cell index={0} colSpan={columns.length}>
                  <Text strong>총 {query.data.items.length}건</Text>
                </ResizableTable.Summary.Cell>
              </ResizableTable.Summary.Row>
            </ResizableTable.Summary>
          ) : null
        }
      />
    </div>
  );
}
