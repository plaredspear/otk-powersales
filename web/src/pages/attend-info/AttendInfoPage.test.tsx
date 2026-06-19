import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AttendInfoPage from './AttendInfoPage';
import * as api from '@/api/attendInfo';
import { useAuthStore } from '@/stores/authStore';
import { entityPermissionKey } from '@/hooks/usePermission';

vi.mock('@/api/attendInfo', async () => {
  const actual = await vi.importActual<typeof import('@/api/attendInfo')>('@/api/attendInfo');
  return {
    ...actual,
    searchAttendInfo: vi.fn(),
  };
});

// 월별 근무내역 탭(기본 활성)의 여사원 목록 — 실제 네트워크 호출 방지용 stub.
vi.mock('@/api/employee', async () => {
  const actual = await vi.importActual<typeof import('@/api/employee')>('@/api/employee');
  const emptyList = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  };
  return {
    ...actual,
    fetchEmployees: vi.fn().mockResolvedValue(emptyList),
    fetchFemaleEmployees: vi.fn().mockResolvedValue(emptyList),
  };
});

const mockedSearch = vi.mocked(api.searchAttendInfo);

/** 기본 활성 탭은 '월별 근무내역' 이므로, HR 목록 검증 전 'HR 적재 근무기간' 탭으로 전환. */
function switchToHrTab() {
  fireEvent.click(screen.getByRole('tab', { name: 'HR 적재 근무기간' }));
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AttendInfoPage />
    </QueryClientProvider>,
  );
}

describe('AttendInfoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
      user: {
        id: 1,
        employeeCode: 'admin',
        username: 'admin',
        name: 'admin',
        orgName: null,
        role: null,
        costCenterCode: null,
        permissions: [
          entityPermissionKey('attend_info', 'READ'),
          entityPermissionKey('attend_info', 'EDIT'),
          entityPermissionKey('attend_info', 'DELETE'),
        ],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
  });

  it('초기 진입 시 타이틀과 필터 영역이 렌더된다', async () => {
    mockedSearch.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
    });
    renderPage();
    expect(screen.getByText('근무기간 조회')).toBeInTheDocument();
    switchToHrTab();
    expect(
      screen.getByText(/SAP HR 인바운드 적재 근무기간 데이터/),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(mockedSearch).toHaveBeenCalled();
    });
  });

  it('write 권한 보유 시 [신규 등록] 버튼 노출', async () => {
    mockedSearch.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
    });
    renderPage();
    switchToHrTab();
    expect(screen.getByRole('button', { name: /신규 등록/ })).toBeInTheDocument();
  });

  it('write 권한 부재 시 [신규 등록] 버튼 미노출', async () => {
    useAuthStore.setState({
      user: {
        id: 1,
        employeeCode: 'reader',
        username: 'reader',
        name: 'reader',
        orgName: null,
        role: null,
        costCenterCode: null,
        permissions: [entityPermissionKey('attend_info', 'READ')],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
    mockedSearch.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
    });
    renderPage();
    switchToHrTab();
    expect(screen.queryByRole('button', { name: /신규 등록/ })).not.toBeInTheDocument();
  });

  it('목록 행이 있을 때 사원명·근태유형 정보가 표시된다', async () => {
    mockedSearch.mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'AI0001',
          employeeCode: '20120253',
          employeeName: '홍길동',
          employeeJobCode: '판촉직',
          attendType: '14',
          attendTypeName: '연차',
          startDate: '20260518',
          endDate: '20260522',
          status: 'N',
          createdAt: '2026-05-18T10:00:00',
          createdByName: null,
        },
      ],
      totalElements: 1,
      totalPages: 1,
    });
    renderPage();
    switchToHrTab();
    await waitFor(() => {
      expect(screen.getByText('AI0001')).toBeInTheDocument();
      expect(screen.getByText('20120253')).toBeInTheDocument();
      expect(screen.getByText('홍길동')).toBeInTheDocument();
      expect(screen.getByText('판촉직')).toBeInTheDocument();
    });
  });
});
