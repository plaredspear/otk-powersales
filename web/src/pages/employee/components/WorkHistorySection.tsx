import { Alert, Card, Empty, Spin, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { EmployeeWorkHistoryItem } from '@/api/employee';
import { useEmployeeWorkHistory } from '@/hooks/employee/useEmployeeWorkHistory';
import ResizableTable from '@/components/common/ResizableTable';

interface Props {
  employeeId: number;
  limit?: number;
}

const COLUMNS: ColumnsType<EmployeeWorkHistoryItem> = [
  {
    title: '근무일자',
    dataIndex: 'workingDate',
    width: 120,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '근무유형',
    dataIndex: 'workingType',
    width: 110,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '카테고리1',
    dataIndex: 'workingCategory1',
    width: 110,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '카테고리3',
    dataIndex: 'workingCategory3',
    width: 110,
    render: (v: string | null) => v ?? '-',
  },
  {
    title: '거래처',
    dataIndex: 'accountName',
    render: (v: string | null, row) => {
      if (!v) return '-';
      return row.accountExternalKey ? `${v} (${row.accountExternalKey})` : v;
    },
  },
  {
    title: '출근',
    dataIndex: 'isClockIn',
    width: 80,
    render: (v: boolean) =>
      v ? <Tag color="green">출근</Tag> : <Tag>미출근</Tag>,
  },
];

export default function WorkHistorySection({ employeeId, limit = 10 }: Props) {
  const { data, isLoading, isError, error } = useEmployeeWorkHistory(employeeId, limit);

  return (
    <Card title={`근무이력 (최근 ${limit}건)`} style={{ marginBottom: 12 }}>
      {isLoading && (
        <div style={{ textAlign: 'center', padding: 16 }}>
          <Spin />
        </div>
      )}
      {isError && (
        <Alert
          type="error"
          message="근무이력 조회 실패"
          description={(error as Error)?.message}
        />
      )}
      {!isLoading && !isError && (
        <ResizableTable
          rowKey="id"
          size="small"
          columns={COLUMNS}
          dataSource={data?.items ?? []}
          pagination={false}
          locale={{ emptyText: <Empty description="근무이력이 없습니다" /> }}
        />
      )}
    </Card>
  );
}
