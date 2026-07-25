import { useState } from 'react';
import { Alert, Button, DatePicker, Input, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import DetailLink from '@/components/common/DetailLink';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import { useErpOrders } from '@/hooks/erpOrder/useErpOrders';
import type { ErpOrder } from '@/api/erpOrder';

/**
 * BigDecimal → 천 단위 콤마 + '원'. 숫자가 아니면 원본 표시.
 *
 * 백엔드 BigDecimal 은 Jackson 이 JSON 숫자로 직렬화하므로 number 로 도착하지만,
 * 직렬화 설정 변화에 대비해 문자열도 함께 허용한다.
 */
function won(v: number | string | null | undefined): string {
  if (v === null || v === undefined) return '-';
  const s = String(v).trim();
  if (s === '') return '-';
  const n = Number(s);
  return Number.isFinite(n) ? `${n.toLocaleString()}원` : s;
}

const DATE_FORMAT = 'YYYY-MM-DD';

/**
 * 기준정보 > ERP주문 조회 목록 페이지 (`/erp-orders`).
 *
 * SAP 인바운드가 적재한 `erp_order` 를 조회 노출. 주문번호/거래처/주문자 키워드 + 납기일·주문일 기간으로
 * 필터. 조회 전용 (등록/수정/삭제 없음).
 */
export default function ErpOrderPage() {
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤.
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/size/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      keyword: '',
      deliveryDateFrom: '',
      deliveryDateTo: '',
      orderDateFrom: '',
      orderDateTo: '',
    },
  });

  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [keywordInput, setKeywordInput] = useState<string | undefined>(() => filters.keyword || undefined);
  const [deliveryRange, setDeliveryRange] = useState<[Dayjs | null, Dayjs | null] | null>(() =>
    filters.deliveryDateFrom || filters.deliveryDateTo
      ? [
          filters.deliveryDateFrom ? dayjs(filters.deliveryDateFrom) : null,
          filters.deliveryDateTo ? dayjs(filters.deliveryDateTo) : null,
        ]
      : null,
  );
  const [orderRange, setOrderRange] = useState<[Dayjs | null, Dayjs | null] | null>(() =>
    filters.orderDateFrom || filters.orderDateTo
      ? [
          filters.orderDateFrom ? dayjs(filters.orderDateFrom) : null,
          filters.orderDateTo ? dayjs(filters.orderDateTo) : null,
        ]
      : null,
  );

  const handleSearch = () => {
    setFilters({
      keyword: keywordInput ?? '',
      deliveryDateFrom: deliveryRange?.[0]?.format(DATE_FORMAT) ?? '',
      deliveryDateTo: deliveryRange?.[1]?.format(DATE_FORMAT) ?? '',
      orderDateFrom: orderRange?.[0]?.format(DATE_FORMAT) ?? '',
      orderDateTo: orderRange?.[1]?.format(DATE_FORMAT) ?? '',
    });
  };

  const { data, isLoading, isError, error, refetch, isFetching } = useErpOrders({
    keyword: filters.keyword || undefined,
    deliveryDateFrom: filters.deliveryDateFrom || undefined,
    deliveryDateTo: filters.deliveryDateTo || undefined,
    orderDateFrom: filters.orderDateFrom || undefined,
    orderDateTo: filters.orderDateTo || undefined,
    page,
    size,
  });

  const columns: ColumnsType<ErpOrder> = [
    {
      title: '주문번호',
      dataIndex: 'sapOrderNumber',
      width: 150,
      render: (val: string, order: ErpOrder) => <DetailLink to={`/erp-orders/${order.id}`}>{val}</DetailLink>,
    },
    { title: '참조주문번호', dataIndex: 'refSapOrderNumber', width: 130, render: (val: string | null) => val ?? '-' },
    { title: '거래처코드', dataIndex: 'sapAccountCode', width: 110, render: (val: string | null) => val ?? '-' },
    { title: '거래처명', dataIndex: 'sapAccountName', width: 180, ellipsis: true, render: (val: string | null) => val ?? '-' },
    { title: '납기일', dataIndex: 'deliveryRequestDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '주문생성일', dataIndex: 'orderDate', width: 110, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '주문자사번', dataIndex: 'employeeCode', width: 100, align: 'center', render: (val: string | null) => val ?? '-' },
    { title: '주문자명', dataIndex: 'employeeName', width: 100, render: (val: string | null) => val ?? '-' },
    {
      title: '총주문금액',
      dataIndex: 'orderSalesAmount',
      width: 130,
      align: 'right',
      render: (val: number | string | null) => won(val),
    },
    { title: '접수채널', dataIndex: 'orderChannelNm', width: 110, render: (val: string | null) => val ?? '-' },
    { title: '주문유형', dataIndex: 'orderTypeNm', width: 110, render: (val: string | null) => val ?? '-' },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="ERP주문 목록을 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={<Button onClick={() => refetch()}>재시도</Button>}
        />
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      style={{
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        height: containerHeight,
        boxSizing: 'border-box',
        minHeight: 0,
      }}
    >
      <Space
        style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', flexShrink: 0 }}
      >
        <Space wrap>
          <Input
            placeholder="주문번호 / 거래처 / 주문자 검색"
            allowClear
            style={{ width: 260 }}
            value={keywordInput ?? ''}
            onChange={(e) => setKeywordInput(e.target.value || undefined)}
            onPressEnter={handleSearch}
          />
          <DatePicker.RangePicker
            placeholder={['납기일 시작', '납기일 종료']}
            value={deliveryRange ?? undefined}
            onChange={(range) => setDeliveryRange(range ? [range[0], range[1]] : null)}
          />
          <DatePicker.RangePicker
            placeholder={['주문일 시작', '주문일 종료']}
            value={orderRange ?? undefined}
            onChange={(range) => setOrderRange(range ? [range[0], range[1]] : null)}
          />
          <Button type="primary" onClick={handleSearch}>
            조회
          </Button>
        </Space>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </Space>
      </Space>

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이가 scrollY 로 body 스크롤. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={isLoading}
          locale={listTableLocale()}
          scroll={{ x: 'max-content', y: scrollY }}
          pagination={buildListPagination({
            page: data?.page ?? page,
            pageSize: size,
            total: data?.totalElements ?? 0,
            // 사이즈 변경 시 setSize 가 page 를 0 으로 자동 리셋(useListQueryParams).
            onPageChange: setPage,
            onSizeChange: setSize,
          })}
        />
      </div>
    </div>
  );
}
