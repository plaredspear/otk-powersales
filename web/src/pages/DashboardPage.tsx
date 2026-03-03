import { useState } from 'react';
import { Alert, Button, Empty, Space, Spin } from 'antd';
import dayjs from 'dayjs';
import { useDashboard } from '@/hooks/dashboard/useDashboard';
import DashboardFilter from './dashboard/DashboardFilter';
import SalesSummaryCard from './dashboard/SalesSummaryCard';
import StaffDeploymentCard from './dashboard/StaffDeploymentCard';
import BasicStatsCard from './dashboard/BasicStatsCard';

export default function DashboardPage() {
  const [yearMonth, setYearMonth] = useState(() => dayjs().format('YYYY-MM'));
  const [branchCode, setBranchCode] = useState<string | undefined>(undefined);

  const { data, isLoading, isError, error, refetch } = useDashboard({ yearMonth, branchCode });

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="대시보드 조회 실패"
          description={(error as Error)?.message || '데이터를 불러오는 중 오류가 발생했습니다.'}
          action={
            <Button size="small" onClick={() => refetch()}>
              재시도
            </Button>
          }
          showIcon
        />
      </div>
    );
  }

  if (!data) {
    return (
      <div style={{ padding: 24 }}>
        <Empty description="데이터가 없습니다" />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <DashboardFilter
          yearMonth={yearMonth}
          branchCode={branchCode}
          onYearMonthChange={setYearMonth}
          onBranchCodeChange={setBranchCode}
        />
        <SalesSummaryCard data={data.salesSummary} />
        <StaffDeploymentCard data={data.staffDeployment} />
        <BasicStatsCard data={data.basicStats} />
      </Space>
    </div>
  );
}
