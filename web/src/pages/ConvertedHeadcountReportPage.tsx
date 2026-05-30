import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Divider, Space, Spin, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  fetchConvertedHeadcountReport,
  exportConvertedHeadcountReport as apiExport,
  type ConvertedHeadcountReportRow,
  type ConvertedHeadcountReportVariant,
} from '@/api/convertedHeadcountReport';

const { Text } = Typography;

interface Props {
  /** 보고서 variant (메뉴별 고정). */
  variant: ConvertedHeadcountReportVariant;
  /** 화면 상단/엑셀 안내용 보고서명. */
  title: string;
}

interface QueryYearMonth {
  year: string;
  month: string;
}

const num = (v: number | null) => (v == null ? '-' : v.toLocaleString());

/**
 * 거래처유형별 환산인원 현황 — SF Report 5변형 이식 (Spec #847).
 *
 * 연/월 선택 후 전사 환산인원을 구분(거래처유형)×근무유형1×지점×연월로 집계. 구분 그룹 + 그룹별 소계 +
 * 전체 합계. variant 별 근무유형5/위탁제외/2팀(코스트센터)/지점 기준 차이는 backend 가 처리. 엑셀 다운로드.
 */
export default function ConvertedHeadcountReportPage({ variant, title }: Props) {
  const [yearMonth, setYearMonth] = useState<Dayjs>(dayjs());
  const [query, setQuery] = useState<QueryYearMonth | null>(null);

  const reportQuery = useQuery({
    queryKey: ['convertedHeadcountReport', variant, query?.year, query?.month],
    queryFn: () => fetchConvertedHeadcountReport(variant, query!.year, query!.month),
    enabled: query != null,
  });

  const handleSearch = () => {
    if (!yearMonth) {
      message.warning('조회 연월은 필수항목입니다.');
      return;
    }
    setQuery({ year: yearMonth.format('YYYY'), month: String(yearMonth.month() + 1) });
  };

  const handleExport = async () => {
    if (!query) return;
    try {
      await apiExport(variant, query.year, query.month);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<ConvertedHeadcountReportRow> = useMemo(
    () => [
      { title: '구분', dataIndex: 'accountType', width: 120, render: (v) => v ?? '-' },
      { title: '근무유형1', dataIndex: 'workingCategory1', width: 110, render: (v) => v ?? '-' },
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
    [],
  );

  const data = reportQuery.data;
  const hasResult = data != null && data.groups.length > 0;

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>조회연월:</span>
        <DatePicker
          picker="month"
          value={yearMonth}
          onChange={(v) => v && setYearMonth(v)}
          allowClear={false}
        />
        <Button type="primary" onClick={handleSearch} loading={reportQuery.isLoading}>
          조회
        </Button>
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

      {reportQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : hasResult ? (
        <>
          {data.groups.map((group) => (
            <div key={group.accountType || '(미지정)'} style={{ marginBottom: 20 }}>
              <Text strong>{group.accountType || '(미지정)'}</Text>
              <Text type="secondary" style={{ marginLeft: 12 }}>
                소계 — 환산인원 {group.subtotalHeadcount.toLocaleString()}
              </Text>
              <Table
                rowKey={(r, idx) =>
                  `${r.workingCategory1 ?? ''}-${r.branchName ?? ''}-${r.yearMonth ?? ''}-${idx}`
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
        <Table
          columns={columns}
          dataSource={[]}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: query == null ? '조회 연월을 선택하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
