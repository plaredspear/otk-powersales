import { describe, expect, it } from 'vitest';
import { buildMonthlySalesSourceHref, toSalesMonthToken } from './monthlySalesSourceLink';

describe('toSalesMonthToken', () => {
  it('목표월을 zero-pad 한 YYYYMM 으로 변환한다 (SF 적재 포맷은 pad 비보장)', () => {
    expect(toSalesMonthToken('2026', '9')).toBe('202609');
    expect(toSalesMonthToken('2026', '09')).toBe('202609');
    expect(toSalesMonthToken('2026', '12')).toBe('202612');
  });

  it('전월(offset -1) 은 직전 달을 가리킨다', () => {
    expect(toSalesMonthToken('2026', '9', -1)).toBe('202608');
  });

  it('1월의 전월은 전년 12월이다 (연도 rollover — backend 산출 규칙 정합)', () => {
    expect(toSalesMonthToken('2026', '1', -1)).toBe('202512');
  });

  it('12월의 다음 달은 다음 해 1월이다', () => {
    expect(toSalesMonthToken('2026', '12', 1)).toBe('202701');
  });

  it('연/월을 해석할 수 없으면 null (링크 미노출)', () => {
    expect(toSalesMonthToken(null, '9')).toBeNull();
    expect(toSalesMonthToken('2026', null)).toBeNull();
    expect(toSalesMonthToken('2026', '')).toBeNull();
    expect(toSalesMonthToken('', '9')).toBeNull();
    expect(toSalesMonthToken('2026', '13')).toBeNull();
    expect(toSalesMonthToken('2026', '0')).toBeNull();
    expect(toSalesMonthToken('연도', '9')).toBeNull();
  });
});

describe('buildMonthlySalesSourceHref', () => {
  it('거래처코드 + 매출월을 조회조건 query 로 붙인다', () => {
    expect(buildMonthlySalesSourceHref('1025008', '202609')).toBe(
      '/settings/orora-monthly-sales?accountCode=1025008&salesMonth=202609',
    );
  });

  it('거래처명이 있으면 표시용으로 함께 전달한다', () => {
    expect(buildMonthlySalesSourceHref('1025008', '202609', 'GS25 역삼점')).toContain(
      'accountName=GS25+%EC%97%AD%EC%82%BC%EC%A0%90',
    );
  });
});
