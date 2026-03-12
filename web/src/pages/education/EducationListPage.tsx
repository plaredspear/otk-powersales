import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Input, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { useEducationPosts } from '@/hooks/education/useEducationPosts';
import { useEducationCategories } from '@/hooks/education/useEducationCategories';
import type { EducationSummary } from '@/api/education';

const CATEGORY_TAG: Record<string, { color: string; label: string }> = {
  c00001: { color: 'orange', label: '시식매뉴얼' },
  c00002: { color: 'red', label: 'CS/안전' },
  c00003: { color: 'blue', label: '교육평가' },
  c00004: { color: 'green', label: '신제품소개' },
};

export default function EducationListPage() {
  const navigate = useNavigate();
  const [category, setCategory] = useState<string | undefined>();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  const { data, isLoading } = useEducationPosts({ category, search: search || undefined, page, size: 10 });
  const { data: categories } = useEducationCategories();

  const columns: ColumnsType<EducationSummary> = [
    {
      title: '#',
      width: 60,
      render: (_v, _r, index) => (page - 1) * 10 + index + 1,
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
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/education/new')}>
          교육 등록
        </Button>
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

      <Table
        rowKey="eduId"
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
          onClick: () => navigate(`/education/${record.eduId}`),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
