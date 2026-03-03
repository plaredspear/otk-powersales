import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import koKR from 'antd/locale/ko_KR';
import App from '@/App';
import { useAuthStore } from '@/stores/authStore';

// Initialize auth state from localStorage
useAuthStore.getState().initialize();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={koKR}>
      <App />
    </ConfigProvider>
  </React.StrictMode>
);
