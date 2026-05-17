import type { ReactNode } from 'react';
import {
  BarChartOutlined,
  TeamOutlined,
  CalendarOutlined,
  BulbOutlined,
  DatabaseOutlined,
  SearchOutlined,
  NotificationOutlined,
  SettingOutlined,
  ToolOutlined,
} from '@ant-design/icons';

export interface MenuItem {
  path?: string;
  name: string;
  icon?: ReactNode;
  children?: MenuItem[];
  requiredPermission?: string;
}

export interface MenuRoute {
  path: string;
  children: MenuItem[];
}

export const menuRoute: MenuRoute = {
  path: '/',
  children: [
    {
      name: '여사원 일정',
      icon: <CalendarOutlined />,
      children: [
        { path: '/schedule', name: '여사원 일정관리', requiredPermission: 'SCHEDULE_READ' },
        { path: '/monthly-integration', name: '월별여사원 통합일정', requiredPermission: 'SCHEDULE_READ' },
        { path: '/work-type-headcount', name: '근무형태별 인원현황', requiredPermission: 'SCHEDULE_READ' },
      ],
    },
    {
      name: '행사/배치',
      icon: <BulbOutlined />,
      children: [
        { path: '/promotions', name: '행사마스터', requiredPermission: 'PROMOTION_READ' },
        { path: '/display-schedules', name: '진열스케줄마스터', requiredPermission: 'PROMOTION_WRITE' },
        { path: '/promotion/ppt-masters', name: '전문행사조' },
        { path: '/promotion/ppt-master-history', name: '전문행사조 이력', requiredPermission: 'PROMOTION_READ' },
        { path: '/deployment', name: '거래처별 진열사원 배치적합성', requiredPermission: 'SALES_COMPARISON_READ' },
        { path: '/alternative-holidays', name: '대체휴무' },
      ],
    },
    {
      name: '인사/근무',
      icon: <TeamOutlined />,
      children: [
        { path: '/employee', name: '여사원 현황', requiredPermission: 'EMPLOYEE_READ' },
        { path: '/leave', name: '휴무관리' },
        { path: '/attendance', name: '근무 등록현황' },
      ],
    },
    {
      name: '매출/실적',
      icon: <BarChartOutlined />,
      children: [
        { path: '/sales/monthly', name: '물류배부' },
        { path: '/sales/electronic', name: '전산실적' },
        { path: '/sales/pos', name: 'POS매출' },
        { path: '/report', name: '보고서' },
      ],
    },
    {
      name: '현장 점검/이슈',
      icon: <SearchOutlined />,
      children: [
        { path: '/field-inspection', name: '현장점검' },
        { path: '/safety-check', name: '안전점검', requiredPermission: 'SAFETY_CHECK_READ' },
        { path: '/product-expiration', name: '유통기한 관리', requiredPermission: 'PRODUCT_EXPIRATION_READ' },
        { path: '/claims', name: '클레임 현황' },
        { path: '/suggestion', name: '제안사항' },
      ],
    },
    {
      name: '알림/교육',
      icon: <NotificationOutlined />,
      children: [
        { path: '/notices', name: '공지사항' },
        { path: '/education', name: '교육' },
      ],
    },
    {
      name: '기준정보',
      icon: <DatabaseOutlined />,
      children: [
        { path: '/product', name: '제품' },
        { path: '/account', name: '거래처', requiredPermission: 'ACCOUNT_READ' },
        { path: '/settings/organizations', name: '조직마스터' },
        { path: '/settings/holiday-masters', name: '공휴일 관리' },
      ],
    },
    {
      name: '시스템',
      icon: <SettingOutlined />,
      children: [
        { path: '/users', name: '사용자 관리', requiredPermission: 'USER_READ' },
        { path: '/settings/permissions', name: '권한 관리' },
        { path: '/admin/agreement-words', name: '동의 약관 등록', requiredPermission: 'AGREEMENT_READ' },
      ],
    },
    {
      name: '운영 도구',
      icon: <ToolOutlined />,
      children: [
        { path: '/admin/tools/naver-geocode', name: 'Naver Geocode 변환 테스트', requiredPermission: 'NAVER_GEOCODE_TEST' },
      ],
    },
  ],
};
