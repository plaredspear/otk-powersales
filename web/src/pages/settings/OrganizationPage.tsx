import { useState } from 'react';
import { Alert, Button, Input, Select, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useOrganizations } from '@/hooks/organization/useOrganizations';
import type { Organization } from '@/api/organization';

const LEVEL_OPTIONS = [
  { value: '', label: '레벨 전체' },
  { value: 'L2', label: 'L2 (본부)' },
  { value: 'L3', label: 'L3 (사업부)' },
  { value: 'L4', label: 'L4 (지점)' },
  { value: 'L5', label: 'L5 (조)' },
];

const renderNull = (val: string | null) => val ?? '-';

export default function OrganizationPage() {
  const [keyword, setKeyword] = useState<string | undefined>();
  const [level, setLevel] = useState<string | undefined>();

  const { data, isLoading, isError, error, refetch } = useOrganizations({ keyword, level });

  const columns: ColumnsType<Organization> = [
    { title: 'L2 조직명', dataIndex: 'org_nm2', width: 120, render: renderNull },
    { title: 'L2 CC코드', dataIndex: 'cc_cd2', width: 80, align: 'center', render: renderNull },
    { title: 'L3 조직명', dataIndex: 'org_nm3', width: 120, render: renderNull },
    { title: 'L3 CC코드', dataIndex: 'cc_cd3', width: 80, align: 'center', render: renderNull },
    { title: 'L4 조직명', dataIndex: 'org_nm4', width: 120, render: renderNull },
    { title: 'L4 CC코드', dataIndex: 'cc_cd4', width: 80, align: 'center', render: renderNull },
    { title: 'L5 조직명', dataIndex: 'org_nm5', width: 120, render: renderNull },
    { title: 'L5 CC코드', dataIndex: 'cc_cd5', width: 80, align: 'center', render: renderNull },
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
          value={level ?? ''}
          options={LEVEL_OPTIONS}
          onChange={(val) => setLevel(val || undefined)}
        />
        <Input.Search
          placeholder="조직명/조직코드/CC코드 검색"
          allowClear
          style={{ width: 280 }}
          onSearch={(val) => setKeyword(val || undefined)}
        />
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        locale={{ emptyText: '검색 결과가 없습니다' }}
        pagination={false}
        footer={() => `총 ${data?.length ?? 0}건`}
      />
    </div>
  );
}
