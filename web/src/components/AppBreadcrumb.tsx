import { useLocation, useNavigate } from 'react-router-dom';
import { Breadcrumb } from 'antd';
import { useBreadcrumb } from '@/hooks/useBreadcrumb';
import { useBreadcrumbContext } from '@/contexts/BreadcrumbContext';

export default function AppBreadcrumb() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { dynamicTitle } = useBreadcrumbContext();
  const items = useBreadcrumb(pathname, dynamicTitle);

  if (!items || items.length === 0) return null;

  return (
    <div style={{ padding: '16px 0 0 24px' }}>
      <Breadcrumb
        items={items.map((item) => ({
          title: item.path ? (
            <a onClick={() => navigate(item.path!)}>{item.label}</a>
          ) : (
            item.label
          ),
        }))}
      />
    </div>
  );
}
