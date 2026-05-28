import { useMemo, useState } from 'react';
import {
  Alert,
  Card,
  Empty,
  Input,
  Popover,
  Segmented,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  usePermissionMatrix,
  usePermissionSetMatrix,
  usePermissionSets,
  useProfiles,
} from '@/hooks/admin/useAdminPermission';
import { menuRoute } from '@/config/menuConfig';
import { buildRows, type PageAccessGuideRow, type RequirementKind } from './pageAccessGuide';

const { Title, Paragraph, Text } = Typography;

const TAG_PREVIEW_COUNT = 5;

/**
 * 운영자 가이드 — 각 페이지가 요구하는 권한과 그 요구를 만족하는 SF Profile / PermissionSet 일람.
 * VIEW_ALL_DATA 권한자만 접근 (라우터 가드).
 *
 * source: `web/src/config/menuConfig.tsx` 의 menu 트리 + backend 권한 매트릭스 endpoint 3개.
 * `web/src/routes.tsx` 의 라우터 가드와 menuConfig 가 권한 메타를 이중 보관하는 점을 의식하여,
 * menuConfig 를 유일한 source-of-truth 로 채택한다 — 두 곳을 동기화하지 못한 페이지는 본 가이드와
 * 실제 라우터 가드가 달라질 수 있음.
 */
export default function PageAccessGuidePage() {
  const profilesQuery = useProfiles();
  const permissionSetsQuery = usePermissionSets();
  const profileMatrixQuery = usePermissionMatrix();
  const permissionSetMatrixQuery = usePermissionSetMatrix();

  const [category, setCategory] = useState<string>('전체');
  const [keyword, setKeyword] = useState('');

  const isLoading =
    profilesQuery.isLoading ||
    permissionSetsQuery.isLoading ||
    profileMatrixQuery.isLoading ||
    permissionSetMatrixQuery.isLoading;

  const isError =
    profilesQuery.isError ||
    permissionSetsQuery.isError ||
    profileMatrixQuery.isError ||
    permissionSetMatrixQuery.isError;

  const errorMessage =
    (profilesQuery.error as Error)?.message ||
    (permissionSetsQuery.error as Error)?.message ||
    (profileMatrixQuery.error as Error)?.message ||
    (permissionSetMatrixQuery.error as Error)?.message ||
    '';

  const allRows: PageAccessGuideRow[] = useMemo(() => {
    if (
      !profilesQuery.data ||
      !permissionSetsQuery.data ||
      !profileMatrixQuery.data ||
      !permissionSetMatrixQuery.data
    ) {
      return [];
    }
    return buildRows({
      menu: menuRoute,
      profiles: profilesQuery.data,
      permissionSets: permissionSetsQuery.data,
      profileMatrix: profileMatrixQuery.data,
      permissionSetMatrix: permissionSetMatrixQuery.data,
    });
  }, [
    profilesQuery.data,
    permissionSetsQuery.data,
    profileMatrixQuery.data,
    permissionSetMatrixQuery.data,
  ]);

  const categories = useMemo(() => {
    const set = new Set(allRows.map((r) => r.category));
    return ['전체', ...Array.from(set)];
  }, [allRows]);

  const filteredRows = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    return allRows.filter((row) => {
      if (category !== '전체' && row.category !== category) return false;
      if (kw && !row.pageName.toLowerCase().includes(kw) && !row.path.toLowerCase().includes(kw)) {
        return false;
      }
      return true;
    });
  }, [allRows, category, keyword]);

  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert type="error" message="페이지 권한 가이드 조회 실패" description={errorMessage} />
      </div>
    );
  }

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card>
        <Title level={4} style={{ marginTop: 0 }}>
          페이지별 필요 권한
        </Title>
        <Paragraph type="secondary" style={{ marginBottom: 0 }}>
          좌측 메뉴의 각 페이지가 요구하는 SF 권한과, 그 요구를 만족하는 Profile / PermissionSet 일람입니다.
          특정 사용자에게 페이지 접근 권한을 부여하려면 아래 표의 Profile 중 하나를 부여하거나
          (Profile 은 사용자당 1개), PermissionSet 을 추가로 부여하세요.
        </Paragraph>
        <Alert
          style={{ marginTop: 12 }}
          type="info"
          showIcon
          message="권한 메타의 source 는 web/src/config/menuConfig.tsx 이며, 실제 라우터 가드 (routes.tsx) 와 동기화되어 있어야 정확합니다."
        />
      </Card>

      <Card>
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          <Space wrap>
            <Segmented
              value={category}
              onChange={(v) => setCategory(String(v))}
              options={categories.map((c) => ({ label: c, value: c }))}
            />
            <Input.Search
              placeholder="페이지명 / 경로 검색"
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              style={{ width: 280 }}
            />
            <Text type="secondary">총 {filteredRows.length}개 페이지</Text>
          </Space>
          <Table<PageAccessGuideRow>
            rowKey="key"
            dataSource={filteredRows}
            columns={columns}
            pagination={{ pageSize: 50, showSizeChanger: true, pageSizeOptions: ['20', '50', '100'] }}
            size="small"
            locale={{ emptyText: <Empty description="해당하는 페이지가 없습니다" /> }}
          />
        </Space>
      </Card>
    </Space>
  );
}

const columns: ColumnsType<PageAccessGuideRow> = [
  {
    title: '카테고리',
    dataIndex: 'category',
    key: 'category',
    width: 140,
    sorter: (a, b) => a.category.localeCompare(b.category),
  },
  {
    title: '페이지명',
    key: 'pageName',
    width: 220,
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong>{row.pageName}</Text>
        <Text type="secondary" style={{ fontSize: 12 }}>{row.path}</Text>
      </Space>
    ),
    sorter: (a, b) => a.pageName.localeCompare(b.pageName),
  },
  {
    title: '요구 권한',
    key: 'requirement',
    width: 280,
    render: (_, row) => (
      <Space direction="vertical" size={2}>
        <Tag color={requirementKindColor(row.requirementKind)}>{requirementKindLabel(row.requirementKind)}</Tag>
        <Text>{row.requirementLabel}</Text>
      </Space>
    ),
    filters: [
      { text: 'entity', value: 'entity' },
      { text: 'system', value: 'system' },
      { text: 'profileName', value: 'profileName' },
      { text: 'open', value: 'open' },
    ],
    onFilter: (value, row) => row.requirementKind === value,
  },
  {
    title: '만족 Profile',
    key: 'satisfyingProfiles',
    render: (_, row) =>
      row.satisfyingProfiles.length === 0 ? (
        <Text type="secondary">-</Text>
      ) : (
        <TagList
          items={row.satisfyingProfiles.map((p) => ({ key: String(p.profileId), label: p.name }))}
          color="blue"
        />
      ),
  },
  {
    title: '만족 PermissionSet',
    key: 'satisfyingPermissionSets',
    render: (_, row) =>
      row.satisfyingPermissionSets.length === 0 ? (
        <Text type="secondary">-</Text>
      ) : (
        <TagList
          items={row.satisfyingPermissionSets.map((ps) => ({
            key: String(ps.permissionSetId),
            label: ps.label ? `${ps.label} (${ps.name})` : ps.name,
          }))}
          color="purple"
        />
      ),
  },
];

function requirementKindLabel(kind: RequirementKind): string {
  switch (kind) {
    case 'entity':
      return 'entity 권한';
    case 'system':
      return '시스템 권한';
    case 'profileName':
      return 'Profile 이름';
    case 'open':
      return '제한 없음';
  }
}

function requirementKindColor(kind: RequirementKind): string {
  switch (kind) {
    case 'entity':
      return 'geekblue';
    case 'system':
      return 'volcano';
    case 'profileName':
      return 'gold';
    case 'open':
      return 'default';
  }
}

function TagList({ items, color }: { items: { key: string; label: string }[]; color: string }) {
  if (items.length <= TAG_PREVIEW_COUNT) {
    return (
      <Space size={4} wrap>
        {items.map((it) => (
          <Tag key={it.key} color={color}>
            {it.label}
          </Tag>
        ))}
      </Space>
    );
  }
  const visible = items.slice(0, TAG_PREVIEW_COUNT);
  const hidden = items.slice(TAG_PREVIEW_COUNT);
  return (
    <Space size={4} wrap>
      {visible.map((it) => (
        <Tag key={it.key} color={color}>
          {it.label}
        </Tag>
      ))}
      <Popover
        title={`전체 ${items.length}개`}
        content={
          <Space size={4} wrap style={{ maxWidth: 360 }}>
            {hidden.map((it) => (
              <Tag key={it.key} color={color}>
                {it.label}
              </Tag>
            ))}
          </Space>
        }
      >
        <Tag style={{ cursor: 'pointer' }}>+{hidden.length}</Tag>
      </Popover>
    </Space>
  );
}
