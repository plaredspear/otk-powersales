import { useEffect, useState } from 'react';
import { Input, Modal, Spin } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { useQuery } from '@tanstack/react-query';
import { fetchAccountsForPromotionLookup, type Account } from '@/api/account';

const PAGE_SIZE = 20;

interface Props {
  open: boolean;
  onClose: () => void;
  onSelect: (account: Account) => void;
}

/**
 * 행사마스터 등록/수정 화면의 거래처 고급 검색 모달.
 *
 * SF 레거시 행사마스터 거래처 Enhanced Lookup(고급 검색) 동등 — 검색창 1개 + 다중 컬럼 결과 그리드 +
 * 라디오 단일 선택 + 선택 버튼. 백엔드 `/api/v1/admin/accounts/lookup` 을 재사용하므로 계정그룹
 * (1000/1010) + 폐업 배제 + 지점 스코프 필터가 그대로 적용된다 (기존 빠른 검색과 동일 게이팅).
 *
 * keyword 는 백엔드에서 거래처명/SAP코드/전화/대표자명/주소/거래처지점명 OR 매칭된다.
 */
export default function AccountAdvancedSearchModal({ open, onClose, onSelect }: Props) {
  const [submittedKeyword, setSubmittedKeyword] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  // 모달을 닫을 때마다 검색 상태 초기화 — 다음 오픈 시 이전 검색 잔상 방지.
  useEffect(() => {
    if (!open) {
      setSubmittedKeyword(undefined);
      setPage(0);
      setSelectedId(null);
    }
  }, [open]);

  const { data, isFetching } = useQuery({
    queryKey: ['account-advanced-search', submittedKeyword, page],
    queryFn: () =>
      fetchAccountsForPromotionLookup({ keyword: submittedKeyword, page, size: PAGE_SIZE }),
    // 검색어 2자 이상 입력 후 검색 실행 시에만 조회 (SF 고급 검색과 동일 — 빈 검색 전체 노출 방지).
    enabled: open && !!submittedKeyword && submittedKeyword.length >= 2,
  });

  const rows = data?.content ?? [];
  const selectedAccount = rows.find((a) => a.id === selectedId) ?? null;

  const handleSearch = (value: string) => {
    const trimmed = value.trim();
    if (trimmed.length < 2) return;
    setSubmittedKeyword(trimmed);
    setPage(0);
    setSelectedId(null);
  };

  const handleConfirm = () => {
    if (selectedAccount) {
      onSelect(selectedAccount);
      onClose();
    }
  };

  const columns: ColumnsType<Account> = [
    { title: '거래처명', dataIndex: 'name', key: 'name', width: 220, fixed: 'left' },
    { title: 'SAP거래처코드', dataIndex: 'externalKey', key: 'externalKey', width: 130 },
    { title: '거래상태', dataIndex: 'accountStatusName', key: 'accountStatusName', width: 90 },
    { title: '전화', dataIndex: 'phone', key: 'phone', width: 130 },
    { title: '거래처유형', dataIndex: 'accountType', key: 'accountType', width: 110 },
    { title: 'ABC유형', dataIndex: 'abcType', key: 'abcType', width: 90 },
    { title: '우편번호', dataIndex: 'zipCode', key: 'zipCode', width: 90 },
    { title: '주소', dataIndex: 'address1', key: 'address1', width: 240 },
    { title: '대표자명', dataIndex: 'representative', key: 'representative', width: 100 },
    { title: '거래처지점명', dataIndex: 'branchName', key: 'branchName', width: 120 },
    { title: '소유자', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
  ];

  return (
    <Modal
      open={open}
      title="거래처 고급 검색"
      okText="선택"
      cancelText="취소"
      onOk={handleConfirm}
      onCancel={onClose}
      okButtonProps={{ disabled: selectedId == null }}
      width={1000}
      destroyOnClose
    >
      <Input.Search
        placeholder="거래처명 / SAP코드 / 전화 / 대표자명 / 주소 / 지점명 검색 (2자 이상)"
        allowClear
        enterButton="검색"
        onSearch={handleSearch}
        style={{ width: '100%', marginBottom: 12 }}
      />
      {isFetching ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
          <Spin />
        </div>
      ) : (
        <ResizableTable<Account>
          dataSource={rows}
          rowKey="id"
          columns={columns}
          size="small"
          scroll={{ x: 1390 }}
          locale={{
            emptyText: submittedKeyword ? '검색 결과가 없습니다' : '검색어를 입력해주세요',
          }}
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (p) => {
              setPage(p - 1);
              setSelectedId(null);
            },
          }}
          rowSelection={{
            type: 'radio',
            selectedRowKeys: selectedId != null ? [selectedId] : [],
            onChange: (keys) => setSelectedId(keys[0] as number),
          }}
          onRow={(record) => ({
            onClick: () => setSelectedId(record.id),
          })}
        />
      )}
    </Modal>
  );
}
