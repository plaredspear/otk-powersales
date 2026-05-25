import { useMemo, useState } from 'react';
import { Alert, Button, Card, Descriptions, Input, Space, Spin, Tree, Typography } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { useUserRoleTree } from '@/hooks/admin/useUserRoleTree';
import type { UserRoleNode } from '@/api/admin/userRole';

const { Title, Paragraph, Text } = Typography;

interface FlatNode {
  node: UserRoleNode;
  ancestors: number[];
}

function flatten(nodes: UserRoleNode[], ancestors: number[] = []): FlatNode[] {
  const out: FlatNode[] = [];
  for (const node of nodes) {
    out.push({ node, ancestors });
    if (node.children.length > 0) {
      out.push(...flatten(node.children, [...ancestors, node.userRoleId]));
    }
  }
  return out;
}

function countAll(nodes: UserRoleNode[]): number {
  let count = 0;
  for (const node of nodes) {
    count += 1 + countAll(node.children);
  }
  return count;
}

function buildTreeData(
  nodes: UserRoleNode[],
  keyword: string,
  matchedIds: Set<number>,
): DataNode[] {
  return nodes
    .filter((node) => {
      if (!keyword) return true;
      if (matchedIds.has(node.userRoleId)) return true;
      return node.children.length > 0 && buildTreeData(node.children, keyword, matchedIds).length > 0;
    })
    .map((node) => {
      const isMatch = keyword.length > 0 && matchedIds.has(node.userRoleId);
      const title = (
        <Space size="small">
          <Text strong={isMatch}>{node.name}</Text>
          {node.developerName && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              ({node.developerName})
            </Text>
          )}
        </Space>
      );
      return {
        key: node.userRoleId,
        title,
        children: buildTreeData(node.children, keyword, matchedIds),
      };
    });
}

export default function UserRoleTreePage() {
  const { data: tree, isLoading, error } = useUserRoleTree();
  const [keyword, setKeyword] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [autoExpandParent, setAutoExpandParent] = useState(true);

  const flat = useMemo(() => (tree ? flatten(tree) : []), [tree]);
  const total = useMemo(() => (tree ? countAll(tree) : 0), [tree]);

  const matched = useMemo(() => {
    if (!keyword) return new Set<number>();
    const lower = keyword.toLowerCase();
    return new Set(
      flat
        .filter(({ node }) => {
          const inName = node.name.toLowerCase().includes(lower);
          const inDev = (node.developerName ?? '').toLowerCase().includes(lower);
          const inRollup = (node.rollupDescription ?? '').toLowerCase().includes(lower);
          return inName || inDev || inRollup;
        })
        .map(({ node }) => node.userRoleId),
    );
  }, [flat, keyword]);

  const treeData = useMemo(
    () => (tree ? buildTreeData(tree, keyword, matched) : []),
    [tree, keyword, matched],
  );

  const selectedDetail = useMemo(() => {
    if (selectedId == null) return null;
    return flat.find(({ node }) => node.userRoleId === selectedId)?.node ?? null;
  }, [flat, selectedId]);

  const handleSearch = (v: string) => {
    setKeyword(v);
    if (v && tree) {
      const lower = v.toLowerCase();
      const keysToExpand: number[] = [];
      const matchedSet = new Set<number>();
      flat.forEach(({ node, ancestors }) => {
        const isMatch =
          node.name.toLowerCase().includes(lower) ||
          (node.developerName ?? '').toLowerCase().includes(lower) ||
          (node.rollupDescription ?? '').toLowerCase().includes(lower);
        if (isMatch) {
          matchedSet.add(node.userRoleId);
          keysToExpand.push(...ancestors);
        }
      });
      setExpandedKeys(Array.from(new Set(keysToExpand)));
      setAutoExpandParent(true);
    } else {
      setExpandedKeys([]);
      setAutoExpandParent(false);
    }
  };

  const handleExpandAll = () => {
    if (!tree) return;
    setExpandedKeys(flat.map(({ node }) => node.userRoleId));
    setAutoExpandParent(false);
  };

  const handleCollapseAll = () => {
    setExpandedKeys([]);
    setAutoExpandParent(false);
  };

  return (
    <div style={{ padding: 16 }}>
      <Title level={3}>역할 (조직 계층)</Title>
      <Paragraph type="secondary">
        Salesforce UserRole 트리. parent_user_role_id 기반 부모-자식 계층으로 구성. 총 {total} 개 역할.
      </Paragraph>

      {error && (
        <Alert
          type="error"
          showIcon
          message="역할 트리 조회 실패"
          description={(error as Error).message}
          style={{ marginBottom: 16 }}
        />
      )}

      <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
        <Card
          style={{ flex: 1, minWidth: 0 }}
          title={
            <Space>
              <span>트리</span>
              <Input.Search
                allowClear
                placeholder="역할명 / DeveloperName / 보고서 표시명 검색"
                onSearch={handleSearch}
                onChange={(e) => {
                  if (!e.target.value) handleSearch('');
                }}
                style={{ width: 320 }}
              />
              <Button size="small" onClick={handleExpandAll}>
                전체 펼치기
              </Button>
              <Button size="small" onClick={handleCollapseAll}>
                전체 접기
              </Button>
            </Space>
          }
        >
          {isLoading ? (
            <div style={{ textAlign: 'center', padding: 40 }}>
              <Spin />
            </div>
          ) : (
            <Tree
              treeData={treeData}
              expandedKeys={expandedKeys}
              autoExpandParent={autoExpandParent}
              onExpand={(keys) => {
                setExpandedKeys(keys);
                setAutoExpandParent(false);
              }}
              onSelect={(keys) => {
                const k = keys[0];
                setSelectedId(typeof k === 'number' ? k : null);
              }}
              selectedKeys={selectedId != null ? [selectedId] : []}
              showLine={{ showLeafIcon: false }}
            />
          )}
        </Card>

        <Card title="역할 상세" style={{ width: 380, flexShrink: 0 }}>
          {selectedDetail ? (
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="역할명">{selectedDetail.name}</Descriptions.Item>
              <Descriptions.Item label="DeveloperName">
                {selectedDetail.developerName ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="상급자">{selectedDetail.parentName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="보고서 표시명">
                {selectedDetail.rollupDescription ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="직속 하위 역할 수">
                {selectedDetail.children.length}
              </Descriptions.Item>
            </Descriptions>
          ) : (
            <Text type="secondary">트리에서 역할을 선택하세요</Text>
          )}
        </Card>
      </div>
    </div>
  );
}
