import { Button, Checkbox, InputNumber, Select, Space } from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

interface ScheduleFilterBarProps {
  year: number;
  month: number;
  selectedCodes: string[];
  onYearChange: (value: number) => void;
  onMonthChange: (value: number) => void;
  onCodesChange: (codes: string[]) => void;
  onSearch: () => void;
  onExport: () => void;
  exportDisabled?: boolean;
  exportLoading?: boolean;
  searchLoading?: boolean;
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
}: ScheduleFilterBarProps) {
  const { data: branches = [] } = useTeamScheduleBranches();

  const allCodes = branches.map((b) => b.branchCode);
  const isAllSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;

  const handleSelectAll = (checked: boolean) => {
    onCodesChange(checked ? allCodes : []);
  };

  return (
    <div style={{ marginBottom: 16 }}>
      <Space wrap>
        <span>년도:</span>
        <InputNumber
          value={year}
          min={2020}
          max={2099}
          onChange={(v) => v != null && onYearChange(v)}
          style={{ width: 100 }}
        />
        <span>월:</span>
        <InputNumber
          value={month}
          min={1}
          max={12}
          onChange={(v) => v != null && onMonthChange(v)}
          style={{ width: 80 }}
        />
        <span>소속지점:</span>
        <Select
          mode="multiple"
          value={selectedCodes}
          onChange={onCodesChange}
          style={{ minWidth: 300 }}
          placeholder="지점을 선택하세요"
          maxTagCount="responsive"
          options={branches.map((b) => ({ label: b.branchName, value: b.branchCode }))}
        />
      </Space>
      <div style={{ marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Checkbox checked={isAllSelected} onChange={(e) => handleSelectAll(e.target.checked)}>
          전체선택
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
          <Button
            icon={<DownloadOutlined />}
            onClick={onExport}
            disabled={exportDisabled}
            loading={exportLoading}
          >
            엑셀 다운로드
          </Button>
        </Space>
      </div>
    </div>
  );
}
