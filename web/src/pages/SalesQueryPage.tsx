import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Row,
  Select,
  Space,
  Statistic,
  Typography,
  message,
} from 'antd';
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import { fetchAccountsForPosSalesLookup } from '@/api/account';
import { fetchPosSales, POS_SALES_EXPORT_PATH, type PosSalesProduct } from '@/api/posSales';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';

const { Text } = Typography;

/**
 * POS매출 — web admin 조회 페이지.
 *
 * 거래처 1곳 + 연월 선택 → POS DB(`live_pos_sales_dh`) 제품별 매출 명세 + 합계.
 * 레거시 `promotion/month/posmain.jsp` 의 web admin 이관 (Backend `GET /api/v1/admin/sales/pos`).
 */
export default function SalesQueryPage() {
  // 검색 조건(거래처 + 조회월)을 URL query string 에 보관 — 새로고침/뒤로가기/링크 공유 시 직전 조회 복원.
  // (테이블 pagination 은 client-side 비제어라 page 는 URL 보관 대상 아님.)
  const { filters, setFilters } = useListQueryParams({
    defaultFilters: { customerId: '', yearMonth: '' },
  });

  // 입력 위젯의 로컬 편집 버퍼. URL filters 를 source of truth 로 두고 마운트 시 1회 초기화.
  const [accountId, setAccountId] = useState<number | undefined>(() =>
    filters.customerId ? Number(filters.customerId) : undefined,
  );
  const [accountKeyword, setAccountKeyword] = useState<string>('');
  const [month, setMonth] = useState<Dayjs>(() =>
    // customParseFormat 플러그인 미사용 → YYYYMM(6자리) 을 ISO 형태로 변환해 파싱.
    filters.yearMonth && filters.yearMonth.length === 6
      ? dayjs(`${filters.yearMonth.slice(0, 4)}-${filters.yearMonth.slice(4, 6)}-01`)
      : dayjs(),
  );

  const accountQuery = useQuery({
    queryKey: ['admin', 'accounts', 'pos-sales-lookup', accountKeyword],
    queryFn: () =>
      fetchAccountsForPosSalesLookup({ keyword: accountKeyword || undefined, page: 0, size: 50 }),
  });

  // 조회는 URL filters 기반 (검색 버튼 클릭 시 setFilters 로 URL 반영 → 아래 query 가 재실행).
  const customerId = filters.customerId ? Number(filters.customerId) : undefined;
  const yearMonth = filters.yearMonth || undefined;
  const hasQuery = customerId != null && yearMonth != null;

  const posSalesQuery = useQuery({
    queryKey: ['posSales', customerId, yearMonth],
    queryFn: () => fetchPosSales(customerId!, yearMonth!),
    enabled: hasQuery,
    placeholderData: (prev) => prev,
  });

  const accountOptions = (accountQuery.data?.content ?? []).map((a) => ({
    value: a.id,
    label: `${a.name ?? '-'} (${a.externalKey ?? '-'})`,
  }));

  const handleSearch = () => {
    if (accountId == null) {
      message.warning('거래처는 필수항목입니다.');
      return;
    }
    setFilters({ customerId: String(accountId), yearMonth: month.format('YYYYMM') });
  };

  const items = posSalesQuery.data?.items ?? [];
  const totalAmount = items.reduce((sum, it) => sum + it.amount, 0);
  const totalQuantity = items.reduce((sum, it) => sum + it.quantity, 0);

  const { run: runExport, downloading: exporting } = useExcelDownload();

  // 조회 화면과 동일한 거래처 + 연월의 제품별 명세를 엑셀로 export.
  const handleExport = () => {
    if (!hasQuery) return;
    runExport(POS_SALES_EXPORT_PATH, 'POS매출.xlsx', {
      params: { customerId: customerId!, yearMonth: yearMonth! },
      totalCount: items.length,
    });
  };

  const columns: ColumnsType<PosSalesProduct> = [
    { title: '제품코드', dataIndex: 'productCode', key: 'productCode', width: 140 },
    { title: '제품명', dataIndex: 'productName', key: 'productName' },
    {
      title: '바코드',
      dataIndex: 'barcode',
      key: 'barcode',
      width: 160,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '납품수량(EA)',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 130,
      align: 'right',
      render: (v: number) => v.toLocaleString(),
    },
    {
      title: '금액(원)',
      dataIndex: 'amount',
      key: 'amount',
      width: 150,
      align: 'right',
      render: (v: number) => v.toLocaleString(),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card>
        <Space wrap>
          <Select
            showSearch
            placeholder="거래처 검색"
            style={{ width: 320 }}
            options={accountOptions}
            loading={accountQuery.isLoading}
            value={accountId}
            filterOption={false}
            onSearch={setAccountKeyword}
            onChange={setAccountId}
            allowClear
            onClear={() => setAccountId(undefined)}
          />
          <DatePicker
            picker="month"
            value={month}
            onChange={(v) => v && setMonth(v)}
            allowClear={false}
            placeholder="조회월"
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            조회
          </Button>
          {hasQuery && (
            <>
              <Button
                icon={<DownloadOutlined />}
                loading={exporting}
                onClick={handleExport}
              >
                엑셀 다운로드
              </Button>
              <RefreshButton
                onRefresh={posSalesQuery.refetch}
                refreshing={posSalesQuery.isFetching}
              />
            </>
          )}
        </Space>
      </Card>

      {hasQuery && (
        <>
          <Row gutter={16}>
            <Col span={8}>
              <Card>
                <Statistic
                  title="거래처"
                  value={posSalesQuery.data?.customerName ?? '-'}
                  valueStyle={{ fontSize: 18 }}
                />
                <Text type="secondary">{posSalesQuery.data?.sapAccountCode ?? '-'}</Text>
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <Statistic title="합계 금액(원)" value={totalAmount} />
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <Statistic title="합계 수량(EA)" value={totalQuantity} />
              </Card>
            </Col>
          </Row>

          <Card>
            <ResizableTable
              rowKey="productCode"
              size="small"
              columns={columns}
              dataSource={items}
              loading={posSalesQuery.isFetching}
              pagination={{ pageSize: 20, showSizeChanger: true }}
              locale={{ emptyText: <Empty description="조회된 POS매출이 없습니다." /> }}
            />
          </Card>
        </>
      )}
    </Space>
  );
}
