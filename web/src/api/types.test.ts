import { describe, expect, it } from 'vitest';
import { isApiErrorBody } from './types';

describe('isApiErrorBody', () => {
  it('returns false for null and undefined', () => {
    expect(isApiErrorBody(null)).toBe(false);
    expect(isApiErrorBody(undefined)).toBe(false);
  });

  it('returns false for primitives', () => {
    expect(isApiErrorBody('error')).toBe(false);
    expect(isApiErrorBody(42)).toBe(false);
    expect(isApiErrorBody(true)).toBe(false);
  });

  it('returns false for an empty object', () => {
    expect(isApiErrorBody({})).toBe(false);
  });

  it('returns false when error is null or empty', () => {
    expect(isApiErrorBody({ error: null })).toBe(false);
    expect(isApiErrorBody({ error: {} })).toBe(false);
  });

  it('returns true when error.code is a string', () => {
    expect(isApiErrorBody({ error: { code: 'X' } })).toBe(true);
    expect(isApiErrorBody({ error: { code: 'X', message: 'Y' } })).toBe(true);
  });
});
