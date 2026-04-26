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
        borderTop: `1px solid ${token.colorBorderSecondary}`,
        borderBottom: `1px solid ${token.colorBorderSecondary}`,
        display: 'flex',
        alignItems: 'center',
        width: '100%',
      }}
    >
      {items && items.length > 0 && (
        <Breadcrumb
          items={items.map((item, index) => {
            const label =
              index === 0 && item.icon ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                  {item.icon}
                  {item.label}
                </span>
              ) : (
                item.label
              );
            return {
              title: item.path ? (
                <a onClick={() => navigate(item.path!)}>{label}</a>
              ) : (
                label
              ),
            };
          })}
        />
      )}
    </div>
  );
}
