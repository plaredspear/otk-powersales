import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { useNotices } from '@/hooks/notice/useNotices';
import { useNoticeFormMeta } from '@/hooks/notice/useNoticeFormMeta';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type { NoticeSummary } from '@/api/notice';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  COMPANY: { color: 'blue', label: '회사공지' },
  BRANCH: { color: 'green', label: '지점공지' },
};

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  DRAFT: { color: 'default', label: '임시저장' },
  PUBLISHED: { color: 'success', label: '발행' },
};

export default function NoticeListPage() {
  const navigate = useNavigate();
  const [category, setCategory] = useState<string | undefined>();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  const { data, isLoading, refetch, isFetching } = useNotices({ category, search: search || undefined, page, size: 10 });
  const { data: formMeta } = useNoticeFormMeta();
  const handleRowClick = useThrottleClick((id: number) => navigate(`/notices/${id}`));
  const handleCreate = useThrottleClick(() => navigate('/notices/new'));

  const columns: ColumnsType<NoticeSummary> = [
    {
      title: '#',
      width: 60,
      render: (_v, _r, index) => (page - 1) * 10 + index + 1,
    },
    {
      title: '제목',
      dataIndex: 'title',
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (status: string) => {
        const tag = STATUS_TAG[status];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{status}</Tag>;
      },
    },
    {
      title: '공개범위',
      dataIndex: 'scope',
      width: 110,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '카테고리',
      dataIndex: 'category',
      width: 120,
      render: (cat: string) => {
        const tag = CATEGORY_TAG[cat];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{cat}</Tag>;
      },
    },
    {
      title: '지점',
      dataIndex: 'branch',
      width: 200,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '작성일',
      dataIndex: 'createdAt',
      width: 120,
      render: (val: string) => val?.substring(0, 10),
    },
    {
      title: '부서',
      dataIndex: 'department',
      width: 140,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '작성자명',
      dataIndex: 'authorName',
      width: 110,
      render: (val: string | null) => val ?? '-',
    },
  ];

  const categoryOptions = [
    { value: '', label: '전체' },
    ...(formMeta?.categories.map((c) => ({ value: c.code, label: c.name })) ?? []),
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            작성
          </Button>
        </Space>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <Select
          style={{ width: 140 }}
          value={category ?? ''}
          options={categoryOptions}
          onChange={(val) => {
            setCategory(val || undefined);
            setPage(1);
          }}
        />
        <Input.Search
          placeholder="제목 또는 내용 입력"
          allowClear
          style={{ width: 300 }}
          onSearch={(val) => {
            setSearch(val);
            setPage(1);
          }}
        />
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        pagination={{
          current: data?.currentPage ?? 1,
          total: data?.totalCount ?? 0,
          pageSize: 10,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.id),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
