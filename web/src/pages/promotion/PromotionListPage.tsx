import { useLocation, useNavigate } from 'react-router-dom';
import { Button, DatePicker, Input, Select, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { usePromotions } from '@/hooks/promotion/usePromotions';
import { usePromotionFormMeta } from '@/hooks/promotion/usePromotionFormMeta';
import { usePermission } from '@/hooks/usePermission';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import type { PromotionListItem } from '@/api/promotion';
import dayjs from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';

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
  const location = useLocation();
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('promotion', 'EDIT');
  // page/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, filters, setFilter } = useListQueryParams({
    defaultFilters: { promotionType: '', startDate: '', endDate: '', keyword: '' },
  });
  const { promotionType, startDate, endDate, keyword } = filters;

  const { data: formMeta } = usePromotionFormMeta();
  // 상세 진입 시 현재 목록의 query string 을 state 로 넘겨, 상세의 "목록으로" 버튼이 직전 조건으로 복귀하게 한다.
  const goToDetail = useThrottleClick((id: number) =>
    navigate(`/promotions/${id}`, { state: { listSearch: location.search } }),
  );
  const goToProduct = useThrottleClick((productCode: string) =>
    navigate(`/product/${encodeURIComponent(productCode)}`, { state: { listSearch: location.search } }),
  );
  const handleCreate = useThrottleClick(() => navigate('/promotions/new'));
  const { data, isLoading } = usePromotions({
    keyword: keyword || undefined,
    promotionType: promotionType || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
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
        <a onClick={() => goToDetail(record.id)}>{val}</a>
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
      render: (val: string | null, record) =>
        val && record.primaryProductCode ? (
          <a onClick={() => goToProduct(record.primaryProductCode!)}>{val}</a>
        ) : (
          val ?? '-'
        ),
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
          onChange={(val) => setFilter('promotionType', val || '')}
        />
        <DatePicker
          placeholder="시작일"
          value={startDate ? dayjs(startDate) : null}
          onChange={(date) => setFilter('startDate', date ? date.format('YYYY-MM-DD') : '')}
        />
        <DatePicker
          placeholder="종료일"
          value={endDate ? dayjs(endDate) : null}
          onChange={(date) => setFilter('endDate', date ? date.format('YYYY-MM-DD') : '')}
        />
        <Input.Search
          placeholder="행사명/행사번호 검색"
          allowClear
          defaultValue={keyword ?? ''}
          style={{ width: 250 }}
          onSearch={(val) => setFilter('keyword', val)}
        />
      </div>

      <ResizableTable
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
      />
    </div>
  );
}
