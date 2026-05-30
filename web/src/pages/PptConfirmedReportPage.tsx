import { useMemo } from 'react';
import { Alert, Button, Space, Spin, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchPptConfirmedReport,
  exportPptConfirmedReport as apiExport,
  type PptConfirmedReportItem,
} from '@/api/pptConfirmedReport';

const { Text } = Typography;

/**
 * 전문행사조 확정 인원 — SF Report new_report_swJ 이식 (Spec #846).
 *
 * isConfirmed=true 전문행사조 마스터를 전사 조회 (검색 조건 없음). 6컬럼 그리드 + 엑셀 다운로드.
 * 기존 /promotion/ppt-masters 화면과 별개 (역할 분리).
 */
export default function PptConfirmedReportPage() {
  const query = useQuery({
    queryKey: ['pptConfirmedReport'],
    queryFn: fetchPptConfirmedReport,
  });

  const handleExport = async () => {
    try {
      await apiExport();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const columns: ColumnsType<PptConfirmedReportItem> = useMemo(
    () => [
      { title: '지점명', dataIndex: 'branchName', width: 120, render: (v) => v ?? '-' },
      { title: '성명', dataIndex: 'fullName', width: 100, render: (v) => v ?? '-' },
      { title: '사번', dataIndex: 'employeeNumber', width: 100, render: (v) => v ?? '-' },
      { title: '거래처명', dataIndex: 'accountName', width: 180, render: (v) => v ?? '-' },
      { title: '거래처코드', dataIndex: 'accountCode', width: 120, render: (v) => v ?? '-' },
      { title: '전문행사조', dataIndex: 'professionalPromotionTeam', width: 140, render: (v) => v ?? '-' },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 12 }} wrap>
        <Button type="primary" onClick={() => query.refetch()} loading={query.isFetching}>
          조회
        </Button>
        <Button onClick={handleExport} disabled={!query.data || query.data.items.length === 0}>
          엑셀 다운로드
        </Button>
      </Space>

      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">전문행사조 확정 인원</Text>
      </div>

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
          rowKey={(r, idx) => `${r.employeeNumber ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: '조회 결과가 없습니다' }}
        />
      )}
    </div>
  );
}
