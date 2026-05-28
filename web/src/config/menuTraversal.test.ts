import { describe, it, expect } from 'vitest';
import { flattenMenuLeaves } from './menuTraversal';
import type { MenuRoute } from './menuConfig';

describe('flattenMenuLeaves', () => {
  it('2단 트리에서 leaf 만 추출 + 카테고리는 부모 name', () => {
    const menu: MenuRoute = {
      path: '/',
      children: [
        {
          name: '시스템',
          children: [
            { path: '/users', name: '사용자 관리' },
            { path: '/admin/permissions/matrix', name: '권한 매트릭스' },
          ],
        },
        {
          name: '인사/근무',
          children: [
            { path: '/employee', name: '여사원 현황' },
          ],
        },
      ],
    };

    const result = flattenMenuLeaves(menu);
    expect(result).toEqual([
      { category: '시스템', item: { path: '/users', name: '사용자 관리' } },
      { category: '시스템', item: { path: '/admin/permissions/matrix', name: '권한 매트릭스' } },
      { category: '인사/근무', item: { path: '/employee', name: '여사원 현황' } },
    ]);
  });

  it('path 없는 항목은 leaf 가 아니므로 제외', () => {
    const menu: MenuRoute = {
      path: '/',
      children: [
        {
          name: '카테고리A',
          children: [
            { name: '헤더만 있는 항목 (path 없음)' },
            { path: '/a', name: '실 페이지 A' },
          ],
        },
      ],
    };

    const result = flattenMenuLeaves(menu);
    expect(result).toEqual([
      { category: '카테고리A', item: { path: '/a', name: '실 페이지 A' } },
    ]);
  });

  it('부모 자체가 leaf (children 없음 + path 있음) 인 경우도 포함, 카테고리는 자기 자신', () => {
    const menu: MenuRoute = {
      path: '/',
      children: [
        { path: '/dashboard', name: '대시보드' },
      ],
    };

    const result = flattenMenuLeaves(menu);
    expect(result).toEqual([
      { category: '대시보드', item: { path: '/dashboard', name: '대시보드' } },
    ]);
  });

  it('빈 menu', () => {
    const menu: MenuRoute = { path: '/', children: [] };
    expect(flattenMenuLeaves(menu)).toEqual([]);
  });

  it('권한 메타 (entity/operation/systemPermission/allowedProfileNames) 가 그대로 보존됨', () => {
    const menu: MenuRoute = {
      path: '/',
      children: [
        {
          name: '시스템',
          children: [
            {
              path: '/admin/permissions/guide',
              name: '권한 사용 가이드',
              allowedProfileNames: ['시스템 관리자'],
            },
            {
              path: '/admin/tools/cache',
              name: 'Redis 캐시 관리',
              systemPermission: 'MODIFY_ALL_DATA',
            },
            {
              path: '/employee',
              name: '여사원 현황',
              entity: 'employee',
              operation: 'READ',
            },
          ],
        },
      ],
    };

    const result = flattenMenuLeaves(menu);
    expect(result[0].item.allowedProfileNames).toEqual(['시스템 관리자']);
    expect(result[1].item.systemPermission).toBe('MODIFY_ALL_DATA');
    expect(result[2].item.entity).toBe('employee');
    expect(result[2].item.operation).toBe('READ');
  });
});
