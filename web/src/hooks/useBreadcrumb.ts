import type { ReactNode } from 'react';
import { createElement, useMemo } from 'react';
import { DashboardOutlined } from '@ant-design/icons';
import { menuRoute } from '@/config/menuConfig';

export interface BreadcrumbItem {
  label: string;
  path: string | null;
  icon?: ReactNode;
}

const dashboardIcon = createElement(DashboardOutlined);

// Build path → breadcrumb trail mapping from menuRoute
const breadcrumbMap: Record<string, BreadcrumbItem[]> = {};
for (const item of menuRoute.routes) {
  if (item.routes) {
    for (const child of item.routes) {
      if (child.path) {
        breadcrumbMap[child.path] = [
          { label: item.name, path: null, icon: item.icon },
          { label: child.name, path: null },
        ];
      }
    }
  } else if (item.path && item.path !== '/') {
    breadcrumbMap[item.path] = [{ label: item.name, path: null, icon: item.icon }];
  }
}

// Find icon for a given base path (for dynamic routes)
function findIconForPath(basePath: string): ReactNode | undefined {
  const entry = breadcrumbMap[basePath];
  if (entry && entry.length > 0) return entry[0].icon;
  return undefined;
}

export function useBreadcrumb(
  pathname: string,
  dynamicTitle: string | null,
): BreadcrumbItem[] | null {
  return useMemo(() => {
    // Dashboard: show dashboard breadcrumb
    if (pathname === '/') {
      return [{ label: '대시보드', path: null, icon: dashboardIcon }];
    }

    // Exact match from menu routes
    if (breadcrumbMap[pathname]) {
      return breadcrumbMap[pathname];
    }

    // Dynamic routes for notices
    if (pathname === '/notices/new') {
      return [
        { label: '공지사항', path: '/notices', icon: findIconForPath('/notices') },
        { label: '새 공지 작성', path: null },
      ];
    }

    const editMatch = /^\/notices\/(\d+)\/edit$/.exec(pathname);
    if (editMatch) {
      const noticeId = editMatch[1];
      return [
        { label: '공지사항', path: '/notices', icon: findIconForPath('/notices') },
        { label: dynamicTitle ?? noticeId, path: `/notices/${noticeId}` },
        { label: '수정', path: null },
      ];
    }

    const detailMatch = /^\/notices\/(\d+)$/.exec(pathname);
    if (detailMatch) {
      const noticeId = detailMatch[1];
      return [
        { label: '공지사항', path: '/notices', icon: findIconForPath('/notices') },
        { label: dynamicTitle ?? noticeId, path: null },
      ];
    }

    // Dynamic routes for promotions
    if (pathname === '/promotions/new') {
      return [
        { label: '행사마스터', path: '/promotions', icon: findIconForPath('/promotions') },
        { label: '행사마스터 등록', path: null },
      ];
    }

    const promotionEditMatch = /^\/promotions\/(\d+)\/edit$/.exec(pathname);
    if (promotionEditMatch) {
      const promoId = promotionEditMatch[1];
      return [
        { label: '행사마스터', path: '/promotions', icon: findIconForPath('/promotions') },
        { label: dynamicTitle ?? promoId, path: `/promotions/${promoId}` },
        { label: '수정', path: null },
      ];
    }

    const promotionDetailMatch = /^\/promotions\/(\d+)$/.exec(pathname);
    if (promotionDetailMatch) {
      const promoId = promotionDetailMatch[1];
      return [
        { label: '행사마스터', path: '/promotions', icon: findIconForPath('/promotions') },
        { label: dynamicTitle ?? promoId, path: null },
      ];
    }

    // No mapping found
    return null;
  }, [pathname, dynamicTitle]);
}
