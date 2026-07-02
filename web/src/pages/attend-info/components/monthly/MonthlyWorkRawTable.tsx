import { useMemo } from 'react';
import { Empty, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { EmployeeWorkHistoryItem } from '@/api/employee';
import ResizableTable from '@/components/common/ResizableTable';

interface Props {
  items: EmployeeWorkHistoryItem[];
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

function workTypeColor(v: string | null): string | undefined {
  if (v === '근무') return 'green';
  if (v === '연차') return 'gold';
  if (v === '대휴') return 'blue';
  return undefined;
}

/** 근무지: 거래처명 → ref 텍스트 → 소속지점코드 순 폴백. */
function resolveWorkplace(row: EmployeeWorkHistoryItem): string {
  return row.accountName ?? row.refAccountName ?? row.costCenterCode ?? '-';
}

const COLUMNS: ColumnsType<EmployeeWorkHistoryItem> = [
  {
    title: '근무일자',
    dataIndex: 'workingDate',
    width: 140,
    render: (v: string | null) => {
      if (!v) return '-';
      const d = dayjs(v);
      return `${d.format('YYYY-MM-DD')} (${WEEKDAYS[d.day()]})`;
    },
  },
  {
    title: '구분',
    dataIndex: 'workingType',
    width: 80,
    render: (v: string | null) => (v ? <Tag color={workTypeColor(v)}>{v}</Tag> : '-'),
  },
  {
    title: '근무지',
    dataIndex: 'accountName',
    render: (_: unknown, row) => resolveWorkplace(row),
  },
  {
    title: '거래처코드',
    dataIndex: 'accountExternalKey',
    width: 110,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '유통형태',
    dataIndex: 'distributionChannelLabel',
    width: 100,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '거래처유형',
    dataIndex: 'abcTypeLabel',
    width: 110,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '근무유형',
    dataIndex: 'workingCategory1',
    width: 90,
    render: (v: string | null) =>
      v ? <Tag color={v === '진열' ? 'blue' : 'orange'}>{v}</Tag> : '-',
  },
  {
    title: '전문행사조',
    dataIndex: 'professionalPromotionTeam',
    width: 130,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '근무형태',
    dataIndex: 'workingCategory3',
    width: 90,
    render: (v: string | null) => (v ? <Tag>{v}</Tag> : '-'),
  },
  {
    title: '상세유형',
    dataIndex: 'secondWorkType',
    width: 110,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '출근',
    dataIndex: 'isClockIn',
    width: 80,
    render: (v: boolean) => (v ? <Tag color="green">출근</Tag> : <Tag>미등록</Tag>),
  },
  {
    title: '출근등록시각',
    dataIndex: 'startTime',
    width: 130,
    render: (v: string | null) => (v ? dayjs(v).format('HH:mm') : '-'),
  },
];

export default function MonthlyWorkRawTable({ items }: Props) {
  const columns = useMemo(() => COLUMNS, []);
  return (
    <ResizableTable
      rowKey="id"
      size="small"
      columns={columns}
      dataSource={items}
      pagination={false}
      scroll={{ x: 'max-content' }}
      locale={{ emptyText: <Empty description="해당 월 근무내역이 없습니다" /> }}
    />
  );
}
