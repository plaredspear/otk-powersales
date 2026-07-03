import { describe, it, expect } from 'vitest';
import {
  countCharacterTypes,
  isLengthValid,
  hasEnoughCharacterTypes,
  isPasswordValid,
} from './passwordPolicy';

describe('passwordPolicy (8자 이상 + 3종 이상 조합)', () => {
  describe('countCharacterTypes', () => {
    it('소문자만 -> 1종', () => {
      expect(countCharacterTypes('abcdefgh')).toBe(1);
    });
    it('소문자+숫자 -> 2종', () => {
      expect(countCharacterTypes('abcd1234')).toBe(2);
    });
    it('대문자+소문자+숫자 -> 3종', () => {
      expect(countCharacterTypes('Abcdefg1')).toBe(3);
    });
    it('4종 모두 -> 4종', () => {
      expect(countCharacterTypes('Abcd123!')).toBe(4);
    });
    it('한글은 카테고리 아님 - 한글+숫자 -> 1종', () => {
      expect(countCharacterTypes('가나다12')).toBe(1);
    });
  });

  describe('isLengthValid', () => {
    it('8자 -> true', () => {
      expect(isLengthValid('abcd1234')).toBe(true);
    });
    it('7자 -> false', () => {
      expect(isLengthValid('abc1234')).toBe(false);
    });
  });

  describe('hasEnoughCharacterTypes', () => {
    it('3종 -> true', () => {
      expect(hasEnoughCharacterTypes('Abcd123')).toBe(true);
    });
    it('2종 -> false', () => {
      expect(hasEnoughCharacterTypes('abcd1234')).toBe(false);
    });
  });

  describe('isPasswordValid', () => {
    it('8자 + 3종 -> true', () => {
      expect(isPasswordValid('Abcd123!')).toBe(true);
    });
    it('임시 비밀번호 pwrs1234! -> true', () => {
      expect(isPasswordValid('pwrs1234!')).toBe(true);
    });
    it('8자지만 2종 -> false', () => {
      expect(isPasswordValid('abcd1234')).toBe(false);
    });
    it('3종이지만 7자 -> false', () => {
      expect(isPasswordValid('Abc12!x')).toBe(false);
    });
    it('반복 문자 있어도 8자 + 3종이면 true (반복금지 없음)', () => {
      expect(isPasswordValid('aaaaA1!x')).toBe(true);
    });
  });
});
