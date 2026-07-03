import client from './client';
import type { ApiResponse } from './types';

// ─────────────────────────────────────────────────────
// 타입 정의 — 현장점검(등록) 테마 (SF Theme__c)
// ─────────────────────────────────────────────────────

export interface ThemeListParams {
  keyword?: string;
  /** 부서 부분일치 필터 */
  department?: string;
  /** 지점코드 정확일치 필터 */
  branchCode?: string;
  page?: number;
  size?: number;
}

export interface ThemeListItem {
  id: number;
  name: string | null;
  title: string | null;
  department: string | null;
  branchCode: string | null;
  startDate: string | null;
  endDate: string | null;
  ownerUserId: number | null;
  ownerName: string | null;
  siteActivityCount: number;
  createdAt: string | null;
}

export interface ThemeListData {
  content: ThemeListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ThemeSiteActivityItem {
  id: number;
  name: string | null;
  branchName: string | null;
  accountName: string | null;
  productName: string | null;
  orgName: string | null;
  employeeName: string | null;
  category: string | null;
  activityDate: string | null;
}

export interface ThemeDetail {
  id: number;
  name: string | null;
  title: string | null;
  department: string | null;
  branchCode: string | null;
  startDate: string | null;
  endDate: string | null;
  ownerUserId: number | null;
  ownerName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  siteActivities: ThemeSiteActivityItem[];
}

export interface ThemeMutationRequest {
  title: string;
  startDate: string | null;
  endDate: string | null;
  /** 소유권 이전 — 수정 시에만 사용. 미지정(null/undefined)이면 소유자 미변경. */
  ownerUserId?: number | null;
}

export interface ThemeMutationResult {
  id: number;
  name: string | null;
}

/** 지점 셀렉터 옵션 — 현재 사용자 권한별 조회 허용 지점 화이트리스트. */
export interface ThemeBranch {
  branchCode: string;
  branchName: string;
}

/** 소유자 변경 Select 후보 — 활성 User. */
export interface ThemeOwnerCandidate {
  id: number;
  username: string;
  employeeCode: string | null;
  name: string | null;
}

// ─────────────────────────────────────────────────────
// API 호출 함수
// ─────────────────────────────────────────────────────

const BASE = '/api/v1/admin/inspection-themes';

export async function fetchThemes(params: ThemeListParams): Promise<ThemeListData> {
  const res = await client.get<ApiResponse<ThemeListData>>(BASE, { params });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 현장점검 테마 관리 화면 지점 셀렉터 옵션 조회.
 *
 * 여사원 현황/여사원 일정과 동일한 권한별 지점 화이트리스트를 반환한다. 화면 게이팅과 동일한
 * inspection_theme READ 로 가드된 전용 endpoint 라, 전문행사조 권한 없는 조장도 목록을 받는다.
 */
export async function fetchThemeBranches(): Promise<ThemeBranch[]> {
  const res = await client.get<ApiResponse<ThemeBranch[]>>(`${BASE}/branches`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return res.data.data;
}

/**
 * 테마 소유자 변경 Select 후보 조회 (활성 User).
 *
 * 화면 게이팅과 동일한 inspection_theme READ 로 가드된 전용 endpoint. `user` READ 권한 없는
 * 테마 EDIT 사용자(여사원 대행 등)도 소유자 후보를 받을 수 있다(SF 레거시 정합 — 소유자 변경은 테마 Edit 권한 기능).
 */
export async function fetchThemeOwnerCandidates(keyword?: string): Promise<ThemeOwnerCandidate[]> {
  const res = await client.get<ApiResponse<{ content: ThemeOwnerCandidate[] }>>(`${BASE}/owner-candidates`, {
    params: { keyword: keyword || undefined, size: 20 },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '소유자 후보 조회에 실패했습니다');
  }
  return res.data.data.content;
}

export async function fetchThemeDetail(id: number): Promise<ThemeDetail> {
  const res = await client.get<ApiResponse<ThemeDetail>>(`${BASE}/${id}`);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 상세 조회에 실패했습니다');
  }
  return res.data.data;
}

export async function createTheme(request: ThemeMutationRequest): Promise<ThemeMutationResult> {
  const res = await client.post<ApiResponse<ThemeMutationResult>>(BASE, request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 등록에 실패했습니다');
  }
  return res.data.data;
}

export async function updateTheme(id: number, request: ThemeMutationRequest): Promise<ThemeMutationResult> {
  const res = await client.put<ApiResponse<ThemeMutationResult>>(`${BASE}/${id}`, request);
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '테마 수정에 실패했습니다');
  }
  return res.data.data;
}

export async function deleteTheme(id: number): Promise<void> {
  const res = await client.delete<ApiResponse<unknown>>(`${BASE}/${id}`);
  if (!res.data.success) {
    throw new Error(res.data.message || '테마 삭제에 실패했습니다');
  }
}

/** 테마 단위 하위 현장점검 결과 엑셀 다운로드 — Blob 응답. */
export async function downloadThemeExcel(id: number, fallbackName: string): Promise<void> {
  const res = await client.get(`${BASE}/${id}/export`, { responseType: 'blob' });
  const disposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${fallbackName}.xlsx`;
  if (disposition) {
    const match = disposition.match(/filename\*=UTF-8''(.+)$/);
    if (match) {
      filename = decodeURIComponent(match[1]);
    }
  }
  const url = window.URL.createObjectURL(new Blob([res.data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
