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
import type { SfEntityOperation, SfSystemPermission } from '@/hooks/usePermission';

export interface MenuItem {
  path?: string;
  name: string;
  icon?: ReactNode;
  children?: MenuItem[];
  entity?: string;
  operation?: SfEntityOperation;
  systemPermission?: SfSystemPermission;
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
        { path: '/schedule', name: '여사원 일정관리', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/monthly-integration', name: '월별여사원 통합일정', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/work-type-headcount', name: '근무형태별 여사원인원현황', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/monthly-input-adequacy', name: '월별 진열사원 투입적합성', entity: 'team_member_schedule', operation: 'READ' },
      ],
    },
    {
      name: '행사/배치',
      icon: <BulbOutlined />,
      children: [
        { path: '/promotions', name: '행사마스터', entity: 'promotion', operation: 'READ' },
        { path: '/display-schedules', name: '진열스케줄마스터', entity: 'promotion', operation: 'EDIT' },
        { path: '/promotion/ppt-masters', name: '전문행사조' },
        { path: '/promotion/ppt-master-history', name: '전문행사조 이력', entity: 'promotion', operation: 'READ' },
        { path: '/deployment', name: '거래처별 진열사원 배치적합성', entity: 'monthly_sales_history', operation: 'READ' },
        { path: '/alternative-holidays', name: '대체휴무' },
      ],
    },
    {
      name: '인사/근무',
      icon: <TeamOutlined />,
      children: [
        { path: '/employee', name: '여사원 현황', entity: 'employee', operation: 'READ' },
        { path: '/leave', name: '휴무관리' },
        { path: '/attendance', name: '근무 등록현황', entity: 'attendance_log', operation: 'READ' },
        { path: '/attend-info', name: '근무기간 조회', entity: 'attend_info', operation: 'READ' },
      ],
    },
    {
      name: '매출/실적',
      icon: <BarChartOutlined />,
      children: [
        { path: '/sales/monthly', name: '월매출 조회', entity: 'monthly_sales_history', operation: 'READ' },
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
        { path: '/safety-check', name: '안전점검', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/product-expiration', name: '유통기한 관리', entity: 'product', operation: 'READ' },
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
        { path: '/account', name: '거래처', entity: 'account', operation: 'READ' },
        { path: '/settings/employees', name: '사원', entity: 'employee', operation: 'READ' },
        { path: '/settings/organizations', name: '조직마스터' },
        { path: '/settings/holiday-masters', name: '공휴일 관리' },
        {
          path: '/settings/employee-input-criteria-masters',
          name: '진열사원 투입기준 마스터',
          entity: 'employee_input_criteria_master',
          operation: 'READ',
        },
      ],
    },
    {
      name: '시스템',
      icon: <SettingOutlined />,
      children: [
        { path: '/users', name: '사용자 관리', entity: 'user', operation: 'READ' },
        { path: '/admin/agreement-words', name: '동의 약관 등록', entity: 'agreement_word', operation: 'READ' },
      ],
    },
    {
      name: '운영 도구',
      icon: <ToolOutlined />,
      children: [
        { path: '/admin/tools/naver-geocode', name: 'Naver Geocode 변환 테스트', systemPermission: 'MODIFY_ALL_DATA' },
        { path: '/admin/tools/scheduled-jobs', name: '스케줄 잡 실행 이력', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/tools/sap-inbound', name: 'SAP Inbound', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/tools/sap-outbound', name: 'SAP Outbound', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/tools/sf-migration-stage1', name: 'SF Migration — Stage 1 적재', systemPermission: 'MODIFY_ALL_DATA' },
        { path: '/admin/tools/sf-migration', name: 'SF Migration — Stage 2 FK', systemPermission: 'MODIFY_ALL_DATA' },
      ],
    },
  ],
};
