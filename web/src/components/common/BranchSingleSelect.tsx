import { useEffect } from 'react';
import { Select, Space, Tag } from 'antd';
import BranchScopeEmptyNotice from './BranchScopeEmptyNotice';

/** 지점 셀렉터 옵션 1건 — 도메인별 지점 화이트리스트 API 응답 형태. */
export interface BranchOption {
  branchCode: string;
  branchName: string;
}

interface BranchSingleSelectProps {
  /** 현재 사용자가 조회 가능한 지점 목록 (도메인별 훅이 조회한 화이트리스트). */
  branches: BranchOption[];
  /** 선택된 지점 코드 (미선택은 undefined). */
  value: string | undefined;
  /** 선택 변경 콜백. 단일지점 사용자는 자동 선택되어 콜백으로 통지된다. */
  onChange: (branchCode: string | undefined) => void;
  /** 라벨 텍스트 (기본 "지점명"). */
  label?: string;
  /**
   * 지점 목록 조회 중 여부. 로딩 중에는 목록이 비어 있어도 "조회 가능한 지점 없음" 안내를 띄우지 않는다
   * (매 진입마다 잘못된 경고가 깜빡이는 것 방지). 미지정이면 로딩이 끝난 것으로 본다.
   */
  isLoading?: boolean;
}

/**
 * 보고서 화면 공용 단일 지점 셀렉터.
 *
 * 사용자 권한별 지점 목록 길이로 UI 를 분기한다:
 * - 단일 지점(조장/지점장 등): 선택지가 없으므로 본인 지점을 자동 선택하고 고정 Tag 로 표시.
 *   backend 는 branchCode 미전송이어도 본인 지점으로 스코프를 강제하지만, 화면 표시 일관성을 위해 자동 선택.
 * - 다중 지점(전사 권한자): 단일 선택 Select. 선택 시 그 지점으로 조회를 좁히고, 미선택이면 전건.
 * 목록이 비면(권한 지점 없음) 조회가 0건이 되므로 그 사실을 [BranchScopeEmptyNotice] 로 명시한다
 * — 종전에는 아무것도 렌더하지 않아 "데이터 없음"과 구분되지 않았다.
 */
export default function BranchSingleSelect({
  branches,
  value,
  onChange,
  label = '지점명',
  isLoading = false,
}: BranchSingleSelectProps) {
  const isSingle = branches.length === 1;

  // 단일지점 사용자는 본인 지점 자동 선택. 권한 주체 변경(대행 종료 등)으로 목록이 바뀌면
  // 현재 목록에 없는 stale 선택값을 정리한다.
  useEffect(() => {
    if (branches.length === 0) return;
    if (value != null && !branches.some((b) => b.branchCode === value)) {
      onChange(undefined);
      return;
    }
    if (isSingle && value == null) {
      onChange(branches[0].branchCode);
    }
  }, [branches, isSingle, value, onChange]);

  if (branches.length === 0) {
    // 로딩 중에는 종전처럼 아무것도 렌더하지 않는다 (거짓 경고 깜빡임 방지).
    if (isLoading) return null;
    return (
      <Space direction="vertical" size={4}>
        <span>{label}:</span>
        <BranchScopeEmptyNotice />
      </Space>
    );
  }

  return (
    <Space direction="vertical" size={4}>
      <span>{label}:</span>
      {isSingle ? (
        <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
          지점: {branches[0].branchName}
        </Tag>
      ) : (
        <Select
          value={value}
          onChange={(v) => onChange(v ?? undefined)}
          options={branches.map((b) => ({ value: b.branchCode, label: b.branchName }))}
          placeholder="전체 지점"
          style={{ minWidth: 200, maxWidth: 320 }}
          allowClear
          showSearch
          optionFilterProp="label"
          filterOption={(input, option) => (option?.label ?? '').toString().includes(input)}
          notFoundContent="항목 없음"
        />
      )}
    </Space>
  );
}
