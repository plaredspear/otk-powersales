import { useState } from 'react';
import { Card, DatePicker, List, Select, Statistic, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { fetchElectronicSales } from '@/api/sales';
import { fetchMyAccounts } from '@/api/accounts';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatNumber, formatWon } from '@/lib/format';

/**
 * 전산(ABC) 매출 조회 (레거시 promotion/month/abcmain).
 * 거래처 1곳 + 연월 → 제품별 실적. 거래처는 내 거래처(/accounts/my)에서 선택.
 */
export default function ElectronicSalesPage() {
  const [customerId, setCustomerId] = useState<number>();
  const [month, setMonth] = useState<Dayjs>(dayjs());
  const yearMonth = month.format('YYYYMM');

  const accounts = useQuery({ queryKey: ['my-accounts', ''], queryFn: () => fetchMyAccounts() });

  const query = useQuery({
    queryKey: ['electronic-sales', customerId, yearMonth],
    queryFn: () => fetchElectronicSales(customerId!, yearMonth),
    enabled: !!customerId,
  });

  return (
    <>
      <DetailHeader title="전산 매출" />
      <Select
        showSearch
        placeholder="거래처 선택"
        style={{ width: '100%', marginBottom: 8 }}
        loading={accounts.isLoading}
        optionFilterProp="label"
        value={customerId}
        onChange={setCustomerId}
        options={accounts.data?.stores.map((s) => ({ value: s.accountId, label: `${s.accountName} (${s.accountCode})` }))}
      />
      <DatePicker
        picker="month"
        value={month}
        allowClear={false}
        onChange={(v) => v && setMonth(v)}
        style={{ width: '100%', marginBottom: 12 }}
      />
      {!customerId ? (
        <Typography.Text type="secondary">거래처를 선택하세요.</Typography.Text>
      ) : (
        <QueryBoundary
          isLoading={query.isLoading}
          isError={query.isError}
          data={query.data}
          onRetry={query.refetch}
          isEmpty={(d) => d.items.length === 0}
          emptyDescription="해당 월 전산매출이 없습니다"
        >
          {(d) => {
            const total = d.items.reduce((s, it) => s + it.amount, 0);
            return (
              <>
                <Card size="small" style={{ marginBottom: 12 }}>
                  <Typography.Text type="secondary">{d.customerName}</Typography.Text>
                  <Statistic title="합계 매출" value={total} formatter={(v) => formatWon(Number(v))} />
                </Card>
                <List
                  dataSource={d.items}
                  renderItem={(it) => (
                    <List.Item>
                      <List.Item.Meta title={it.productName} description={it.productCode} />
                      <div style={{ textAlign: 'right' }}>
                        <div>{formatWon(it.amount)}</div>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {formatNumber(it.quantity)} EA
                        </Typography.Text>
                      </div>
                    </List.Item>
                  )}
                />
              </>
            );
          }}
        </QueryBoundary>
      )}
    </>
  );
}
