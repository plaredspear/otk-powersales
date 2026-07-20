import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios, { AxiosError } from 'axios';
import { changePassword, ChangePasswordError } from './auth';

vi.mock('axios');
const mockedPost = vi.mocked(axios.post);

/** AxiosError(status) 를 응답 본문과 함께 생성하는 헬퍼. */
function axiosErrorWith(status: number, message?: string): AxiosError {
  const err = new AxiosError('request failed');
  // 실제 런타임의 instanceof 판별을 위해 프로토타입을 맞춘다.
  Object.setPrototypeOf(err, AxiosError.prototype);
  err.response = {
    status,
    data: message ? { success: false, error: { message } } : { success: false },
    statusText: '',
    headers: {},
    config: {} as never,
  };
  return err;
}

describe('changePassword', () => {
  beforeEach(() => {
    mockedPost.mockReset();
    localStorage.clear();
  });

  it('성공 시 데이터를 반환한다', async () => {
    mockedPost.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          passwordChangeRequired: false,
          accessToken: 'a',
          refreshToken: 'r',
          expiresIn: 3600,
        },
      },
    });
    const data = await changePassword({ newPassword: 'Newpw123!' });
    expect(data.accessToken).toBe('a');
  });

  it('401 은 sessionExpired=true 로 감싼다 (자동 refresh 불가 → 재로그인 유도)', async () => {
    mockedPost.mockRejectedValueOnce(axiosErrorWith(401, '토큰이 만료되었습니다'));
    await expect(changePassword({ newPassword: 'Newpw123!' })).rejects.toMatchObject({
      name: 'ChangePasswordError',
      sessionExpired: true,
      message: '토큰이 만료되었습니다',
    });
  });

  it('401 이외의 에러는 sessionExpired=false 로 감싼다', async () => {
    mockedPost.mockRejectedValueOnce(axiosErrorWith(400, '비밀번호 정책 위반'));
    const err = await changePassword({ newPassword: 'weak' }).catch((e) => e);
    expect(err).toBeInstanceOf(ChangePasswordError);
    expect((err as ChangePasswordError).sessionExpired).toBe(false);
    expect((err as ChangePasswordError).message).toBe('비밀번호 정책 위반');
  });

  it('success=false 응답도 ChangePasswordError(sessionExpired=false) 로 던진다', async () => {
    mockedPost.mockResolvedValueOnce({
      data: { success: false, error: { message: '변경 실패' } },
    });
    const err = await changePassword({ newPassword: 'Newpw123!' }).catch((e) => e);
    expect(err).toBeInstanceOf(ChangePasswordError);
    expect((err as ChangePasswordError).sessionExpired).toBe(false);
  });
});
