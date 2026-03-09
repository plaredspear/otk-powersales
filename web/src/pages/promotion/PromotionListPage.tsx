import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Input, Select, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { usePromotions } from '@/hooks/promotion/usePromotions';
import { usePromotionTypes } from '@/hooks/promotion/usePromotionTypes';
import type { PromotionListItem } from '@/api/promotion';
import dayjs from 'dayjs';

const { Title } = Typography;

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

const CATEGORY_TAG: Record<string, string> = {
  라면: 'red',
  냉장: 'blue',
  냉동: 'cyan',
  만두: 'orange',
};

const CATEGORY_OPTIONS = [
  { value: '', label: '전체' },
  { value: '라면', label: '라면' },
  { value: '냉장', label: '냉장' },
  { value: '냉동', label: '냉동' },
  { value: '만두', label: '만두' },
];

function formatAmount(val: number | null): string {
  if (val == null) return '-';
  return `${Math.floor(val / 1000).toLocaleString()}천`;
}

function formatDateRange(start: string, end: string): string {
  const s = dayjs(start);
  const e = dayjs(end);
  return `${s.format('MM/DD')}~${e.format('MM/DD')}`;
}

export default function PromotionListPage() {
  const navigate = useNavigate();
  const [promotionTypeId, setPromotionTypeId] = useState<number | undefined>();
  const [category, setCategory] = useState<string | undefined>();
  const [startDate, setStartDate] = useState<string | undefined>();
  const [endDate, setEndDate] = useState<string | undefined>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);

  const { data: promotionTypes } = usePromotionTypes();
  const { data, isLoading } = usePromotions({
    keyword: keyword || undefined,
    promotionTypeId,
    category,
    startDate,
    endDate,
    page,
    size: 20,
  });

  const promotionTypeOptions = [
    { value: 0, label: '전체' },
    ...(promotionTypes?.filter((t) => t.isActive).map((t) => ({ value: t.id, label: t.name })) ??
      []),
  ];

  const columns: ColumnsType<PromotionListItem> = [
    {
      title: '행사번호',
      dataIndex: 'promotionNumber',
      width: 130,
      render: (val: string, record) => (
        <a onClick={() => navigate(`/promotions/${record.id}`)}>{val}</a>
      ),
    },
    {
      title: '행사명',
      dataIndex: 'promotionName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '유형',
      dataIndex: 'promotionTypeName',
      width: 90,
      align: 'center',
      render: (val: string | null) => {
        if (!val) return <Tag>-</Tag>;
        const color = PROMOTION_TYPE_TAG[val] ?? undefined;
        return <Tag color={color}>{val}</Tag>;
      },
    },
    {
      title: '카테고리',
      dataIndex: 'category',
      width: 90,
      align: 'center',
      render: (val: string | null) => {
        if (!val) return <Tag>-</Tag>;
        const color = CATEGORY_TAG[val] ?? undefined;
        return <Tag color={color}>{val}</Tag>;
      },
    },
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 150,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '기간',
      width: 160,
      align: 'center',
      render: (_: unknown, record: PromotionListItem) =>
        formatDateRange(record.startDate, record.endDate),
    },
    {
      title: '목표금액',
      dataIndex: 'targetAmount',
      width: 110,
      align: 'right',
      render: (val: number | null) => formatAmount(val),
    },
    {
      title: '실적금액',
      dataIndex: 'actualAmount',
      width: 110,
      align: 'right',
      render: (val: number | null) => formatAmount(val),
    },
    {
      title: '제품유형',
      dataIndex: 'productType',
      width: 90,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '지점명',
      dataIndex: 'branchName',
      width: 100,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '마감',
      dataIndex: 'isClosed',
      width: 60,
      align: 'center',
      render: (val: boolean) => (val ? <Tag color="red">마감</Tag> : null),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Title level={4} style={{ margin: 0 }}>
          행사마스터 관리
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/promotions/new')}>
          행사마스터 등록
        </Button>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 130 }}
          value={promotionTypeId ?? 0}
          options={promotionTypeOptions}
          onChange={(val) => {
            setPromotionTypeId(val || undefined);
            setPage(0);
          }}
        />
        <Select
          style={{ width: 120 }}
          value={category ?? ''}
          options={CATEGORY_OPTIONS}
          onChange={(val) => {
            setCategory(val || undefined);
            setPage(0);
          }}
        />
        <DatePicker
          placeholder="시작일"
          onChange={(date) => {
            setStartDate(date ? date.format('YYYY-MM-DD') : undefined);
            setPage(0);
          }}
        />
        <DatePicker
          placeholder="종료일"
          onChange={(date) => {
            setEndDate(date ? date.format('YYYY-MM-DD') : undefined);
            setPage(0);
          }}
        />
        <Input.Search
          placeholder="행사명/행사번호 검색"
          allowClear
          style={{ width: 250 }}
          onSearch={(val) => {
            setKeyword(val);
            setPage(0);
          }}
        />
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 1300 }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
        onRow={(record) => ({
          onClick: () => navigate(`/promotions/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
