import { describe, it, expect, vi, beforeEach } from 'vitest';
import { manualRegisterEmployee, type EmployeeManualRegisterRequest } from './employee';
import client from './client';

vi.mock('./client', () => ({
  default: { post: vi.fn() },
}));

const mockedPost = vi.mocked(client.post);

const request: EmployeeManualRegisterRequest = {
  employeeCode: '100123',
  name: '김여사',
};

/**
 * 사원 수동 등록 endpoint 선택 — 화면별 게이팅 권한과 API 가드를 일치시키기 위한 분리.
 * 여사원 현황은 `female_employee:CREATE`, 설정>사원 목록은 공용 `employee:EDIT` 가드를 탄다.
 */
describe('manualRegisterEmployee - endpoint 선택', () => {
  beforeEach(() => {
    mockedPost.mockReset();
    mockedPost.mockResolvedValue({
      data: { success: true, data: { id: 1 } },
    } as never);
  });

  it('isFemaleEmployee=true - 여사원 전용 endpoint 호출 (female_employee:CREATE 가드)', async () => {
    await manualRegisterEmployee(request, { isFemaleEmployee: true });

    expect(mockedPost).toHaveBeenCalledWith('/api/v1/admin/female-employees/manual', request);
  });

  it('isFemaleEmployee=false - 공용 사원 endpoint 호출 (employee:EDIT 가드)', async () => {
    await manualRegisterEmployee(request, { isFemaleEmployee: false });

    expect(mockedPost).toHaveBeenCalledWith('/api/v1/admin/employees/manual', request);
  });

  it('options 미전달 - 공용 사원 endpoint 가 기본 (설정>사원 목록 기존 동작 유지)', async () => {
    await manualRegisterEmployee(request);

    expect(mockedPost).toHaveBeenCalledWith('/api/v1/admin/employees/manual', request);
  });

  it('success=false - 에러 메시지를 throw', async () => {
    mockedPost.mockResolvedValueOnce({
      data: { success: false, message: '사번이 중복됩니다' },
    } as never);

    await expect(manualRegisterEmployee(request, { isFemaleEmployee: true })).rejects.toThrow(
      '사번이 중복됩니다',
    );
  });
});
