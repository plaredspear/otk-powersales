import { useState } from 'react';
import { Alert, Button, Input, Select, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useOrganizations } from '@/hooks/organization/useOrganizations';
import type { Organization } from '@/api/organization';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';

const LEVEL_OPTIONS = [
  { value: '', label: '레벨 전체' },
  { value: 'L2', label: 'L2 (본부)' },
  { value: 'L3', label: 'L3 (사업부)' },
  { value: 'L4', label: 'L4 (지점)' },
  { value: 'L5', label: 'L5 (조)' },
];

const renderNull = (val: string | null) => val ?? '-';

interface OrganizationAppliedFilters {
  keyword?: string;
  level?: string;
}

export default function OrganizationPage() {
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 applied 로 반영 (필터 변경만으로 조회하지 않음)
  const [keywordInput, setKeywordInput] = useState('');
  const [levelInput, setLevelInput] = useState('');
  const [applied, setApplied] = useState<OrganizationAppliedFilters>({});

  const { data, isLoading, isError, error, refetch, isFetching } = useOrganizations(applied);

  const handleSearch = () => {
    setApplied({
      keyword: keywordInput || undefined,
      level: levelInput || undefined,
    });
  };

  const columns: ColumnsType<Organization> = [
    { title: 'L2 조직명', dataIndex: 'orgNameLevel2', width: 120, render: renderNull },
    { title: 'L2 CC코드', dataIndex: 'costCenterLevel2', width: 80, align: 'center', render: renderNull },
    { title: 'L3 조직명', dataIndex: 'orgNameLevel3', width: 120, render: renderNull },
    { title: 'L3 CC코드', dataIndex: 'costCenterLevel3', width: 80, align: 'center', render: renderNull },
    { title: 'L4 조직명', dataIndex: 'orgNameLevel4', width: 120, render: renderNull },
    { title: 'L4 CC코드', dataIndex: 'costCenterLevel4', width: 80, align: 'center', render: renderNull },
    { title: 'L5 조직명', dataIndex: 'orgNameLevel5', width: 120, render: renderNull },
    { title: 'L5 CC코드', dataIndex: 'costCenterLevel5', width: 80, align: 'center', render: renderNull },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="조직마스터를 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={<Button onClick={() => refetch()}>재시도</Button>}
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 160 }}
          value={levelInput}
          options={LEVEL_OPTIONS}
          onChange={setLevelInput}
        />
        <Input
          placeholder="조직명/조직코드/CC코드 검색"
          allowClear
          style={{ width: 280 }}
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
        <Space style={{ marginLeft: 'auto' }}>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </Space>
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        locale={listTableLocale()}
        pagination={false}
        footer={() => `총 ${data?.length ?? 0}건`}
      />
    </div>
  );
}
