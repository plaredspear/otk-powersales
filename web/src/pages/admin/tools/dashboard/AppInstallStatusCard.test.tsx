import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AppInstallStatusCard from './AppInstallStatusCard';
import { fetchUninstalledFemaleStaffSummary } from '@/api/admin/appInstallStatus';
import { downloadExcel } from '@/lib/excelDownload';

vi.mock('@/api/admin/appInstallStatus', async () => {
  const actual = await vi.importActual<typeof import('@/api/admin/appInstallStatus')>(
    '@/api/admin/appInstallStatus',
  );
  return { ...actual, fetchUninstalledFemaleStaffSummary: vi.fn() };
});

vi.mock('@/lib/excelDownload', async () => {
  const actual = await vi.importActual<typeof import('@/lib/excelDownload')>('@/lib/excelDownload');
  return { ...actual, downloadExcel: vi.fn() };
});

const mockedFetch = vi.mocked(fetchUninstalledFemaleStaffSummary);
const mockedDownload = vi.mocked(downloadExcel);

function renderCard() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AppInstallStatusCard />
    </QueryClientProvider>,
  );
}

describe('AppInstallStatusCard', () => {
  beforeEach(() => {
    mockedFetch.mockReset();
    mockedDownload.mockReset();
  });

  it('미설치 인원과 집계 모수를 표시한다', async () => {
    mockedFetch.mockResolvedValue({ uninstalledCount: 7, targetCount: 120 });
    renderCard();

    expect(await screen.findByText('7')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
  });

  it('조회 기준(판정 / 모수 / 해석 주의)을 화면에 명시한다', async () => {
    mockedFetch.mockResolvedValue({ uninstalledCount: 7, targetCount: 120 });
    renderCard();
    await screen.findByText('7');

    expect(screen.getByText('조회 기준')).toBeInTheDocument();
    expect(screen.getByText('미설치 판정')).toBeInTheDocument();
    expect(screen.getByText('해석 시 주의')).toBeInTheDocument();
    // '집계 모수' 는 기준 표의 라벨 + 수치 타이틀 두 곳에 나온다.
    expect(screen.getAllByText('집계 모수').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/푸시 토큰\(FCM\)도 없는 사원/)).toBeInTheDocument();
  });

  it('명단 다운로드 클릭 시 엑셀 export 를 호출한다', async () => {
    mockedFetch.mockResolvedValue({ uninstalledCount: 7, targetCount: 120 });
    renderCard();
    await screen.findByText('7');

    await userEvent.click(screen.getByRole('button', { name: /명단 다운로드/ }));

    await waitFor(() =>
      expect(mockedDownload).toHaveBeenCalledWith(
        '/api/v1/admin/tools/app-install/uninstalled-female-staff/export',
        '앱미설치여사원.xlsx',
        {},
      ),
    );
  });

  it('미설치 0명이면 다운로드하지 않고 안내만 노출한다', async () => {
    mockedFetch.mockResolvedValue({ uninstalledCount: 0, targetCount: 120 });
    renderCard();
    await screen.findByText('120');

    await userEvent.click(screen.getByRole('button', { name: /명단 다운로드/ }));

    expect(mockedDownload).not.toHaveBeenCalled();
  });

  it('조회 실패 시 권한 안내를 노출하고 다운로드 버튼을 막는다', async () => {
    mockedFetch.mockRejectedValue(new Error('403'));
    renderCard();

    expect(await screen.findByText(/시스템 관리자 권한이 필요합니다/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /명단 다운로드/ })).toBeDisabled();
  });
});
