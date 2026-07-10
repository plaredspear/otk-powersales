import client from './client';
import { downloadExcel } from '@/lib/excelDownload';
import type { ApiResponse } from './types';

/**
 * 환산인원 현황 보고서 variant — SF Report 변형 (Spec #847 — 거래처유형 5종 + 대리점/대형마트 5종).
 * 백엔드 ConvertedHeadcountReportVariant enum 값과 1:1.
 */
export type ConvertedHeadcountReportVariant =
  | 'PERMANENT_TEMP_ALL' // 1-1 상시,임시 전체
  | 'PERMANENT_ONLY_EXCL_CONSIGN' // 1-2 상시 (위탁농협 제외)
  | 'TEMP_ALL' // 1-4 임시 전체
  | 'TEMP_ONLY_EXCL_CONSIGN' // 1-5 임시 전체 (위탁농협 제외)
  | 'TEAM2_PERMANENT_TEMP_ALL' // (2팀)2-1 상시,임시 전체
  | 'AGENCY_PERMANENT_TEMP_ALL' // 3-1 대리점 상시,임시 전체
  | 'AGENCY_PERMANENT_ONLY' // 3-2 대리점 only 상시
  | 'AGENCY_TEMP_ONLY' // 3-3 대리점 only 임시
  | 'HYPERMARKET_PERMANENT' // 대형마트 상시
  | 'HYPERMARKET_PERMANENT_WC3' // 대형마트 상시 (근무유형3 추가)
  | 'SEGMENTED_ALL' // 세분화 거래처유형별 (ABC유형 그룹)
  | 'TEAM2_SPLIT_CHECK'; // 거래처유형별 (상시,임시, 영업지원2팀 분리) 확인용

/**
 * variant 메타 — 좌측 세로 탭 순서/그룹/라벨 + 화면·엑셀 보고서명의 단일 출처.
 * shortLabel 은 탭에 노출하는 축약 라벨(공통 prefix 는 group 으로 분리), title 은 화면 상단/엑셀 안내용 전체명.
 */
export interface ConvertedHeadcountReportVariantMeta {
  variant: ConvertedHeadcountReportVariant;
  /** 좌측 탭 그룹명 (그룹 구분 헤더). */
  group: string;
  /** 탭에 노출하는 축약 라벨. */
  shortLabel: string;
  /** 화면 상단/엑셀 안내용 보고서 전체명. */
  title: string;
}

/** 좌측 세로 탭 노출 순서 그대로. group 이 바뀌는 지점에 그룹 헤더가 그려진다. */
export const CONVERTED_HEADCOUNT_REPORT_VARIANTS: ConvertedHeadcountReportVariantMeta[] = [
  { variant: 'PERMANENT_TEMP_ALL', group: '거래처유형별', shortLabel: '상시·임시 전체', title: '거래처유형별 환산인원 (상시·임시 전체)' },
  { variant: 'PERMANENT_ONLY_EXCL_CONSIGN', group: '거래처유형별', shortLabel: '상시, 위탁농협 제외', title: '거래처유형별 환산인원 (상시, 위탁농협 제외)' },
  { variant: 'TEMP_ALL', group: '거래처유형별', shortLabel: '임시 전체', title: '거래처유형별 환산인원 (임시 전체)' },
  { variant: 'TEMP_ONLY_EXCL_CONSIGN', group: '거래처유형별', shortLabel: '임시 전체, 위탁농협 제외', title: '거래처유형별 환산인원 (임시 전체, 위탁농협 제외)' },
  { variant: 'TEAM2_PERMANENT_TEMP_ALL', group: '거래처유형별', shortLabel: '(2팀) 상시·임시 전체', title: '(2팀) 거래처유형별 환산인원 (상시·임시 전체)' },
  { variant: 'AGENCY_PERMANENT_TEMP_ALL', group: '대리점', shortLabel: '상시·임시 전체', title: '대리점 환산인원 (상시·임시 전체)' },
  { variant: 'AGENCY_PERMANENT_ONLY', group: '대리점', shortLabel: 'only 상시', title: '대리점 환산인원 (only 상시)' },
  { variant: 'AGENCY_TEMP_ONLY', group: '대리점', shortLabel: 'only 임시', title: '대리점 환산인원 (only 임시)' },
  { variant: 'HYPERMARKET_PERMANENT', group: '대형마트', shortLabel: '상시', title: '대형마트 환산인원 (상시)' },
  { variant: 'HYPERMARKET_PERMANENT_WC3', group: '대형마트', shortLabel: '상시, 근무유형3 추가', title: '대형마트 환산인원 (상시, 근무유형3 추가)' },
  { variant: 'SEGMENTED_ALL', group: '기타', shortLabel: '세분화 거래처유형별', title: '세분화 거래처유형별 환산인원' },
  { variant: 'TEAM2_SPLIT_CHECK', group: '기타', shortLabel: '거래처유형별 (2팀 분리) 확인용', title: '거래처유형별 환산인원 (상시·임시, 영업지원2팀 분리) 확인용' },
];

/** 기본 진입 variant (쿼리 파라미터 미지정 시). */
export const DEFAULT_CONVERTED_HEADCOUNT_VARIANT: ConvertedHeadcountReportVariant = 'PERMANENT_TEMP_ALL';

/** 환산인원 집계 1행 — 구분 × 근무유형1 (× 근무유형3) × 지점 × 연월 × SUM(환산인원). */
export interface ConvertedHeadcountReportRow {
  accountType: string | null;
  workingCategory1: string | null;
  workingCategory3: string | null;
  branchName: string | null;
  yearMonth: string | null;
  convertedHeadcount: number;
}

/** 구분(거래처유형) 그룹 — 소계 보유. */
export interface ConvertedHeadcountReportGroup {
  accountType: string;
  subtotalHeadcount: number;
  rows: ConvertedHeadcountReportRow[];
}

export interface ConvertedHeadcountReportResult {
  variant: string;
  year: string;
  month: string;
  /** 근무유형3 컬럼 표시 여부 (대리점 3종 + 대형마트 근무유형3 추가). */
  includeWorkingCategory3: boolean;
  /** 구분 그룹 라벨이 거래처유형(false) 인지 ABC유형(true) 인지 (세분화 variant). */
  groupByAbcType: boolean;
  groups: ConvertedHeadcountReportGroup[];
  totalHeadcount: number;
}

const BASE = '/api/v1/admin/female-employees/converted-headcount-report';

function failureMessage(label: string, res: { data: ApiResponse<unknown> }): string {
  return res.data.error?.message || res.data.message || `${label} 조회에 실패했습니다`;
}

/**
 * 지점 스코프가 적용되는(여사원 소속 지점 기준) 소속기준 variant 판별.
 *
 * 거래처기준 variant(2-1/대리점 3종)는 지점 축이 거래처 소재지라 스코프를 적용하지 않는다(전사 유지).
 * 소속기준 variant 에만 지점 셀렉터를 노출한다.
 */
export function isBranchScopedVariant(variant: ConvertedHeadcountReportVariant): boolean {
  const accountBranchVariants: ConvertedHeadcountReportVariant[] = [
    'TEAM2_PERMANENT_TEMP_ALL',
    'AGENCY_PERMANENT_TEMP_ALL',
    'AGENCY_PERMANENT_ONLY',
    'AGENCY_TEMP_ONLY',
  ];
  return !accountBranchVariants.includes(variant);
}

/** 거래처유형별 환산인원 현황 조회. branchCode 지정 시 소속기준 variant 를 그 지점으로 좁힘. */
export async function fetchConvertedHeadcountReport(
  variant: ConvertedHeadcountReportVariant,
  year: string,
  month: string,
  branchCode?: string,
): Promise<ConvertedHeadcountReportResult> {
  const res = await client.get<ApiResponse<ConvertedHeadcountReportResult>>(`${BASE}/${variant}`, {
    params: { year, month, branchCode: branchCode || undefined },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(failureMessage('거래처유형별 환산인원 현황', res));
  }
  return res.data.data;
}

/** 거래처유형별 환산인원 현황 엑셀 다운로드. branchCode 지정 시 소속기준 variant 를 그 지점으로 좁힘. */
export async function exportConvertedHeadcountReport(
  variant: ConvertedHeadcountReportVariant,
  year: string,
  month: string,
  branchCode?: string,
): Promise<void> {
  await downloadExcel(`${BASE}/${variant}/export`, `거래처유형별환산인원_${year}-${month}.xlsx`, {
    params: { year, month, branchCode: branchCode || undefined },
  });
}
