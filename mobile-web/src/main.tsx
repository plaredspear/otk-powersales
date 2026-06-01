import '@ant-design/v5-patch-for-react-19';
import './index.css';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import koKR from 'antd/locale/ko_KR';
import App from '@/App';
import { useAuthStore } from '@/stores/authStore';
import { mobileTheme } from '@/theme/mobileTheme';
import queryClient from '@/lib/queryClient';

// localStorage 토큰으로 인증 상태 복원
useAuthStore.getState().initialize();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={koKR} theme={mobileTheme}>
        <AntdApp>
          <App />
        </AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
