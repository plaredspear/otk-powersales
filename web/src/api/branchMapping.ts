import client from './client';
import type { ApiResponse } from './types';

/**
 * 지점 코드 맵핑 조회 API — `시스템 > 지점 코드 맵핑` 화면 전용.
 *
 * backend `BranchCodeExpander` 가 지점 스코프 조회에서 코드를 어떻게 확장하는지 보여주는 진단 화면.
 * 확장 코드마다 현행 조직명이 붙어 있어, 조직명이 없는 코드(= 폐기된 이력 코드)와
 * 현행 타 조직을 끌어오는 롤업을 구분할 수 있다.
 */

/** 매핑 성격 — backend `BranchMappingType` enum 과 1:1. */
export type BranchMappingType = 'NONE' | 'ROLLUP' | 'DUAL_CODE' | 'LEGACY';

export interface BranchMappingExpandedCode {
  code: string;
  /** null 이면 현행 조직에 없는 코드 = 조직 개편으로 폐기된 이력 코드. */
  orgName: string | null;
  /** 매핑 행의 지점코드 자기 자신인지 여부. */
  isSelf: boolean;
}

export interface BranchMappingListItem {
  branchCode: string;
  label: string | null;
  orgName: string | null;
  type: BranchMappingType;
  /** 사람이 읽는 유형명 — backend 가 함께 내려준다. */
  typeLabel: string;
  expandedCodes: BranchMappingExpandedCode[];
  expandedCount: number;
  /** 확장 결과 중 현행 조직에서 해석되지 않은 코드 수. */
  unresolvedCount: number;
  /** `branch_mapping.included_branch_codes` 원본 CSV (공백/중복 포함). */
  rawIncludedBranchCodes: string;
}

export interface BranchMappingListResponse {
  content: BranchMappingListItem[];
  /** 유형별 건수. key 는 BranchMappingType. */
  typeCounts: Record<string, number>;
  /** 런타임 캐시가 비어 있는지 (Stage1 적재 후 reload 미실행 = stale 신호). */
  cacheEmpty: boolean;
}

export interface FetchBranchMappingsParams {
  /** 지점코드 / 라벨 / 조직명 / 확장 코드를 동시에 대상으로 하는 부분 일치 검색어. */
  keyword?: string;
}

export async function fetchBranchMappings(
  params: FetchBranchMappingsParams,
): Promise<BranchMappingListResponse> {
  const res = await client.get<ApiResponse<BranchMappingListResponse>>(
    '/api/v1/admin/branch-mappings',
    { params },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 코드 맵핑 조회에 실패했습니다');
  }
  return res.data.data;
}
