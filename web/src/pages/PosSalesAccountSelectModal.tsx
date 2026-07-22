import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Checkbox, Input, Modal, Select, Space, Tag, Typography, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { TableRowSelection } from 'antd/es/table/interface';
import { useQuery } from '@tanstack/react-query';
import {
  MAX_SELECTABLE_ACCOUNTS,
  fetchPosSalesAccounts,
  type PosSalesAccountItem,
} from '@/api/posSales';
import { fetchFilterOptions } from '@/api/electronicSalesDashboard';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import type { Branch } from '@/api/team-schedule';

const { Text } = Typography;

/** 부모(메인)로 반환하는 선택 결과 — id 뿐 아니라 칩 라벨 표시용 메타도 함께 넘긴다. */
export type PosSalesSelectedAccount = Pick<PosSalesAccountItem, 'accountId' | 'accountName'>;

interface PosSalesAccountSelectModalProps {
  open: boolean;
  onClose: () => void;
  /** POS매출 전용 지점 목록 (조직 트리 스코프). */
  branches: Branch[];
  /** 모달 오픈 시점의 기존 선택 (재오픈 시 이어서 편집). */
  initialSelected: PosSalesSelectedAccount[];
  /** [선택 완료] 확정 콜백 — 부모는 이 결과로 칩을 갱신한다 (조회는 별도 버튼). */
  onConfirm: (selected: PosSalesSelectedAccount[]) => void;
}

/** 지점 목록 미로드 시 기본값 — 매 렌더 새 배열 identity 를 만들지 않도록 모듈 상수로 고정. */
const EMPTY_BRANCHES: Branch[] = [];

/**
 * 거래처 선택 모달 — POS매출 조회의 거래처 검색/선택 단계를 별도 컨텍스트로 격리한다.
 *
 * 기존에는 메인 화면 필터바의 [조회] 버튼이 "거래처 목록 조회"를 수행해 POS매출 조회로 오인되던
 * 문제가 있었다. 거래처를 고르는 행위(지점/유통형태/거래처유형/거래처명 검색 → 목록 체크)를 이
 * 모달로 옮겨, 메인에는 선택된 거래처 칩과 [POS 매출 조회] 최종 버튼만 남긴다.
 *
 * 조회 API 는 기존 1단 endpoint(`/accounts`) 를 그대로 재사용한다 (외부 POS DB 미접촉, 즉시 응답).
 * 유통형태/거래처유형 옵션은 전산실적과 동일 filter-options 를 재사용한다 (동일 권한 가드).
 */
export default function PosSalesAccountSelectModal({
  open,
  onClose,
  branches = EMPTY_BRANCHES,
  initialSelected,
  onConfirm,
}: PosSalesAccountSelectModalProps) {
  // 거래처 검색 조건 (모달 내부 버퍼)
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [distributionChannels, setDistributionChannels] = useState<string[]>([]);
  const [accountTypes, setAccountTypes] = useState<string[]>([]);
  const [customerKeyword, setCustomerKeyword] = useState<string>('');
  // [거래처 검색] 클릭 시점에 확정되는 조회 조건.
  const [query, setQuery] = useState<{
    codes: string[];
    customerKeyword?: string;
    distributionChannels: string[];
    accountTypes: string[];
  } | null>(null);

  // 선택된 거래처 (id → 메타). 재오픈 시 부모의 기존 선택으로 초기화.
  const [selectedMap, setSelectedMap] = useState<Map<number, PosSalesSelectedAccount>>(new Map());

  // 모달이 열릴 때마다 부모의 현재 선택으로 초기화하고, 검색 조건/결과는 리셋한다.
  useEffect(() => {
    if (!open) return;
    setSelectedMap(new Map(initialSelected.map((a) => [a.accountId, a])));
    setQuery(null);
    setCustomerKeyword('');
    setDistributionChannels([]);
    setAccountTypes([]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 지점 옵션 (가나다순). 단일지점 사용자는 열릴 때 본인 지점 자동 선택.
  const branchOptions = useMemo(
    () =>
      branches
        .map((b) => ({ value: b.branchCode, label: b.branchName }))
        .sort((a, b) => a.label.localeCompare(b.label, 'ko')),
    [branches],
  );
  const allCodes = useMemo(() => branches.map((b) => b.branchCode), [branches]);
  const allBranchSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;
  const someBranchSelected = selectedCodes.length > 0 && !allBranchSelected;
  const singleBranch = branches.length === 1;
  const firstBranchCode = branches[0]?.branchCode;

  // 지점 목록 로드/모달 오픈 시 단일지점 자동 선택 + stale 코드 정리.
  const allCodesKey = allCodes.join(',');
  useEffect(() => {
    if (!open || branches.length === 0) return;
    // selectedCodes 는 트리거가 아니라 최신값 읽기 목적이므로 함수형 updater 로 접근한다
    // (deps 에 넣으면 사용자가 수동 해제할 때마다 재실행돼 자동 재선택되는 루프 발생).
    setSelectedCodes((prev) => {
      const valid = prev.filter((c) => allCodes.includes(c));
      if (valid.length !== prev.length) return valid;
      if (singleBranch && prev.length === 0 && firstBranchCode) return [firstBranchCode];
      return prev; // 변화 없으면 identity 유지 → 불필요한 리렌더 없음
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, singleBranch, firstBranchCode, allCodesKey]);

  // 유통형태/거래처유형 옵션 — 전산실적과 동일 filter-options 재사용 (동일 queryKey 로 캐시 공유).
  const filterOptionsQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'filter-options'],
    queryFn: fetchFilterOptions,
    staleTime: 10 * 60 * 1000,
  });
  const filterOptions = filterOptionsQuery.data;
  const distributionChannelOptions = useMemo(
    () => (filterOptions?.distributionChannels ?? []).map((v) => ({ value: v, label: v })),
    [filterOptions],
  );
  // 거래처유형 옵션 — 유통형태 미선택 시 전체, 선택 시 선택된 유통형태들의 종속 거래처유형 합집합.
  const accountTypeOptions = useMemo(() => {
    if (!filterOptions) return [];
    let labels: string[];
    if (distributionChannels.length === 0) {
      labels = filterOptions.accountTypes;
    } else {
      const union = new Set<string>();
      distributionChannels.forEach((dist) => {
        (filterOptions.dependentAccountTypes[dist] ?? []).forEach((t) => union.add(t));
      });
      labels = filterOptions.accountTypes.filter((t) => union.has(t));
    }
    return labels.map((v) => ({ value: v, label: v }));
  }, [filterOptions, distributionChannels]);

  // 유통형태 변경 시, 새 집합의 종속 거래처유형에 속하지 않는 선택값 정리.
  const handleDistributionChange = (next: string[]) => {
    setDistributionChannels(next);
    if (accountTypes.length > 0 && next.length > 0 && filterOptions) {
      const allowed = new Set<string>();
      next.forEach((dist) => {
        (filterOptions.dependentAccountTypes[dist] ?? []).forEach((t) => allowed.add(t));
      });
      const kept = accountTypes.filter((t) => allowed.has(t));
      if (kept.length !== accountTypes.length) setAccountTypes(kept);
    }
  };

  const handleToggleAllBranches = () => {
    setSelectedCodes(allBranchSelected ? [] : allCodes);
  };

  // 거래처 목록 조회 (외부 POS DB 미접촉).
  const accountsQuery = useQuery({
    queryKey: ['posSalesDashboard', 'accounts', query],
    queryFn: () => {
      const q = query!;
      return fetchPosSalesAccounts({
        costCenterCodes: q.codes,
        customerKeyword: q.customerKeyword,
        distributionChannels: q.distributionChannels,
        accountTypes: q.accountTypes,
      });
    },
    enabled: open && query != null,
    placeholderData: (prev) => prev,
  });
  const accounts = accountsQuery.data;

  const handleSearch = () => {
    if (selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    setQuery({
      codes: selectedCodes,
      customerKeyword: customerKeyword.trim() || undefined,
      distributionChannels,
      accountTypes,
    });
  };

  const selectedIds = useMemo(() => [...selectedMap.keys()], [selectedMap]);

  // 거래처 행 선택 토글 — 선택 해제는 항상 허용, 신규 선택은 상한(20) 가드.
  const toggleAccount = (record: PosSalesAccountItem) => {
    setSelectedMap((prev) => {
      const next = new Map(prev);
      if (next.has(record.accountId)) {
        next.delete(record.accountId);
        return next;
      }
      if (next.size >= MAX_SELECTABLE_ACCOUNTS) {
        message.warning(`거래처는 최대 ${MAX_SELECTABLE_ACCOUNTS}개까지 선택할 수 있습니다.`);
        return prev;
      }
      next.set(record.accountId, {
        accountId: record.accountId,
        accountName: record.accountName,
      });
      return next;
    });
  };

  const rowSelection: TableRowSelection<PosSalesAccountItem> = {
    // 최대 20개 상한이라 "전체 선택"은 무의미 → 숨김.
    hideSelectAll: true,
    selectedRowKeys: selectedIds,
    onSelect: (record) => toggleAccount(record),
    getCheckboxProps: (record) => ({
      // 상한 도달 시 미선택 행은 체크 비활성 (이미 선택된 행은 해제 가능하도록 유지)
      disabled: selectedMap.size >= MAX_SELECTABLE_ACCOUNTS && !selectedMap.has(record.accountId),
    }),
  };

  const columns: ColumnsType<PosSalesAccountItem> = useMemo(
    () => [
      { title: '거래처', dataIndex: 'accountName', width: 200, render: (v) => v ?? '-' },
      { title: 'SAP코드', dataIndex: 'sapAccountCode', width: 120, render: (v) => v ?? '-' },
      { title: '유통형태', dataIndex: 'distributionChannel', width: 140, render: (v) => v ?? '-' },
      { title: '거래처유형', dataIndex: 'accountType', width: 140, render: (v) => v ?? '-' },
      { title: '지점', dataIndex: 'branchName', width: 140, render: (v) => v ?? '-' },
    ],
    [],
  );

  const handleConfirm = () => {
    onConfirm([...selectedMap.values()]);
    onClose();
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title="거래처 선택"
      width={900}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose}>
          취소
        </Button>,
        <Button
          key="confirm"
          type="primary"
          onClick={handleConfirm}
          disabled={selectedMap.size === 0}
        >
          선택 완료 ({selectedMap.size})
        </Button>,
      ]}
    >
      {/* ── 거래처 검색 조건 ── */}
      <Space wrap align="end" style={{ marginBottom: 12 }}>
        <div>
          <span>지점명:</span>
          <div style={{ marginTop: 4 }}>
            {singleBranch ? (
              <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
                지점: {branches[0].branchName}
              </Tag>
            ) : (
              <Select
                mode="multiple"
                value={selectedCodes}
                onChange={(values) => setSelectedCodes(values as string[])}
                options={branchOptions}
                placeholder="지점 선택"
                style={{ width: 260 }}
                maxTagCount="responsive"
                allowClear
                showSearch
                optionFilterProp="label"
                popupRender={(menu) => (
                  <>
                    <div style={{ padding: '4px 12px', borderBottom: '1px solid #f0f0f0' }}>
                      <Checkbox
                        checked={allBranchSelected}
                        indeterminate={someBranchSelected}
                        onChange={handleToggleAllBranches}
                      >
                        전체 ({selectedCodes.length}/{allCodes.length})
                      </Checkbox>
                    </div>
                    {menu}
                  </>
                )}
                notFoundContent="항목 없음"
              />
            )}
          </div>
        </div>
        <div>
          <span>유통형태:</span>
          <div style={{ marginTop: 4 }}>
            <Select
              mode="multiple"
              value={distributionChannels}
              onChange={handleDistributionChange}
              options={distributionChannelOptions}
              placeholder="전체"
              style={{ width: 200 }}
              maxTagCount="responsive"
              allowClear
              showSearch
              optionFilterProp="label"
              loading={filterOptionsQuery.isLoading}
              notFoundContent="항목 없음"
            />
          </div>
        </div>
        <div>
          <span>거래처유형:</span>
          <div style={{ marginTop: 4 }}>
            <Select
              mode="multiple"
              value={accountTypes}
              onChange={setAccountTypes}
              options={accountTypeOptions}
              placeholder="전체"
              style={{ width: 200 }}
              maxTagCount="responsive"
              allowClear
              showSearch
              optionFilterProp="label"
              loading={filterOptionsQuery.isLoading}
              notFoundContent="항목 없음"
            />
          </div>
        </div>
        <div>
          <span>거래처 검색:</span>
          <div style={{ marginTop: 4 }}>
            <Input
              placeholder="거래처명 부분 일치"
              value={customerKeyword}
              onChange={(e) => setCustomerKeyword(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 180 }}
              allowClear
            />
          </div>
        </div>
        <Button
          type="primary"
          icon={<SearchOutlined />}
          onClick={handleSearch}
          disabled={selectedCodes.length === 0}
          loading={accountsQuery.isFetching}
        >
          거래처 검색
        </Button>
      </Space>

      {accountsQuery.isError && (
        <Alert
          type="error"
          message={(accountsQuery.error as Error)?.message ?? '거래처 조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {/* ── 검색 결과 목록 (체크박스 선택) ── */}
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">
          {accounts?.totalElements ?? 0}건 · 선택 {selectedMap.size}/{MAX_SELECTABLE_ACCOUNTS}
        </Text>
      </div>
      <ResizableTable
        rowKey={(r) => r.accountId}
        size="small"
        columns={columns}
        dataSource={accounts?.items ?? []}
        loading={accountsQuery.isLoading}
        rowSelection={rowSelection}
        scroll={{ y: 320 }}
        pagination={false}
        // 행 아무 곳이나 클릭해도 선택/해제 (작은 체크박스만 조준할 필요 없음)
        onRow={(record) => ({
          onClick: () => toggleAccount(record),
          style: { cursor: 'pointer' },
        })}
        locale={listTableLocale({ searched: query != null })}
      />
    </Modal>
  );
}
