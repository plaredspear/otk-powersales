import { useEffect, useState } from 'react';
import { Empty, Input, Modal, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  searchPosSalesProducts,
  type PosSalesProductSearchItem,
} from '@/api/posSales';

const { Text } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
  /** 선택 확정 시 호출 — 선택된 제품 목록을 전달한다. */
  onAdd: (items: PosSalesProductSearchItem[]) => void;
}

/** 검색 결과 행 식별 키 (제품코드 + 바코드). 바코드 없는 행은 매출 조회 키가 없어 선택 불가. */
function rowKey(p: PosSalesProductSearchItem): string {
  return `${p.productCode ?? ''}|${p.barcode ?? ''}`;
}

/**
 * POS매출 매출 조회 제품 검색 모달 — 제품명/제품코드/바코드 통합 검색 + 다중 선택.
 *
 * 모바일 POS매출 제품 검색 시트(`PosProductSearchSheet`) 정합. 검색어는 300ms 디바운스 후
 * `searchPosSalesProducts`(type=text) 로 조회하며, 숫자 입력 시 제품코드+바코드 포함 매칭된다.
 * 바코드는 스캔이 아니라 번호 입력으로 매칭한다.
 */
export default function PosProductSearchModal({ open, onClose, onAdd }: Props) {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [selectedRows, setSelectedRows] = useState<PosSalesProductSearchItem[]>([]);

  // destroyOnClose 로 모달이 닫히면 내부 state 가 폐기되어 다음 열림 시 초기값으로 재마운트되므로
  // 별도 초기화 effect 는 불필요하다.
  useEffect(() => {
    const h = setTimeout(() => setDebounced(keyword.trim()), 300);
    return () => clearTimeout(h);
  }, [keyword]);

  const searchQuery = useQuery({
    queryKey: ['admin', 'sales', 'pos', 'products', debounced],
    queryFn: () => searchPosSalesProducts(debounced, 'text'),
    enabled: open && debounced.length > 0,
  });

  const results = searchQuery.data ?? [];

  const columns: ColumnsType<PosSalesProductSearchItem> = [
    { title: '제품코드', dataIndex: 'productCode', width: 130, render: (v: string | null) => v ?? '-' },
    { title: '제품명', dataIndex: 'productName', render: (v: string | null) => v ?? '-' },
    { title: '바코드', dataIndex: 'barcode', width: 160, render: (v: string | null) => v ?? '-' },
  ];

  const handleOk = () => {
    onAdd(selectedRows);
    onClose();
  };

  return (
    <Modal
      open={open}
      title="매출 조회 제품 검색"
      onCancel={onClose}
      onOk={handleOk}
      okText={selectedRows.length > 0 ? `추가 (${selectedRows.length})` : '추가'}
      okButtonProps={{ disabled: selectedRows.length === 0 }}
      cancelText="취소"
      width={720}
      destroyOnClose
    >
      <Input.Search
        allowClear
        placeholder="제품명 / 제품코드 / 바코드번호로 검색"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        style={{ marginBottom: 12 }}
      />
      <Table
        rowKey={rowKey}
        size="small"
        columns={columns}
        dataSource={results}
        loading={searchQuery.isFetching}
        pagination={false}
        scroll={{ y: 360 }}
        rowSelection={{
          selectedRowKeys: selectedKeys,
          onChange: (keys, rows) => {
            setSelectedKeys(keys as string[]);
            setSelectedRows(rows);
          },
          // 바코드 없는 제품은 매출 조회 키(바코드)가 없어 선택 불가.
          getCheckboxProps: (record) => ({
            disabled: (record.barcode ?? '').trim().length === 0,
          }),
        }}
        locale={{
          emptyText:
            debounced.length === 0 ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={<Text type="secondary">제품명·제품코드·바코드번호로 검색하세요.</Text>}
              />
            ) : (
              <Empty description="검색 결과가 없습니다." />
            ),
        }}
      />
    </Modal>
  );
}
