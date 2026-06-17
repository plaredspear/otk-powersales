import type { ReactNode } from 'react';
import { useEffect, useMemo } from 'react';
import { Button, Checkbox, InputNumber, Select, Space, Tag } from 'antd';
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

  // 본인 지점만 조회 권한이 있는 단일지점 사용자(지점 옵션 1개)는 선택지가 없으므로
  // 본인 지점을 자동 선택하고 드롭다운을 비활성화한다. 빈 placeholder("지점 선택")가
  // 떠 있어 "미선택"으로 오해하게 만드는 문제를 방지. 다중지점/전사 권한은 기존 선택 UI 유지.
  const singleBranch = branches.length === 1;
  useEffect(() => {
    if (singleBranch && selectedCodes.length === 0) {
      onCodesChange([branches[0].branchCode]);
    }
  }, [singleBranch, branches, selectedCodes.length, onCodesChange]);

  const handleToggleAll = () => {
    onCodesChange(allSelected ? [] : allCodes);
  };

  return (
    <div
      style={{
        marginBottom: 16,
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'flex-end',
        justifyContent: 'space-between',
        gap: 8,
      }}
    >
      <Space wrap align="end">
        <Space direction="vertical" size={4}>
          <span>지점명:</span>
          {singleBranch ? (
            <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
              지점: {branches[0].branchName}
            </Tag>
          ) : (
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
          )}
        </Space>
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
      </Space>
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
  );
}
