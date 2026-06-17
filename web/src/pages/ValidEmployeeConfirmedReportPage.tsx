import { useMemo, useState } from 'react';
import { Alert, Button, Space, Spin, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchScheduleList,
  SCHEDULE_EXPORT_PATH,
  type ScheduleListItem,
} from '@/api/schedule';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;

const num = (v: number | null) => (v == null ? '-' : v.toLocaleString());

/**
 * 진열사원 스케줄 마스터 2-2. 유효사원(확정) 보고서 — SF Report X1_m0t 이식.
 *
 * 진열 스케줄(DisplayWorkScheduleMaster) 중 ValidData='유효' AND Confirmed=true 건을 전사 조회.
 * 기존 진열 스케줄 조회 인프라(`/admin/schedule/list?preset=VALID_CONFIRMED`)를 재사용 — preset 고정.
 * 별도 필터 입력 없이 조회 즉시 유효사원(확정) 목록 표시 + 엑셀 다운로드. 기존 /display-schedule 화면과 별개(보고서 메뉴).
 */
export default function ValidEmployeeConfirmedReportPage() {
  const [requested, setRequested] = useState(false);

  const query = useQuery({
    queryKey: ['validEmployeeConfirmedReport'],
    // 보고서는 전체 단면 — 큰 페이지 사이즈로 일괄 조회 (VALID_CONFIRMED preset 고정)
    queryFn: () => fetchScheduleList({ preset: 'VALID_CONFIRMED', page: 0, size: 1000 }),
    enabled: requested,
  });

  const handleSearch = () => setRequested(true);

  const { run: runExport, downloading: exporting } = useExcelDownload();

  const handleExport = () => {
    const ids = (query.data?.content ?? []).map((r) => r.id);
    if (ids.length === 0) return;
    runExport(SCHEDULE_EXPORT_PATH, '진열스케줄.xlsx', { method: 'post', data: { ids } });
  };

  const columns: ColumnsType<ScheduleListItem> = useMemo(
    () => [
      { title: '사번', dataIndex: 'employeeCode', width: 100, fixed: 'left' },
      { title: '성명', dataIndex: 'employeeName', width: 90, fixed: 'left' },
      { title: '거래처코드', dataIndex: 'accountCode', width: 110, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 160, render: (v) => v ?? '-' },
      { title: '근무유형3', dataIndex: 'typeOfWork3', width: 100, render: (v) => v ?? '-' },
      { title: '근무유형5', dataIndex: 'typeOfWork5', width: 100, render: (v) => v ?? '-' },
      { title: '시작일', dataIndex: 'startDate', width: 110, render: (v) => v ?? '-' },
      { title: '종료일', dataIndex: 'endDate', width: 110, render: (v) => v ?? '-' },
      { title: '코스트센터', dataIndex: 'costCenterCode', width: 100, render: (v) => v ?? '-' },
      {
        title: '전월매출',
        dataIndex: 'lastMonthRevenue',
        width: 120,
        align: 'right',
        render: num,
      },
    ],
    [],
  );

  const rows = query.data?.content ?? [];

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        <Button type="primary" onClick={handleSearch} loading={query.isLoading}>
          조회
        </Button>
        {requested && (
          <RefreshButton onRefresh={() => query.refetch()} refreshing={query.isFetching} />
        )}
        <Button onClick={handleExport} disabled={rows.length === 0} loading={exporting}>
          엑셀 다운로드
        </Button>
      </Space>

      {requested && !query.isLoading && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">유효사원(확정) {rows.length}건</Text>
        </div>
      )}

      {query.isError && (
        <Alert
          type="error"
          message={(query.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {query.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <ResizableTable
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={rows}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: !requested ? '조회 버튼을 눌러주세요' : '조회 결과가 없습니다' }}
        />
      )}
    </div>
  );
}
