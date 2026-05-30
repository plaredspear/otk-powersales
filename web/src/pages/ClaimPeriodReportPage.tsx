import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Space, Spin, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import type { Dayjs } from 'dayjs';
import {
  fetchClaimPeriodReport,
  exportClaimPeriodReport as apiExport,
  type ClaimPeriodReportItem,
  type ClaimPeriodReportType,
} from '@/api/claimPeriodReport';

const { Text } = Typography;

interface Props {
  /** PACKAGING(포장불량만) / ALL(모든 클레임). 메뉴별 고정. */
  type: ClaimPeriodReportType;
}

interface QueryRange {
  startDate: string;
  endDate: string;
}

/**
 * 기간별 클레임 보고서 — SF Report X3_ONLY_veg(포장불량만) / X4_3xv(모든 클레임) 이식 (Spec #843).
 *
 * ClaimDate 기간 + status='전송완료' 클레임을 전사 조회. type=PACKAGING 이면 claimType1/상세SNS 컬럼 숨김(21컬럼),
 * ALL 이면 표시(23컬럼). 기간은 사용자 직접 입력(기본값 없음). 수량 합계 행 표시 + 엑셀 다운로드.
 */
export default function ClaimPeriodReportPage({ type }: Props) {
  const isAll = type === 'ALL';
  const [startDate, setStartDate] = useState<Dayjs | null>(null);
  const [endDate, setEndDate] = useState<Dayjs | null>(null);
  const [range, setRange] = useState<QueryRange | null>(null);

  const query = useQuery({
    queryKey: ['claimPeriodReport', type, range?.startDate, range?.endDate],
    queryFn: () => fetchClaimPeriodReport(range!.startDate, range!.endDate, type),
    enabled: range != null,
  });

  const handleSearch = () => {
    if (!startDate || !endDate) {
      message.warning('시작일과 종료일은 필수항목입니다.');
      return;
    }
    if (endDate.isBefore(startDate)) {
      message.warning('종료일은 시작일 이후여야 합니다.');
      return;
    }
    setRange({ startDate: startDate.format('YYYY-MM-DD'), endDate: endDate.format('YYYY-MM-DD') });
  };

  const handleExport = async () => {
    if (!range) return;
    try {
      await apiExport(range.startDate, range.endDate, type);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<ClaimPeriodReportItem> = useMemo(() => {
    const cols: ColumnsType<ClaimPeriodReportItem> = [
      { title: '클레임번호', dataIndex: 'claimName', width: 110, fixed: 'left', render: (v) => v ?? '-' },
      { title: '인터페이스일시', dataIndex: 'interfaceDate', width: 150, render: (v) => v ?? '-' },
      { title: '클레임일자', dataIndex: 'claimDate', width: 110, render: (v) => v ?? '-' },
      ...(isAll
        ? ([{ title: '클레임대분류', dataIndex: 'claimType1', width: 100, render: (v) => v ?? '-' }] as ColumnsType<ClaimPeriodReportItem>)
        : []),
      { title: '지점명', dataIndex: 'branchName', width: 100, render: (v) => v ?? '-' },
      { title: '사번', dataIndex: 'employeeCode', width: 100, render: (v) => v ?? '-' },
      { title: '사원명', dataIndex: 'employeeName', width: 90, render: (v) => v ?? '-' },
      { title: '연락처', dataIndex: 'mobilePhone', width: 130, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 160, render: (v) => v ?? '-' },
      ...(isAll
        ? ([{ title: '상세SNS명', dataIndex: 'detailSnsName', width: 120, render: (v) => v ?? '-' }] as ColumnsType<ClaimPeriodReportItem>)
        : []),
      { title: '거래처외부키', dataIndex: 'externalKey', width: 110, render: (v) => v ?? '-' },
      { title: '제품명', dataIndex: 'productName', width: 140, render: (v) => v ?? '-' },
      { title: '제품코드', dataIndex: 'productCode', width: 100, render: (v) => v ?? '-' },
      { title: '제조일자', dataIndex: 'manufacturingDate', width: 110, render: (v) => v ?? '-' },
      { title: '유통기한', dataIndex: 'expirationDate', width: 110, render: (v) => v ?? '-' },
      { title: '수량', dataIndex: 'quantity', width: 80, align: 'right', render: (v) => v ?? '-' },
      { title: '클레임소분류', dataIndex: 'claimType2', width: 110, render: (v) => v ?? '-' },
      { title: '클레임내용', dataIndex: 'defectDescription', width: 200, render: (v) => v ?? '-' },
      { title: '상담번호', dataIndex: 'counselNumber', width: 100, render: (v) => v ?? '-' },
      { title: '처리상태', dataIndex: 'actionStatus', width: 90, render: (v) => v ?? '-' },
      { title: '처리코드', dataIndex: 'actionCode', width: 90, render: (v) => v ?? '-' },
      { title: '사유유형', dataIndex: 'reasonType', width: 100, render: (v) => v ?? '-' },
      { title: '처리내용', dataIndex: 'actContent', width: 200, render: (v) => v ?? '-' },
    ];
    return cols;
  }, [isAll]);

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        <span>시작일:</span>
        <DatePicker value={startDate} onChange={(v) => setStartDate(v)} />
        <span>종료일:</span>
        <DatePicker value={endDate} onChange={(v) => setEndDate(v)} />
        <Button type="primary" onClick={handleSearch} loading={query.isLoading}>
          조회
        </Button>
        <Button onClick={handleExport} disabled={!query.data || query.data.items.length === 0}>
          엑셀 다운로드
        </Button>
      </Space>

      {range != null && (
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">
            {range.startDate} ~ {range.endDate} 기간별 클레임 ({isAll ? '모든 클레임' : '포장불량'})
          </Text>
        </div>
      )}

      {query.isError && (
        <Alert
          type="error"
          message={(query.error as Error)?.message ?? '조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {query.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <Table
          rowKey={(r, idx) => `${r.claimName ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText:
              range == null ? '조회 기간을 선택하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
          summary={() =>
            query.data && query.data.items.length > 0 ? (
              <Table.Summary fixed>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={columns.length}>
                    <Text strong>합계 수량: {query.data.totalQuantity}</Text>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              </Table.Summary>
            ) : null
          }
        />
      )}
    </div>
  );
}
