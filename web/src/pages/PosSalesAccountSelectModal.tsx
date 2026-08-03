import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Input,
  Modal,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { InfoCircleOutlined, SearchOutlined } from '@ant-design/icons';
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
 *
 * ## 지점 선행 강제 완화
 * 다중지점 사용자도 지점을 먼저 고를 필요 없이 **거래처명만으로 검색**할 수 있다. 운영자가 실제로
 * 아는 것은 거래처명이지 소속 지점이 아니기 때문이다. 거래처를 고르면 서버가 역산해 내려준
 * `selectorBranchCode` 로 지점 체크박스를 **합집합 자동 선택**한다 (지점 다중 선택 UI 규칙).
 * 역산 불가(AMBIGUOUS / OUT_OF_SCOPE) 건은 자동 선택 대신 안내만 한다.
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
  // 선택 거래처 → 자동 반영한 지점 코드. 거래처 해제 시 "아무도 안 쓰는 지점만" 되돌리기 위한 역색인.
  const [autoBranchByAccount, setAutoBranchByAccount] = useState<Map<number, string>>(new Map());

  // 모달이 열릴 때마다 부모의 현재 선택으로 초기화하고, 검색 조건/결과는 리셋한다.
  useEffect(() => {
    if (!open) return;
    setSelectedMap(new Map(initialSelected.map((a) => [a.accountId, a])));
    setAutoBranchByAccount(new Map());
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
    // 값은 거래처유형마스터 코드, 표시는 "{코드} {이름}".
    () => (filterOptions?.distributionChannels ?? []).map((o) => ({ value: o.code, label: o.label })),
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

  // 지점을 먼저 고르지 않아도 거래처명만으로 검색할 수 있다 (지점 선행 강제 완화).
  // 둘 다 비면 권한 범위 전건 스캔이 되므로 서버(400)와 같은 기준으로 화면에서 먼저 막는다.
  const handleSearch = () => {
    if (selectedCodes.length === 0 && !customerKeyword.trim()) {
      message.warning('지점 또는 거래처명 중 하나는 입력해주세요.');
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

  /**
   * 거래처 선택 시 그 거래처의 지점을 셀렉터에 자동 반영 (다중 지점 UI 규칙 — 합집합).
   *
   * 역산이 불가한 경우(롤업 중복 AMBIGUOUS / 권한 밖 OUT_OF_SCOPE)는 엉뚱한 지점을 고르지 않도록
   * 자동 선택을 생략하고 안내만 한다. 지점 자동 선택은 거래처 재검색 시의 필터로만 작용하고
   * 최종 POS 조회는 선택 거래처 id 기준이라, 생략돼도 조회 자체는 막히지 않는다.
   */
  const applyBranchOfAccount = (record: PosSalesAccountItem) => {
    if (record.selectorBranchStatus !== 'RESOLVED' || !record.selectorBranchCode) {
      message.warning(
        record.selectorBranchStatus === 'AMBIGUOUS'
          ? `${record.accountName ?? '거래처'}의 지점을 자동으로 특정할 수 없습니다. 지점을 직접 선택해주세요.`
          : `${record.accountName ?? '거래처'}의 지점이 조회 권한 범위 밖입니다.`,
      );
      return;
    }
    const code = record.selectorBranchCode;
    setAutoBranchByAccount((prev) => new Map(prev).set(record.accountId, code));
    setSelectedCodes((prev) => (prev.includes(code) ? prev : [...prev, code]));
  };

  /** 거래처 해제 시, 남은 선택 거래처 중 아무도 쓰지 않게 된 자동 선택 지점만 되돌린다. */
  const releaseBranchOfAccount = (accountId: number) => {
    const code = autoBranchByAccount.get(accountId);
    if (!code) return;
    const next = new Map(autoBranchByAccount);
    next.delete(accountId);
    setAutoBranchByAccount(next);
    if (![...next.values()].includes(code)) {
      setSelectedCodes((codes) => codes.filter((c) => c !== code));
    }
  };

  // 거래처 행 선택 토글 — 선택 해제는 항상 허용, 신규 선택은 상한(20) 가드.
  const toggleAccount = (record: PosSalesAccountItem) => {
    if (selectedMap.has(record.accountId)) {
      setSelectedMap((prev) => {
        const next = new Map(prev);
        next.delete(record.accountId);
        return next;
      });
      releaseBranchOfAccount(record.accountId);
      return;
    }
    if (selectedMap.size >= MAX_SELECTABLE_ACCOUNTS) {
      message.warning(`거래처는 최대 ${MAX_SELECTABLE_ACCOUNTS}개까지 선택할 수 있습니다.`);
      return;
    }
    setSelectedMap((prev) =>
      new Map(prev).set(record.accountId, {
        accountId: record.accountId,
        accountName: record.accountName,
      }),
    );
    applyBranchOfAccount(record);
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
            {/* 거래처를 먼저 골라 지점이 역으로 채워졌음을 명시 — 사용자가 체크 이유를 알 수 있게. */}
            {!singleBranch && autoBranchByAccount.size > 0 && (
              <Text type="secondary" style={{ display: 'block', marginTop: 4, fontSize: 12 }}>
                선택 거래처의 지점이 자동 반영되었습니다.
              </Text>
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
        <Space size={4}>
          <Text type="secondary">
            {accounts?.totalElements ?? 0}건 · 선택 {selectedMap.size}/{MAX_SELECTABLE_ACCOUNTS}
          </Text>
          <Tooltip
            title={`한 번에 조회할 수 있는 거래처는 최대 ${MAX_SELECTABLE_ACCOUNTS}개입니다. 과도한 동시 조회로 인한 시스템 부하·장애를 방지하기 위한 제한입니다.`}
          >
            <InfoCircleOutlined style={{ color: 'rgba(0, 0, 0, 0.45)', cursor: 'help' }} />
          </Tooltip>
        </Space>
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
