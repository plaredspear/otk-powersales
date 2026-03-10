import { useContext } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Breadcrumb, theme } from 'antd';
import { useBreadcrumb } from '@/hooks/useBreadcrumb';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

export default function AppBreadcrumb() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { dynamicTitle } = useContext(BreadcrumbContext);
  const items = useBreadcrumb(pathname, dynamicTitle);
  const { token } = theme.useToken();

  return (
    <div
      style={{
        height: 28,
        paddingLeft: 24,
        background: '#fff',
        borderBottom: `1px solid ${token.colorBorderSecondary}`,
        display: 'flex',
        alignItems: 'center',
        width: '100%',
      }}
    >
      {items && items.length > 0 && (
        <Breadcrumb
          items={items.map((item) => ({
            title: item.path ? (
              <a onClick={() => navigate(item.path!)}>{item.label}</a>
            ) : (
              item.label
            ),
          }))}
        />
      )}
    </div>
  );
}
