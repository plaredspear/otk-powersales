import { useState } from 'react';
import { Button, Card, DatePicker, Input, Select, Space, Tag } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { usePPTHistories } from '@/hooks/promotion/usePPTHistories';
import { usePPTBranches } from '@/hooks/promotion/usePPTBranches';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import { EXCEL_EXPORT_MAX_ROWS } from '@/lib/excelDownload';
import { PPT_HISTORY_EXPORT_PATH, type PPTHistory } from '@/api/pptMaster';
import {
  PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
  getPPTTeamTypeColor,
} from '@/constants/pptTeamType';
import PPTHistoryDetailModal from './components/PPTHistoryDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

const TEAM_TYPE_FILTER_OPTIONS = [
  { value: '', label: '전체' },
  ...PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
];

export default function PPTHistoryPage() {
  // 페이지 전체 스크롤 제거 — 필터 카드는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소
  // (대행 배너 등)를 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/필터/사이즈를 URL query string 에 보관 — 새로고침/뒤로가기/공유 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      employeeName: '',
      employeeCode: '',
      teamType: '',
      branchCode: '',
      changedAtFrom: '',
      changedAtTo: '',
    },
  });

  // 지점 셀렉터 — 권한별 지점 화이트리스트.
  //  - 다중 지점: Select 로 선택
  //  - 단일 지점(조장 등): 고정 Tag 로 지점명 표시 (PeriodBranchFilterBar 정합).
  const { data: branches } = usePPTBranches();
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));
  const singleBranch = branches?.length === 1 ? branches[0] : null;
  const isMultiBranch = (branches?.length ?? 0) > 1;

  // 입력 위젯은 편집 버퍼 — URL 이 source of truth. 마운트 시 URL 값으로 초기화.
  const [filterEmployeeName, setFilterEmployeeName] = useState(filters.employeeName);
  const [filterEmployeeCode, setFilterEmployeeCode] = useState(filters.employeeCode);
  const [filterTeamType, setFilterTeamType] = useState(filters.teamType);
  const [filterBranchCode, setFilterBranchCode] = useState(filters.branchCode);
  const [filterChangedRange, setFilterChangedRange] = useState<[Dayjs | null, Dayjs | null] | null>(
    () =>
      filters.changedAtFrom || filters.changedAtTo
        ? [
            filters.changedAtFrom ? dayjs(filters.changedAtFrom) : null,
            filters.changedAtTo ? dayjs(filters.changedAtTo) : null,
          ]
        : null,
  );

  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedHistory, setSelectedHistory] = useState<PPTHistory | null>(null);

  const { run: runExport, downloading: exporting } = useExcelDownload();

  const { data, isLoading, refetch, isFetching } = usePPTHistories({
    page,
    size,
    employeeName: filters.employeeName || undefined,
    employeeCode: filters.employeeCode || undefined,
    teamType: filters.teamType || undefined,
    branchCode: filters.branchCode || undefined,
    changedAtFrom: filters.changedAtFrom || undefined,
    changedAtTo: filters.changedAtTo || undefined,
  });

  const handleSearch = () => {
    setFilters({
      employeeName: filterEmployeeName,
      employeeCode: filterEmployeeCode,
      teamType: filterTeamType,
      branchCode: filterBranchCode,
      changedAtFrom: filterChangedRange?.[0]?.format('YYYY-MM-DD') ?? '',
      changedAtTo: filterChangedRange?.[1]?.format('YYYY-MM-DD') ?? '',
    });
  };

  const handleReset = () => {
    setFilterEmployeeName('');
    setFilterEmployeeCode('');
    setFilterTeamType('');
    setFilterBranchCode('');
    setFilterChangedRange(null);
    setFilters({
      employeeName: '',
      employeeCode: '',
      teamType: '',
      branchCode: '',
      changedAtFrom: '',
      changedAtTo: '',
    });
  };

  const handleRowClick = (record: PPTHistory) => {
    setSelectedHistory(record);
    setDetailOpen(true);
  };

  // 현재 적용된 검색 조건(URL filters)을 그대로 전량 export (목록과 동일 가시 범위).
  const handleExport = () => {
    runExport(PPT_HISTORY_EXPORT_PATH, `전문행사조이력_${dayjs().format('YYYYMMDD')}.xlsx`, {
      params: {
        ...(filters.employeeName ? { employeeName: filters.employeeName } : {}),
        ...(filters.employeeCode ? { employeeCode: filters.employeeCode } : {}),
        ...(filters.teamType ? { teamType: filters.teamType } : {}),
        ...(filters.branchCode ? { branchCode: filters.branchCode } : {}),
        ...(filters.changedAtFrom ? { changedAtFrom: filters.changedAtFrom } : {}),
        ...(filters.changedAtTo ? { changedAtTo: filters.changedAtTo } : {}),
      },
      totalCount: data?.totalElements,
      maxRows: EXCEL_EXPORT_MAX_ROWS,
    });
  };

  const columns: ColumnsType<PPTHistory> = [
    {
      title: '전문행사조 이력번호',
      dataIndex: 'name',
      width: 160,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '소속',
      dataIndex: 'orgName',
      width: 160,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 110,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '사원',
      dataIndex: 'employeeName',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '변경 전',
      dataIndex: 'oldValue',
      width: 140,
      align: 'center',
      render: (val: string | null) =>
        val == null ? '-' : <Tag color={getPPTTeamTypeColor(val)}>{val}</Tag>,
    },
    {
      title: '변경 후',
      dataIndex: 'newValue',
      width: 140,
      align: 'center',
      render: (val: string) => <Tag color={getPPTTeamTypeColor(val)}>{val}</Tag>,
    },
    {
      title: '변경 시점',
      dataIndex: 'changedAt',
      width: 160,
      align: 'center',
      render: (val: string) => dayjs(val).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '거래처명',
      dataIndex: 'accountName',
      width: 160,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처코드',
      dataIndex: 'accountCode',
      width: 120,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
  ];

  return (
    // 페이지 전체 스크롤 제거 — 컨테이너를 실측 가용 높이에 고정. 필터 카드는 고정, 테이블 body 만 스크롤.
    <div
      ref={containerRef}
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: containerHeight,
        minHeight: 0,
        boxSizing: 'border-box',
      }}
    >
      <Card size="small" style={{ marginBottom: 16, flexShrink: 0 }}>
        <Space wrap>
          {isMultiBranch && (
            <Select
              placeholder="지점 (전체)"
              value={filterBranchCode || undefined}
              onChange={(v) => setFilterBranchCode(v ?? '')}
              style={{ width: 160 }}
              options={branchOptions}
              allowClear
              showSearch
              optionFilterProp="label"
            />
          )}
          {singleBranch && (
            <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
              지점: {singleBranch.branchName}
            </Tag>
          )}
          <Input
            placeholder="사원명"
            value={filterEmployeeName}
            onChange={(e) => setFilterEmployeeName(e.target.value)}
            style={{ width: 120 }}
            onPressEnter={handleSearch}
          />
          <Input
            placeholder="사번"
            value={filterEmployeeCode}
            onChange={(e) => setFilterEmployeeCode(e.target.value)}
            style={{ width: 120 }}
            onPressEnter={handleSearch}
          />
          <Select
            placeholder="전문행사조"
            value={filterTeamType}
            onChange={setFilterTeamType}
            style={{ width: 160 }}
            options={TEAM_TYPE_FILTER_OPTIONS}
          />
          <DatePicker.RangePicker
            value={filterChangedRange ?? undefined}
            onChange={(range) => setFilterChangedRange(range as [Dayjs | null, Dayjs | null] | null)}
            placeholder={['변경일 시작', '변경일 종료']}
          />
          <Button onClick={handleReset}>초기화</Button>
          <Button type="primary" onClick={handleSearch}>
            조회
          </Button>
          <Button icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
            엑셀 다운로드
          </Button>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </Space>
      </Card>

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이에서 헤더 행/페이지네이션을 뺀 값이 scrollY. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={isLoading}
          locale={listTableLocale()}
          pagination={buildListPagination({
            page,
            pageSize: size,
            total: data?.totalElements ?? 0,
            // 사이즈 변경 시 setSize 가 page 를 0 으로 자동 리셋(useListQueryParams). 순수 이동은 setPage.
            onPageChange: setPage,
            onSizeChange: setSize,
          })}
          onRow={(record) => ({
            onClick: () => handleRowClick(record),
            style: { cursor: 'pointer' },
          })}
          // 테이블 body(행) 만 세로 스크롤. y 는 wrapper 실측 높이(하드코딩 없음).
          scroll={{ x: 970, y: scrollY }}
        />
      </div>

      <PPTHistoryDetailModal
        open={detailOpen}
        history={selectedHistory}
        onClose={() => setDetailOpen(false)}
      />
    </div>
  );
}
