import type { ReactNode } from 'react';
import {
  DashboardOutlined,
  BarChartOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  ExclamationCircleOutlined,
  BulbOutlined,
  RestOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  ShoppingOutlined,
  UserOutlined,
  SearchOutlined,
  FileTextOutlined,
  NotificationOutlined,
  GiftOutlined,
  SettingOutlined,
} from '@ant-design/icons';

export interface MenuItem {
  path?: string;
  name: string;
  icon?: ReactNode;
  routes?: MenuItem[];
}

export interface MenuRoute {
  path: string;
  routes: MenuItem[];
}

export const menuRoute: MenuRoute = {
  path: '/',
  routes: [
    {
      path: '/',
      name: '대시보드',
      icon: <DashboardOutlined />,
    },
    {
      name: '매출조회',
      icon: <BarChartOutlined />,
      routes: [
        { path: '/sales/monthly', name: '물류배부' },
        { path: '/sales/electronic', name: '전산실적' },
        { path: '/sales/pos', name: 'POS매출' },
      ],
    },
    {
      name: '여사원',
      icon: <TeamOutlined />,
      routes: [
        { path: '/schedule', name: '일정관리' },
        { path: '/deployment', name: '배치' },
      ],
    },
    {
      name: '근무',
      icon: <ClockCircleOutlined />,
      routes: [{ path: '/attendance', name: '등록현황' }],
    },
    { path: '/promotions', name: '행사마스터', icon: <GiftOutlined /> },
    { path: '/event-team', name: '전문행사조', icon: <CalendarOutlined /> },
    { path: '/claim', name: '클레임 현황', icon: <ExclamationCircleOutlined /> },
    { path: '/suggestion', name: '제안사항', icon: <BulbOutlined /> },
    { path: '/leave', name: '휴무관리', icon: <RestOutlined /> },
    { path: '/safety-check', name: '안전점검', icon: <SafetyCertificateOutlined /> },
    { path: '/product', name: '제품', icon: <ShoppingOutlined /> },
    { path: '/account', name: '거래처', icon: <ShopOutlined /> },
    { path: '/employee', name: '사원', icon: <UserOutlined /> },
    { path: '/field-inspection', name: '현장점검', icon: <SearchOutlined /> },
    { path: '/report', name: '보고서', icon: <FileTextOutlined /> },
    { path: '/notices', name: '공지사항', icon: <NotificationOutlined /> },
    {
      name: '설정',
      icon: <SettingOutlined />,
      routes: [
        { path: '/settings/promotion-types', name: '행사유형 관리' },
      ],
    },
  ],
};
