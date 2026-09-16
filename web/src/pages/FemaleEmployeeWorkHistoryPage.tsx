import { useMemo, useState } from 'react';
import { Alert, DatePicker, Input, Space, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  fetchWorkHistory,
  exportWorkHistory as apiExportWorkHistory,
  type FemaleEmployeeWorkHistoryItem,
} from '@/api/femaleEmployeeWorkHistory';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useReportBranches } from '@/hooks/female-employee/useReportBranches';
import RefreshButton from '@/components/common/RefreshButton';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';

const { Text } = Typography;

interface QueryParams {
  employeeCode: string;
  /** YYYY-MM-DD */
  startDate: string;
  /** YYYY-MM-DD */
  endDate: string;
  codes: string[];
}

/** 서버(`AdminFemaleEmployeeWorkHistoryService.MAX_RANGE_DAYS`) 와 동일한 조회기간 상한 (일). */
const MAX_RANGE_DAYS = 366;

/**
 * 여사원 근무내역 (개인별 조회) — SF Report `new_report_nEX` 이식 (Spec #840).
 *
 * 사번 + 조회기간(시작일~종료일, 최대 366일) + 지점(선택) 으로 특정 여사원의 근무내역을 조회.
 * 기본 기간은 당월 1일~말일. 지점 스코프는 배치 점검(#839)과 동일 —
 * 전사 권한자는 선택 지점으로 좁히고, 지점 사용자는 본인 소속 지점(costCenterCode) 으로 강제
 * (backend DataScope 가드). 레거시 SF Report 는 전사(scope=organization) 였으나 지점 필터는 신규 도입.
 * 15컬럼 그리드 + 엑셀 다운로드.
 */
export default function FemaleEmployeeWorkHistoryPage() {
  // 지점 옵션 — 보고서 공용 /report-branches (안전점검·환산인원과 동일 소스로 통일)
  const { data: reportBranches = [], isLoading: branchesLoading } = useReportBranches();
  const [employeeCode, setEmployeeCode] = useState<string>('');
  // 기본 기간 = 당월 1일~말일 (종전 "조회월" 기본값과 동일 범위).
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().startOf('month'),
    dayjs().endOf('month'),
  ]);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const query = useQuery({
    queryKey: ['femaleEmployeeWorkHistory', queryParams],
    queryFn: () => {
      const p = queryParams!;
      return fetchWorkHistory(p.employeeCode, p.startDate, p.endDate, p.codes);
    },
    enabled: queryParams != null,
  });

  // 기간 검증은 서버(400) 도 하지만, 조회 버튼을 미리 막아 왕복 없이 사유를 보여준다.
  const rangeDays = dateRange[1].diff(dateRange[0], 'day');
  const rangeInvalid = rangeDays < 0 || rangeDays > MAX_RANGE_DAYS;

  const handleSearch = () => {
    const code = employeeCode.trim();
    if (!code) {
      message.warning('사번은 필수항목입니다.');
      return;
    }
    if (rangeDays < 0) {
      message.warning('시작일은 종료일보다 이후일 수 없습니다.');
      return;
    }
    if (rangeDays > MAX_RANGE_DAYS) {
      message.warning(`조회 기간은 최대 ${MAX_RANGE_DAYS}일까지 가능합니다.`);
      return;
    }
    setQueryParams({
      employeeCode: code,
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      codes: selectedCodes,
    });
  };

  const handleExport = async () => {
    if (!queryParams) return;
    try {
      await apiExportWorkHistory(
        queryParams.employeeCode,
        queryParams.startDate,
        queryParams.endDate,
        queryParams.codes,
      );
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<FemaleEmployeeWorkHistoryItem> = useMemo(
    () => [
      { title: '일정명', dataIndex: 'scheduleName', width: 160, fixed: 'left', render: (v) => v ?? '-' },
      { title: '성명', dataIndex: 'name', width: 90, fixed: 'left' },
      { title: '사번', dataIndex: 'employeeCode', width: 100 },
      { title: '나이', dataIndex: 'age', width: 70, render: (v) => v ?? '-' },
      { title: '근무일자', dataIndex: 'workingDate', width: 110, render: (v) => v ?? '-' },
      { title: '거래처지점명', dataIndex: 'accountBranchName', width: 120, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountSapCode', width: 110, render: (v) => v ?? '-' },
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
      <PeriodBranchFilterBar
        branches={reportBranches}
        branchesLoading={branchesLoading}
        selectedCodes={selectedCodes}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!query.data || query.data.items.length === 0}
        searchLoading={query.isLoading}
        searchDisabled={employeeCode.trim().length === 0 || rangeInvalid}
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
        extraFilters={
          <Space direction="vertical" size={4}>
            <span>사번:</span>
            <Input
              value={employeeCode}
              onChange={(e) => setEmployeeCode(e.target.value)}
              onPressEnter={handleSearch}
              placeholder="사번 입력"
              style={{ width: 160 }}
              allowClear
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
            사번 {queryParams.employeeCode} · {queryParams.startDate} ~ {queryParams.endDate} ·{' '}
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
        rowKey={(r, idx) => `${r.employeeCode}-${r.workingDate ?? ''}-${idx}`}
        size="small"
        columns={columns}
        dataSource={query.data?.items ?? []}
        loading={query.isLoading}
        pagination={false}
        scroll={{ x: 'max-content' }}
        locale={listTableLocale({
          searched: queryParams != null,
          beforeSearchText: '사번·조회기간·지점을 선택한 후 조회 버튼을 눌러주세요.',
        })}
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
