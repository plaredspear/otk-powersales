import { useMemo, useState } from 'react';
import { Alert, Button, Card, DatePicker, Divider, Space, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import {
  fetchPromotionTargetActualReport,
  exportPromotionTargetActualReport as apiExport,
  type PromotionTargetActualReportRow,
} from '@/api/promotionTargetActualReport';
import { usePromotionReportBranches } from '@/hooks/promotion/usePromotionReportBranches';
import BranchSingleSelect from '@/components/common/BranchSingleSelect';
import PromotionActualDonutChart from '@/components/charts/PromotionActualDonutChart';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';

const { Text } = Typography;

interface QueryRange {
  startDate: string;
  endDate: string;
  branchCode?: string;
}

const num = (v: number | null) => (v == null ? '-' : v.toLocaleString());

/**
 * 행사사원 목표 대비 실적 (영업지원실용) — SF Report new_report_AtQ 이식 (Spec #845).
 *
 * ScheduleDate 기간(사용자 입력 필수) 내 행사사원 목표/실적을 전사 조회. 행사명 그룹 + 그룹별 소계 + 전체 합계 +
 * 지점별 행사실적 구성 도넛 차트. 엑셀 다운로드. 기존 /promotions 화면과 별개 (역할 분리).
 */
export default function PromotionTargetActualReportPage() {
  const [startDate, setStartDate] = useState<Dayjs | null>(null);
  const [endDate, setEndDate] = useState<Dayjs | null>(null);
  const [branchCode, setBranchCode] = useState<string | undefined>(undefined);
  const [range, setRange] = useState<QueryRange | null>(null);

  const { data: branches = [] } = usePromotionReportBranches();

  const query = useQuery({
    queryKey: ['promotionTargetActualReport', range?.startDate, range?.endDate, range?.branchCode],
    queryFn: () => fetchPromotionTargetActualReport(range!.startDate, range!.endDate, range!.branchCode),
    enabled: range != null,
  });

  const handleSearch = () => {
    if (!startDate || !endDate) {
      message.warning('시작일과 종료일은 필수항목입니다.');
      return;
    }
    if (endDate.isBefore(startDate)) {
      message.warning('종료일은 시작일 이후여야 합니다.');
      return;
    }
    setRange({
      startDate: startDate.format('YYYY-MM-DD'),
      endDate: endDate.format('YYYY-MM-DD'),
      branchCode,
    });
  };

  const handleExport = async () => {
    if (!range) return;
    try {
      await apiExport(range.startDate, range.endDate, range.branchCode);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<PromotionTargetActualReportRow> = useMemo(
    () => [
      { title: '지점명', dataIndex: 'branchName', width: 100, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 150, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountCode', width: 100, render: (v) => v ?? '-' },
      { title: '대표제품', dataIndex: 'primaryProductName', width: 130, render: (v) => v ?? '-' },
      { title: '매대조건', dataIndex: 'category1', width: 100, render: (v) => v ?? '-' },
      { title: '기타제품', dataIndex: 'otherProduct', width: 120, render: (v) => v ?? '-' },
      { title: '사번', dataIndex: 'employeeCode', width: 90, render: (v) => v ?? '-' },
      { title: '소속', dataIndex: 'employeeOrgName', width: 100, render: (v) => v ?? '-' },
      { title: '사원명', dataIndex: 'employeeName', width: 90, render: (v) => v ?? '-' },
      { title: '전문행사조', dataIndex: 'professionalPromotionTeam', width: 110, render: (v) => v ?? '-' },
      { title: '행사일자', dataIndex: 'scheduleDate', width: 110, render: (v) => v ?? '-' },
      { title: '목표금액', dataIndex: 'targetAmount', width: 110, align: 'right', render: num },
      { title: '실적금액', dataIndex: 'actualAmount', width: 110, align: 'right', render: num },
      { title: '매대위치', dataIndex: 'standLocation', width: 100, render: (v) => v ?? '-' },
      { title: '대표수량', dataIndex: 'primarySalesQuantity', width: 90, align: 'right', render: num },
      { title: '대표금액', dataIndex: 'primaryProductAmount', width: 110, align: 'right', render: num },
      { title: '기타수량', dataIndex: 'otherSalesQuantity', width: 90, align: 'right', render: num },
      { title: '기타금액', dataIndex: 'otherSalesAmount', width: 110, align: 'right', render: num },
      { title: '근무구분2', dataIndex: 'workType2', width: 90, render: (v) => v ?? '-' },
      { title: '근무구분3', dataIndex: 'workType3', width: 90, render: (v) => v ?? '-' },
      { title: '근무보고여부', dataIndex: 'isWorkReport', width: 110, render: (v) => v || '-' },
      { title: '출근일자', dataIndex: 'commuteDate', width: 160, render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap align="end">
        <BranchSingleSelect branches={branches} value={branchCode} onChange={setBranchCode} />
        <Space direction="vertical" size={4}>
          <span>시작일:</span>
          <DatePicker value={startDate} onChange={(v) => setStartDate(v)} />
        </Space>
        <Space direction="vertical" size={4}>
          <span>종료일:</span>
          <DatePicker value={endDate} onChange={(v) => setEndDate(v)} />
        </Space>
        <Button type="primary" onClick={handleSearch} loading={query.isLoading}>
          조회
        </Button>
        {range != null && (
          <RefreshButton onRefresh={() => query.refetch()} refreshing={query.isFetching} />
        )}
        <Button onClick={handleExport} disabled={!query.data || query.data.groups.length === 0}>
          엑셀 다운로드
        </Button>
      </Space>

      {range != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {range.startDate} ~ {range.endDate} 행사사원 목표 대비 실적
          </Text>
        </div>
      )}

      {query.isError && (
        <Alert
          type="error"
          message={(query.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {query.data && query.data.groups.length > 0 ? (
        <>
          <Card size="small" style={{ marginBottom: 16 }}>
            <PromotionActualDonutChart data={query.data.chart} />
          </Card>

          {query.data.groups.map((group) => (
            <div key={group.promotionName ?? '(미지정)'} style={{ marginBottom: 20 }}>
              <Text strong>{group.promotionName ?? '(미지정)'}</Text>
              <Text type="secondary" style={{ marginLeft: 12 }}>
                소계 — 목표 {group.subtotalTargetAmount.toLocaleString()} / 실적{' '}
                {group.subtotalActualAmount.toLocaleString()} / 대표수량{' '}
                {group.subtotalPrimaryQuantity.toLocaleString()} / 기타수량{' '}
                {group.subtotalOtherQuantity.toLocaleString()}
              </Text>
              <ResizableTable
                rowKey={(r, idx) => `${r.employeeCode ?? ''}-${r.scheduleDate ?? ''}-${idx}`}
                size="small"
                columns={columns}
                dataSource={group.rows}
                pagination={false}
                scroll={{ x: 'max-content' }}
                style={{ marginTop: 6 }}
              />
            </div>
          ))}

          <Divider />
          <Text strong>
            합계 — 목표 {query.data.totalTargetAmount.toLocaleString()} / 실적{' '}
            {query.data.totalActualAmount.toLocaleString()} / 대표수량{' '}
            {query.data.totalPrimaryQuantity.toLocaleString()} / 기타수량{' '}
            {query.data.totalOtherQuantity.toLocaleString()}
          </Text>
        </>
      ) : (
        <ResizableTable
          columns={columns}
          dataSource={[]}
          loading={query.isLoading}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={listTableLocale({ searched: range != null })}
        />
      )}
    </div>
  );
}
