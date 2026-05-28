import type { MenuItem, MenuRoute } from '@/config/menuConfig';

/**
 * 메뉴 트리에서 leaf (path 있는 실제 페이지 항목) 만 평탄화하여 카테고리 정보와 함께 반환.
 *
 * - 카테고리: 부모 MenuItem.name. 최상위 메뉴(부모)는 자체적으로 path 가 없고 children 만 갖는 구조라
 *   각 leaf 의 직계 부모를 카테고리로 본다.
 * - leaf 판정: `item.path` 가 있고 `item.children` 이 없음
 * - 부모 1단만 가정 (현 menuConfig 구조는 2단). 더 깊은 중첩이 생기면 본 함수 재방문 필요.
 */
export interface FlattenedMenuLeaf {
  category: string;
  item: MenuItem;
}

export function flattenMenuLeaves(menu: MenuRoute): FlattenedMenuLeaf[] {
  const result: FlattenedMenuLeaf[] = [];
  for (const parent of menu.children) {
    const category = parent.name;
    if (!parent.children || parent.children.length === 0) {
      // 부모 자체가 leaf 인 경우 — 카테고리는 자기 자신
      if (parent.path) {
        result.push({ category, item: parent });
      }
      continue;
    }
    for (const child of parent.children) {
      if (child.path && (!child.children || child.children.length === 0)) {
        result.push({ category, item: child });
      }
    }
  }
  return result;
}
