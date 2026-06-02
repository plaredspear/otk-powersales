import { useMemo, useState } from 'react';
import { Alert, Empty, Input, Spin, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  exportList as apiExportList,
  fetchList,
  type ElectronicSalesDashboardListItem,
} from '@/api/electronicSalesDashboard';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import ResizableTable from '@/components/common/ResizableTable';
import ElectronicSalesDashboardDetailModal from './ElectronicSalesDashboardDetailModal';

const { Text } = Typography;

interface QueryParams {
  year: number;
  month: number;
  codes: string[];
  customerKeyword?: string;
}

/**
 * 월 매출(전산실적) — web admin 대시보드 페이지.
 *
 * 레거시 「월 매출 조회(전산)」(`/sales/abcMain` → `abcmain.jsp`, POS `live_tot_sales_dh`) 동등.
 * 권한 범위 거래처 N건의 월간 전산매출 합계 명세 + 페이징 + 정렬 + 엑셀 export.
 * row 클릭 시 제품별 상세 modal.
 */
export default function ElectronicSalesDashboardPage() {
  const today = new Date();
  const [year, setYear] = useState<number>(today.getFullYear());
  const [month, setMonth] = useState<number>(today.getMonth() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [customerKeyword, setCustomerKeyword] = useState<string>('');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [sort, setSort] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<ElectronicSalesDashboardListItem | null>(null);

  const listQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'list', queryParams, page, pageSize, sort],
    queryFn: () => {
      const p = queryParams!;
      return fetchList({
        year: p.year,
        month: p.month,
        costCenterCodes: p.codes,
        customerKeyword: p.customerKeyword,
        page,
        size: pageSize,
        sort,
      });
    },
    enabled: queryParams != null,
    placeholderData: (prev) => prev,
  });

  const handleSearch = () => {
    if (selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    setPage(0);
    setQueryParams({
      year,
      month,
      codes: selectedCodes,
      customerKeyword: customerKeyword.trim() || undefined,
    });
  };

  const handleExport = async () => {
    if (!queryParams) return;
    try {
      await apiExportList({
        year: queryParams.year,
        month: queryParams.month,
        costCenterCodes: queryParams.codes,
        customerKeyword: queryParams.customerKeyword,
        sort,
      });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const list = listQuery.data;

  const formatWon = (v: number | null | undefined) =>
    v == null ? '-' : `${v.toLocaleString()}원`;
  const formatQty = (v: number | null | undefined) =>
    v == null ? '-' : v.toLocaleString();

  const columns: ColumnsType<ElectronicSalesDashboardListItem> = useMemo(
    () => [
      {
        title: '거래처',
        dataIndex: 'accountName',
        width: 180,
        fixed: 'left',
        render: (v, row) => (
          <a onClick={() => setDetailTarget(row)} role="button">
            {v ?? '-'}
          </a>
        ),
      },
      { title: 'SAP코드', dataIndex: 'sapAccountCode', width: 110, render: (v) => v ?? '-' },
      { title: '지점', dataIndex: 'branchName', width: 120, render: (v) => v ?? '-' },
      {
        title: '전산매출 금액',
        dataIndex: 'salesAmount',
        width: 150,
        sorter: true,
        align: 'right',
        render: (v) => formatWon(v),
      },
      {
        title: '전산매출 수량',
        dataIndex: 'salesQuantity',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatQty(v),
      },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        year={year}
        month={month}
        selectedCodes={selectedCodes}
        onYearChange={setYear}
        onMonthChange={setMonth}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!list || list.items.length === 0}
        searchLoading={listQuery.isLoading}
        extraFilters={
          <div>
            <span>거래처 검색:</span>
            <div style={{ marginTop: 4 }}>
              <Input
                placeholder="거래처명 부분 일치"
                value={customerKeyword}
                onChange={(e) => setCustomerKeyword(e.target.value)}
                style={{ width: 220 }}
                allowClear
              />
            </div>
          </div>
        }
      />

      {queryParams != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {queryParams.year}-{String(queryParams.month).padStart(2, '0')} · {queryParams.codes.length}개 지점
            {queryParams.customerKeyword && ` · 거래처: ${queryParams.customerKeyword}`}
          </Text>
        </div>
      )}

      {listQuery.isError && (
        <Alert
          type="error"
          message={(listQuery.error as Error)?.message ?? '명세 조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {queryParams == null ? (
        <Empty description="조회 조건을 설정하고 조회 버튼을 눌러주세요" />
      ) : listQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <ResizableTable
          rowKey={(r) => r.accountId}
          size="small"
          columns={columns}
          dataSource={list?.items ?? []}
          pagination={{
            current: page + 1,
            pageSize,
            total: list?.pageInfo.totalElements ?? 0,
            showSizeChanger: true,
            pageSizeOptions: [10, 20, 50, 100],
            onChange: (p, ps) => {
              setPage(p - 1);
              setPageSize(ps);
            },
          }}
          onChange={(_pagination, _filters, sorter) => {
            if (!Array.isArray(sorter) && sorter.order && sorter.field) {
              const field = String(sorter.field);
              const direction = sorter.order === 'descend' ? 'desc' : 'asc';
              setSort(`${field},${direction}`);
              setPage(0);
            } else {
              setSort(undefined);
            }
          }}
          // scroll 미지정 → ResizableTable 이 컬럼 width 합을 x 로 자동 계산하여
          // tableLayout: fixed 로 전환, 리사이즈/ellipsis 가 동작한다 (기존 'max-content' 는 폭 고정을 막음).
          locale={{ emptyText: '조회 결과가 없습니다' }}
        />
      )}

      <ElectronicSalesDashboardDetailModal
        open={detailTarget != null}
        onClose={() => setDetailTarget(null)}
        customerId={detailTarget?.accountId ?? null}
        customerName={detailTarget?.accountName ?? null}
        year={queryParams?.year ?? year}
        month={queryParams?.month ?? month}
      />
    </div>
  );
}
