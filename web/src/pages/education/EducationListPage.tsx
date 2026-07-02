import { useNavigate } from 'react-router-dom';
import { Button, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { useEducationPosts } from '@/hooks/education/useEducationPosts';
import { useEducationCategories } from '@/hooks/education/useEducationCategories';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import type { EducationSummary } from '@/api/education';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  c00001: { color: 'orange', label: '시식매뉴얼' },
  c00002: { color: 'red', label: 'CS/안전' },
  c00003: { color: 'blue', label: '교육평가' },
  c00004: { color: 'green', label: '신제품소개' },
};

export default function EducationListPage() {
  const navigate = useNavigate();
  // page/size/필터를 URL query string 에 보관 — 상세 이동 후 복귀/새로고침 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilter } = useListQueryParams({
    defaultFilters: {
      category: '',
      search: '',
    },
  });

  const { data, isLoading, refetch, isFetching } = useEducationPosts({
    category: filters.category || undefined,
    search: filters.search || undefined,
    page: page + 1,
    size,
  });
  const { data: categories } = useEducationCategories();
  const handleRowClick = useThrottleClick((id: string) => navigate(`/education/${id}`));
  const handleCreate = useThrottleClick(() => navigate('/education/new'));

  const columns: ColumnsType<EducationSummary> = [
    {
      title: '#',
      width: 60,
      render: (_v, _r, index) => page * size + index + 1,
    },
    {
      title: '카테고리',
      dataIndex: 'eduCode',
      width: 120,
      render: (code: string) => {
        const tag = CATEGORY_TAG[code];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{code}</Tag>;
      },
    },
    {
      title: '제목',
      dataIndex: 'eduTitle',
    },
    {
      title: '첨부',
      dataIndex: 'attachmentCount',
      width: 60,
      align: 'center',
      render: (count: number) => (count > 0 ? count : '-'),
    },
    {
      title: '등록일',
      dataIndex: 'instDate',
      width: 120,
      render: (val: string) => val?.substring(0, 10),
    },
  ];

  const categoryOptions = [
    { value: '', label: '전체' },
    ...(categories?.map((c) => ({ value: c.eduCode, label: c.eduCodeNm })) ?? []),
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            교육 등록
          </Button>
        </Space>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <Select
          style={{ width: 140 }}
          value={filters.category}
          options={categoryOptions}
          onChange={(val) => setFilter('category', val)}
        />
        <Input.Search
          placeholder="제목 또는 내용 입력"
          allowClear
          enterButton="조회"
          style={{ width: 300 }}
          defaultValue={filters.search}
          onSearch={(val) => setFilter('search', val)}
        />
      </div>

      <ResizableTable
        rowKey="eduId"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        locale={listTableLocale()}
        pagination={buildListPagination({
          page: data ? data.currentPage - 1 : page,
          pageSize: size,
          total: data?.totalCount ?? 0,
          onPageChange: setPage,
          onSizeChange: setSize,
        })}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.eduId),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
