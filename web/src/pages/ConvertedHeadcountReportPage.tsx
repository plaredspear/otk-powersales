import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Divider, Space, Tabs, Typography, message } from 'antd';
import type { TabsProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import dayjs, { type Dayjs } from 'dayjs';
import {
  fetchConvertedHeadcountReport,
  exportConvertedHeadcountReport as apiExport,
  isBranchScopedVariant,
  CONVERTED_HEADCOUNT_REPORT_VARIANTS,
  DEFAULT_CONVERTED_HEADCOUNT_VARIANT,
  type ConvertedHeadcountReportRow,
  type ConvertedHeadcountReportVariant,
} from '@/api/convertedHeadcountReport';
import { useReportBranches } from '@/hooks/female-employee/useReportBranches';
import BranchSingleSelect from '@/components/common/BranchSingleSelect';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';

const { Text } = Typography;

interface ContentProps {
  /** 활성 탭의 보고서 variant. */
  variant: ConvertedHeadcountReportVariant;
  /** 화면 상단/엑셀 안내용 보고서명. */
  title: string;
}

interface QueryYearMonth {
  year: string;
  month: string;
  branchCode?: string;
}

const num = (v: number | null) => (v == null ? '-' : v.toLocaleString());

const VALID_VARIANTS = new Set(CONVERTED_HEADCOUNT_REPORT_VARIANTS.map((m) => m.variant));

/**
 * 좌측 세로 탭 items — 그룹이 바뀌는 지점에 disabled 그룹 헤더를 끼워 넣어 구분한다.
 * disabled 항목은 클릭 대상이 아니므로 activeKey 후보가 되지 않는다.
 * 콘텐츠(children)는 활성 variant 항목에만 채우고, destroyInactiveTabPane 로 활성 탭만 마운트한다.
 */
function buildTabItems(activeVariant: ConvertedHeadcountReportVariant): NonNullable<TabsProps['items']> {
  const items: NonNullable<TabsProps['items']> = [];
  let prevGroup: string | null = null;
  for (const m of CONVERTED_HEADCOUNT_REPORT_VARIANTS) {
    if (m.group !== prevGroup) {
      items.push({
        key: `__group__${m.group}`,
        label: <Text type="secondary" strong>{`── ${m.group} ──`}</Text>,
        disabled: true,
      });
      prevGroup = m.group;
    }
    items.push({
      key: m.variant,
      label: m.shortLabel,
      // 활성 탭만 실제 콘텐츠(조회/쿼리). 비활성 탭은 destroyInactiveTabPane 로 언마운트되므로 빈 노드.
      children:
        m.variant === activeVariant ? (
          <ConvertedHeadcountReportContent key={m.variant} variant={m.variant} title={m.title} />
        ) : null,
    });
  }
  return items;
}

/**
 * 여사원 환산인원 — 좌측 세로 탭 셸.
 *
 * 12개 variant 를 그룹(거래처유형별/대리점/대형마트/기타) 세로 탭으로 노출하고, 활성 variant 를
 * URL `?variant=` 로 동기화(북마크·뒤로가기 유지). 콘텐츠는 variant 별 조회/집계/엑셀을 담당.
 */
export default function ConvertedHeadcountReportPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const rawVariant = searchParams.get('variant');
  const activeVariant: ConvertedHeadcountReportVariant =
    rawVariant && VALID_VARIANTS.has(rawVariant as ConvertedHeadcountReportVariant)
      ? (rawVariant as ConvertedHeadcountReportVariant)
      : DEFAULT_CONVERTED_HEADCOUNT_VARIANT;

  const items = useMemo(() => buildTabItems(activeVariant), [activeVariant]);

  const handleTabChange = (key: string) => {
    // variant 만 교체하고 나머지 쿼리 파라미터는 보존. 히스토리는 replace 로 누적 방지.
    const next = new URLSearchParams(searchParams);
    next.set('variant', key);
    setSearchParams(next, { replace: true });
  };

  return (
    <Tabs
      tabPosition="left"
      activeKey={activeVariant}
      onChange={handleTabChange}
      items={items}
      destroyInactiveTabPane
      style={{ padding: 8 }}
    />
  );
}

/**
 * 거래처유형별 환산인원 현황 — SF Report 5변형 이식 (Spec #847).
 *
 * 연/월 선택 후 전사 환산인원을 구분(거래처유형)×근무유형1×지점×연월로 집계. 구분 그룹 + 그룹별 소계 +
 * 전체 합계. variant 별 근무유형5/위탁제외/2팀(코스트센터)/지점 기준 차이는 backend 가 처리. 엑셀 다운로드.
 */
function ConvertedHeadcountReportContent({ variant, title }: ContentProps) {
  const [yearMonth, setYearMonth] = useState<Dayjs>(dayjs());
  const [branchCode, setBranchCode] = useState<string | undefined>(undefined);
  const [query, setQuery] = useState<QueryYearMonth | null>(null);

  // 소속기준 variant 만 지점 스코프가 적용되므로 그 경우에만 셀렉터 노출.
  const branchScoped = isBranchScopedVariant(variant);
  const { data: branches = [] } = useReportBranches();

  const reportQuery = useQuery({
    queryKey: ['convertedHeadcountReport', variant, query?.year, query?.month, query?.branchCode],
    queryFn: () => fetchConvertedHeadcountReport(variant, query!.year, query!.month, query!.branchCode),
    enabled: query != null,
  });

  const handleSearch = () => {
    if (!yearMonth) {
      message.warning('조회 연월은 필수항목입니다.');
      return;
    }
    setQuery({
      year: yearMonth.format('YYYY'),
      month: String(yearMonth.month() + 1),
      branchCode: branchScoped ? branchCode : undefined,
    });
  };

  const handleExport = async () => {
    if (!query) return;
    try {
      await apiExport(variant, query.year, query.month, query.branchCode);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const data = reportQuery.data;
  const hasResult = data != null && data.groups.length > 0;
  const showWc3 = data?.includeWorkingCategory3 ?? false;
  const groupLabel = data?.groupByAbcType ? 'ABC유형' : '구분';

  const columns: ColumnsType<ConvertedHeadcountReportRow> = useMemo(
    () => [
      { title: groupLabel, dataIndex: 'accountType', width: 120, render: (v) => v ?? '-' },
      { title: '근무유형1', dataIndex: 'workingCategory1', width: 110, render: (v) => v ?? '-' },
      ...(showWc3
        ? [
            {
              title: '근무유형3',
              dataIndex: 'workingCategory3',
              width: 110,
              render: (v: string | null) => v ?? '-',
            },
          ]
        : []),
      { title: '지점', dataIndex: 'branchName', width: 130, render: (v) => v ?? '-' },
      { title: '연월', dataIndex: 'yearMonth', width: 100, render: (v) => v ?? '-' },
      {
        title: '환산인원',
        dataIndex: 'convertedHeadcount',
        width: 110,
        align: 'right',
        render: num,
      },
    ],
    [showWc3, groupLabel],
  );

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap align="end">
        {branchScoped && (
          <BranchSingleSelect branches={branches} value={branchCode} onChange={setBranchCode} />
        )}
        <Space direction="vertical" size={4}>
          <span>조회연월:</span>
          <DatePicker
            picker="month"
            value={yearMonth}
            onChange={(v) => v && setYearMonth(v)}
            allowClear={false}
          />
        </Space>
        <Button type="primary" onClick={handleSearch} loading={reportQuery.isLoading}>
          조회
        </Button>
        {query != null && (
          <RefreshButton onRefresh={() => reportQuery.refetch()} refreshing={reportQuery.isFetching} />
        )}
        <Button onClick={handleExport} disabled={!hasResult}>
          엑셀 다운로드
        </Button>
      </Space>

      {query != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {query.year}-{query.month.padStart(2, '0')} {title}
          </Text>
        </div>
      )}

      {reportQuery.isError && (
        <Alert
          type="error"
          message={(reportQuery.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {hasResult ? (
        <>
          {data.groups.map((group) => (
            <div key={group.accountType || '(미지정)'} style={{ marginBottom: 20 }}>
              <Text strong>{group.accountType || '(미지정)'}</Text>
              <Text type="secondary" style={{ marginLeft: 12 }}>
                소계 — 환산인원 {group.subtotalHeadcount.toLocaleString()}
              </Text>
              <ResizableTable
                rowKey={(r, idx) =>
                  `${r.workingCategory1 ?? ''}-${r.workingCategory3 ?? ''}-${r.branchName ?? ''}-${r.yearMonth ?? ''}-${idx}`
                }
                size="small"
                columns={columns}
                dataSource={group.rows}
                pagination={false}
                scroll={{ x: 'max-content' }}
                style={{ marginTop: 6 }}
              />
            </div>
          ))}

          <Divider />
          <Text strong>합계 — 환산인원 {data.totalHeadcount.toLocaleString()}</Text>
        </>
      ) : (
        <ResizableTable
          columns={columns}
          dataSource={[]}
          loading={reportQuery.isLoading}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={listTableLocale({ searched: query != null })}
        />
      )}
    </div>
  );
}
