import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PPTMasterPage from './PPTMasterPage';
import type { PPTMaster } from '@/api/pptMaster';
import { useAuthStore } from '@/stores/authStore';

const sampleMaster: PPTMaster = {
  id: 1,
  name: 'PM0010001',
  branchName: '서울지점',
  employeeId: 100,
  employeeName: '백은경',
  employeeCode: 'EMP005',
  employeeStatus: '재직',
  employeeAppLoginActive: true,
  employeeEndDate: null,
  accountId: 200,
  accountCode: 'ACC001',
  accountName: '롯데마트',
  accountType: '대형마트',
  teamType: '라면세일조',
  startDate: '2026-01-01',
  endDate: null,
  isConfirmed: false,
} as PPTMaster;

vi.mock('@/hooks/promotion/usePPTMasters', () => ({
  usePPTMasters: () => ({
    data: { content: [sampleMaster], totalElements: 1, totalPages: 1, number: 0, size: 20 },
    isLoading: false,
    refetch: vi.fn(),
    isFetching: false,
  }),
  useCreatePPTMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdatePPTMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeletePPTMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useConfirmPPTMastersByIds: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

vi.mock('@/hooks/common/useExcelDownload', () => ({
  useExcelDownload: () => ({ run: vi.fn(), downloading: false }),
}));

function setPermissions(permissions: string[], profileName: string | null = '5.영업사원') {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      profileName,
      isSalesSupport: false,
      costCenterCode: null,
      permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PPTMasterPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('PPTMasterPage 권한 게이팅', () => {
  beforeEach(() => {
    // READ 만 보유 (페이지 진입자 기본). 쓰기 권한(C/E/D) 전부 미보유.
    setPermissions(['professional_promotion_team_master:R']);
  });

  describe('쓰기 권한 미보유 (READ only)', () => {
    it('마스터 등록 버튼은 숨겨진다 (CREATE 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /마스터 등록/ })).not.toBeInTheDocument();
    });

    it('엑셀 업로드 버튼은 숨겨진다 (CREATE 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /엑셀 업로드/ })).not.toBeInTheDocument();
    });

    it('엑셀 템플릿 다운로드 버튼은 숨겨진다 (CREATE 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /엑셀 템플릿 다운로드/ })).not.toBeInTheDocument();
    });

    it('행 액션(수정/복제/삭제) 버튼은 숨겨진다', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '복제' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
    });

    it('선택 일괄 확정 버튼은 숨겨진다', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /선택 일괄 확정/ })).not.toBeInTheDocument();
    });

    it('엑셀 다운로드(READ) 버튼은 노출된다', () => {
      renderPage();
      // READ only 에서는 '엑셀 템플릿 다운로드'가 숨겨지므로 /엑셀 다운로드/ 는 '엑셀 다운로드'만 매칭.
      expect(screen.getByRole('button', { name: /엑셀 다운로드/ })).toBeEnabled();
    });
  });

  describe('CREATE 권한만 보유', () => {
    beforeEach(() => {
      setPermissions(['professional_promotion_team_master:R', 'professional_promotion_team_master:C']);
    });

    it('마스터 등록 버튼이 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /마스터 등록/ })).toBeInTheDocument();
    });

    it('복제 버튼만 노출되고 수정/삭제는 숨겨진다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: '복제' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
    });

    it('엑셀 업로드 + 엑셀 템플릿 다운로드 버튼이 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /엑셀 업로드/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /엑셀 템플릿 다운로드/ })).toBeInTheDocument();
    });
  });

  describe('EDIT 권한만 보유', () => {
    beforeEach(() => {
      setPermissions(['professional_promotion_team_master:R', 'professional_promotion_team_master:E']);
    });

    it('마스터 등록 버튼은 숨겨진다 (CREATE 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /마스터 등록/ })).not.toBeInTheDocument();
    });

    it('수정 버튼만 노출되고 복제/삭제는 숨겨진다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '복제' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
    });

    it('엑셀 업로드 + 엑셀 템플릿 다운로드 버튼은 숨겨진다 (CREATE 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /엑셀 업로드/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /엑셀 템플릿 다운로드/ })).not.toBeInTheDocument();
    });

    it('선택 일괄 확정 버튼이 노출된다 (EDIT)', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /선택 일괄 확정/ })).toBeInTheDocument();
    });
  });

  describe('DELETE 권한만 보유', () => {
    beforeEach(() => {
      setPermissions(['professional_promotion_team_master:R', 'professional_promotion_team_master:D']);
    });

    it('삭제 버튼만 노출되고 수정/복제는 숨겨진다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '복제' })).not.toBeInTheDocument();
    });
  });

  describe('CREATE+EDIT+DELETE 모두 보유', () => {
    beforeEach(() => {
      setPermissions([
        'professional_promotion_team_master:R',
        'professional_promotion_team_master:C',
        'professional_promotion_team_master:E',
        'professional_promotion_team_master:D',
      ]);
    });

    it('마스터 등록 + 행 액션(수정/복제/삭제) 모두 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /마스터 등록/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '복제' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument();
    });
  });

  describe('시스템 관리자', () => {
    it('권한 set 이 비어도 등록/수정/복제/삭제 버튼이 모두 노출된다', () => {
      setPermissions([], '시스템 관리자');
      renderPage();
      expect(screen.getByRole('button', { name: /마스터 등록/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '복제' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument();
    });
  });

  // 전문행사조 사원은 여사원이므로 사원명 링크는 진입자 권한에 따라 상세 URL 이 갈린다.
  // female_employee 권한 보유자(조장 등)는 여사원 상세로, employee 권한만 있는 관리자는 사원 상세로.
  describe('사원명 상세 링크 분기', () => {
    it('female_employee READ 보유 시 여사원 상세(/female-employee/:id) 로 링크된다', () => {
      setPermissions(['professional_promotion_team_master:R', 'female_employee:R']);
      renderPage();
      const link = screen.getByRole('link', { name: '백은경' });
      expect(link).toHaveAttribute('href', '/female-employee/100');
    });

    it('female_employee 미보유 + employee READ 시 사원 상세(/employee/:id) 로 링크된다', () => {
      setPermissions(['professional_promotion_team_master:R', 'employee:R']);
      renderPage();
      const link = screen.getByRole('link', { name: '백은경' });
      expect(link).toHaveAttribute('href', '/employee/100');
    });
  });
});
