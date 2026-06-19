import { useMemo, useState } from 'react';
import { Alert, DatePicker, Empty, Select, Space, Spin, Tabs, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useEmployees } from '@/hooks/employee/useEmployees';
import { useEmployeeMonthlyWorkHistory } from '@/hooks/employee/useEmployeeWorkHistory';
import RefreshButton from '@/components/common/RefreshButton';
import MonthlyWorkRawTable from './MonthlyWorkRawTable';
import MonthlyWorkInsight from './MonthlyWorkInsight';

const { Text } = Typography;

/**
 * 근무기간 조회 — 월별 개인 근무내역(어디서/어떻게).
 *
 * 인원 1명 + 월을 지정하면 team_member_schedule 기반으로 그 사람이 한 달 동안
 * 어디 근무지에서 어떻게 근무했는지 조회. 일자별 원시표(A) / 근무 인사이트(요약+캘린더, B·C) 2탭.
 */
export default function MonthlyWorkDetailTab() {
  const [keyword, setKeyword] = useState('');
  const [employeeId, setEmployeeId] = useState<number | undefined>(undefined);
  const [employeeLabel, setEmployeeLabel] = useState<string>('');
  const [period, setPeriod] = useState<Dayjs>(dayjs());

  const empQuery = useEmployees({ keyword: keyword || undefined, page: 0, size: 20 });

  const yearMonth = period.format('YYYY-MM');
  const histQuery = useEmployeeMonthlyWorkHistory(employeeId, employeeId ? yearMonth : undefined);

  const empOptions = useMemo(
    () =>
      (empQuery.data?.content ?? []).map((e) => ({
        value: e.id,
        label: `${e.name} (${e.employeeCode})${e.costCenterCode ? ` · ${e.costCenterCode}` : ''}`,
      })),
    [empQuery.data],
  );

  const items = histQuery.data?.items ?? [];

  return (
    <div>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>인원:</span>
        <Select
          showSearch
          allowClear
          filterOption={false}
          value={employeeId}
          placeholder="이름/사번 검색"
          style={{ width: 280 }}
          onSearch={setKeyword}
          loading={empQuery.isFetching}
          notFoundContent={empQuery.isFetching ? <Spin size="small" /> : '검색 결과 없음'}
          options={empOptions}
          onChange={(value, option) => {
            setEmployeeId(value);
            setEmployeeLabel(Array.isArray(option) ? '' : (option?.label ?? ''));
          }}
        />
        <span>년·월:</span>
        <DatePicker
          picker="month"
          value={period}
          onChange={(v) => v && setPeriod(v)}
          allowClear={false}
        />
        {employeeId != null && (
          <RefreshButton onRefresh={histQuery.refetch} refreshing={histQuery.isFetching} />
        )}
      </Space>

      {employeeId != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {employeeLabel} · {period.year()}년 {period.month() + 1}월 — 총 {items.length}건
          </Text>
        </div>
      )}

      {histQuery.isError && (
        <Alert
          type="error"
          message={(histQuery.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {employeeId == null ? (
        <Empty description="인원을 선택하면 해당 월 근무내역을 조회합니다" />
      ) : histQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <Tabs
          defaultActiveKey="raw"
          items={[
            {
              key: 'raw',
              label: '일자별 내역',
              children: <MonthlyWorkRawTable items={items} />,
            },
            {
              key: 'insight',
              label: '근무 인사이트',
              children: (
                <MonthlyWorkInsight
                  items={items}
                  year={period.year()}
                  month={period.month() + 1}
                />
              ),
            },
          ]}
        />
      )}
    </div>
  );
}
