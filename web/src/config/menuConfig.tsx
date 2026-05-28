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
  MobileOutlined,
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
  /**
   * 허용 Profile.name 집합. 지정 시 entity/systemPermission 충족과 무관하게
   * user.profileName 매칭이 안 되면 메뉴 숨김. (라우터 가드와 동일 의미)
   */
  allowedProfileNames?: string[];
  /**
   * 사이드바에는 노출하지 않지만 PageAccessGuide 같은 라우트 인벤토리에는 포함할 sub-route.
   * 모페이지(list page) 아래에 등록/수정/상세 같은 항목을 묶을 때 사용.
   * AdminLayout 사이드바 구성에는 영향이 없으며 ProLayout 의 children 처리와 분리된다.
   */
  subRoutes?: MenuItem[];
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
        { path: '/monthly-input-adequacy', name: '월별 진열사원 투입적합성', entity: 'monthly_sales_history', operation: 'READ' },
      ],
    },
    {
      name: '행사/배치',
      icon: <BulbOutlined />,
      children: [
        {
          path: '/promotions',
          name: '행사마스터',
          entity: 'promotion',
          operation: 'READ',
          subRoutes: [
            { path: '/promotions/new', name: '행사마스터 등록', entity: 'promotion', operation: 'CREATE' },
            { path: '/promotions/:id', name: '행사마스터 상세', entity: 'promotion', operation: 'READ' },
            { path: '/promotions/:id/edit', name: '행사마스터 수정', entity: 'promotion', operation: 'EDIT' },
          ],
        },
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
        {
          path: '/employee',
          name: '여사원 현황',
          entity: 'employee',
          operation: 'READ',
          subRoutes: [
            { path: '/employee/:employeeId', name: '여사원 상세', entity: 'employee', operation: 'READ' },
          ],
        },
        { path: '/leave', name: '휴무관리' },
        { path: '/attendance', name: '근무 등록현황', entity: 'attendance_log', operation: 'READ' },
        { path: '/attend-info', name: '근무기간 조회', entity: 'attend_info', operation: 'READ' },
      ],
    },
    {
      name: '매출/실적',
      icon: <BarChartOutlined />,
      children: [
        { path: '/sales/monthly', name: '월 매출(물류배부)', entity: 'monthly_sales_history', operation: 'READ' },
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
        {
          path: '/claims',
          name: '제품 클레임',
          subRoutes: [
            { path: '/claims/new', name: '제품 클레임 등록' },
            { path: '/claims/:claimId', name: '제품 클레임 상세' },
          ],
        },
        {
          path: '/suggestion',
          name: '물류 클레임',
          entity: 'suggestion',
          operation: 'READ',
          subRoutes: [
            { path: '/suggestion/new', name: '물류 클레임 등록' },
            { path: '/suggestion/:id', name: '물류 클레임 상세' },
          ],
        },
      ],
    },
    {
      name: '알림/교육',
      icon: <NotificationOutlined />,
      children: [
        {
          path: '/notices',
          name: '공지사항',
          entity: 'notice',
          operation: 'READ',
          subRoutes: [
            { path: '/notices/new', name: '공지사항 등록', entity: 'notice', operation: 'CREATE' },
            { path: '/notices/:id', name: '공지사항 상세', entity: 'notice', operation: 'READ' },
            { path: '/notices/:id/edit', name: '공지사항 수정', entity: 'notice', operation: 'EDIT' },
          ],
        },
        {
          path: '/education',
          name: '교육',
          subRoutes: [
            { path: '/education/new', name: '교육 등록' },
            { path: '/education/:id', name: '교육 상세' },
            { path: '/education/:id/edit', name: '교육 수정' },
          ],
        },
      ],
    },
    {
      name: '기준정보',
      icon: <DatabaseOutlined />,
      children: [
        {
          path: '/product',
          name: '제품',
          subRoutes: [
            { path: '/product/:productCode', name: '제품 상세' },
          ],
        },
        { path: '/account', name: '거래처', entity: 'account', operation: 'READ' },
        {
          path: '/settings/employees',
          name: '사원',
          entity: 'employee',
          operation: 'READ',
          subRoutes: [
            {
              path: '/settings/admin-accounts/new',
              name: '관리자 계정 등록',
              allowedProfileNames: ['시스템 관리자'],
            },
          ],
        },
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
      name: '모바일앱',
      icon: <MobileOutlined />,
      children: [
        { path: '/admin/agreement-words', name: '동의 약관 등록', entity: 'agreement_word', operation: 'READ' },
      ],
    },
    {
      name: '시스템',
      icon: <SettingOutlined />,
      children: [
        {
          path: '/users',
          name: '사용자 관리',
          entity: 'user',
          operation: 'READ',
          subRoutes: [
            { path: '/users/:id', name: '사용자 상세', entity: 'user', operation: 'READ' },
          ],
        },
        {
          path: '/admin/permissions/profiles',
          name: '프로파일 관리',
          entity: 'profile',
          operation: 'READ',
          subRoutes: [
            { path: '/admin/permissions/profiles/:profileId', name: '프로파일 상세', entity: 'profile', operation: 'READ' },
          ],
        },
        {
          path: '/admin/permissions/permission-sets',
          name: '권한 세트 관리',
          entity: 'permission_set',
          operation: 'READ',
          subRoutes: [
            { path: '/admin/permissions/permission-sets/:permissionSetId', name: '권한 세트 상세', entity: 'permission_set', operation: 'READ' },
          ],
        },
        { path: '/admin/permissions/matrix', name: '권한 매트릭스', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/permissions/page-access-guide', name: '페이지별 필요 권한', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/user-roles', name: '역할 (조직 계층)', entity: 'user_role', operation: 'READ' },
        { path: '/admin/permissions/guide', name: '권한 사용 가이드', allowedProfileNames: ['시스템 관리자'] },
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
        { path: '/admin/tools/cache', name: 'Redis 캐시 관리', systemPermission: 'MODIFY_ALL_DATA' },
      ],
    },
  ],
};
