import { describe, it, expect } from 'vitest';
// ProLayout(@ant-design/pro-layout) 이 내부적으로 사용하는 것과 동일한 라우트 변환·매칭 유틸.
// public export 를 쓴다(내부 /es/... 경로 결합 회피). package.json devDependencies 에 버전 고정.
import { transformRoute, getMatchMenu } from '@umijs/route-utils';
import { withMenuCategoryKeys, menuRoute, type MenuItem } from './menuConfig';

/**
 * ProLayout(@umijs/route-utils) 이 route 를 menuData 로 변환한 뒤 pathname 으로 selected/open 카테고리를
 * 계산하는 실제 경로(transformRoute → getMatchMenu)를 그대로 태워, 사이드바 활성 카테고리를 검증한다.
 *
 * 회귀 대상 버그: path 없는 카테고리가 2단 중첩되면 ProLayout 내부 mergePath 가 그 path 를 `'/'` 로
 * collapse 시켜, 대시보드(pathname `'/'`)에서 마지막 생존 카테고리(보고서 > 기타)가 잘못 열리던 문제.
 * withMenuCategoryKeys 로 카테고리에 placeholder path 를 주입해 collapse 를 막는다.
 */
function matchedCategoryNames(items: MenuItem[], pathname: string): string[] {
  // ProLayout getMenuData 와 동일: transformRoute(route.children, false, undefined, true) → getMatchMenu(pathname, menuData, true)
  const { menuData } = transformRoute(withMenuCategoryKeys(items), false, undefined, true);
  const matched = getMatchMenu(pathname, menuData, true);
  return matched.filter((m) => m.children && m.children.length > 0).map((m) => m.name as string);
}

describe('withMenuCategoryKeys + ProLayout 활성 카테고리', () => {
  const menu: MenuItem[] = [
    {
      name: '보고서',
      children: [
        { name: '여사원 근무', children: [{ path: '/female-employee-placement-check', name: '여사원 배치 점검' }] },
        {
          name: '기타',
          children: [{ path: '/promotion-target-actual-report', name: '행사사원 목표 대비 실적' }],
        },
      ],
    },
    {
      name: '매출/실적',
      children: [{ path: '/sales/monthly', name: '월 매출' }],
    },
  ];

  it('대시보드(/)에서는 어떤 카테고리도 열리지 않는다 (기타 오작동 회귀 방지)', () => {
    expect(matchedCategoryNames(menu, '/')).toEqual([]);
  });

  it('보고서 하위 페이지 접속 시 해당 부모 카테고리 체인만 열린다', () => {
    const names = matchedCategoryNames(menu, '/promotion-target-actual-report');
    expect(names).toContain('보고서');
    expect(names).toContain('기타');
    expect(names).not.toContain('여사원 근무');
  });

  it('withMenuCategoryKeys 는 path 없는 모든 카테고리에 / 로 시작하는 고유 path 를 부여한다', () => {
    const keyed = withMenuCategoryKeys(menu);
    const report = keyed[0];
    expect(report.path).toMatch(/^\//);
    const etc = report.children!.find((c) => c.name === '기타')!;
    // depth2 카테고리도 고유 path 를 가져 '/' 로 collapse 되지 않는다.
    expect(etc.path).toMatch(/^\//);
    expect(etc.path).not.toBe('/');
    // path 를 이미 가진 leaf 는 건드리지 않는다.
    const leaf = etc.children![0];
    expect(leaf.path).toBe('/promotion-target-actual-report');
  });

  it('실제 menuConfig 트리에서도 대시보드(/)에서 열리는 카테고리가 없다', () => {
    expect(matchedCategoryNames(menuRoute.children, '/')).toEqual([]);
  });
});

/**
 * 게이팅 누락 회귀 방지 — AdminLayout 의 itemAllowed 는 `entity && operation` 이 **둘 다 없으면
 * 무조건 노출**(`return true`) 한다. 따라서 API 에 `@RequiresSfPermission` 가드가 있는 화면인데
 * menuConfig 에 entity/operation 을 빠뜨리면 "메뉴는 보이는데 들어가면 403" 이 된다.
 *
 * 실제 사고: 공휴일 관리 / 조직마스터 / 대체휴무가 게이팅 없이 노출되어 권한 없는 조장에게도
 * 메뉴가 보였다 (영업일관리마스터만 게이팅이 있어 정상 동작). 같은 축의 마스터 화면들이라
 * 하나만 누락되어도 눈에 띄지 않는다 — 목록으로 고정해 재발을 막는다.
 */
describe('기준정보 마스터 메뉴 권한 게이팅', () => {
  function findByPath(items: MenuItem[], path: string): MenuItem | undefined {
    for (const item of items) {
      if (item.path === path) return item;
      const found = item.children && findByPath(item.children, path);
      if (found) return found;
    }
    return undefined;
  }

  // path → backend @RequiresSfPermission(entity) 와 일치해야 하는 게이팅 entity.
  const GATED_MASTER_MENUS: Array<[string, string]> = [
    ['/settings/organizations', 'organization'],
    ['/settings/holiday-masters', 'holiday_master'],
    ['/admin/working-day-masters', 'working_day_master'],
    ['/settings/alternative-holidays', 'alternative_holiday'],
  ];

  it.each(GATED_MASTER_MENUS)('%s 는 entity=%s READ 로 게이팅된다', (path, entity) => {
    const item = findByPath(menuRoute.children, path);
    expect(item).toBeDefined();
    expect(item!.entity).toBe(entity);
    expect(item!.operation).toBe('READ');
  });
});
