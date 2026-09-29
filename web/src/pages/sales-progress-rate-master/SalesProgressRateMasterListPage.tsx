import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Input, Select, Tag, Tooltip, Typography } from 'antd';
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import { useSalesProgressRateMasters } from '@/hooks/sales-progress-rate-master/useSalesProgressRateMasters';
import { useSalesProgressRateMasterBranches } from '@/hooks/sales-progress-rate-master/useSalesProgressRateMasterBranches';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import type { SalesProgressRateMasterListItem } from '@/api/salesProgressRateMaster';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import DetailLink from '@/components/common/DetailLink';
import { buildListPagination } from '@/lib/listPagination';
import {
  MONTHLY_SALES_HISTORY_ENTITY,
  buildMonthlySalesSourceHref,
  toSalesMonthToken,
} from '@/lib/monthlySalesSourceLink';
import { usePermission } from '@/hooks/usePermission';
import { listTableLocale } from '@/lib/listTableLocale';

function formatAmount(value: number | null): string {
  return value != null ? value.toLocaleString() : '-';
}

/** 당월/전월 실적 컬럼 헤더 tooltip 본문 — 값의 출처 + 원천(ORORA) 갱신 시점을 함께 보여준다. */
const ACTUALS_SOURCE_HINT =
  '기준정보 > ORORA 월매출(월매출이력)의 마감 합계(전산마감 합계 + 물류마감 합계)에서 조회 시점에 산출됩니다. 금액을 클릭하면 해당 거래처·월의 원천 데이터를 볼 수 있습니다.';

/** 적재 시각 표기 — null 이면 '-'. */
function formatDateTime(value: string | null | undefined): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

/**
 * 현재 목록 행들의 원천(월매출이력) 최종 적재 시각 중 가장 최근 값.
 *
 * 행마다 거래처·매출월이 달라 적재 시각도 제각각이라, 헤더에는 "이 목록 기준 가장 최근" 을 표기한다
 * (행별 정확한 값은 금액 cell tooltip). 전부 SF 이관 스냅샷 폴백이면 null.
 */
function latestSourceUpdatedAt(
  rows: SalesProgressRateMasterListItem[],
  key: 'currentMonthSourceUpdatedAt' | 'previousMonthSourceUpdatedAt',
): string | null {
  const values = rows.map((row) => row[key]).filter((v): v is string => !!v);
  return values.length > 0 ? values.reduce((a, b) => (a > b ? a : b)) : null;
}

function formatRate(value: number | null): string {
  // SF ProgressRate__c (Percent, scale=0) — 서버는 비율(예: 0.85)을 전달, 화면은 소수점 없이 반올림한 정수 % 표기.
  return value != null ? `${Math.round(value * 100)}%` : '-';
}

export default function SalesProgressRateMasterListPage() {
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소를
  // 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/size/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: { targetYear: '', targetMonth: '', branchCode: '', keyword: '' },
  });
  const { targetYear, targetMonth, branchCode, keyword } = filters;
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [targetYearInput, setTargetYearInput] = useState(targetYear);
  const [targetMonthInput, setTargetMonthInput] = useState(targetMonth);
  const [branchCodeInput, setBranchCodeInput] = useState<string | undefined>(
    () => branchCode || undefined,
  );
  const [keywordInput, setKeywordInput] = useState(keyword);

  // 권한별 지점 화이트리스트 — 지점 1개면 셀렉터 대신 고정 Tag 표시 (거래처 마스터 화면과 동일 패턴).
  const { hasEntityPermission } = usePermission();
  const { data: branches } = useSalesProgressRateMasterBranches();
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));
  const singleBranch = branches?.length === 1 ? branches[0] : null;
  const isMultiBranch = (branches?.length ?? 0) > 1;

  const handleSearch = () => {
    setFilters({
      targetYear: targetYearInput,
      targetMonth: targetMonthInput,
      branchCode: branchCodeInput ?? '',
      keyword: keywordInput,
    });
  };

  const { data, isLoading, refetch, isFetching } = useSalesProgressRateMasters({
    keyword: keyword || undefined,
    targetYear: targetYear || undefined,
    targetMonth: targetMonth || undefined,
    branchCode: branchCode || undefined,
    page,
    size,
  });

  const rows = data?.content ?? [];

  /**
   * 당월/전월 실적 컬럼 헤더 — hover 시 산출 출처 + **ORORA 최종 적재 시각**을 보여준다.
   * 숫자가 언제 기준인지(= 어제 마감까지인지, 지난달에 멈춘 건지)를 화면에서 바로 판단할 수 있게 한다.
   */
  const actualsColumnTitle = (
    label: string,
    sourceKey: 'currentMonthSourceUpdatedAt' | 'previousMonthSourceUpdatedAt',
  ) => {
    const latest = latestSourceUpdatedAt(rows, sourceKey);
    return (
      <Tooltip
        title={
          <div style={{ whiteSpace: 'pre-line' }}>
            {`${ACTUALS_SOURCE_HINT}\n\n${
              latest
                ? `현재 목록 기준 ORORA 최종 적재: ${formatDateTime(latest)}`
                : 'ORORA 적재 이력 없음 — SF 이관 시점 값으로 표시 중입니다.'
            }`}
          </div>
        }
      >
        <span>{label} ⓘ</span>
      </Tooltip>
    );
  };

  // 실적 금액 → 원천 기준정보(ORORA 월매출) 링크. 권한(monthly_sales_history:R) 이 없거나
  // 거래처코드/목표월을 해석할 수 없으면 링크 없이 숫자만 노출한다.
  const canViewMonthlySales = hasEntityPermission(MONTHLY_SALES_HISTORY_ENTITY, 'READ');

  /** @param monthOffset 0 = 당월 실적, -1 = 전월 실적 (해당 매출월의 월매출이력으로 이동). */
  const renderAmountWithSource = (
    value: number | null,
    row: SalesProgressRateMasterListItem,
    monthOffset: number,
  ) => {
    const salesMonth = toSalesMonthToken(row.targetYear, row.targetMonth, monthOffset);
    // 행별 정확한 적재 시각 — 헤더의 "목록 기준 최신" 과 달리 이 행/월의 원천 시각이다.
    const sourceUpdatedAt =
      monthOffset === 0 ? row.currentMonthSourceUpdatedAt : row.previousMonthSourceUpdatedAt;
    const sourceText = sourceUpdatedAt
      ? `ORORA 적재 ${formatDateTime(sourceUpdatedAt)}`
      : 'ORORA 적재 없음 (SF 이관 값)';

    if (!canViewMonthlySales || !row.accountCode || !salesMonth) {
      return <Tooltip title={sourceText}>{formatAmount(value)}</Tooltip>;
    }
    return (
      <Tooltip
        title={`${salesMonth.slice(0, 4)}년 ${salesMonth.slice(4, 6)}월 월매출이력 보기 · ${sourceText}`}
      >
        <Link to={buildMonthlySalesSourceHref(row.accountCode, salesMonth, row.accountName)}>
          {formatAmount(value)}
        </Link>
      </Tooltip>
    );
  };

  const columns: ColumnsType<SalesProgressRateMasterListItem> = [
    {
      title: '이름',
      dataIndex: 'name',
      width: 140,
      fixed: 'left',
      render: (val: string | null, record) =>
        val ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <DetailLink to={`/sales-progress-rate-masters/${record.id}`}>{val}</DetailLink>
            <Typography.Text copyable={{ text: val, tooltips: ['이름 복사', '복사됨'] }} />
          </span>
        ) : (
          <DetailLink to={`/sales-progress-rate-masters/${record.id}`}>(이름 없음)</DetailLink>
        ),
    },
    {
      title: '목표 년도',
      dataIndex: 'targetYear',
      width: 90,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '목표 월',
      dataIndex: 'targetMonth',
      width: 80,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 160,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처지점명',
      dataIndex: 'accountBranchName',
      width: 130,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처코드',
      dataIndex: 'accountCode',
      width: 120,
      align: 'center',
      render: (val: string | null) =>
        val ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            {val}
            <Typography.Text copyable={{ text: val, tooltips: ['거래처코드 복사', '복사됨'] }} />
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '거래처유형',
      dataIndex: 'accountType',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '상온 목표 금액',
      dataIndex: 'rtTargetAmount',
      width: 120,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '라면 목표 금액',
      dataIndex: 'rmTargetAmount',
      width: 120,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '냉동/냉장 목표 금액',
      dataIndex: 'frTargetAmount',
      width: 140,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '유지 목표 금액',
      dataIndex: 'foTargetAmount',
      width: 120,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '합계 목표 금액',
      dataIndex: 'targetSum',
      width: 130,
      align: 'right',
      render: (val: number) => (val != null ? val.toLocaleString() : '-'),
    },
    {
      title: actualsColumnTitle('당월 매출 실적', 'currentMonthSourceUpdatedAt'),
      dataIndex: 'currentMonthSalesAmount',
      width: 130,
      align: 'right',
      render: (val: number | null, row: SalesProgressRateMasterListItem) =>
        renderAmountWithSource(val, row, 0),
    },
    {
      title: actualsColumnTitle('전월 매출 실적', 'previousMonthSourceUpdatedAt'),
      dataIndex: 'previousMonthSalesAmount',
      width: 130,
      align: 'right',
      render: (val: number | null, row: SalesProgressRateMasterListItem) =>
        renderAmountWithSource(val, row, -1),
    },
    {
      title: '매출 진도율',
      dataIndex: 'progressRate',
      width: 100,
      align: 'right',
      render: formatRate,
    },
  ];

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
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', flexShrink: 0 }}>
        {isMultiBranch && (
          <Select
            placeholder="지점 (전체)"
            style={{ width: 160 }}
            value={branchCodeInput || undefined}
            options={branchOptions}
            allowClear
            showSearch
            optionFilterProp="label"
            onChange={(val) => setBranchCodeInput(val || undefined)}
          />
        )}
        {singleBranch && (
          <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
            지점: {singleBranch.branchName}
          </Tag>
        )}
        <Input
          placeholder="목표 년도"
          allowClear
          style={{ width: 110 }}
          value={targetYearInput ?? ''}
          onChange={(e) => setTargetYearInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Input
          placeholder="목표 월"
          allowClear
          style={{ width: 90 }}
          value={targetMonthInput ?? ''}
          onChange={(e) => setTargetMonthInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Input
          placeholder="이름/거래처명 검색"
          allowClear
          value={keywordInput ?? ''}
          style={{ width: 250 }}
          onChange={(e) => setKeywordInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
        <div style={{ marginLeft: 'auto' }}>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </div>
      </div>

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이가 scrollY 로 body 스크롤. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="id"
          columns={columns}
          dataSource={rows}
          loading={isLoading}
          locale={listTableLocale()}
          scroll={{ x: 1900, y: scrollY }}
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
