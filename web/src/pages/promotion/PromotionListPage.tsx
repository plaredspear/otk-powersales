import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Input, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { usePromotions } from '@/hooks/promotion/usePromotions';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { usePermission } from '@/hooks/usePermission';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type { PromotionListItem } from '@/api/promotion';
import dayjs from 'dayjs';

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

function formatDate(value: string): string {
  return dayjs(value).format('YYYY-MM-DD');
}

export default function PromotionListPage() {
  const navigate = useNavigate();
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('promotion', 'EDIT');
  const [promotionType, setPromotionType] = useState<string | undefined>();
  const [startDate, setStartDate] = useState<string | undefined>();
  const [endDate, setEndDate] = useState<string | undefined>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);

  const { data: formMeta } = usePromotionFormMeta();
  const handleRowClick = useThrottleClick((id: number) => navigate(`/promotions/${id}`));
  const handleCreate = useThrottleClick(() => navigate('/promotions/new'));
  const { data, isLoading } = usePromotions({
    keyword: keyword || undefined,
    promotionType,
    startDate,
    endDate,
    page,
    size: 20,
  });

  const promotionTypeOptions = [
    { value: '', label: '전체' },
    ...(formMeta?.promotionTypes.map((t) => ({ value: t.name, label: t.name })) ?? []),
  ];

  const columns: ColumnsType<PromotionListItem> = [
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 160,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '시작일',
      dataIndex: 'startDate',
      width: 110,
      align: 'center',
      render: formatDate,
    },
    {
      title: '종료일',
      dataIndex: 'endDate',
      width: 110,
      align: 'center',
      render: formatDate,
    },
    {
      title: '행사번호',
      dataIndex: 'promotionNumber',
      width: 130,
      render: (val: string, record) => (
        <a onClick={() => handleRowClick(record.id)}>{val}</a>
      ),
    },
    {
      title: '행사명',
      dataIndex: 'promotionName',
      width: 200,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처코드',
      dataIndex: 'accountCode',
      width: 110,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '대표제품',
      dataIndex: 'primaryProductName',
      width: 180,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '행사유형',
      dataIndex: 'promotionType',
      width: 90,
      align: 'center',
      render: (val: string | null) => {
        if (!val) return <Tag>-</Tag>;
        const color = PROMOTION_TYPE_TAG[val] ?? undefined;
        return <Tag color={color}>{val}</Tag>;
      },
    },
    {
      title: '매대위치',
      dataIndex: 'standLocation',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '제품유형',
      dataIndex: 'category1',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          marginBottom: 16,
        }}
      >
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            행사마스터 등록
          </Button>
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 130 }}
          value={promotionType ?? ''}
          options={promotionTypeOptions}
          onChange={(val) => {
            setPromotionType(val || undefined);
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
        scroll={{ x: 1400 }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.id),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
