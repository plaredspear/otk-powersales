import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { notification } from 'antd';
import AppLoginActiveModal from './AppLoginActiveModal';
import { updateEmployeeAppLoginActive } from '@/api/employee';
import type { EmployeeDetail } from '@/api/employee';

vi.mock('@/api/employee', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/employee')>();
  return { ...actual, updateEmployeeAppLoginActive: vi.fn() };
});

const mockedUpdate = vi.mocked(updateEmployeeAppLoginActive);

const baseEmployee = {
  id: 100,
  employeeCode: '100100',
  name: 'SAP사원',
  origin: 'SAP',
  status: '재직',
  role: '여사원',
  jobCode: '판촉직',
  appLoginActive: false,
  lockingFlag: false,
} as unknown as EmployeeDetail;

function renderModal(overrides: Partial<EmployeeDetail> = {}) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const onClose = vi.fn();
  const employee = { ...baseEmployee, ...overrides } as EmployeeDetail;
  const utils = render(
    <QueryClientProvider client={client}>
      <AppLoginActiveModal employee={employee} open onClose={onClose} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose, employee };
}

describe('AppLoginActiveModal', () => {
  beforeEach(() => {
    mockedUpdate.mockReset();
  });

  it('비활성 사원 -> 활성화 확인 모달 + 사번/이름/현재 상태 표시', () => {
    const { baseElement } = renderModal();
    expect(screen.getByText('앱 로그인 활성화')).toBeInTheDocument();
    // 사번/이름/상태는 한 문단 안에 라벨과 섞여 렌더된다 (텍스트 노드 분리)
    expect(baseElement.textContent).toContain('100100');
    expect(baseElement.textContent).toContain('SAP사원');
    expect(baseElement.textContent).toContain('현재 상태: 비활성');
  });

  it('활성화 성공 -> 성공 알림 + 모달 닫힘 (잠금 플래그는 전송하지 않는다)', async () => {
    mockedUpdate.mockResolvedValue({ ...baseEmployee, appLoginActive: true } as EmployeeDetail);
    const successSpy = vi.spyOn(notification, 'success');
    const { onClose } = renderModal();

    await userEvent.click(screen.getByRole('button', { name: '확인' }));

    await waitFor(() => expect(mockedUpdate).toHaveBeenCalledWith(100, true));
    expect(successSpy).toHaveBeenCalledWith(
      expect.objectContaining({ message: '앱 로그인이 활성화되었습니다' }),
    );
    expect(onClose).toHaveBeenCalled();
    successSpy.mockRestore();
  });

  it('잠긴 사원 활성화 -> 사전 경고 배너 노출', () => {
    renderModal({ lockingFlag: true });
    expect(screen.getByText('시스템 접근 잠금 상태입니다')).toBeInTheDocument();
  });

  it('잠긴 사원 활성화 결과가 되돌려지면 -> 사유 경고 알림', async () => {
    // 서버 정책(EmployeeTriggerHandler.cls:40 동등)이 요청을 되돌린 응답
    mockedUpdate.mockResolvedValue({
      ...baseEmployee,
      lockingFlag: true,
      appLoginActive: false,
    } as EmployeeDetail);
    const warnSpy = vi.spyOn(notification, 'warning');
    renderModal({ lockingFlag: true });

    await userEvent.click(screen.getByRole('button', { name: '확인' }));

    await waitFor(() =>
      expect(warnSpy).toHaveBeenCalledWith(
        expect.objectContaining({ message: '앱 로그인을 활성화할 수 없습니다' }),
      ),
    );
    warnSpy.mockRestore();
  });

  it('활성 사원 -> 비활성화 모달로 열리고 현장 여사원 보호 시 유지 경고', async () => {
    mockedUpdate.mockResolvedValue({
      ...baseEmployee,
      appLoginActive: true,
    } as EmployeeDetail);
    const warnSpy = vi.spyOn(notification, 'warning');
    renderModal({ appLoginActive: true });

    expect(screen.getByText('앱 로그인 비활성화')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '확인' }));

    await waitFor(() => expect(mockedUpdate).toHaveBeenCalledWith(100, false));
    expect(warnSpy).toHaveBeenCalledWith(
      expect.objectContaining({ message: '앱 로그인 활성 상태가 유지되었습니다' }),
    );
    warnSpy.mockRestore();
  });
});
