import type { MenuItem, MenuRoute } from '@/config/menuConfig';

/**
 * 메뉴 트리에서 leaf (path 있는 실제 페이지 항목) 만 평탄화하여 카테고리 정보와 함께 반환.
 *
 * - 카테고리: 부모 MenuItem.name. 최상위 메뉴(부모)는 자체적으로 path 가 없고 children 만 갖는 구조라
 *   각 leaf 의 직계 부모를 카테고리로 본다.
 * - leaf 판정: `item.path` 가 있고 `item.children` 이 없음
 * - 부모 1단만 가정 (현 menuConfig 구조는 2단). 더 깊은 중첩이 생기면 본 함수 재방문 필요.
 * - `includeSubRoutes=true` 일 때 각 leaf 의 `subRoutes` 도 같은 카테고리로 평탄화하여 결과에 포함.
 *   사이드바 메뉴 렌더링에는 사용하지 않고, PageAccessGuide 같은 라우트 인벤토리에서만 켠다.
 */
export interface FlattenedMenuLeaf {
  category: string;
  item: MenuItem;
  /** 모페이지(list)의 sub-route 인지 여부. PageAccessGuide 에서 row 들여쓰기 등에 활용. */
  isSubRoute: boolean;
  /** subRoute 인 경우 모페이지 name. 그 외에는 null. */
  parentName: string | null;
}

export interface FlattenOptions {
  includeSubRoutes?: boolean;
}

export function flattenMenuLeaves(menu: MenuRoute, options: FlattenOptions = {}): FlattenedMenuLeaf[] {
  const { includeSubRoutes = false } = options;
  const result: FlattenedMenuLeaf[] = [];

  const pushLeafWithSubRoutes = (category: string, leaf: MenuItem) => {
    result.push({ category, item: leaf, isSubRoute: false, parentName: null });
    if (includeSubRoutes && leaf.subRoutes && leaf.subRoutes.length > 0) {
      for (const sub of leaf.subRoutes) {
        if (sub.path) {
          result.push({ category, item: sub, isSubRoute: true, parentName: leaf.name });
        }
      }
    }
  };

  for (const parent of menu.children) {
    const category = parent.name;
    if (!parent.children || parent.children.length === 0) {
      // 부모 자체가 leaf 인 경우 — 카테고리는 자기 자신
      if (parent.path) {
        pushLeafWithSubRoutes(category, parent);
      }
      continue;
    }
    for (const child of parent.children) {
      if (child.path && (!child.children || child.children.length === 0)) {
        pushLeafWithSubRoutes(category, child);
      }
    }
  }
  return result;
}
