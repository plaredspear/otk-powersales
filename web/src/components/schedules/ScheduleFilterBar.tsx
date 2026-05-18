import type { ReactNode } from 'react';
import { Button, Checkbox, InputNumber, Space, Transfer } from 'antd';
import type { TransferProps } from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

interface ScheduleFilterBarProps {
  year: number;
  month?: number;
  selectedCodes: string[];
  onYearChange: (value: number) => void;
  onMonthChange?: (value: number) => void;
  onCodesChange: (codes: string[]) => void;
  onSearch: () => void;
  onExport: () => void;
  exportDisabled?: boolean;
  exportLoading?: boolean;
  searchLoading?: boolean;
  hideExport?: boolean;
  showMonth?: boolean;
  extraFilters?: ReactNode;
}

export default function ScheduleFilterBar({
  year,
  month,
  selectedCodes,
  onYearChange,
  onMonthChange,
  onCodesChange,
  onSearch,
  onExport,
  exportDisabled = true,
  exportLoading = false,
  searchLoading = false,
  hideExport = false,
  showMonth = true,
  extraFilters,
}: ScheduleFilterBarProps) {
  const { data: branches = [] } = useTeamScheduleBranches();

  const allCodes = branches.map((b) => b.branchCode);
  const isAllSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;

  const handleSelectAll = (checked: boolean) => {
    onCodesChange(checked ? allCodes : []);
  };

  const transferDataSource = branches.map((b) => ({
    key: b.branchCode,
    title: b.branchName,
  }));

  const handleTransferChange: TransferProps['onChange'] = (nextTargetKeys) => {
    onCodesChange(nextTargetKeys.map((key) => String(key)));
  };

  return (
    <div style={{ marginBottom: 16 }}>
      <Space wrap align="start">
        <Space direction="vertical" size={4}>
          <span>년도:</span>
          <InputNumber
            value={year}
            min={2020}
            max={2099}
            onChange={(v) => v != null && onYearChange(v)}
            style={{ width: 100 }}
            parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
          />
        </Space>
        {showMonth && (
          <Space direction="vertical" size={4}>
            <span>월:</span>
            <InputNumber
              value={month}
              min={1}
              max={12}
              onChange={(v) => v != null && onMonthChange?.(v)}
              style={{ width: 80 }}
              parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
            />
          </Space>
        )}
        {extraFilters}
        <Space direction="vertical" size={4}>
          <span>지점명:</span>
          <Transfer
            dataSource={transferDataSource}
            targetKeys={selectedCodes}
            onChange={handleTransferChange}
            render={(item) => item.title ?? ''}
            titles={['선택가능', '선택완료']}
            disabled={isAllSelected}
            listStyle={{ width: 220, height: 220 }}
            showSearch
            filterOption={(input, item) => (item.title ?? '').includes(input)}
            locale={{
              itemUnit: '개',
              itemsUnit: '개',
              searchPlaceholder: '검색',
              notFoundContent: '항목 없음',
            }}
          />
        </Space>
      </Space>
      <div style={{ marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Checkbox checked={isAllSelected} onChange={(e) => handleSelectAll(e.target.checked)}>
          소속지점 전체선택
        </Checkbox>
        <Space>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={onSearch}
            disabled={selectedCodes.length === 0}
            loading={searchLoading}
          >
            조회
          </Button>
          {!hideExport && (
            <Button
              icon={<DownloadOutlined />}
              onClick={onExport}
              disabled={exportDisabled}
              loading={exportLoading}
            >
              엑셀 다운로드
            </Button>
          )}
        </Space>
      </div>
    </div>
  );
}
