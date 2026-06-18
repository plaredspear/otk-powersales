import { useState } from 'react';
import { Button, Card, DatePicker, Input, Select, Space, Tag } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { usePPTHistories } from '@/hooks/promotion/usePPTHistories';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { EXCEL_EXPORT_MAX_ROWS } from '@/lib/excelDownload';
import { PPT_HISTORY_EXPORT_PATH, type PPTHistory } from '@/api/pptMaster';
import {
  PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
  getPPTTeamTypeColor,
} from '@/constants/pptTeamType';
import PPTHistoryDetailModal from './components/PPTHistoryDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const TEAM_TYPE_FILTER_OPTIONS = [
  { value: '', label: '전체' },
  ...PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
];

const DEFAULT_SIZE = 20;

export default function PPTHistoryPage() {
  // page/필터를 URL query string 에 보관 — 새로고침/뒤로가기/공유 시 직전 조건 복원.
  const { page, setPage, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      employeeName: '',
      employeeCode: '',
      teamType: '',
      changedAtFrom: '',
      changedAtTo: '',
      size: String(DEFAULT_SIZE),
    },
  });
  const size = Number.parseInt(filters.size, 10) || DEFAULT_SIZE;

  // 입력 위젯은 편집 버퍼 — URL 이 source of truth. 마운트 시 URL 값으로 초기화.
  const [filterEmployeeName, setFilterEmployeeName] = useState(filters.employeeName);
  const [filterEmployeeCode, setFilterEmployeeCode] = useState(filters.employeeCode);
  const [filterTeamType, setFilterTeamType] = useState(filters.teamType);
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
    changedAtFrom: filters.changedAtFrom || undefined,
    changedAtTo: filters.changedAtTo || undefined,
  });

  const handleSearch = () => {
    setFilters({
      employeeName: filterEmployeeName,
      employeeCode: filterEmployeeCode,
      teamType: filterTeamType,
      changedAtFrom: filterChangedRange?.[0]?.format('YYYY-MM-DD') ?? '',
      changedAtTo: filterChangedRange?.[1]?.format('YYYY-MM-DD') ?? '',
    });
  };

  const handleReset = () => {
    setFilterEmployeeName('');
    setFilterEmployeeCode('');
    setFilterTeamType('');
    setFilterChangedRange(null);
    setFilters({
      employeeName: '',
      employeeCode: '',
      teamType: '',
      changedAtFrom: '',
      changedAtTo: '',
      size: String(DEFAULT_SIZE),
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
  ];

  return (
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
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

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: data?.totalElements ?? 0,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          onChange: (nextPage, pageSize) => {
            if (pageSize !== size) {
              setFilters({ size: String(pageSize) });
            } else {
              setPage(nextPage - 1);
            }
          },
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record),
          style: { cursor: 'pointer' },
        })}
        scroll={{ x: 970 }}
        size="middle"
      />

      <PPTHistoryDetailModal
        open={detailOpen}
        history={selectedHistory}
        onClose={() => setDetailOpen(false)}
      />
    </div>
  );
}
