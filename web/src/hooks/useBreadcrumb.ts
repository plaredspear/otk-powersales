import { useMemo } from 'react';
import { menuRoute } from '@/config/menuConfig';

export interface BreadcrumbItem {
  label: string;
  path: string | null;
}

// Build path → breadcrumb trail mapping from menuRoute
const breadcrumbMap: Record<string, BreadcrumbItem[]> = {};
for (const item of menuRoute.routes) {
  if (item.routes) {
    for (const child of item.routes) {
      if (child.path) {
        breadcrumbMap[child.path] = [
          { label: item.name, path: null }, // group name, not clickable
          { label: child.name, path: null }, // current page, not clickable
        ];
      }
    }
  } else if (item.path && item.path !== '/') {
    breadcrumbMap[item.path] = [{ label: item.name, path: null }];
  }
}

export function useBreadcrumb(
  pathname: string,
  dynamicTitle: string | null,
): BreadcrumbItem[] | null {
  return useMemo(() => {
    // Dashboard: no breadcrumb
    if (pathname === '/') return null;

    // Exact match from menu routes
    if (breadcrumbMap[pathname]) {
      return breadcrumbMap[pathname];
    }

    // Dynamic routes for notices
    if (pathname === '/notices/new') {
      return [
        { label: '공지사항', path: '/notices' },
        { label: '새 공지 작성', path: null },
      ];
    }

    const editMatch = /^\/notices\/(\d+)\/edit$/.exec(pathname);
    if (editMatch) {
      const noticeId = editMatch[1];
      return [
        { label: '공지사항', path: '/notices' },
        { label: dynamicTitle ?? noticeId, path: `/notices/${noticeId}` },
        { label: '수정', path: null },
      ];
    }

    const detailMatch = /^\/notices\/(\d+)$/.exec(pathname);
    if (detailMatch) {
      const noticeId = detailMatch[1];
      return [
        { label: '공지사항', path: '/notices' },
        { label: dynamicTitle ?? noticeId, path: null },
      ];
    }

    // Dynamic routes for promotions
    if (pathname === '/promotions/new') {
      return [
        { label: '행사마스터', path: '/promotions' },
        { label: '행사마스터 등록', path: null },
      ];
    }

    const promotionEditMatch = /^\/promotions\/(\d+)\/edit$/.exec(pathname);
    if (promotionEditMatch) {
      const promoId = promotionEditMatch[1];
      return [
        { label: '행사마스터', path: '/promotions' },
        { label: dynamicTitle ?? promoId, path: `/promotions/${promoId}` },
        { label: '수정', path: null },
      ];
    }

    const promotionDetailMatch = /^\/promotions\/(\d+)$/.exec(pathname);
    if (promotionDetailMatch) {
      const promoId = promotionDetailMatch[1];
      return [
        { label: '행사마스터', path: '/promotions' },
        { label: dynamicTitle ?? promoId, path: null },
      ];
    }

    // No mapping found
    return null;
  }, [pathname, dynamicTitle]);
}
