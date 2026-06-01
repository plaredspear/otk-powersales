import { useState } from 'react';
import { Button, Card, DatePicker, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { usePPTHistories } from '@/hooks/promotion/usePPTHistories';
import type { PPTHistory, PPTHistorySearchParams } from '@/api/pptMaster';
import {
  PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
  getPPTTeamTypeColor,
} from '@/constants/pptTeamType';
import PPTHistoryDetailModal from './components/PPTHistoryDetailModal';
import ResizableTable from '@/components/common/ResizableTable';

const TEAM_TYPE_FILTER_OPTIONS = [
  { value: '', label: '전체' },
  ...PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
];

const DEFAULT_PARAMS: PPTHistorySearchParams = {
  page: 0,
  size: 20,
};

function statusColor(status: string | null | undefined): string {
  if (status == null) return 'default';
  if (status === '재직') return 'green';
  if (status === '휴직') return 'warning';
  return 'default';
}

export default function PPTHistoryPage() {
  const [searchParams, setSearchParams] = useState<PPTHistorySearchParams>(DEFAULT_PARAMS);
  const [filterEmployeeName, setFilterEmployeeName] = useState('');
  const [filterEmployeeCode, setFilterEmployeeCode] = useState('');
  const [filterTeamType, setFilterTeamType] = useState('');
  const [filterChangedRange, setFilterChangedRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedHistory, setSelectedHistory] = useState<PPTHistory | null>(null);

  const { data, isLoading } = usePPTHistories(searchParams);

  const handleSearch = () => {
    setSearchParams({
      ...searchParams,
      page: 0,
      employeeName: filterEmployeeName || undefined,
      employeeCode: filterEmployeeCode || undefined,
      teamType: filterTeamType || undefined,
      changedAtFrom: filterChangedRange?.[0]?.format('YYYY-MM-DD') || undefined,
      changedAtTo: filterChangedRange?.[1]?.format('YYYY-MM-DD') || undefined,
    });
  };

  const handleReset = () => {
    setFilterEmployeeName('');
    setFilterEmployeeCode('');
    setFilterTeamType('');
    setFilterChangedRange(null);
    setSearchParams(DEFAULT_PARAMS);
  };

  const handleRowClick = (record: PPTHistory) => {
    setSelectedHistory(record);
    setDetailOpen(true);
  };

  const columns: ColumnsType<PPTHistory> = [
    {
      title: '변경 시각',
      dataIndex: 'changedAt',
      width: 160,
      align: 'center',
      render: (val: string) => dayjs(val).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 110,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '사원명',
      dataIndex: 'employeeName',
      width: 100,
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
      title: '재직상태',
      dataIndex: 'status',
      width: 100,
      align: 'center',
      render: (val: string | null) =>
        val == null ? '-' : <Tag color={statusColor(val)}>{val}</Tag>,
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
        </Space>
      </Card>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        pagination={{
          current: (data?.number ?? 0) + 1,
          pageSize: data?.size ?? 20,
          total: data?.totalElements ?? 0,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          onChange: (page, pageSize) =>
            setSearchParams((prev) => ({ ...prev, page: page - 1, size: pageSize })),
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record),
          style: { cursor: 'pointer' },
        })}
        scroll={{ x: 920 }}
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
