import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Modal, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { useAccounts } from '@/hooks/account/useAccounts';
import { useInventorySearch } from '@/hooks/product/useProducts';
import {
  useProductInventorySearchStore,
  type InventorySearchTarget,
} from '@/stores/productInventorySearchStore';
import type { InventorySearchResultItem } from '@/api/product';

interface Props {
  open: boolean;
  onClose: () => void;
}

/**
 * UC-03 (목록 일괄) + UC-04 (단건 Quick Action) 공용 재고조회 모달.
 *
 * 진입 시 store 의 targets 가 1~50건이면 거래처/납기일 입력 + 조회 가능.
 * targets 가 0건이면 진입 단계에서 차단 (호출 측이 이미 처리하지만 안전망).
 */
export default function InventorySearchModal({ open, onClose }: Props) {
  const targets = useProductInventorySearchStore((s) => s.targets);
  const [accountId, setAccountId] = useState<number | undefined>();
  const [deliveryDate, setDeliveryDate] = useState<Dayjs | null>(null);
  const [accountKeyword, setAccountKeyword] = useState<string>('');
  const [results, setResults] = useState<InventorySearchResultItem[] | null>(null);

  const inventorySearch = useInventorySearch();
  const { data: accountList, isLoading: accountLoading } = useAccounts({
    keyword: accountKeyword || undefined,
    page: 0,
    size: 20,
  });

  const minDate = dayjs().add(1, 'day').startOf('day');

  const disabledDate = (current: Dayjs) => {
    return current && current.isBefore(minDate);
  };

  const canSubmit = useMemo(() => {
    return targets.length > 0 && targets.length <= 50 && accountId != null && deliveryDate != null;
  }, [targets, accountId, deliveryDate]);

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      const response = await inventorySearch.mutateAsync({
        accountId: accountId!,
        productCodes: targets.map((t) => t.productCode),
        deliveryRequestDate: deliveryDate!.format('YYYY-MM-DD'),
      });
      setResults(response.results);
      message.success('요청 성공');
    } catch (err) {
      setResults(null);
      message.error((err as Error)?.message || '요청 중 에러가 발생했습니다.');
    }
  };

  const handleClose = () => {
    setAccountId(undefined);
    setDeliveryDate(null);
    setAccountKeyword('');
    setResults(null);
    onClose();
  };

  const targetColumns: ColumnsType<InventorySearchTarget> = [
    { title: '제품코드', dataIndex: 'productCode', width: 110 },
    { title: '제품명', dataIndex: 'name', ellipsis: true, render: (v: string | null) => v ?? '-' },
    { title: '카테고리1', dataIndex: 'category1', width: 110, render: (v: string | null) => v ?? '-' },
    { title: '카테고리2', dataIndex: 'category2', width: 110, render: (v: string | null) => v ?? '-' },
  ];

  const resultColumns: ColumnsType<InventorySearchResultItem> = [
    { title: '제품코드', dataIndex: 'productCode', width: 110 },
    { title: '제품명', dataIndex: 'productName', ellipsis: true, render: (v: string | null) => v ?? '-' },
    {
      title: '환산치수량',
      dataIndex: 'conversionQuantity',
      width: 100,
      align: 'right',
      render: (v: number) => v.toLocaleString(),
    },
    {
      title: '공급제한수량',
      dataIndex: 'supplyLimitQuantity',
      width: 120,
      align: 'right',
      render: (v: number) => (v === 2147483647 ? '제한없음' : v.toLocaleString()),
    },
    {
      title: '단가',
      dataIndex: 'unitPrice',
      width: 110,
      align: 'right',
      render: (v: number) => (v != null ? v.toLocaleString() : '-'),
    },
    {
      title: '단위',
      dataIndex: 'unit',
      width: 60,
      align: 'center',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '메시지',
      dataIndex: 'message',
      render: (v: string | null) => (v ? <Tag color="orange">{v}</Tag> : '-'),
    },
  ];

  const accountOptions = (accountList?.content ?? []).map((a) => ({
    value: a.id,
    label: `${a.name ?? '-'} (${a.externalKey ?? '-'})`,
  }));

  return (
    <Modal
      title="재고조회"
      open={open}
      width={960}
      onCancel={handleClose}
      footer={[
        <Button key="cancel" onClick={handleClose}>닫기</Button>,
        <Button
          key="submit"
          type="primary"
          loading={inventorySearch.isPending}
          disabled={!canSubmit}
          onClick={handleSubmit}
        >
          조회
        </Button>,
      ]}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={`선택한 제품 ${targets.length}건 (최대 50건)`}
      />

      <Table
        rowKey="productCode"
        size="small"
        columns={targetColumns}
        dataSource={targets}
        pagination={false}
        style={{ marginBottom: 16 }}
      />

      <Space style={{ marginBottom: 16, width: '100%' }} wrap>
        <Select
          showSearch
          placeholder="거래처 검색"
          style={{ width: 320 }}
          options={accountOptions}
          loading={accountLoading}
          value={accountId}
          filterOption={false}
          onSearch={setAccountKeyword}
          onChange={setAccountId}
          allowClear
          onClear={() => setAccountId(undefined)}
        />
        <DatePicker
          placeholder="납기일 (내일 이후)"
          value={deliveryDate}
          onChange={setDeliveryDate}
          disabledDate={disabledDate}
          style={{ width: 200 }}
        />
      </Space>

      {results && (
        <Table
          rowKey="productCode"
          size="small"
          columns={resultColumns}
          dataSource={results}
          pagination={false}
          locale={{ emptyText: '조회 결과가 없습니다' }}
        />
      )}
    </Modal>
  );
}
