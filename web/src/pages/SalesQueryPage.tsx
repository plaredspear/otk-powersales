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
  Table,
  Typography,
  message,
} from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import { fetchAccountsForPosSalesLookup } from '@/api/account';
import { fetchPosSales, type PosSalesProduct } from '@/api/posSales';

const { Text } = Typography;

interface QueryParams {
  customerId: number;
  yearMonth: string;
}

/**
 * POS매출 — web admin 조회 페이지.
 *
 * 거래처 1곳 + 연월 선택 → POS DB(`live_pos_sales_dh`) 제품별 매출 명세 + 합계.
 * 레거시 `promotion/month/posmain.jsp` 의 web admin 이관 (Backend `GET /api/v1/admin/sales/pos`).
 */
export default function SalesQueryPage() {
  const [accountId, setAccountId] = useState<number | undefined>();
  const [accountKeyword, setAccountKeyword] = useState<string>('');
  const [month, setMonth] = useState<Dayjs>(dayjs());
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);

  const accountQuery = useQuery({
    queryKey: ['admin', 'accounts', 'pos-sales-lookup', accountKeyword],
    queryFn: () =>
      fetchAccountsForPosSalesLookup({ keyword: accountKeyword || undefined, page: 0, size: 50 }),
  });

  const posSalesQuery = useQuery({
    queryKey: ['posSales', queryParams],
    queryFn: () => fetchPosSales(queryParams!.customerId, queryParams!.yearMonth),
    enabled: queryParams != null,
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
    setQueryParams({ customerId: accountId, yearMonth: month.format('YYYYMM') });
  };

  const items = posSalesQuery.data?.items ?? [];
  const totalAmount = items.reduce((sum, it) => sum + it.amount, 0);
  const totalQuantity = items.reduce((sum, it) => sum + it.quantity, 0);

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
        </Space>
      </Card>

      {queryParams != null && (
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
            <Table
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
