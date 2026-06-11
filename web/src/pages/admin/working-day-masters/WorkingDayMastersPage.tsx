import { useState } from 'react';
import { Card, DatePicker, Space, Statistic, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { useWorkingDayMasters } from '@/hooks/workingDayMaster/useWorkingDayMasters';
import type { WorkingDayMasterListItem } from '@/api/workingDayMaster';

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'] as const;

/** ISO 일자 문자열을 "YYYY-MM-DD (요일)" 로 표기. */
function formatWorkingDate(iso: string | null): string {
  if (!iso) return '-';
  const d = dayjs(iso);
  return `${d.format('YYYY-MM-DD')} (${WEEKDAY_LABELS[d.day()]})`;
}

export default function WorkingDayMastersPage() {
  const [month, setMonth] = useState<Dayjs>(dayjs());
  const year = month.year();
  const monthNum = month.month() + 1;
  const { data, isLoading, isFetching, refetch } = useWorkingDayMasters(year, monthNum);

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

  return (
    <Card title="영업일 마스터">
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

      <ResizableTable<WorkingDayMasterListItem>
        rowKey="id"
        loading={isLoading}
        columns={columns}
        dataSource={data?.content ?? []}
        pagination={false}
      />
    </Card>
  );
}
