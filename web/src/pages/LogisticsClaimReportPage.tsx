import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Space, Spin, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import {
  fetchLogisticsClaimReport,
  exportLogisticsClaimReport as apiExport,
  type LogisticsClaimReportItem,
  type LogisticsClaimReportPeriod,
} from '@/api/logisticsClaimReport';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;

interface Props {
  /** THIS_MONTH(당월) / LAST_MONTH(전월) / CUSTOM(기간별). 메뉴별 고정. */
  period: LogisticsClaimReportPeriod;
}

const PERIOD_LABEL: Record<LogisticsClaimReportPeriod, string> = {
  THIS_MONTH: '당월',
  LAST_MONTH: '전월',
  CUSTOM: '기간별',
};

interface CustomRange {
  startDate: string;
  endDate: string;
}

/**
 * (영업본부) 물류 클레임 보고서 — SF Report OLS_dmK(기간별)/new_report_6dy(당월)/OLS_NDx(전월) 이식 (Spec #844).
 *
 * category='물류 클레임' Suggestion 을 전사 조회. 당월/전월은 진입 시 자동 조회(기간 자동 산출),
 * 기간별(CUSTOM)은 시작/종료일 직접 입력 후 조회. 22컬럼 그리드 + 엑셀 다운로드.
 */
export default function LogisticsClaimReportPage({ period }: Props) {
  const isCustom = period === 'CUSTOM';
  const [startDate, setStartDate] = useState<Dayjs | null>(null);
  const [endDate, setEndDate] = useState<Dayjs | null>(null);
  const [customRange, setCustomRange] = useState<CustomRange | null>(null);
  // 당월/전월은 진입 시 자동 조회 활성화
  const [autoEnabled, setAutoEnabled] = useState<boolean>(!isCustom);

  // 메뉴(period) 전환 시 상태 초기화
  useEffect(() => {
    setStartDate(null);
    setEndDate(null);
    setCustomRange(null);
    setAutoEnabled(period !== 'CUSTOM');
  }, [period]);

  const enabled = isCustom ? customRange != null : autoEnabled;

  const query = useQuery({
    queryKey: ['logisticsClaimReport', period, customRange?.startDate, customRange?.endDate],
    queryFn: () =>
      isCustom
        ? fetchLogisticsClaimReport(period, customRange!.startDate, customRange!.endDate)
        : fetchLogisticsClaimReport(period),
    enabled,
  });

  const handleSearch = () => {
    if (isCustom) {
      if (!startDate || !endDate) {
        message.warning('시작일과 종료일은 필수항목입니다.');
        return;
      }
      if (endDate.isBefore(startDate)) {
        message.warning('종료일은 시작일 이후여야 합니다.');
        return;
      }
      setCustomRange({ startDate: startDate.format('YYYY-MM-DD'), endDate: endDate.format('YYYY-MM-DD') });
    } else {
      setAutoEnabled(true);
      query.refetch();
    }
  };

  const handleExport = async () => {
    try {
      if (isCustom) {
        if (!customRange) return;
        await apiExport(period, customRange.startDate, customRange.endDate);
      } else {
        await apiExport(period);
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<LogisticsClaimReportItem> = useMemo(
    () => [
      { title: '소유자명', dataIndex: 'custName', width: 100, fixed: 'left', render: (v) => v ?? '-' },
      { title: '생성일시', dataIndex: 'createdDate', width: 150, render: (v) => v ?? '-' },
      { title: '클레임일자', dataIndex: 'claimDate', width: 110, render: (v) => v ?? '-' },
      { title: '책임물류센터', dataIndex: 'responsibleLogisticsCenter', width: 120, render: (v) => v ?? '-' },
      { title: '물류책임', dataIndex: 'logisticsResponsibility', width: 100, render: (v) => v ?? '-' },
      { title: '클레임유형', dataIndex: 'claimType', width: 100, render: (v) => v ?? '-' },
      { title: '제목', dataIndex: 'title', width: 160, render: (v) => v ?? '-' },
      { title: '내용', dataIndex: 'content', width: 200, render: (v) => v ?? '-' },
      { title: '제품코드', dataIndex: 'productCode', width: 100, render: (v) => v ?? '-' },
      { title: '제품명', dataIndex: 'productName', width: 140, render: (v) => v ?? '-' },
      { title: '제품카테고리', dataIndex: 'productCategory', width: 110, render: (v) => v ?? '-' },
      { title: '지점명', dataIndex: 'branchName', width: 100, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountCode', width: 110, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 160, render: (v) => v ?? '-' },
      { title: '조직명', dataIndex: 'orgName', width: 100, render: (v) => v ?? '-' },
      { title: '사번', dataIndex: 'employeeCode', width: 100, render: (v) => v ?? '-' },
      { title: '사원명', dataIndex: 'employeeName', width: 90, render: (v) => v ?? '-' },
      { title: '직위', dataIndex: 'jikwee', width: 80, render: (v) => v ?? '-' },
      { title: '직무코드', dataIndex: 'jobCode', width: 90, render: (v) => v ?? '-' },
      { title: '차량번호', dataIndex: 'carNumber', width: 100, render: (v) => v ?? '-' },
      { title: '처리상태', dataIndex: 'actionStatus', width: 90, render: (v) => v ?? '-' },
      { title: '처리내용', dataIndex: 'actionContent', width: 200, render: (v) => v ?? '-' },
      { title: '중복제안번호', dataIndex: 'duplicateProposalNum', width: 120, render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        {isCustom ? (
          <>
            <span>시작일:</span>
            <DatePicker value={startDate} onChange={(v) => setStartDate(v)} />
            <span>종료일:</span>
            <DatePicker value={endDate} onChange={(v) => setEndDate(v)} />
          </>
        ) : (
          <span>
            {PERIOD_LABEL[period]}
            {query.data ? ` (${query.data.startDate} ~ ${query.data.endDate})` : ''}
          </span>
        )}
        <Button type="primary" onClick={handleSearch} loading={query.isLoading || query.isFetching}>
          조회
        </Button>
        <Button onClick={handleExport} disabled={!query.data || query.data.items.length === 0}>
          엑셀 다운로드
        </Button>
      </Space>

      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">(영업본부) 물류 클레임 ({PERIOD_LABEL[period]})</Text>
      </div>

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
          rowKey={(r, idx) => `${r.claimDate ?? ''}-${r.employeeCode ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText:
              isCustom && customRange == null
                ? '조회 기간을 선택하고 조회 버튼을 눌러주세요'
                : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
