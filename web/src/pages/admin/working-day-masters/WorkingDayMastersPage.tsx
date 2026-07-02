import { useMemo, useState } from 'react';
import { Calendar, Card, DatePicker, Segmented, Space, Statistic, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';
import { useWorkingDayMasters } from '@/hooks/workingDayMaster/useWorkingDayMasters';
import type { WorkingDayMasterListItem } from '@/api/workingDayMaster';

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'] as const;

type ViewType = 'calendar' | 'list';

/** ISO 일자 문자열을 "YYYY-MM-DD (요일)" 로 표기. */
function formatWorkingDate(iso: string | null): string {
  if (!iso) return '-';
  const d = dayjs(iso);
  return `${d.format('YYYY-MM-DD')} (${WEEKDAY_LABELS[d.day()]})`;
}

export default function WorkingDayMastersPage() {
  const [month, setMonth] = useState<Dayjs>(dayjs());
  const [view, setView] = useState<ViewType>('calendar');
  const year = month.year();
  const monthNum = month.month() + 1;
  const { data, isLoading, isFetching, refetch } = useWorkingDayMasters(year, monthNum);

  // 캘린더 셀에서 일자별 영업일 정보를 O(1) 로 찾기 위한 맵. (key: YYYY-MM-DD)
  const itemByDate = useMemo(() => {
    const map = new Map<string, WorkingDayMasterListItem>();
    for (const item of data?.content ?? []) {
      if (item.workingDate) map.set(dayjs(item.workingDate).format('YYYY-MM-DD'), item);
    }
    return map;
  }, [data?.content]);

  const columns: ColumnsType<WorkingDayMasterListItem> = [
    {
      title: '일자',
      dataIndex: 'workingDate',
      width: 200,
      render: (v: string | null) => formatWorkingDate(v),
    },
    {
      title: '구분',
      dataIndex: 'isWorkingDay',
      width: 100,
      render: (isWorkingDay: boolean) =>
        isWorkingDay ? <Tag color="blue">영업일</Tag> : <Tag>휴일</Tag>,
    },
    {
      title: '영업일값',
      dataIndex: 'workingDateCheck',
      width: 100,
      render: (v: number | null) => (v == null ? '-' : v),
    },
    { title: '코드', dataIndex: 'name', width: 160, ellipsis: true },
    {
      title: '수정일시',
      dataIndex: 'updatedAt',
      width: 160,
      render: (v: string | null) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '수정자',
      dataIndex: 'lastModifiedByName',
      width: 120,
      render: (v: string | null) => v ?? '-',
    },
  ];

  // 캘린더 날짜 셀 렌더. 현재 선택한 월에 속한 날짜만 영업일/휴일 태그를 표시한다.
  const renderCalendarCell = (date: Dayjs) => {
    if (date.month() !== month.month() || date.year() !== month.year()) return null;
    const item = itemByDate.get(date.format('YYYY-MM-DD'));
    if (!item) return null;
    return (
      <div style={{ textAlign: 'center' }}>
        {item.isWorkingDay ? <Tag color="blue">영업일</Tag> : <Tag>휴일</Tag>}
      </div>
    );
  };

  return (
    <Card title="영업일관리마스터">
      <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
        운영이 관리하는 영업일 달력입니다. <Tag color="blue">영업일</Tag> 인 날짜가 월 매출 기준 진도율
        계산의 영업일 수에 반영됩니다. (조회 전용)
      </Typography.Paragraph>

      <div
        style={{
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <Space size="middle">
          <Segmented<ViewType>
            value={view}
            onChange={setView}
            options={[
              { label: '캘린더', value: 'calendar' },
              { label: '목록', value: 'list' },
            ]}
          />
          <DatePicker
            picker="month"
            value={month}
            onChange={(v) => v && setMonth(v)}
            allowClear={false}
            format="YYYY년 MM월"
          />
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </Space>
        <Space size="large">
          <Statistic title="영업일" value={data?.workingDayCount ?? 0} suffix="일" />
          <Statistic title="휴일" value={data?.holidayCount ?? 0} suffix="일" />
        </Space>
      </div>

      {view === 'calendar' ? (
        <Calendar
          value={month}
          // 패널 내부 이동(월 변경/날짜 선택)을 외부 month 상태와 동기화.
          onPanelChange={(v) => setMonth(v)}
          onSelect={(v, info) => {
            if (info.source === 'date' && v.month() !== month.month()) setMonth(v);
          }}
          // 내부 연/월 셀렉터는 숨기고 상단 DatePicker 로 월을 제어한다.
          headerRender={() => null}
          cellRender={(current, info) => (info.type === 'date' ? renderCalendarCell(current) : null)}
        />
      ) : (
        <ResizableTable<WorkingDayMasterListItem>
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data?.content ?? []}
          pagination={false}
          locale={listTableLocale()}
        />
      )}
    </Card>
  );
}
