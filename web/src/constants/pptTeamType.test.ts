import { describe, it, expect } from 'vitest';
import {
  PPT_TEAM_TYPES,
  PPT_TEAM_TYPES_FOR_MASTER,
  PPT_TEAM_TYPE_OPTIONS,
  PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL,
  getPPTTeamTypeColor,
  isPPTTeamType,
} from './pptTeamType';

describe('pptTeamType', () => {
  describe('상수', () => {
    it('PPT_TEAM_TYPES는 6개 값을 포함한다', () => {
      expect(PPT_TEAM_TYPES).toHaveLength(6);
      expect(PPT_TEAM_TYPES).toContain('일반');
      expect(PPT_TEAM_TYPES).toContain('카레세일조');
    });

    it('PPT_TEAM_TYPES_FOR_MASTER는 5개(일반 제외) 값을 포함한다', () => {
      expect(PPT_TEAM_TYPES_FOR_MASTER).toHaveLength(5);
      expect(PPT_TEAM_TYPES_FOR_MASTER).not.toContain('일반');
    });

    it('PPT_TEAM_TYPE_OPTIONS는 마스터 등록용 5개 옵션을 가진다', () => {
      expect(PPT_TEAM_TYPE_OPTIONS).toHaveLength(5);
      expect(PPT_TEAM_TYPE_OPTIONS[0]).toEqual({ value: '라면세일조', label: '라면세일조' });
    });

    it('PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL는 일반 포함 6개 옵션을 가진다', () => {
      expect(PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL).toHaveLength(6);
      expect(PPT_TEAM_TYPE_OPTIONS_WITH_GENERAL[0]).toEqual({ value: '일반', label: '일반' });
    });
  });

  describe('getPPTTeamTypeColor', () => {
    it('정의된 값은 매핑된 색상을 반환한다', () => {
      expect(getPPTTeamTypeColor('라면세일조')).toBe('red');
      expect(getPPTTeamTypeColor('프레시세일조_냉장')).toBe('blue');
      expect(getPPTTeamTypeColor('프레시세일조_냉동')).toBe('cyan');
      expect(getPPTTeamTypeColor('프레시세일조_만두')).toBe('green');
      expect(getPPTTeamTypeColor('카레세일조')).toBe('orange');
      expect(getPPTTeamTypeColor('일반')).toBe('default');
    });

    it("legacy 표시명 '카레행사조'도 orange 로 방어 매핑한다", () => {
      expect(getPPTTeamTypeColor('카레행사조')).toBe('orange');
    });

    it('null/undefined/미정의 값은 default 반환', () => {
      expect(getPPTTeamTypeColor(null)).toBe('default');
      expect(getPPTTeamTypeColor(undefined)).toBe('default');
      expect(getPPTTeamTypeColor('존재하지않는값')).toBe('default');
      expect(getPPTTeamTypeColor('')).toBe('default');
    });
  });

  describe('isPPTTeamType 타입 가드', () => {
    it('유효한 enum 값은 true', () => {
      expect(isPPTTeamType('라면세일조')).toBe(true);
      expect(isPPTTeamType('일반')).toBe(true);
    });

    it('그 외 값은 false', () => {
      expect(isPPTTeamType('이상한값')).toBe(false);
      expect(isPPTTeamType(null)).toBe(false);
      expect(isPPTTeamType(undefined)).toBe(false);
      expect(isPPTTeamType(123)).toBe(false);
    });
  });
});
