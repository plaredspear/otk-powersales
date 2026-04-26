import { useAuthStore } from '@/stores/authStore';

export function usePermission() {
  const permissions = useAuthStore((state) => state.user?.permissions ?? []);

  const hasPermission = (permission: string): boolean => permissions.includes(permission);

  const hasAnyPermission = (perms: string[]): boolean => perms.some((p) => permissions.includes(p));

  return { hasPermission, hasAnyPermission };
}
