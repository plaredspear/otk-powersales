import { useState } from 'react';
import { Alert, Button, Card, DatePicker, Input, Space, Statistic, Tag, Typography, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';
import { useDailySalesHistories } from '@/hooks/dailySalesHistory/useDailySalesHistories';
import type { DailySalesHistoryListItem } from '@/api/dailySalesHistory';
import type { Account } from '@/api/account';
import DailySalesAccountSearchModal from './components/DailySalesAccountSearchModal';

const { Text, Title } = Typography;

/**
 * `yyyyMMdd` 8자 문자열을 `YYYY-MM-DD` 로 표기.
 *
 * dayjs customParseFormat 플러그인 미사용 환경이라 문자열 슬라이스로 변환한다 (8자가 아니면 원본 그대로).
 */
function formatSalesDate(value: string): string {
  if (!/^\d{8}$/.test(value)) return value;
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
}

/** `yyyyMM` 6자 문자열을 `YYYY년 MM월` 로 표기. */
function formatSalesMonth(value: string): string {
  if (!/^\d{6}$/.test(value)) return value;
  return `${value.slice(0, 4)}년 ${value.slice(4, 6)}월`;
}

/** 금액(원) 표기 — null 은 '-'. */
function formatAmount(value: number | null): string {
  return value == null ? '-' : value.toLocaleString();
}

/**
 * 기준정보 > ORORA 일매출.
 *
 * ORORA 일별 매출 적재 배치(매일 11:00, `OroraDailySalesMaterializeBatch`)가 메인 RDS
 * `daily_sales_history` 에 적재한 결과를 거래처 + 매출월 단위로 확인하는 조회 전용 화면이다.
 * 거래처가 필수 조건인 B형(보고서형) — 조회 전에는 안내 문구를 표시하고, 한 거래처의 한 달치는
 * 최대 31행이라 페이지네이션을 두지 않는다.
 *
 * 권한 가드는 `daily_sales_history` — `AdminDailySalesHistoryController` 및 거래처 lookup 과 동일하다.
 */
export default function OroraDailySalesPage() {
  // 입력 버퍼 (조회 버튼/Enter 전까지 API 미호출).
  const [accountCodeInput, setAccountCodeInput] = useState('');
  const [selectedAccountName, setSelectedAccountName] = useState<string | null>(null);
  const [monthInput, setMonthInput] = useState<Dayjs>(dayjs());
  const [searchOpen, setSearchOpen] = useState(false);
  // 조회 실행 시점의 확정 조건. null 이면 조회 전.
  const [submitted, setSubmitted] = useState<{ accountCode: string; salesMonth: string } | null>(null);

  const { data, isLoading, isFetching, isError, error, refetch } = useDailySalesHistories(submitted);

  const handleSearch = () => {
    const accountCode = accountCodeInput.trim();
    if (!accountCode) {
      message.warning('거래처코드는 필수항목입니다. 직접 입력하거나 고급 검색으로 선택해주세요.');
      return;
    }
    setSubmitted({ accountCode, salesMonth: monthInput.format('YYYYMM') });
  };

  const handleAccountSelect = (account: Account) => {
    setAccountCodeInput(account.externalKey ?? '');
    setSelectedAccountName(account.name);
  };

  const columns: ColumnsType<DailySalesHistoryListItem> = [
    {
      title: '매출발생일자',
      dataIndex: 'salesDate',
      width: 130,
      render: (v: string) => formatSalesDate(v),
    },
    { title: '거래처코드', dataIndex: 'sapAccountCode', width: 120 },
    {
      title: '전산매출실적 (원)',
      dataIndex: 'erpSalesAmount',
      width: 150,
      align: 'right',
      render: (v: number | null) => formatAmount(v),
    },
    {
      title: '물류배부매출실적 (원)',
      dataIndex: 'erpDistributionAmount',
      width: 170,
      align: 'right',
      render: (v: number | null) => formatAmount(v),
    },
    {
      title: '원장매출 (원)',
      dataIndex: 'ledgerAmount',
      width: 140,
      align: 'right',
      render: (v: number | null) => formatAmount(v),
    },
    { title: '적재키 (Externalkey)', dataIndex: 'externalKey', width: 190, ellipsis: true },
    {
      title: '수정일시',
      dataIndex: 'updatedAt',
      width: 160,
      render: (v: string | null) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ margin: '0 0 12px' }}>
        ORORA 일매출
      </Title>
      <Tag color="orange" style={{ marginBottom: 16 }}>
        ORORA 일별 매출 적재 배치(매일 11:00)가 적재한 일별매출이력 — 조회 전용
      </Tag>

      <Space style={{ marginBottom: 12, display: 'flex' }} wrap align="end">
        <Space direction="vertical" size={4}>
          <span>거래처코드:</span>
          <Space.Compact>
            <Input
              value={accountCodeInput}
              placeholder="거래처코드"
              allowClear
              style={{ width: 200 }}
              onChange={(e) => {
                setAccountCodeInput(e.target.value);
                setSelectedAccountName(null);
              }}
              onPressEnter={handleSearch}
            />
            <Button icon={<SearchOutlined />} onClick={() => setSearchOpen(true)}>
              고급 검색
            </Button>
          </Space.Compact>
        </Space>
        <Space direction="vertical" size={4}>
          <span>매출발생년월:</span>
          <DatePicker
            picker="month"
            value={monthInput}
            onChange={(v) => v && setMonthInput(v)}
            allowClear={false}
            format="YYYY년 MM월"
          />
        </Space>
        <Button type="primary" onClick={handleSearch} loading={isLoading}>
          조회
        </Button>
        {submitted != null && <RefreshButton onRefresh={refetch} refreshing={isFetching} />}
      </Space>

      {selectedAccountName && submitted == null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">선택 거래처: {selectedAccountName}</Text>
        </div>
      )}

      {submitted != null && data && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {data.accountName ?? '-'} ({data.sapAccountCode})
            {data.branchName ? ` · ${data.branchName}` : ''} · {formatSalesMonth(data.salesMonth)} 적재{' '}
            {data.content.length}건
          </Text>
        </div>
      )}

      {/* 조회 결과 금액 합계 — 테이블 하단 Summary 행과 동일 값이지만, 스크롤 없이 바로 확인할 수 있게 상단에도 노출한다. */}
      {submitted != null && data && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Space size="large" wrap>
            <Statistic
              title="전산매출실적 합계"
              value={data.totalErpSalesAmount}
              suffix="원"
              valueStyle={{ fontSize: 20 }}
            />
            <Statistic
              title="물류배부매출실적 합계"
              value={data.totalErpDistributionAmount}
              suffix="원"
              valueStyle={{ fontSize: 20 }}
            />
          </Space>
        </Card>
      )}

      {isError && (
        <Alert
          type="error"
          message={(error as Error)?.message ?? '조회에 실패했습니다'}
          style={{ marginBottom: 8 }}
        />
      )}

      <ResizableTable<DailySalesHistoryListItem>
        rowKey="id"
        columns={columns}
        dataSource={data?.content ?? []}
        loading={isLoading}
        pagination={false}
        scroll={{ x: 'max-content' }}
        locale={listTableLocale({
          searched: submitted != null,
          beforeSearchText: '거래처와 매출발생년월을 선택한 후 조회 버튼을 눌러주세요.',
        })}
        summary={() =>
          data && data.content.length > 0 ? (
            <ResizableTable.Summary fixed>
              <ResizableTable.Summary.Row>
                <ResizableTable.Summary.Cell index={0} colSpan={2}>
                  <Text strong>합계</Text>
                </ResizableTable.Summary.Cell>
                <ResizableTable.Summary.Cell index={2} align="right">
                  <Text strong>{data.totalErpSalesAmount.toLocaleString()}</Text>
                </ResizableTable.Summary.Cell>
                <ResizableTable.Summary.Cell index={3} align="right">
                  <Text strong>{data.totalErpDistributionAmount.toLocaleString()}</Text>
                </ResizableTable.Summary.Cell>
                <ResizableTable.Summary.Cell index={4} align="right">
                  <Text strong>{data.totalLedgerAmount.toLocaleString()}</Text>
                </ResizableTable.Summary.Cell>
                <ResizableTable.Summary.Cell index={5} colSpan={2} />
              </ResizableTable.Summary.Row>
            </ResizableTable.Summary>
          ) : null
        }
      />

      <DailySalesAccountSearchModal
        open={searchOpen}
        onClose={() => setSearchOpen(false)}
        onSelect={handleAccountSelect}
        initialKeyword={accountCodeInput}
      />
    </div>
  );
}
