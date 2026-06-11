import type { ReactNode } from 'react';
import { useMemo } from 'react';
import { Button, Checkbox, InputNumber, Select, Space } from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

interface PeriodBranchFilterBarProps {
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
  /** 액션 버튼 행(조회/엑셀 다운로드)의 조회 버튼 왼쪽에 들어가는 추가 액션. 예: 새로고침 버튼. */
  extraActions?: ReactNode;
}

export default function PeriodBranchFilterBar({
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
  extraActions,
}: PeriodBranchFilterBarProps) {
  const { data: branches = [] } = useTeamScheduleBranches();

  const branchOptions = useMemo(
    () => branches.map((b) => ({ value: b.branchCode, label: b.branchName })),
    [branches],
  );

  const allCodes = useMemo(() => branches.map((b) => b.branchCode), [branches]);
  const allSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;
  const someSelected = selectedCodes.length > 0 && !allSelected;

  const handleToggleAll = () => {
    onCodesChange(allSelected ? [] : allCodes);
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
          <Select
            mode="multiple"
            value={selectedCodes}
            onChange={(values) => onCodesChange(values as string[])}
            options={branchOptions}
            placeholder="지점 선택"
            style={{ minWidth: 320, maxWidth: 520 }}
            maxTagCount="responsive"
            allowClear
            showSearch
            optionFilterProp="label"
            filterOption={(input, option) =>
              (option?.label ?? '').toString().includes(input)
            }
            popupRender={(menu) => (
              <>
                <div style={{ padding: '4px 12px', borderBottom: '1px solid #f0f0f0' }}>
                  <Checkbox
                    checked={allSelected}
                    indeterminate={someSelected}
                    onChange={handleToggleAll}
                  >
                    전체 ({selectedCodes.length}/{allCodes.length})
                  </Checkbox>
                </div>
                {menu}
              </>
            )}
            notFoundContent="항목 없음"
          />
        </Space>
      </Space>
      <div style={{ marginTop: 8, display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
        <Space>
          {extraActions}
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
