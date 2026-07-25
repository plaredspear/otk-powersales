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
 * 사원 수동 등록은 기준정보 > 사원(`/settings/employees`) 한 화면에서만 관리한다.
 * 여사원 현황은 조회 전용이라 등록 경로를 갖지 않는다.
 */
describe('manualRegisterEmployee', () => {
  beforeEach(() => {
    mockedPost.mockReset();
    mockedPost.mockResolvedValue({
      data: { success: true, data: { id: 1 } },
    } as never);
  });

  it('공용 사원 등록 endpoint 를 호출한다', async () => {
    await manualRegisterEmployee(request);

    expect(mockedPost).toHaveBeenCalledWith('/api/v1/admin/employees/manual', request);
  });

  it('success=false - 에러 메시지를 throw', async () => {
    mockedPost.mockResolvedValueOnce({
      data: { success: false, message: '사번이 중복됩니다' },
    } as never);

    await expect(manualRegisterEmployee(request)).rejects.toThrow('사번이 중복됩니다');
  });
});
