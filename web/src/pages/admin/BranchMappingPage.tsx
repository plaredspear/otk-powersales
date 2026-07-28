import { useState } from 'react';
import { Alert, Button, Descriptions, Drawer, Input, Select, Space, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useBranchMappings } from '@/hooks/organization/useBranchMappings';
import type {
  BranchMappingExpandedCode,
  BranchMappingListItem,
  BranchMappingType,
} from '@/api/branchMapping';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';
import BranchMappingGuide from './BranchMappingGuide';

const { Text } = Typography;

/**
 * 유형별 태그 색 — 검토가 필요한 롤업을 가장 눈에 띄게.
 *
 * ROLLUP 은 지점 스코프에 **현행 타 조직** 데이터를 끌어오므로 사용처별 확인이 필요하다.
 * LEGACY(폐기 코드 확장)는 의도된 동작이라 중립색.
 */
const TYPE_COLORS: Record<BranchMappingType, string> = {
  ROLLUP: 'volcano',
  DUAL_CODE: 'purple',
  LEGACY: 'gold',
  NONE: 'default',
};

const TYPE_OPTIONS: { value: '' | BranchMappingType; label: string }[] = [
  { value: '', label: '유형 전체' },
  { value: 'ROLLUP', label: '롤업' },
  { value: 'LEGACY', label: '이력' },
  { value: 'DUAL_CODE', label: '이중코드' },
  { value: 'NONE', label: '없음' },
];

/** 확장 코드 1건 태그 — 조직명이 없으면(=폐기 이력 코드) 회색 처리해 즉시 구분되게 한다. */
function ExpandedCodeTag({ item }: { item: BranchMappingExpandedCode }) {
  const unresolved = item.orgName === null;
  return (
    <Tag
      color={unresolved ? undefined : 'blue'}
      style={{
        marginInlineEnd: 4,
        marginBottom: 2,
        opacity: unresolved ? 0.55 : 1,
        fontWeight: item.isSelf ? 600 : 400,
      }}
    >
      {item.code}
      {item.orgName ? ` ${item.orgName}` : ' (현행 없음)'}
    </Tag>
  );
}

export default function BranchMappingPage() {
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 applied 로 반영.
  const [keywordInput, setKeywordInput] = useState('');
  const [applied, setApplied] = useState<{ keyword?: string }>({});
  // 유형 필터는 서버 왕복 없이 클라이언트에서 거른다 (전건 74행 규모).
  const [typeFilter, setTypeFilter] = useState<'' | BranchMappingType>('');
  const [selected, setSelected] = useState<BranchMappingListItem | null>(null);

  const { data, isLoading, isError, error, refetch, isFetching } = useBranchMappings(applied);

  const handleSearch = () => setApplied({ keyword: keywordInput || undefined });

  const rows = (data?.content ?? []).filter((row) => !typeFilter || row.type === typeFilter);

  const columns: ColumnsType<BranchMappingListItem> = [
    { title: '지점코드', dataIndex: 'branchCode', width: 100 },
    { title: '라벨', dataIndex: 'label', width: 150, render: (val: string | null) => val ?? '-' },
    {
      title: '조직명',
      dataIndex: 'orgName',
      width: 140,
      render: (val: string | null) =>
        val ?? <Text type="secondary">현행 없음</Text>,
    },
    {
      title: '유형',
      dataIndex: 'type',
      width: 100,
      align: 'center',
      render: (type: BranchMappingType, record) => (
        <Tag color={TYPE_COLORS[type]}>{record.typeLabel}</Tag>
      ),
    },
    { title: '확장 수', dataIndex: 'expandedCount', width: 80, align: 'center' },
    {
      title: '확장 결과',
      dataIndex: 'expandedCodes',
      width: 460,
      render: (codes: BranchMappingExpandedCode[]) => (
        <div>
          {codes.map((code) => (
            <ExpandedCodeTag key={code.code} item={code} />
          ))}
        </div>
      ),
    },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="지점 코드 맵핑을 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={<Button onClick={() => refetch()}>재시도</Button>}
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="지점 스코프 조회 시 적용되는 지점 코드 확장 결과입니다."
        description={
          <>
            지점을 선택해 조회할 때 backend 가 해당 지점 코드를 아래 <b>확장 결과</b>의 코드 집합으로
            넓혀 데이터를 조회합니다. 회색 태그는 현행 조직에 없는 코드(조직 개편으로 폐기된 이력
            코드)입니다. 확장이 조회 결과에 미치는 영향은 아래 안내를 펼쳐 확인하세요.
          </>
        }
      />

      <BranchMappingGuide />

      {data?.cacheEmpty && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="확장 캐시가 비어 있습니다"
          description="지점매핑 데이터는 있으나 서버 메모리 캐시가 로드되지 않은 상태입니다. 데이터 적재 이후 서버가 재기동되지 않은 경우 발생하며, 현재 지점 확장이 동작하지 않습니다."
        />
      )}

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 140 }}
          value={typeFilter}
          options={TYPE_OPTIONS}
          onChange={setTypeFilter}
        />
        <Input
          placeholder="지점코드/라벨/조직명/확장코드 검색"
          allowClear
          style={{ width: 300 }}
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
        rowKey="branchCode"
        columns={columns}
        dataSource={rows}
        loading={isLoading}
        locale={listTableLocale()}
        pagination={false}
        scroll={{ x: 'max-content' }}
        onRow={(record) => ({
          onClick: () => setSelected(record),
          style: { cursor: 'pointer' },
        })}
        footer={() => `총 ${rows.length}건`}
      />

      <Drawer
        title={selected ? `지점 코드 맵핑 — ${selected.branchCode}` : ''}
        width={520}
        open={selected !== null}
        onClose={() => setSelected(null)}
      >
        {selected && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="지점코드">{selected.branchCode}</Descriptions.Item>
            <Descriptions.Item label="라벨">{selected.label ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="조직명">
              {selected.orgName ?? <Text type="secondary">현행 조직에 없음</Text>}
            </Descriptions.Item>
            <Descriptions.Item label="유형">
              <Tag color={TYPE_COLORS[selected.type]}>{selected.typeLabel}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="확장 결과">
              {selected.expandedCodes.map((code) => (
                <div key={code.code} style={{ marginBottom: 4 }}>
                  <ExpandedCodeTag item={code} />
                  {code.isSelf && <Text type="secondary">자기 자신</Text>}
                </div>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="미해석 코드 수">
              {selected.unresolvedCount}
              {selected.unresolvedCount > 0 && (
                <Text type="secondary"> (현행 조직에 없는 코드)</Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="원본 CSV">
              <Text code style={{ wordBreak: 'break-all' }}>
                {selected.rawIncludedBranchCodes}
              </Text>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
}
