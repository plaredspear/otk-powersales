import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Empty,
  Input,
  InputNumber,
  message,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useAttendInfoBranches,
  useWorkHistoryPeriodSummary,
  useWorkHistoryPeriodSummaryExport,
} from '@/hooks/attend-info/useAttendInfo';
import type { WorkHistoryPeriodSummaryItem } from '@/api/attendInfo';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

// 각 컬럼에 기본 width 를 지정해야 ResizableTable 의 헤더 리사이즈 핸들이 활성화된다.
const COLUMNS: ColumnsType<WorkHistoryPeriodSummaryItem> = [
  { title: '소속지점', dataIndex: 'orgName', width: 120, ellipsis: true, render: (v: string | null) => v ?? '-' },
  { title: '사번', dataIndex: 'employeeCode', width: 100, ellipsis: true, render: (v: string | null) => v ?? '-' },
  { title: '이름', dataIndex: 'employeeName', width: 100, ellipsis: true, render: (v: string | null) => v ?? '-' },
  { title: '직위', dataIndex: 'title', width: 90, ellipsis: true, render: (v: string | null) => v ?? '-' },
  {
    title: '총 근무일수',
    dataIndex: 'totalWorkingDays',
    width: 110,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '근무 거래처 수',
    dataIndex: 'workingAccountCount',
    width: 120,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  { title: '진열', dataIndex: 'displayDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '행사', dataIndex: 'eventDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '근무', dataIndex: 'workDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '연차', dataIndex: 'annualLeaveDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '대휴', dataIndex: 'altHolidayDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
];

interface QueryParams {
  fromYearMonth: string;
  toYearMonth: string;
  costCenterCodes: string[];
  keyword: string;
}

function toYearMonth(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

export default function WorkHistoryPeriodPage() {
  const now = dayjs();
  const [fromYear, setFromYear] = useState(now.year());
  const [fromMonth, setFromMonth] = useState(now.month() + 1);
  const [toYear, setToYear] = useState(now.year());
  const [toMonth, setToMonth] = useState(now.month() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [keyword, setKeyword] = useState('');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const { data: branches = [] } = useAttendInfoBranches();
  const branchOptions = useMemo(
    () => branches.map((b) => ({ value: b.branchCode, label: b.branchName })),
    [branches],
  );
  const allCodes = useMemo(() => branches.map((b) => b.branchCode), [branches]);
  const allSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;
  const someSelected = selectedCodes.length > 0 && !allSelected;
  const singleBranch = branches.length === 1;

  const { data, isLoading, isError, error } = useWorkHistoryPeriodSummary(queryParams);
  const exportMutation = useWorkHistoryPeriodSummaryExport();

  const fromYm = toYearMonth(fromYear, fromMonth);
  const toYm = toYearMonth(toYear, toMonth);
  const rangeInvalid = fromYm > toYm;

  const handleSearch = () => {
    if (rangeInvalid) return;
    // 단일지점 사용자는 본인 지점 자동 선택, 미선택이면 빈 배열(권한 스코프 전체)로 조회.
    const codes = singleBranch && selectedCodes.length === 0 ? allCodes : selectedCodes;
    setQueryParams({ fromYearMonth: fromYm, toYearMonth: toYm, costCenterCodes: codes, keyword });
  };

  const handleToggleAll = () => {
    setSelectedCodes(allSelected ? [] : allCodes);
  };

  const handleExport = () => {
    if (!queryParams) return;
    exportMutation.mutate(queryParams, {
      onError: (e) =>
        message.error(e instanceof Error ? e.message : '엑셀 다운로드에 실패했습니다'),
    });
  };

  const exportDisabled = queryParams == null || !data || data.items.length === 0;

  return (
    <div style={{ padding: 16 }}>
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
                onChange={(values) => setSelectedCodes(values as string[])}
                options={branchOptions}
                placeholder="지점 선택 (미선택 시 전체)"
                style={{ minWidth: 280, maxWidth: 480 }}
                maxTagCount="responsive"
                allowClear
                showSearch
                optionFilterProp="label"
                filterOption={(input, option) => (option?.label ?? '').toString().includes(input)}
                popupRender={(menu) => (
                  <>
                    <div style={{ padding: '4px 12px', borderBottom: '1px solid #f0f0f0' }}>
                      <Checkbox checked={allSelected} indeterminate={someSelected} onChange={handleToggleAll}>
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
            <span>시작 년월:</span>
            <Space.Compact>
              <InputNumber
                value={fromYear}
                min={2020}
                max={2099}
                onChange={(v) => v != null && setFromYear(v)}
                style={{ width: 90 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
              <InputNumber
                value={fromMonth}
                min={1}
                max={12}
                onChange={(v) => v != null && setFromMonth(v)}
                style={{ width: 64 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
            </Space.Compact>
          </Space>
          <Space direction="vertical" size={4}>
            <span>종료 년월:</span>
            <Space.Compact>
              <InputNumber
                value={toYear}
                min={2020}
                max={2099}
                onChange={(v) => v != null && setToYear(v)}
                style={{ width: 90 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
              <InputNumber
                value={toMonth}
                min={1}
                max={12}
                onChange={(v) => v != null && setToMonth(v)}
                style={{ width: 64 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
            </Space.Compact>
          </Space>
          <Space direction="vertical" size={4}>
            <span>사번/이름:</span>
            <Input
              allowClear
              placeholder="사번 또는 이름"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 160 }}
            />
          </Space>
        </Space>
        <Space>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
            disabled={rangeInvalid}
            loading={isLoading}
          >
            조회
          </Button>
          <Button
            icon={<DownloadOutlined />}
            onClick={handleExport}
            disabled={exportDisabled}
            loading={exportMutation.isPending}
          >
            엑셀 다운로드
          </Button>
        </Space>
      </div>

      {rangeInvalid && (
        <Alert
          type="warning"
          message="시작 년월은 종료 년월보다 이후일 수 없습니다"
          style={{ marginBottom: 16 }}
        />
      )}

      {isError && (
        <Alert
          type="error"
          message="조회 실패"
          description={error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다'}
          style={{ marginBottom: 16 }}
        />
      )}

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          {queryParams != null && data && (
            <Text style={{ marginBottom: 8, display: 'block' }}>총 {formatNumber(data.totalCount)}명</Text>
          )}
          <ResizableTable
            rowKey={(record) => record.employeeCode ?? record.employeeName ?? ''}
            columns={COLUMNS}
            dataSource={data?.items ?? []}
            pagination={false}
            size="small"
            sticky
            scroll={{ x: 'max-content' }}
            locale={{
              emptyText:
                queryParams == null ? (
                  '조회 조건을 설정하고 조회 버튼을 눌러주세요'
                ) : (
                  <Empty description="조회 결과가 없습니다" />
                ),
            }}
          />
        </>
      )}
    </div>
  );
}
