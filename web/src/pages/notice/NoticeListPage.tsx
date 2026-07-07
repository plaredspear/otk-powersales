import { useNavigate } from 'react-router-dom';
import { Button, Input, Select, Space, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { useNotices } from '@/hooks/notice/useNotices';
import { useNoticeFormMeta } from '@/hooks/notice/useNoticeFormMeta';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import type { NoticeSummary } from '@/api/notice';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

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
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소를
  // 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/size/필터를 URL query string 에 보관 — 상세 이동 후 복귀/새로고침 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilter } = useListQueryParams({
    defaultFilters: {
      category: '',
      search: '',
    },
  });

  const { data, isLoading, refetch, isFetching } = useNotices({
    category: filters.category || undefined,
    search: filters.search || undefined,
    page: page + 1,
    size,
  });
  const { data: formMeta } = useNoticeFormMeta();
  const handleRowClick = useThrottleClick((id: number) => navigate(`/notices/${id}`));
  const handleCreate = useThrottleClick(() => navigate('/notices/new'));

  const columns: ColumnsType<NoticeSummary> = [
    {
      title: '#',
      width: 60,
      render: (_v, _r, index) => page * size + index + 1,
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
    <div
      ref={containerRef}
      style={{
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        height: containerHeight,
        boxSizing: 'border-box',
        minHeight: 0,
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16, flexShrink: 0 }}>
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            작성
          </Button>
        </Space>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexShrink: 0 }}>
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

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이가 scrollY 로 body 스크롤. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={isLoading}
          locale={listTableLocale()}
          scroll={{ x: 'max-content', y: scrollY }}
          pagination={buildListPagination({
            page: data ? data.currentPage - 1 : page,
            pageSize: size,
            total: data?.totalCount ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize,
          })}
          onRow={(record) => ({
            onClick: () => handleRowClick(record.id),
            style: { cursor: 'pointer' },
          })}
        />
      </div>
    </div>
  );
}
