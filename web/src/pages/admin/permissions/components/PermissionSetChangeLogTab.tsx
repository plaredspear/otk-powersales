import { useState } from 'react';
import { Alert, Button, Spin, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { usePermissionSetChangeLog } from '@/hooks/admin/usePermissionSetChangeLog';
import type { PermissionSetChangeLogEntry } from '@/api/admin/permission';
import PermissionSetChangeLogDiffModal from './PermissionSetChangeLogDiffModal';

const PAGE_SIZE = 20;

interface Props {
  permissionSetId: number;
}

const EVENT_TYPE_COLOR: Record<PermissionSetChangeLogEntry['eventType'], string> = {
  CREATE: 'green',
  UPDATE_META: 'blue',
  UPDATE_FLAGS: 'gold',
  DELETE: 'red',
};

/**
 * Spec #837 — PS 변경 이력 탭.
 *
 * 시간순 desc 페이지네이션 + 단건 diff 모달. 보존 기간 별도 정책 없음 (영구) — backend 가 적재만 책임.
 */
export default function PermissionSetChangeLogTab({ permissionSetId }: Props) {
  const [page, setPage] = useState(0);
  const [diffTarget, setDiffTarget] = useState<PermissionSetChangeLogEntry | null>(null);
  const { data, isLoading, isError, error } = usePermissionSetChangeLog(permissionSetId, page, PAGE_SIZE);

  if (isLoading && !data) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (isError) {
    return <Alert type="error" message="변경 이력 조회 실패" description={(error as Error)?.message} />;
  }

  const columns: ColumnsType<PermissionSetChangeLogEntry> = [
    { title: '일시', dataIndex: 'changedAt', key: 'changedAt', width: 180 },
    { title: '행위자', dataIndex: 'changedByName', key: 'changedByName', width: 160, render: (v) => v ?? '-' },
    {
      title: '이벤트',
      dataIndex: 'eventType',
      key: 'eventType',
      width: 140,
      render: (v: PermissionSetChangeLogEntry['eventType']) => (
        <Tag color={EVENT_TYPE_COLOR[v]}>{v}</Tag>
      ),
    },
    { title: '사유', dataIndex: 'changeReason', key: 'changeReason', render: (v) => v ?? '-' },
    {
      title: '',
      key: 'action',
      width: 100,
      render: (_, row) => (
        <Button size="small" onClick={() => setDiffTarget(row)}>
          상세
        </Button>
      ),
    },
  ];

  return (
    <>
      <ResizableTable<PermissionSetChangeLogEntry>
        dataSource={data?.content ?? []}
        rowKey="changeLogId"
        columns={columns}
        size="small"
        pagination={{
          current: page + 1,
          pageSize: PAGE_SIZE,
          total: data?.totalElements ?? 0,
          onChange: (p) => setPage(p - 1),
        }}
      />
      <PermissionSetChangeLogDiffModal
        open={!!diffTarget}
        entry={diffTarget}
        onClose={() => setDiffTarget(null)}
      />
    </>
  );
}
