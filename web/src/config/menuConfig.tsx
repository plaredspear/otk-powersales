import type { ReactNode } from 'react';
import {
  BarChartOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  ExclamationCircleOutlined,
  BulbOutlined,
  RestOutlined,
  SafetyCertificateOutlined,
  DatabaseOutlined,
  SearchOutlined,
  FileTextOutlined,
  NotificationOutlined,
  ReadOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';

export interface MenuItem {
  path?: string;
  name: string;
  icon?: ReactNode;
  children?: MenuItem[];
}

export interface MenuRoute {
  path: string;
  children: MenuItem[];
}

export const menuRoute: MenuRoute = {
  path: '/',
  children: [
    {
      name: '여사원 배치',
      icon: <TeamOutlined />,
      children: [
        { path: '/schedule', name: '일정관리' },
        { path: '/promotions', name: '행사마스터' },
        { path: '/display-schedules', name: '진열스케줄마스터' },
      ],
    },
    {
      name: '근무',
      icon: <ClockCircleOutlined />,
      children: [{ path: '/attendance', name: '등록현황' }],
    },
    { path: '/promotion/ppt-masters', name: '전문행사조', icon: <CalendarOutlined /> },
    { path: '/claim', name: '클레임 현황', icon: <ExclamationCircleOutlined /> },
    { path: '/suggestion', name: '제안사항', icon: <BulbOutlined /> },
    {
      name: '휴무관리',
      icon: <RestOutlined />,
      children: [
        { path: '/leave', name: '휴무관리' },
        { path: '/alternative-holidays', name: '대체휴무' },
      ],
    },
    {
      name: '매출조회',
      icon: <BarChartOutlined />,
      children: [
        { path: '/sales/monthly', name: '물류배부' },
        { path: '/sales/electronic', name: '전산실적' },
        { path: '/sales/pos', name: 'POS매출' },
      ],
    },
    {
      name: '여사원관리',
      icon: <UserOutlined />,
      children: [
        { path: '/monthly-integration', name: '월별 통합일정' },
        { path: '/monthly-integration/category', name: '근무형태별 인원현황' },
      ],
    },
    { path: '/safety-check', name: '안전점검', icon: <SafetyCertificateOutlined /> },
    {
      name: '사원',
      icon: <UserOutlined />,
      children: [
        { path: '/employee', name: '여사원 현황' },
      ],
    },
    { path: '/field-inspection', name: '현장점검', icon: <SearchOutlined /> },
    { path: '/report', name: '보고서', icon: <FileTextOutlined /> },
    { path: '/notices', name: '공지사항', icon: <NotificationOutlined /> },
    { path: '/education', name: '교육', icon: <ReadOutlined /> },
    {
      name: 'SAP 데이터',
      icon: <DatabaseOutlined />,
      children: [
        { path: '/product', name: '제품' },
        { path: '/account', name: '거래처' },
        { path: '/settings/organizations', name: '조직마스터' },
      ],
    },
    {
      name: '설정',
      icon: <SettingOutlined />,
      children: [
        { path: '/settings/promotion-types', name: '행사유형 관리' },
        { path: '/settings/holiday-masters', name: '공휴일 관리' },
      ],
    },
  ],
};
