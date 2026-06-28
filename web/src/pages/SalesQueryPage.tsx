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
  Tag,
  Typography,
  message,
} from 'antd';
import { SearchOutlined, DownloadOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import { fetchAccountsForPosSalesLookup } from '@/api/account';
import {
  fetchPosSales,
  getPosSalesBranches,
  POS_SALES_EXPORT_PATH,
  type PosSalesProduct,
  type PosSalesProductSearchItem,
} from '@/api/posSales';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { useAuthStore } from '@/stores/authStore';
import PosProductSearchModal from './pos/PosProductSearchModal';

/** 매출 조회 제품으로 누적된 항목 — 바코드를 매출 조회 조건으로 사용. */
export interface SelectedPosProduct {
  productCode: string;
  productName: string;
  barcode: string;
}

/** 누적 목록 중복 판정 키 — 매출 조회 키가 바코드이므로 바코드 기준. */
function productKey(p: { barcode: string }): string {
  return p.barcode;
}

const { Text } = Typography;
const { RangePicker } = DatePicker;

const DATE_FORMAT = 'YYYY-MM-DD';
/** 레거시 `posmain.jsp` daterangepicker `maxSpan: { days: 31 }` 정합 — 두 끝점 일수 차이 최대 31. */
const MAX_RANGE_DAYS = 31;

/**
 * POS매출 — web admin 조회 페이지.
 *
 * 거래처 1곳 + 기간(시작/종료일) 선택 → POS DB(`live_pos_sales_dh`) 제품별 매출 명세 + 합계.
 * 레거시 `promotion/month/posmain.jsp` 의 daterangepicker 정합 (Backend `GET /api/v1/admin/sales/pos`).
 * 최초 진입 시 기본값은 당월 1일 ~ 오늘. 조회기간은 레거시 `maxSpan: { days: 31 }` 정합으로 최대 31일
 * (UI 제약만 — 레거시처럼 서버측 기간 검증은 없음).
 */
export default function SalesQueryPage() {
  // 검색 조건(거래처 + 조회기간)을 URL query string 에 보관 — 새로고침/뒤로가기/링크 공유 시 직전 조회 복원.
  // (테이블 pagination 은 client-side 비제어라 page 는 URL 보관 대상 아님.)
  const { filters, setFilters } = useListQueryParams({
    defaultFilters: { customerId: '', startDate: '', endDate: '', barcodes: '' },
  });

  // 입력 위젯의 로컬 편집 버퍼. URL filters 를 source of truth 로 두고 마운트 시 1회 초기화.
  const [accountId, setAccountId] = useState<number | undefined>(() =>
    filters.customerId ? Number(filters.customerId) : undefined,
  );
  const [accountKeyword, setAccountKeyword] = useState<string>('');
  // 조회기간 — 최초 화면 기본값은 당월 1일 ~ 오늘. URL 에 직전 기간이 있으면 그 값으로 복원하되,
  // 손상된 URL(파싱 불가 날짜) 은 기본값으로 fallback 해 Invalid Date 가 쿼리/URL 로 새는 것을 막는다.
  const [range, setRange] = useState<[Dayjs, Dayjs]>(() => {
    const start = filters.startDate ? dayjs(filters.startDate) : null;
    const end = filters.endDate ? dayjs(filters.endDate) : null;
    return start?.isValid() && end?.isValid()
      ? [start, end]
      : [dayjs().startOf('month'), dayjs()];
  });
  // 31일 제약(disabledDate)을 한쪽 끝 선택 시점 기준으로 계산하기 위한, 캘린더에서 "지금까지 찍은" 임시 값.
  // 시작/종료 중 하나만 찍힌 상태에서 그 기준으로 ±31일 밖을 비활성화한다 (레거시 maxSpan 정합).
  const [pickerHalf, setPickerHalf] = useState<Dayjs | null>(null);

  // 레거시 maxSpan: { days: 31 } 정합 — 한쪽 끝이 선택된 동안 그 기준에서 일수 차이가 31 을 넘는
  // 날짜를 비활성화한다. daterangepicker 의 maxSpan 은 "두 끝점의 day diff ≤ 31" 이므로 동일하게 둔다.
  const disabledRangeDate = (current: Dayjs): boolean => {
    if (!current || !pickerHalf) return false;
    return Math.abs(current.diff(pickerHalf, 'day')) > MAX_RANGE_DAYS;
  };

  // 지점 셀렉터 — 권한별 지점 화이트리스트 (전문행사조 PPTMasterPage 정합).
  //  - 다중 지점: Select 로 선택 → 거래처 lookup 검색을 해당 지점으로 스코프
  //  - 단일 지점(조장 등): 고정 Tag 로 지점명 표시. branchCode 빈 값이면 backend sharing policy 가
  //    본인 소속 지점으로 자동 스코프하므로 별도 전송 불필요.
  // 지점은 거래처 검색을 좁히는 보조 필터라 로컬 state 로 두고 lookup 쿼리 키에 포함한다.
  const userId = useAuthStore((state) => state.user?.id);
  const branchesQuery = useQuery({
    queryKey: ['admin', 'sales', 'pos', 'branches', userId],
    queryFn: getPosSalesBranches,
  });
  const branches = branchesQuery.data;
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));
  const singleBranch = branches?.length === 1 ? branches[0] : null;
  const isMultiBranch = (branches?.length ?? 0) > 1;
  const [branchCode, setBranchCode] = useState<string>('');

  // 매출 조회 제품 — 제품명/제품코드/바코드로 검색해 누적한 제품 목록 (mobile POS매출 정합).
  // 조회 시 바코드 있는 항목만 모아 barcodes 조건으로 전송. 비어 있으면 거래처 전체 집계.
  // URL 의 barcodes 로 마운트 시 복원한다 — 바코드만 알 수 있어 제품명/코드는 비지만, 새로고침/공유
  // 진입 시에도 필터 Tag 가 보여 재조회로 필터가 조용히 유실되는 것을 막는다.
  const [selectedProducts, setSelectedProducts] = useState<SelectedPosProduct[]>(() =>
    (filters.barcodes ? filters.barcodes.split(',') : [])
      .filter((b) => b.length > 0)
      .map<SelectedPosProduct>((barcode) => ({ productCode: '', productName: '', barcode })),
  );
  const [productModalOpen, setProductModalOpen] = useState(false);

  // 검색 결과를 누적 (제품코드+바코드 중복 제거). 바코드 없는 제품은 매출 조회 키가 없어 제외.
  const handleAddProducts = (items: PosSalesProductSearchItem[]) => {
    setSelectedProducts((prev) => {
      const existing = new Set(prev.map(productKey));
      const additions = items
        .filter((it) => (it.barcode ?? '').trim().length > 0)
        .map<SelectedPosProduct>((it) => ({
          productCode: it.productCode ?? '',
          productName: it.productName ?? '',
          barcode: (it.barcode ?? '').trim(),
        }))
        .filter((p) => !existing.has(productKey(p)));
      return [...prev, ...additions];
    });
  };

  const handleRemoveProduct = (key: string) => {
    setSelectedProducts((prev) => prev.filter((p) => productKey(p) !== key));
  };

  const accountQuery = useQuery({
    queryKey: ['admin', 'accounts', 'pos-sales-lookup', accountKeyword, branchCode],
    queryFn: () =>
      fetchAccountsForPosSalesLookup({
        keyword: accountKeyword || undefined,
        branchCode: branchCode || undefined,
        page: 0,
        size: 50,
      }),
  });

  // 조회는 URL filters 기반 (검색 버튼 클릭 시 setFilters 로 URL 반영 → 아래 query 가 재실행).
  const customerId = filters.customerId ? Number(filters.customerId) : undefined;
  const startDate = filters.startDate || undefined;
  const endDate = filters.endDate || undefined;
  // 조회에 사용할 바코드 목록 — 검색 버튼 클릭 시점에 선택 제품에서 확정해 URL 에 보관 (복원/공유 정합).
  const barcodes = filters.barcodes
    ? filters.barcodes.split(',').filter((b) => b.length > 0)
    : [];
  // 손상된 URL 로 잘못된 fetch 가 나가지 않도록 날짜 유효성까지 확인.
  const hasQuery =
    customerId != null &&
    startDate != null &&
    endDate != null &&
    dayjs(startDate).isValid() &&
    dayjs(endDate).isValid();

  const posSalesQuery = useQuery({
    queryKey: ['admin', 'sales', 'pos', customerId, startDate, endDate, filters.barcodes],
    queryFn: () => fetchPosSales(customerId!, startDate!, endDate!, barcodes),
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
    setFilters({
      customerId: String(accountId),
      startDate: range[0].format(DATE_FORMAT),
      endDate: range[1].format(DATE_FORMAT),
      // 선택 제품의 바코드를 조회 조건으로 확정 (비어 있으면 거래처 전체 집계).
      barcodes: selectedProducts.map((p) => p.barcode).join(','),
    });
  };

  const items = posSalesQuery.data?.items ?? [];
  // 합계는 서버 산출분(totalAmount/totalQuantity) 사용 — 명세 행 합산과 동일하나 단일 출처로 유지.
  const totalAmount = posSalesQuery.data?.totalAmount ?? 0;
  const totalQuantity = posSalesQuery.data?.totalQuantity ?? 0;

  const { run: runExport, downloading: exporting } = useExcelDownload();

  // 조회 화면과 동일한 거래처 + 기간 + 선택 바코드의 제품별 명세를 엑셀로 export.
  const handleExport = () => {
    if (!hasQuery) return;
    runExport(POS_SALES_EXPORT_PATH, 'POS매출.xlsx', {
      params: {
        customerId: customerId!,
        startDate: startDate!,
        endDate: endDate!,
        ...(barcodes.length > 0 ? { barcodes: barcodes.join(',') } : {}),
      },
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
          {isMultiBranch && (
            <Select
              placeholder="지점 (전체)"
              value={branchCode || undefined}
              onChange={(v) => {
                setBranchCode(v ?? '');
                // 새 지점 범위 밖일 수 있는 기존 거래처 선택은 초기화.
                setAccountId(undefined);
              }}
              style={{ width: 160 }}
              options={branchOptions}
              allowClear
              showSearch
              optionFilterProp="label"
            />
          )}
          {singleBranch && (
            <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
              지점: {singleBranch.branchName}
            </Tag>
          )}
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
          <RangePicker
            value={range}
            // 캘린더에서 한쪽 끝만 찍힌 동안 그 기준으로 31일 밖을 비활성화하기 위해 임시 끝값을 추적.
            onCalendarChange={(dates) => {
              setPickerHalf(dates?.[0] ?? dates?.[1] ?? null);
            }}
            onOpenChange={(open) => {
              if (!open) setPickerHalf(null); // 닫히면 제약 기준 해제 (다음 열림에서 새로 추적).
            }}
            disabledDate={disabledRangeDate}
            onChange={(v) => {
              // allowClear=false 라 null 가능성은 없으나 양끝 모두 존재할 때만 반영.
              if (v && v[0] && v[1]) setRange([v[0], v[1]]);
            }}
            allowClear={false}
            format={DATE_FORMAT}
            placeholder={['시작일', '종료일']}
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

        {/* 매출 조회 제품 — 제품명/제품코드/바코드로 검색해 누적. 미선택 시 거래처 전체 집계. */}
        <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 12 }}>
          <Space wrap>
            <Button icon={<PlusOutlined />} onClick={() => setProductModalOpen(true)}>
              매출 조회 제품 추가
            </Button>
            <Text type="secondary">
              {selectedProducts.length > 0
                ? `${selectedProducts.length}개 제품 선택됨`
                : '제품 미선택 시 거래처 전체 매출을 조회합니다'}
            </Text>
            {selectedProducts.length > 0 && (
              <Button type="link" size="small" onClick={() => setSelectedProducts([])}>
                전체 해제
              </Button>
            )}
          </Space>
          {selectedProducts.length > 0 && (
            <Space wrap size={[4, 4]}>
              {selectedProducts.map((p) => {
                const key = productKey(p);
                // URL 복원 항목은 제품명/코드가 비어 바코드만 표시. 검색 추가 항목은 "제품명 (바코드)".
                const label = p.productName || p.productCode;
                return (
                  <Tag key={key} closable onClose={() => handleRemoveProduct(key)} color="blue">
                    {label ? `${label} (${p.barcode})` : `바코드 ${p.barcode}`}
                  </Tag>
                );
              })}
            </Space>
          )}
        </Space>
      </Card>

      <PosProductSearchModal
        open={productModalOpen}
        onClose={() => setProductModalOpen(false)}
        onAdd={handleAddProducts}
      />

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
