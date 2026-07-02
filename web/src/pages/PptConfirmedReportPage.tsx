import { useMemo, useState } from 'react';
import { Alert, Button, Select, Space, Spin, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchPptConfirmedReport,
  exportPptConfirmedReport as apiExport,
  type PptConfirmedReportItem,
} from '@/api/pptConfirmedReport';
import { usePPTBranches } from '@/hooks/promotion/usePPTBranches';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;

/**
 * 전문행사조 확정 인원 — SF Report new_report_swJ 이식 (Spec #846).
 *
 * isConfirmed=true 전문행사조 마스터를 전사 조회 (검색 조건 없음). 6컬럼 그리드 + 엑셀 다운로드.
 * 기존 /promotion/ppt-masters 화면과 별개 (역할 분리).
 */
export default function PptConfirmedReportPage() {
  // 지점 셀렉터 — 권한별 지점 화이트리스트.
  //  - 다중 지점: Select 로 선택
  //  - 단일 지점(조장 등): 고정 Tag 로 지점명 표시 (PeriodBranchFilterBar 정합).
  const { data: branches } = usePPTBranches();
  const branchOptions = (branches ?? []).map((b) => ({ value: b.branchCode, label: b.branchName }));
  const singleBranch = branches?.length === 1 ? branches[0] : null;
  const isMultiBranch = (branches?.length ?? 0) > 1;

  // 지점 선택 버퍼 — "조회" 버튼 시점에만 applied 로 반영 (필터 변경만으로 조회하지 않음).
  const [branchCode, setBranchCode] = useState<string>('');
  const [appliedBranchCode, setAppliedBranchCode] = useState<string>('');
  // 마운트 자동 조회 없음 — 조회 버튼 클릭 시점에만 fetch.
  const [requested, setRequested] = useState(false);

  const query = useQuery({
    queryKey: ['pptConfirmedReport', appliedBranchCode],
    queryFn: () => fetchPptConfirmedReport(appliedBranchCode || undefined),
    enabled: requested,
  });

  const handleSearch = () => {
    if (requested && appliedBranchCode === branchCode) {
      query.refetch();
    } else {
      setAppliedBranchCode(branchCode);
      setRequested(true);
    }
  };

  const handleExport = async () => {
    try {
      await apiExport(appliedBranchCode || undefined);
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
        {isMultiBranch && (
          <Select
            placeholder="지점 (전체)"
            value={branchCode || undefined}
            onChange={(v) => setBranchCode(v ?? '')}
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
        <Button type="primary" onClick={handleSearch} loading={query.isFetching}>
          조회
        </Button>
        {query.data && (
          <RefreshButton onRefresh={() => query.refetch()} refreshing={query.isFetching} />
        )}
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
        <ResizableTable
          rowKey={(r, idx) => `${r.employeeNumber ?? ''}-${idx}`}
          size="small"
          columns={columns}
          dataSource={query.data?.items ?? []}
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: requested ? '조회 결과가 없습니다' : '조회 버튼을 눌러주세요',
          }}
        />
      )}
    </div>
  );
}
