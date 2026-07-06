import type { ReactNode } from 'react';
import { useEffect, useMemo } from 'react';
import { Button, Checkbox, InputNumber, Select, Space, Tag } from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

interface BranchOption {
  branchCode: string;
  branchName: string;
}

interface PeriodBranchFilterBarProps {
  year?: number;
  month?: number;
  selectedCodes: string[];
  onYearChange?: (value: number) => void;
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
  /** 년도/월 InputNumber 대신 렌더할 기간 UI (예: 일 단위 RangePicker). 지정 시 year/month 미사용. */
  periodFilter?: ReactNode;
  /** 지점 미선택 외의 추가 조회 차단 조건 (예: 기간 검증 실패). */
  searchDisabled?: boolean;
  /**
   * 지점 셀렉터 옵션 소스. 지정 시 이 목록을 그대로 사용하고, 미지정 시 여사원 일정
   * (`team_member_schedule` 가드) 지점 목록을 fallback 으로 조회한다.
   *
   * 화면 게이팅 권한과 지점 API 가드를 정합시키려면 각 화면이 자기 도메인 권한으로 가드된 지점
   * 훅 결과를 여기에 주입한다 (예: 매출 계열은 `useSalesBranches`). fallback 은 게이팅이
   * `team_member_schedule` 인 여사원 일정 계열 화면 전용이다.
   */
  branches?: BranchOption[];
}

/**
 * 지점 셀렉터 옵션 소스 wrapper.
 *
 * `branches` prop 을 명시하면 그것을 그대로 렌더하고, 미지정 시에만 여사원 일정
 * (`team_member_schedule` 가드) 지점 훅을 호출한다. 훅을 조건부 호출하지 않도록 fallback 전용
 * 하위 컴포넌트로 분리해, `branches` 를 주입한 화면(예: 매출 계열)에서는 team-schedule 호출이
 * 전혀 발생하지 않는다.
 */
export default function PeriodBranchFilterBar(props: PeriodBranchFilterBarProps) {
  if (props.branches !== undefined) {
    return <PeriodBranchFilterBarView {...props} branches={props.branches} />;
  }
  return <PeriodBranchFilterBarWithTeamScheduleBranches {...props} />;
}

/** `branches` 미지정 시 fallback — 여사원 일정 지점 목록을 조회해 주입. */
function PeriodBranchFilterBarWithTeamScheduleBranches(props: PeriodBranchFilterBarProps) {
  const { data: branches = [] } = useTeamScheduleBranches();
  return <PeriodBranchFilterBarView {...props} branches={branches} />;
}

function PeriodBranchFilterBarView({
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
  periodFilter,
  searchDisabled = false,
  branches = [],
}: PeriodBranchFilterBarProps) {
  // 드롭다운 옵션은 지점명(label) 가나다순으로 정렬해 노출한다 (한국어 로케일 기준).
  const branchOptions = useMemo(
    () =>
      branches
        .map((b) => ({ value: b.branchCode, label: b.branchName }))
        .sort((a, b) => a.label.localeCompare(b.label, 'ko')),
    [branches],
  );

  const allCodes = useMemo(() => branches.map((b) => b.branchCode), [branches]);
  const allSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;
  const someSelected = selectedCodes.length > 0 && !allSelected;

  // 본인 지점만 조회 권한이 있는 단일지점 사용자(지점 옵션 1개)는 선택지가 없으므로
  // 본인 지점을 자동 선택하고 선택 UI 대신 고정 Tag 로 표시한다. 빈 placeholder("지점 선택")가
  // 떠 있어 "미선택"으로 오해하게 만드는 문제를 방지. 다중지점/전사 권한은 기존 선택 UI 유지.
  const singleBranch = branches.length === 1;
  useEffect(() => {
    if (branches.length === 0) return;
    // 권한 주체가 바뀌면(예: 대행 종료 후 관리자 복귀) 이전 사용자 지점 목록에서 선택했던
    // 코드가 현재 목록에 더 이상 없을 수 있다. stale 코드를 먼저 정리해 잘못된 선택/표시를 방지.
    const validCodes = selectedCodes.filter((code) => allCodes.includes(code));
    if (validCodes.length !== selectedCodes.length) {
      onCodesChange(validCodes);
      return;
    }
    // 단일지점 사용자는 본인 지점을 자동 선택.
    if (singleBranch && selectedCodes.length === 0) {
      onCodesChange([branches[0].branchCode]);
    }
  }, [singleBranch, branches, allCodes, selectedCodes, onCodesChange]);

  const handleToggleAll = () => {
    onCodesChange(allSelected ? [] : allCodes);
  };

  // 년도/월 입력에서 엔터 시 조회 트리거. 단, 조회 버튼과 동일한 활성 조건(지점 선택 + 조회 중 아님)을
  // 만족할 때만 실행해 미선택 상태에서의 의도치 않은 조회를 막는다.
  const handleInputEnter = () => {
    if (selectedCodes.length === 0 || searchDisabled || searchLoading) return;
    onSearch();
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
        {periodFilter ?? (
          <>
            <Space direction="vertical" size={4}>
              <span>년도:</span>
              <InputNumber
                value={year}
                min={2020}
                max={2099}
                onChange={(v) => v != null && onYearChange?.(v)}
                onPressEnter={handleInputEnter}
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
                  onPressEnter={handleInputEnter}
                  style={{ width: 80 }}
                  parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
                />
              </Space>
            )}
          </>
        )}
        {extraFilters}
      </Space>
      <Space>
        {extraActions}
        <Button
          type="primary"
          icon={<SearchOutlined />}
          onClick={onSearch}
          disabled={selectedCodes.length === 0 || searchDisabled}
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
