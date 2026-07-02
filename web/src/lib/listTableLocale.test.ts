import { describe, it, expect } from 'vitest';
import { listTableLocale, BEFORE_SEARCH_TEXT, NO_RESULT_TEXT } from './listTableLocale';

describe('listTableLocale', () => {
  it('옵션 없이 호출하면(A형) 결과 없음 문구', () => {
    expect(listTableLocale()).toEqual({ emptyText: NO_RESULT_TEXT });
  });

  it('searched=false(B형 조회 전) 이면 조회 전 안내 문구', () => {
    expect(listTableLocale({ searched: false })).toEqual({ emptyText: BEFORE_SEARCH_TEXT });
  });

  it('searched=true(B형 조회 후 0건) 이면 결과 없음 문구', () => {
    expect(listTableLocale({ searched: true })).toEqual({ emptyText: NO_RESULT_TEXT });
  });

  it('조회 전 안내 문구를 페이지 고유 문구로 대체', () => {
    expect(listTableLocale({ searched: false, beforeSearchText: '지점과 기간을 선택해주세요.' })).toEqual({
      emptyText: '지점과 기간을 선택해주세요.',
    });
  });

  it('결과 없음 문구를 페이지 고유 문구로 대체', () => {
    expect(listTableLocale({ emptyText: '등록된 사원이 없습니다.' })).toEqual({
      emptyText: '등록된 사원이 없습니다.',
    });
  });

  it('searched=false 여도 emptyText 커스텀은 조회 전 문구에 영향 없음', () => {
    expect(listTableLocale({ searched: false, emptyText: '무시됨' })).toEqual({
      emptyText: BEFORE_SEARCH_TEXT,
    });
  });
});
