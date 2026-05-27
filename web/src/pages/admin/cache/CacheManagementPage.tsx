import { Button, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  evictCache,
  fetchCacheList,
  type CacheInfo,
  type EvictResult,
} from '@/api/admin/cache';

const { Title, Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'cache', 'list'] as const;

export default function CacheManagementPage() {
  const queryClient = useQueryClient();

  const {
    data: caches = [],
    isLoading,
    refetch,
  } = useQuery({ queryKey: QUERY_KEY, queryFn: fetchCacheList });

  const evictMutation = useMutation({
    mutationFn: (cacheName: string) => evictCache(cacheName),
    onSuccess: (result: EvictResult) => {
      message.success(
        `${result.cacheName} 무효화 완료 (${result.keysBefore} → ${result.keysAfter})`,
      );
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: Error) => {
      message.error(err.message || '캐시 무효화에 실패했습니다');
    },
  });

  const handleEvictClick = (cacheName: string, keys: number) => {
    Modal.confirm({
      title: 'Redis 캐시 무효화',
      content: (
        <div>
          <Paragraph style={{ marginBottom: 8 }}>
            <Text strong>{cacheName}</Text> 의 모든 key 를 즉시 삭제합니다.
          </Paragraph>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            현재 추정 key 개수: {keys < 0 ? '추정 불가' : `${keys}개`}
            <br />
            본 작업은 운영자 audit log 에 기록됩니다.
          </Paragraph>
        </div>
      ),
      okText: '무효화',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () => evictMutation.mutateAsync(cacheName),
    });
  };

  const columns: ColumnsType<CacheInfo> = [
    {
      title: 'Cache Name',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <code style={{ fontSize: 13 }}>{name}</code>,
    },
    {
      title: '추정 key 개수',
      dataIndex: 'estimatedKeyCount',
      key: 'estimatedKeyCount',
      width: 160,
      render: (count: number) =>
        count < 0 ? (
          <Tag>추정 불가</Tag>
        ) : count === 0 ? (
          <Tag color="default">0</Tag>
        ) : (
          <Tag color="blue">{count.toLocaleString()}</Tag>
        ),
    },
    {
      title: '작업',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Button
          danger
          size="small"
          loading={evictMutation.isPending && evictMutation.variables === record.name}
          onClick={() => handleEvictClick(record.name, record.estimatedKeyCount)}
        >
          전체 evict
        </Button>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 8 }}>
        Redis 캐시 관리
      </Title>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        cache name 전체 단위 evict 지원. 코드 수정/배포 후 stale 결과가 24h TTL 또는 SAP daily
        sync 까지 남아있는 사례에 사용하세요.
      </Paragraph>

      <Space style={{ marginBottom: 12 }}>
        <Button onClick={() => refetch()} loading={isLoading}>
          새로고침
        </Button>
      </Space>

      <Table<CacheInfo>
        rowKey="name"
        dataSource={caches}
        columns={columns}
        loading={isLoading}
        pagination={false}
        size="middle"
      />
    </div>
  );
}
