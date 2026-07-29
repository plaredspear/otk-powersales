import { useState } from 'react';
import { Alert, Button, Card, DatePicker, Input, Space, Statistic, Tag, Typography, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType, ColumnType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';
import { useMonthlySalesHistories } from '@/hooks/monthlySalesHistory/useMonthlySalesHistories';
import type { MonthlySalesHistoryListItem } from '@/api/monthlySalesHistory';
import type { Account } from '@/api/account';
import MonthlySalesAccountSearchModal from './components/MonthlySalesAccountSearchModal';

const { Text, Title } = Typography;

/** `yyyyMM` 6자 문자열을 `YYYY년 MM월` 로 표기. */
function formatSalesMonth(value: string): string {
  if (!/^\d{6}$/.test(value)) return value;
  return `${value.slice(0, 4)}년 ${value.slice(4, 6)}월`;
}

/** 금액(원) 표기 — null 은 '-'. */
function formatAmount(value: number | null): string {
  return value == null ? '-' : value.toLocaleString();
}

/** KST wall-clock ISO 문자열을 `YYYY-MM-DD HH:mm` 으로 표기 — null 은 '-'. */
function formatDateTime(value: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

/** 온도대별 금액 컬럼 1개 정의 — 전산/물류 컬럼 그룹이 동일 형태라 헬퍼로 묶는다. */
function amountColumn(
  title: string,
  dataIndex: keyof MonthlySalesHistoryListItem,
  width = 130,
): ColumnType<MonthlySalesHistoryListItem> {
  return {
    title,
    dataIndex,
    width,
    align: 'right',
    render: (v: number | null) => formatAmount(v),
  };
}

/**
 * 기준정보 > ORORA 월매출.
 *
 * ORORA 월별 마감 적재 배치(`OroraMonthlySalesMaterializeBatch`)가 메인 RDS `monthly_sales_history` 에
 * 적재한 결과를 거래처 + 매출년월 단위로 확인하는 조회 전용 화면이다. 일매출 화면과 동일한 B형(보고서형)
 * 구성 — 거래처가 필수 조건이고, 조회 전에는 안내 문구를 표시한다.
 *
 * 적재 upsert 키(거래처코드 + yyyy + MM)상 정상 데이터는 1행이라 페이지네이션을 두지 않는다.
 * 2행 이상이면 SF 이관분의 중복 row 이며, 그대로 노출해 적재 이상을 드러낸다.
 *
 * 금액은 ORORA 월별 적재 경로가 실제로 채우는 10종을 모두 보여준다 (전산마감 온도대 4종 + 합계,
 * 물류마감 온도대 4종 + 합계). 일매출 화면이 온도대별 컬럼을 감춘 것과 반대인데, 일별 적재 경로와 달리
 * 월별 적재 경로는 이 컬럼들을 채우기 때문이다.
 *
 * 권한 가드는 `monthly_sales_history` — `AdminMonthlySalesHistoryController` 및 거래처 lookup 과 동일하며,
 * 월 매출(물류배부/전산실적)·POS매출 등 기존 매출 화면과 같은 entity 다.
 */
export default function OroraMonthlySalesPage() {
  // 입력 버퍼 (조회 버튼/Enter 전까지 API 미호출).
  const [accountCodeInput, setAccountCodeInput] = useState('');
  const [selectedAccountName, setSelectedAccountName] = useState<string | null>(null);
  const [monthInput, setMonthInput] = useState<Dayjs>(dayjs());
  const [searchOpen, setSearchOpen] = useState(false);
  // 조회 실행 시점의 확정 조건. null 이면 조회 전.
  const [submitted, setSubmitted] = useState<{ accountCode: string; salesMonth: string } | null>(null);

  const { data, isLoading, isFetching, isError, error, refetch } = useMonthlySalesHistories(submitted);

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

  const columns: ColumnsType<MonthlySalesHistoryListItem> = [
    {
      title: '매출발생년월',
      dataIndex: 'salesYear',
      width: 130,
      fixed: 'left',
      render: (_: unknown, row) =>
        row.salesYear && row.salesMonth ? `${row.salesYear}년 ${row.salesMonth}월` : '-',
    },
    { title: '거래처코드', dataIndex: 'sapAccountCode', width: 120, fixed: 'left' },
    {
      title: '전산마감실적 (원)',
      children: [
        amountColumn('상온', 'abcClosingAmount1'),
        amountColumn('라면', 'abcClosingAmount2'),
        amountColumn('냉장냉동', 'abcClosingAmount3'),
        amountColumn('유지', 'abcClosingAmount4'),
        amountColumn('합계', 'abcClosingSumAmount', 150),
      ],
    },
    {
      title: '물류마감실적 (원)',
      children: [
        amountColumn('상온', 'shipClosingAmount1'),
        amountColumn('라면', 'shipClosingAmount2'),
        amountColumn('냉장냉동', 'shipClosingAmount3'),
        amountColumn('유지', 'shipClosingAmount4'),
        amountColumn('합계', 'shipClosingSumAmount', 150),
      ],
    },
    {
      // 삭제 행은 목록에 남기되(적재 결과 확인이 목적) 합계에서는 빠지므로, 왜 합계와 안 맞는지
      // 화면에서 바로 드러나게 상태를 표시한다.
      title: '상태',
      dataIndex: 'isDeleted',
      width: 90,
      render: (v: boolean) =>
        v ? <Tag color="red">삭제됨</Tag> : <Tag color="green">정상</Tag>,
    },
    { title: '적재키 (Externalkey)', dataIndex: 'externalKey', width: 180, ellipsis: true },
    {
      title: '적재일시',
      dataIndex: 'updatedAt',
      width: 160,
      render: (v: string | null) => formatDateTime(v),
    },
  ];

  // 삭제 행이 섞여 있으면 합계 모수가 목록과 다르다는 점을 명시한다.
  const deletedCount = data?.content.filter((row) => row.isDeleted).length ?? 0;

  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ margin: '0 0 12px' }}>
        ORORA 월매출
      </Title>
      <Tag color="orange" style={{ marginBottom: 16 }}>
        ORORA 월별 마감 적재 배치(매월 9일 11:30, 전월분)가 적재한 월매출이력 — 조회 전용
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

      {/*
        조회 결과 금액 합계 — 온도대별 컬럼이 많아 가로 스크롤이 생기므로 합계는 상단에 고정 노출한다.
        값은 적재된 합계 컬럼(abcClosingSumAmount / shipClosingSumAmount) 기준이며 온도대 1~4 의
        재합산이 아니다 — 개별 컬럼이 비고 합계에만 값이 든 거래처/월이 있어 재합산하면 매출이 누락된다.
      */}
      {submitted != null && data && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Space size="large" wrap>
            <Statistic
              title="전산마감실적 합계"
              value={data.totalAbcClosingAmount}
              suffix="원"
              valueStyle={{ fontSize: 20 }}
            />
            <Statistic
              title="물류마감실적 합계"
              value={data.totalShipClosingAmount}
              suffix="원"
              valueStyle={{ fontSize: 20 }}
            />
          </Space>
          {/*
            마지막 적재 시각 = 조회한 거래처 + 매출년월 행의 최신 적재일시. 배치 실행 이력은 대상 월을
            대조할 수 없고 SF 이관분에는 이력 자체가 없어 쓰지 않는다 (일매출 화면과 동일 판단).
          */}
          <div style={{ marginTop: 12 }}>
            <Text type="secondary">
              마지막 적재 시각: {formatDateTime(data.lastMaterializedAt)} (
              {formatSalesMonth(data.salesMonth)} 기준)
            </Text>
          </div>
          {deletedCount > 0 && (
            <div style={{ marginTop: 4 }}>
              <Text type="warning">
                삭제된 행 {deletedCount}건은 목록에만 표시되고 위 합계에서는 제외되었습니다.
              </Text>
            </div>
          )}
        </Card>
      )}

      {isError && (
        <Alert
          type="error"
          message={(error as Error)?.message ?? '조회에 실패했습니다'}
          style={{ marginBottom: 8 }}
        />
      )}

      <ResizableTable<MonthlySalesHistoryListItem>
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
      />

      <MonthlySalesAccountSearchModal
        open={searchOpen}
        onClose={() => setSearchOpen(false)}
        onSelect={handleAccountSelect}
        initialKeyword={accountCodeInput}
      />
    </div>
  );
}
