import { describe, expect, it } from 'vitest';
import {
  TEMPORARY_PASSWORD_FALLBACK,
  temporaryPasswordFor,
} from './temporaryPassword';

describe('temporaryPasswordFor (backend TemporaryPasswordPolicy 미러)', () => {
  it('사번이 있으면 {사번}@pwrs 를 반환한다', () => {
    expect(temporaryPasswordFor('100123')).toBe('100123@pwrs');
  });

  it('사번이 undefined 면 종전 고정값으로 되돌아간다', () => {
    expect(temporaryPasswordFor(undefined)).toBe(TEMPORARY_PASSWORD_FALLBACK);
  });

  it('사번이 null 이면 종전 고정값으로 되돌아간다', () => {
    expect(temporaryPasswordFor(null)).toBe(TEMPORARY_PASSWORD_FALLBACK);
  });

  it('사번이 공백만이면 종전 고정값으로 되돌아간다', () => {
    expect(temporaryPasswordFor('   ')).toBe(TEMPORARY_PASSWORD_FALLBACK);
  });
});
