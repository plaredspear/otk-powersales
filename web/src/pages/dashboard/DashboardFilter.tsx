import { DatePicker, Select, Space } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';

interface DashboardFilterProps {
  yearMonth: string;
  branchCode: string | undefined;
  onYearMonthChange: (value: string) => void;
  onBranchCodeChange: (value: string | undefined) => void;
}

export default function DashboardFilter({
  yearMonth,
  branchCode,
  onYearMonthChange,
  onBranchCodeChange,
}: DashboardFilterProps) {
  const handleMonthChange = (_date: Dayjs | null, dateString: string | string[]) => {
    const str = Array.isArray(dateString) ? dateString[0] : dateString;
    if (str) {
      onYearMonthChange(str);
    }
  };

  return (
    <Space size="middle" wrap>
      <DatePicker
        picker="month"
        value={dayjs(yearMonth, 'YYYY-MM')}
        onChange={handleMonthChange}
        format="YYYY년 MM월"
        allowClear={false}
        style={{ width: 160 }}
      />
      <Select
        placeholder="지점 전체"
        value={branchCode}
        onChange={onBranchCodeChange}
        allowClear
        style={{ width: 160 }}
        options={[{ label: '지점 전체', value: undefined as unknown as string }]}
      />
    </Space>
  );
}
