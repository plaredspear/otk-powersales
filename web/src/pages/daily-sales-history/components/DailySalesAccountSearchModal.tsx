import { useEffect, useState } from 'react';
import { Input, Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import ResizableTable from '@/components/common/ResizableTable';
import { fetchAccountsForDailySalesLookup, type Account } from '@/api/account';
import { listTableLocale } from '@/lib/listTableLocale';

const PAGE_SIZE = 20;

interface Props {
  open: boolean;
  onClose: () => void;
  onSelect: (account: Account) => void;
  /** 화면의 거래처코드 입력값 — 모달 진입 시 이어받아 즉시 조회한다 (2자 이상일 때). */
  initialKeyword?: string;
}

/**
 * ORORA 일매출 화면의 거래처 고급 검색 모달.
 *
 * 행사마스터 고급 검색과 동일한 UX(검색창 1개 + 다중 컬럼 결과 그리드 + 라디오 단일 선택 + 선택 버튼)이며,
 * 백엔드는 daily_sales_history.READ 로 가드된 `/api/v1/admin/accounts/lookup-for-daily-sales` 를 호출한다.
 * keyword 는 거래처명/SAP코드/전화/대표자명/주소/거래처지점명 OR 매칭된다.
 */
export default function DailySalesAccountSearchModal({
  open,
  onClose,
  onSelect,
  initialKeyword,
}: Props) {
  const [keywordInput, setKeywordInput] = useState('');
  // 검색 실행 시점의 확정 키워드 (쿼리 파라미터). null 이면 아직 검색 전.
  const [submittedKeyword, setSubmittedKeyword] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  // 모달 오픈 시 화면 입력값을 이어받아 즉시 조회, 닫을 때 검색 상태 초기화 (이전 검색 잔상 방지).
  useEffect(() => {
    if (open) {
      const seed = initialKeyword?.trim() ?? '';
      setKeywordInput(seed);
      setSubmittedKeyword(seed.length >= 2 ? seed : null);
    } else {
      setKeywordInput('');
      setSubmittedKeyword(null);
      setPage(0);
      setSelectedId(null);
    }
    // initialKeyword 는 오픈 시점 값만 사용 — 열려 있는 동안 부모 입력 변화로 검색이 리셋되지 않게 한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const { data, isFetching } = useQuery({
    queryKey: ['daily-sales-account-search', submittedKeyword, page],
    queryFn: () =>
      fetchAccountsForDailySalesLookup({
        keyword: submittedKeyword ?? undefined,
        page,
        size: PAGE_SIZE,
      }),
    // 검색 실행 시에만 조회 (빈 검색으로 전 거래처가 노출되지 않게 한다).
    enabled: open && submittedKeyword != null,
  });

  const rows = data?.content ?? [];
  const selectedAccount = rows.find((a) => a.id === selectedId) ?? null;

  const handleSearch = () => {
    const trimmed = keywordInput.trim();
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
    { title: '거래처유형', dataIndex: 'accountType', key: 'accountType', width: 110 },
    { title: 'ABC유형', dataIndex: 'abcType', key: 'abcType', width: 90 },
    { title: '주소', dataIndex: 'address1', key: 'address1', width: 240 },
    { title: '대표자명', dataIndex: 'representative', key: 'representative', width: 100 },
    { title: '거래처지점명', dataIndex: 'branchName', key: 'branchName', width: 120 },
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
      width="min(1200px, calc(100vw - 64px))"
      destroyOnClose
    >
      <Input.Search
        placeholder="거래처명 / SAP코드 / 전화 / 대표자명 / 주소 / 지점명 검색 (2자 이상)"
        allowClear
        enterButton="조회"
        value={keywordInput}
        onChange={(e) => setKeywordInput(e.target.value)}
        onSearch={handleSearch}
        style={{ width: '100%', marginBottom: 12 }}
      />
      <ResizableTable<Account>
        dataSource={rows}
        rowKey="id"
        columns={columns}
        loading={isFetching}
        scroll={{ x: 1100 }}
        locale={listTableLocale({
          searched: submittedKeyword != null,
          beforeSearchText: '검색어를 2자 이상 입력한 뒤 조회해주세요.',
        })}
        pagination={{
          current: page + 1,
          pageSize: PAGE_SIZE,
          total: data?.totalElements ?? 0,
          showSizeChanger: false,
          showTotal: (t) => `총 ${t}건`,
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
    </Modal>
  );
}
