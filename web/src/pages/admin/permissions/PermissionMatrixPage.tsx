import { useMemo, useState } from 'react';
import { Alert, Card, Input, Space, Spin, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { usePermissionMatrix } from '@/hooks/admin/useAdminPermission';
import type { EntityProfileRow } from '@/api/admin/permission';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Title } = Typography;

/**
 * Spec #803 — entity × Profile 권한 매트릭스 페이지 (read-only).
 *
 * Profile 별 표본 user 의 SfPermissionResolver 평탄화 결과를 entity 행으로 분해.
 * 5분 캐시 → Profile/Assignment 변경 후 최대 5분 stale.
 */
export default function PermissionMatrixPage() {
  const { data, isLoading, isError, error, refetch, isFetching } = usePermissionMatrix();
  const [entityKeyword, setEntityKeyword] = useState('');

  const filteredRows: EntityProfileRow[] = useMemo(() => {
    if (!data) return [];
    if (!entityKeyword) return data.rows;
    const kw = entityKeyword.toLowerCase();
    return data.rows.filter((r) => r.entity.toLowerCase().includes(kw));
  }, [data, entityKeyword]);

  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Alert type="error" message="권한 매트릭스 조회 실패" description={(error as Error)?.message} />
      </div>
    );
  }

  const renderBit = (v: boolean) => (v ? <Tag color="green">✓</Tag> : <Tag>-</Tag>);

  const columns: ColumnsType<EntityProfileRow> = [
    {
      title: 'Entity',
      dataIndex: 'entity',
      key: 'entity',
      fixed: 'left',
      width: 240,
      sorter: (a, b) => a.entity.localeCompare(b.entity),
    },
    ...data.profiles.flatMap((profile) => [
      {
        title: `${profile.name} · R`,
        key: `${profile.profileId}-R`,
        width: 80,
        render: (_: unknown, row: EntityProfileRow) =>
          renderBit(row.byProfile.find((p) => p.profileId === profile.profileId)?.canRead ?? false),
      },
      {
        title: `${profile.name} · C`,
        key: `${profile.profileId}-C`,
        width: 80,
        render: (_: unknown, row: EntityProfileRow) =>
          renderBit(row.byProfile.find((p) => p.profileId === profile.profileId)?.canCreate ?? false),
      },
      {
        title: `${profile.name} · E`,
        key: `${profile.profileId}-E`,
        width: 80,
        render: (_: unknown, row: EntityProfileRow) =>
          renderBit(row.byProfile.find((p) => p.profileId === profile.profileId)?.canEdit ?? false),
      },
      {
        title: `${profile.name} · D`,
        key: `${profile.profileId}-D`,
        width: 80,
        render: (_: unknown, row: EntityProfileRow) =>
          renderBit(row.byProfile.find((p) => p.profileId === profile.profileId)?.canDelete ?? false),
      },
    ]),
  ];

  return (
    <div style={{ padding: 16 }}>
      <Title level={4}>권한 매트릭스</Title>
      <Alert
        type="info"
        showIcon
        message="Profile 별 부여된 PermissionSet 영향 포함 합집합 매트릭스입니다. 운영 진단용 read-only 표시 (5분 캐시)."
        style={{ marginBottom: 16 }}
      />
      <Card>
        <Space style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', width: '100%' }}>
          <Input.Search
            placeholder="entity 검색"
            allowClear
            onSearch={(value) => setEntityKeyword(value)}
            style={{ width: 240 }}
          />
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </Space>
        <ResizableTable<EntityProfileRow>
          dataSource={filteredRows}
          rowKey="entity"
          columns={columns}
          pagination={false}
          size="small"
          scroll={{ x: 'max-content', y: 600 }}
        />
      </Card>
    </div>
  );
}
