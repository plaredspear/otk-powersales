import type { ReactNode } from 'react';
import {
  BarChartOutlined,
  TeamOutlined,
  CalendarOutlined,
  BulbOutlined,
  UsergroupAddOutlined,
  DatabaseOutlined,
  SearchOutlined,
  NotificationOutlined,
  SettingOutlined,
  ToolOutlined,
  MobileOutlined,
  ReadOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import type { SfEntityOperation, SfSystemPermission } from '@/hooks/usePermission';

export interface MenuItem {
  path?: string;
  name: string;
  /**
   * ProLayout submenu 식별 key. 지정 시 ProLayout 이 객체 해시 대신 이 값을 그대로 쓴다
   * (@umijs/route-utils transformRoute: `key: item.key || getKeyByPath(item)`).
   * 사이드바 검색 강제 펼침에서 openKeys 를 외부에서 안정적으로 지정하기 위해 카테고리에 부여한다.
   */
  key?: string;
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
      name: '근무배치 마스터',
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
        { path: '/display-work-schedules', name: '진열스케줄마스터', entity: 'display_work_schedule', operation: 'READ' },
      ],
    },
    {
      name: '전문행사조',
      icon: <UsergroupAddOutlined />,
      children: [
        { path: '/promotion/ppt-masters', name: '전문행사조', entity: 'professional_promotion_team_master', operation: 'READ' },
        { path: '/promotion/ppt-master-history', name: '전문행사조 이력', entity: 'professional_promotion_team_master', operation: 'READ' },
      ],
    },
    {
      name: '여사원 일정',
      icon: <CalendarOutlined />,
      children: [
        { path: '/schedule', name: '여사원 일정관리', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/monthly-integration', name: '월별여사원 통합일정', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/work-type-headcount', name: '근무형태별 여사원인원현황', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/monthly-input-adequacy', name: '월별 진열사원 투입적합성', entity: 'monthly_sales_history', operation: 'READ' },
        { path: '/deployment', name: '진열사원 배치 적합성', entity: 'monthly_sales_history', operation: 'READ' },
      ],
    },
    {
      name: '인사/근무',
      icon: <TeamOutlined />,
      children: [
        {
          path: '/female-employee',
          name: '여사원 현황',
          entity: 'female_employee',
          operation: 'READ',
          subRoutes: [
            { path: '/female-employee/:employeeId', name: '여사원 상세', entity: 'female_employee', operation: 'READ' },
          ],
        },
        { path: '/attend-info', name: '근무기간 조회', entity: 'attend_info', operation: 'READ' },
        { path: '/work-history-period', name: '기간별 근무내역', entity: 'attend_info', operation: 'READ' },
      ],
    },
    {
      name: '매출/실적',
      icon: <BarChartOutlined />,
      children: [
        { path: '/sales/monthly', name: '월 매출(물류배부)', entity: 'monthly_sales_history', operation: 'READ' },
        { path: '/sales/electronic', name: '월 매출(전산실적)', entity: 'monthly_sales_history', operation: 'READ' },
        { path: '/sales/pos', name: 'POS매출', entity: 'monthly_sales_history', operation: 'READ' },
      ],
    },
    {
      name: '보고서',
      icon: <FileTextOutlined />,
      children: [
        {
          path: '/female-employee-placement-check',
          name: '여사원 배치 점검',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/female-employee-work-history',
          name: '여사원 근무내역',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/female-employee-safety-check-report',
          name: '판매여사원 안전점검 현황',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/female-employee-safety-check-report-rpa',
          name: '판매여사원 안전점검 현황 (RPA)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/claim-period-report-packaging',
          name: '기간별 클레임 (포장불량)',
          entity: 'claim',
          operation: 'READ',
        },
        {
          path: '/claim-period-report-all',
          name: '기간별 클레임 (모든 클레임)',
          entity: 'claim',
          operation: 'READ',
        },
        {
          path: '/logistics-claim-report-period',
          name: '물류 클레임 (기간별)',
          entity: 'suggestion',
          operation: 'READ',
        },
        {
          path: '/logistics-claim-report-this-month',
          name: '물류 클레임 (당월)',
          entity: 'suggestion',
          operation: 'READ',
        },
        {
          path: '/logistics-claim-report-last-month',
          name: '물류 클레임 (전월)',
          entity: 'suggestion',
          operation: 'READ',
        },
        {
          path: '/promotion-target-actual-report',
          name: '행사사원 목표 대비 실적',
          entity: 'promotion',
          operation: 'READ',
        },
        {
          path: '/ppt-confirmed-members-report',
          name: '전문행사조 확정 인원',
          entity: 'professional_promotion_team_master',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-permanent-temp-all',
          name: '거래처유형별 환산인원 (상시·임시 전체)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-permanent-excl-consign',
          name: '거래처유형별 환산인원 (상시, 위탁농협 제외)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-temp-all',
          name: '거래처유형별 환산인원 (임시 전체)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-temp-excl-consign',
          name: '거래처유형별 환산인원 (임시 전체, 위탁농협 제외)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-team2-permanent-temp-all',
          name: '(2팀) 거래처유형별 환산인원 (상시·임시 전체)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-agency-permanent-temp-all',
          name: '대리점 환산인원 (상시·임시 전체)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-agency-permanent-only',
          name: '대리점 환산인원 (only 상시)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-agency-temp-only',
          name: '대리점 환산인원 (only 임시)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-hypermarket-permanent',
          name: '대형마트 환산인원 (상시)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-hypermarket-permanent-wc3',
          name: '대형마트 환산인원 (상시, 근무유형3 추가)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-segmented',
          name: '세분화 거래처유형별 환산인원',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/converted-headcount-report-team2-split-check',
          name: '거래처유형별 환산인원 (상시·임시, 영업지원2팀 분리) 확인용',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
        {
          path: '/valid-employee-confirmed-report',
          name: '진열사원 유효사원 (확정)',
          entity: 'team_member_schedule',
          operation: 'READ',
        },
      ],
    },
    {
      name: '현장 점검/이슈',
      icon: <SearchOutlined />,
      children: [
        { path: '/inspection-themes', name: '현장점검 테마 관리', entity: 'inspection_theme', operation: 'READ' },
        { path: '/field-inspection', name: '현장점검', entity: 'site_activity', operation: 'READ' },
        { path: '/safety-check', name: '안전점검', entity: 'team_member_schedule', operation: 'READ' },
        { path: '/product-expiration', name: '소비기한 관리', entity: 'product', operation: 'READ' },
        {
          path: '/claims',
          name: '제품 클레임',
          subRoutes: [
            { path: '/claims/new', name: '제품 클레임 등록' },
            { path: '/claims/:claimId', name: '제품 클레임 상세' },
          ],
        },
        {
          path: '/proposal',
          name: '제안사항',
          entity: 'suggestion',
          operation: 'READ',
          subRoutes: [
            { path: '/proposal/new', name: '제안사항 등록' },
            { path: '/proposal/:id', name: '제안사항 상세' },
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
        { path: '/admin/working-day-masters', name: '영업일관리마스터', entity: 'working_day_master', operation: 'READ' },
        { path: '/settings/alternative-holidays', name: '대체휴무' },
        {
          path: '/settings/employee-input-criteria-masters',
          name: '진열사원 투입기준 마스터',
          entity: 'employee_input_criteria_master',
          operation: 'READ',
        },
        {
          path: '/sales-progress-rate-masters',
          name: '거래처목표등록마스터',
          entity: 'sales_progress_rate_master',
          operation: 'READ',
          subRoutes: [
            {
              path: '/sales-progress-rate-masters/:id',
              name: '거래처목표등록마스터 상세',
              entity: 'sales_progress_rate_master',
              operation: 'READ',
            },
          ],
        },
        { path: '/attendance', name: '근무 등록현황', entity: 'attendance_log', operation: 'READ' },
      ],
    },
    {
      name: '모바일앱',
      icon: <MobileOutlined />,
      children: [
        { path: '/admin/agreement-words', name: '동의 약관 등록', entity: 'agreement_word', operation: 'READ' },
        { path: '/admin/app-packages', name: '앱 버전 관리', systemPermission: 'MODIFY_ALL_DATA' },
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
            { path: '/admin/permissions/permission-sets/new', name: '권한 세트 등록', systemPermission: 'MANAGE_USERS' },
            { path: '/admin/permissions/permission-sets/:permissionSetId/edit', name: '권한 세트 편집', systemPermission: 'MANAGE_USERS' },
          ],
        },
        { path: '/admin/permissions/matrix', name: '권한 매트릭스', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/permissions/page-access-guide', name: '페이지별 필요 권한', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/user-roles', name: '역할 (조직 계층)', entity: 'user_role', operation: 'READ' },
      ],
    },
    {
      name: '개발자 도구',
      icon: <ToolOutlined />,
      children: [
        { path: '/admin/tools/dashboard', name: '대시보드', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/tools/external-api', name: '외부 API 테스트', systemPermission: 'MODIFY_ALL_DATA' },
        { path: '/admin/tools/scheduled-jobs', name: '스케줄 잡', systemPermission: 'VIEW_ALL_DATA' },
        { path: '/admin/tools/sap-integration', name: 'SAP 연동', systemPermission: 'VIEW_ALL_DATA' },
        // SF Migration Stage 1/2 는 런칭 전 일회성 운영 도구 — 권한 부트스트랩 닭-달걀 회피 위해
        // 사이드 메뉴에서 제외하고 URL 직접 진입으로만 노출 (라우트 가드도 제거, backend 는 로그인만 요구).
        // 직접 진입 URL: Stage 1 적재 = /admin/tools/sf-migration-1, Stage 2 FK = /admin/tools/sf-migration-2.
        { path: '/admin/tools/cache', name: 'Redis 캐시 관리', systemPermission: 'MODIFY_ALL_DATA' },
        { path: '/admin/permissions/guide', name: '권한 사용 가이드', allowedProfileNames: ['시스템 관리자'] },
      ],
    },
    {
      name: '시스템 안내',
      icon: <ReadOutlined />,
      children: [
        { path: '/admin/docs', name: '시스템 안내 홈', allowedProfileNames: ['시스템 관리자'] },
        { path: '/admin/docs/overview', name: '시스템 개요', allowedProfileNames: ['시스템 관리자'] },
        { path: '/admin/docs/domains', name: '도메인/모듈 맵', allowedProfileNames: ['시스템 관리자'] },
        { path: '/admin/docs/api', name: 'API 카탈로그', allowedProfileNames: ['시스템 관리자'] },
        { path: '/admin/docs/flows', name: '데이터 흐름(DFD)', allowedProfileNames: ['시스템 관리자'] },
      ],
    },
  ],
};

/** 메뉴 검색어 정규화: 공백 제거 + 소문자화. 검색창과 필터가 동일 규칙을 쓰도록 단일 소유. */
export function normalizeMenuKeyword(keyword: string): string {
  return keyword.replace(/\s+/g, '').toLowerCase();
}

/**
 * 메뉴 트리를 검색어로 필터링한다. (사이드바 검색 전용)
 * - 한글 부분일치 (대소문자/공백 무시). 매칭 항목 + 매칭된 자식을 가진 부모 카테고리를 유지한다.
 * - subRoutes 는 사이드바 비노출이므로 검색 대상에서 제외한다.
 * - 부모 자신의 name 이 매칭되면 자식 전체를 그대로 보존한다 (카테고리 단위 탐색).
 */
/** 카테고리(자식 보유 항목)에 부여할 안정 submenu key. name 은 메뉴 SoT 에서 고유하다. */
function menuCategoryKey(item: MenuItem): string {
  return item.key ?? item.path ?? `cat:${item.name}`;
}

export function filterMenuByKeyword(items: MenuItem[], keyword: string): MenuItem[] {
  const normalized = normalizeMenuKeyword(keyword);
  if (!normalized) return items;
  const matches = (name: string) => normalizeMenuKeyword(name).includes(normalized);

  const walk = (list: MenuItem[]): MenuItem[] =>
    list.reduce<MenuItem[]>((acc, item) => {
      const selfMatch = matches(item.name);
      if (item.children) {
        // 부모 자신이 매칭되면 자식 전체 보존, 아니면 매칭된 자식만 남긴다.
        const nextChildren = selfMatch ? item.children : walk(item.children);
        if (selfMatch || nextChildren.length > 0) {
          // 안정 key 부여 → ProLayout 이 해시 대신 이 key 로 submenu 를 식별, openKeys 제어 가능.
          acc.push({ ...item, key: menuCategoryKey(item), children: nextChildren });
        }
      } else if (selfMatch) {
        acc.push(item);
      }
      return acc;
    }, []);

  return walk(items);
}

/**
 * 메뉴 트리에서 자식을 가진 카테고리들의 submenu key 목록을 모은다 (검색 시 강제 펼침용).
 * filterMenuByKeyword 가 부여한 것과 동일한 key 규칙을 사용한다.
 */
export function collectMenuOpenKeys(items: MenuItem[]): string[] {
  return items.reduce<string[]>((acc, item) => {
    if (item.children && item.children.length > 0) {
      acc.push(menuCategoryKey(item));
      acc.push(...collectMenuOpenKeys(item.children));
    }
    return acc;
  }, []);
}
