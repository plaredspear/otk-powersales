import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import ProtectedRoute from '@/components/ProtectedRoute';
import RouteErrorBoundary from '@/components/RouteErrorBoundary';
import PageSpinner from '@/components/common/PageSpinner';
import PermissionRoute from '@/components/PermissionRoute';
import RoleRoute from '@/components/RoleRoute';
import AdminLayout from '@/layouts/AdminLayout';

const LoginPage = lazy(() => import('@/pages/LoginPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const SalesQueryPage = lazy(() => import('@/pages/SalesQueryPage'));
const MonthlySalesDashboardPage = lazy(() => import('@/pages/MonthlySalesDashboardPage'));
const ElectronicSalesDashboardPage = lazy(() => import('@/pages/ElectronicSalesDashboardPage'));
const SchedulePage = lazy(() => import('@/pages/schedule/SchedulePage'));
const DeploymentPage = lazy(() => import('@/pages/DeploymentPage'));
const AttendancePage = lazy(() => import('@/pages/AttendancePage'));
const ClaimListPage = lazy(() => import('@/pages/claims/ClaimListPage'));
const ClaimDetailPage = lazy(() => import('@/pages/claims/ClaimDetailPage'));
const ClaimCreatePage = lazy(() => import('@/pages/claims/ClaimCreatePage'));
const SuggestionListPage = lazy(() => import('@/pages/suggestion/SuggestionListPage'));
const SuggestionDetailPage = lazy(() => import('@/pages/suggestion/SuggestionDetailPage'));
const SuggestionCreatePage = lazy(() => import('@/pages/suggestion/SuggestionCreatePage'));
const ProposalListPage = lazy(() => import('@/pages/proposal/ProposalListPage'));
const ProposalDetailPage = lazy(() => import('@/pages/proposal/ProposalDetailPage'));
const ProposalCreatePage = lazy(() => import('@/pages/proposal/ProposalCreatePage'));
const LeavePage = lazy(() => import('@/pages/LeavePage'));
const SafetyCheckPage = lazy(() => import('@/pages/SafetyCheckPage'));
const ProductPage = lazy(() => import('@/pages/ProductPage'));
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage'));
const AccountPage = lazy(() => import('@/pages/AccountPage'));
const AccountDetailPage = lazy(() => import('@/pages/AccountDetailPage'));
const EmployeePage = lazy(() => import('@/pages/EmployeePage'));
const EmployeeListPage = lazy(() => import('@/pages/settings/EmployeeListPage'));
const EmployeeDetailPage = lazy(() => import('@/pages/EmployeeDetailPage'));
const FieldInspectionPage = lazy(() => import('@/pages/FieldInspectionPage'));
const ThemeManagementPage = lazy(() => import('@/pages/ThemeManagementPage'));
const ReportPage = lazy(() => import('@/pages/ReportPage'));
const NoticeListPage = lazy(() => import('@/pages/notice/NoticeListPage'));
const NoticeDetailPage = lazy(() => import('@/pages/notice/NoticeDetailPage'));
const NoticeFormPage = lazy(() => import('@/pages/notice/NoticeFormPage'));
const PromotionListPage = lazy(() => import('@/pages/promotion/PromotionListPage'));
const PromotionDetailPage = lazy(() => import('@/pages/promotion/PromotionDetailPage'));
const PromotionFormPage = lazy(() => import('@/pages/promotion/PromotionFormPage'));
const SalesProgressRateMasterListPage = lazy(
  () => import('@/pages/sales-progress-rate-master/SalesProgressRateMasterListPage'),
);
const SalesProgressRateMasterDetailPage = lazy(
  () => import('@/pages/sales-progress-rate-master/SalesProgressRateMasterDetailPage'),
);
const EducationListPage = lazy(() => import('@/pages/education/EducationListPage'));
const EducationDetailPage = lazy(() => import('@/pages/education/EducationDetailPage'));
const EducationFormPage = lazy(() => import('@/pages/education/EducationFormPage'));
const DisplaySchedulePage = lazy(() => import('@/pages/DisplaySchedulePage'));
const OrganizationPage = lazy(() => import('@/pages/settings/OrganizationPage'));
const AlternativeHolidayPage = lazy(() => import('@/pages/alternative-holidays/AlternativeHolidayPage'));
const HolidayMasterListPage = lazy(() => import('@/pages/holiday-masters/HolidayMasterListPage'));
const EmployeeInputCriteriaMasterListPage = lazy(
  () => import('@/pages/employee-input-criteria-masters/EmployeeInputCriteriaMasterListPage'),
);
const AttendInfoPage = lazy(() => import('@/pages/attend-info/AttendInfoPage'));
const MonthlyIntegrationSchedulePage = lazy(() => import('@/pages/schedules/MonthlyIntegrationSchedulePage'));
const CategorySchedulePage = lazy(() => import('@/pages/schedules/CategorySchedulePage'));
const MonthlyInputAdequacyPage = lazy(() => import('@/pages/MonthlyInputAdequacyPage'));
const FemaleEmployeePlacementCheckPage = lazy(() => import('@/pages/FemaleEmployeePlacementCheckPage'));
const FemaleEmployeeWorkHistoryPage = lazy(() => import('@/pages/FemaleEmployeeWorkHistoryPage'));
const FemaleEmployeeSafetyCheckReportPage = lazy(() => import('@/pages/FemaleEmployeeSafetyCheckReportPage'));
const FemaleEmployeeSafetyCheckReportRpaPage = lazy(() => import('@/pages/FemaleEmployeeSafetyCheckReportRpaPage'));
const ClaimPeriodReportPage = lazy(() => import('@/pages/ClaimPeriodReportPage'));
const LogisticsClaimReportPage = lazy(() => import('@/pages/LogisticsClaimReportPage'));
const PromotionTargetActualReportPage = lazy(() => import('@/pages/PromotionTargetActualReportPage'));
const PptConfirmedReportPage = lazy(() => import('@/pages/PptConfirmedReportPage'));
const ConvertedHeadcountReportPage = lazy(() => import('@/pages/ConvertedHeadcountReportPage'));
const ValidEmployeeConfirmedReportPage = lazy(() => import('@/pages/ValidEmployeeConfirmedReportPage'));
const PPTMasterPage = lazy(() => import('@/pages/promotion/PPTMasterPage'));
const PPTHistoryPage = lazy(() => import('@/pages/promotion/PPTHistoryPage'));
const ProductExpirationPage = lazy(() => import('@/pages/ProductExpirationPage'));
const AdminAccountRegisterPage = lazy(() => import('@/pages/settings/AdminAccountRegisterPage'));
const NaverGeocodeTestPage = lazy(() => import('@/pages/admin/NaverGeocodeTestPage'));
const ExternalApiTestPage = lazy(() => import('@/pages/admin/tools/external-api/ExternalApiTestPage'));
const AppPackagePage = lazy(() => import('@/pages/admin/app-packages/AppPackagePage'));
const ScheduledJobsPage = lazy(() => import('@/pages/admin/scheduled-jobs/ScheduledJobsPage'));
const SapIntegrationPage = lazy(() => import('@/pages/admin/tools/sap-integration/SapIntegrationPage'));
const SfMigrationPage = lazy(() => import('@/pages/admin/tools/sf-migration/SfMigrationPage'));
const SfMigrationStage1Page = lazy(() => import('@/pages/admin/tools/sf-migration-stage1/SfMigrationStage1Page'));
const HerokuMigrationPage = lazy(() => import('@/pages/admin/tools/heroku-migration/HerokuMigrationPage'));
const HerokuMigrationStage1Page = lazy(() => import('@/pages/admin/tools/heroku-migration-stage1/HerokuMigrationStage1Page'));
const CacheManagementPage = lazy(() => import('@/pages/admin/cache/CacheManagementPage'));
const AgreementWordsPage = lazy(() => import('@/pages/admin/agreement-words/AgreementWordsPage'));
const WorkingDayMastersPage = lazy(() => import('@/pages/admin/working-day-masters/WorkingDayMastersPage'));
const UserListPage = lazy(() => import('@/pages/users/UserListPage'));
const UserDetailPage = lazy(() => import('@/pages/users/UserDetailPage'));
const ProfileListPage = lazy(() => import('@/pages/admin/permissions/ProfileListPage'));
const ProfileDetailPage = lazy(() => import('@/pages/admin/permissions/ProfileDetailPage'));
const ProfileEditPage = lazy(() => import('@/pages/admin/permissions/ProfileEditPage'));
const PermissionSetListPage = lazy(() => import('@/pages/admin/permissions/PermissionSetListPage'));
const PermissionSetDetailPage = lazy(() => import('@/pages/admin/permissions/PermissionSetDetailPage'));
const PermissionSetCreatePage = lazy(() => import('@/pages/admin/permissions/PermissionSetCreatePage'));
const PermissionSetEditPage = lazy(() => import('@/pages/admin/permissions/PermissionSetEditPage'));
const PermissionMatrixPage = lazy(() => import('@/pages/admin/permissions/PermissionMatrixPage'));
const PageAccessGuidePage = lazy(() => import('@/pages/admin/permissions/PageAccessGuidePage'));
const PermissionGuidePage = lazy(() => import('@/pages/admin/permissions/PermissionGuidePage'));
const UserRoleTreePage = lazy(() => import('@/pages/admin/user-roles/UserRoleTreePage'));
const SystemDocsHomePage = lazy(() => import('@/pages/admin/docs/SystemDocsHomePage'));
const SystemOverviewPage = lazy(() => import('@/pages/admin/docs/SystemOverviewPage'));
const DomainMapPage = lazy(() => import('@/pages/admin/docs/DomainMapPage'));
const ApiCatalogPage = lazy(() => import('@/pages/admin/docs/ApiCatalogPage'));
const DataFlowPage = lazy(() => import('@/pages/admin/docs/DataFlowPage'));

// eslint-disable-next-line react-refresh/only-export-components
function LazyWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<PageSpinner />}>{children}</Suspense>
  );
}

export const router = createBrowserRouter(
  [
    {
      path: '/login',
      element: (
        <LazyWrapper>
          <LoginPage />
        </LazyWrapper>
      ),
      errorElement: <RouteErrorBoundary />,
    },
    {
      element: <ProtectedRoute />,
      errorElement: <RouteErrorBoundary />,
      children: [
        {
          element: <AdminLayout />,
          children: [
            // pathless layout route: 하위 페이지 청크 로드 실패 등의 에러를
            // AdminLayout 의 <Outlet> 자리(콘텐츠 영역)에서만 처리하여 사이드바 등 레이아웃은 유지한다.
            {
              errorElement: <RouteErrorBoundary />,
              children: [
                { path: '/', element: <LazyWrapper><DashboardPage /></LazyWrapper> },
                {
                  element: <PermissionRoute entity="monthly_sales_history" operation="READ" />,
                  children: [
                    { path: '/sales/monthly', element: <LazyWrapper><MonthlySalesDashboardPage /></LazyWrapper> },
                    { path: '/sales/electronic', element: <LazyWrapper><ElectronicSalesDashboardPage /></LazyWrapper> },
                    { path: '/sales/pos', element: <LazyWrapper><SalesQueryPage /></LazyWrapper> },
                  ],
                },
                { path: '/schedule', element: <LazyWrapper><SchedulePage /></LazyWrapper> },
                { path: '/deployment', element: <LazyWrapper><DeploymentPage /></LazyWrapper> },
                {
                  element: <PermissionRoute entity="attendance_log" operation="READ" />,
                  children: [
                    { path: '/attendance', element: <LazyWrapper><AttendancePage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="attend_info" operation="READ" />,
                  children: [
                    { path: '/attend-info', element: <LazyWrapper><AttendInfoPage /></LazyWrapper> },
                  ],
                },
                { path: '/claims', element: <LazyWrapper><ClaimListPage /></LazyWrapper> },
                { path: '/claims/new', element: <LazyWrapper><ClaimCreatePage /></LazyWrapper> },
                { path: '/claims/:claimId', element: <LazyWrapper><ClaimDetailPage /></LazyWrapper> },
                { path: '/suggestion', element: <LazyWrapper><SuggestionListPage /></LazyWrapper> },
                { path: '/suggestion/new', element: <LazyWrapper><SuggestionCreatePage /></LazyWrapper> },
                { path: '/suggestion/:id', element: <LazyWrapper><SuggestionDetailPage /></LazyWrapper> },
                { path: '/proposal', element: <LazyWrapper><ProposalListPage /></LazyWrapper> },
                { path: '/proposal/new', element: <LazyWrapper><ProposalCreatePage /></LazyWrapper> },
                { path: '/proposal/:id', element: <LazyWrapper><ProposalDetailPage /></LazyWrapper> },
                { path: '/leave', element: <LazyWrapper><LeavePage /></LazyWrapper> },
                { path: '/product', element: <LazyWrapper><ProductPage /></LazyWrapper> },
                { path: '/product/:productCode', element: <LazyWrapper><ProductDetailPage /></LazyWrapper> },
                { path: '/field-inspection', element: <LazyWrapper><FieldInspectionPage /></LazyWrapper> },
                { path: '/inspection-themes', element: <LazyWrapper><ThemeManagementPage /></LazyWrapper> },
                { path: '/report', element: <LazyWrapper><ReportPage /></LazyWrapper> },
                {
                  element: <PermissionRoute entity="notice" operation="READ" />,
                  children: [
                    { path: '/notices', element: <LazyWrapper><NoticeListPage /></LazyWrapper> },
                    { path: '/notices/:id', element: <LazyWrapper><NoticeDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="notice" operation="CREATE" />,
                  children: [
                    { path: '/notices/new', element: <LazyWrapper><NoticeFormPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="notice" operation="EDIT" />,
                  children: [
                    { path: '/notices/:id/edit', element: <LazyWrapper><NoticeFormPage /></LazyWrapper> },
                  ],
                },
                { path: '/education', element: <LazyWrapper><EducationListPage /></LazyWrapper> },
                { path: '/education/new', element: <LazyWrapper><EducationFormPage /></LazyWrapper> },
                { path: '/education/:id', element: <LazyWrapper><EducationDetailPage /></LazyWrapper> },
                { path: '/education/:id/edit', element: <LazyWrapper><EducationFormPage /></LazyWrapper> },
                { path: '/alternative-holidays', element: <LazyWrapper><AlternativeHolidayPage /></LazyWrapper> },
                { path: '/settings/alternative-holidays', element: <LazyWrapper><AlternativeHolidayPage /></LazyWrapper> },
                { path: '/settings/organizations', element: <LazyWrapper><OrganizationPage /></LazyWrapper> },
                { path: '/settings/holiday-masters', element: <LazyWrapper><HolidayMasterListPage /></LazyWrapper> },
                { path: '/settings/employee-input-criteria-masters', element: <LazyWrapper><EmployeeInputCriteriaMasterListPage /></LazyWrapper> },
                {
                  element: <RoleRoute allowedProfileNames={['시스템 관리자']} />,
                  children: [
                    { path: '/settings/admin-accounts/new', element: <LazyWrapper><AdminAccountRegisterPage /></LazyWrapper> },
                  ],
                },
                { path: '/work-type-headcount', element: <LazyWrapper><CategorySchedulePage /></LazyWrapper> },
                { path: '/monthly-integration', element: <LazyWrapper><MonthlyIntegrationSchedulePage /></LazyWrapper> },
                {
                  element: <PermissionRoute entity="monthly_sales_history" operation="READ" />,
                  children: [
                    { path: '/monthly-input-adequacy', element: <LazyWrapper><MonthlyInputAdequacyPage /></LazyWrapper> },
                  ],
                },
                { path: '/promotion/ppt-masters', element: <LazyWrapper><PPTMasterPage /></LazyWrapper> },
                { path: '/promotion/ppt-master-history', element: <LazyWrapper><PPTHistoryPage /></LazyWrapper> },
                {
                  element: <PermissionRoute entity="product" operation="READ" />,
                  children: [
                    { path: '/product-expiration', element: <LazyWrapper><ProductExpirationPage /></LazyWrapper> },
                  ],
                },
                // Permission-guarded routes
                {
                  element: <PermissionRoute entity="promotion" operation="READ" />,
                  children: [
                    { path: '/promotions', element: <LazyWrapper><PromotionListPage /></LazyWrapper> },
                    { path: '/promotions/:id', element: <LazyWrapper><PromotionDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="promotion" operation="CREATE" />,
                  children: [
                    { path: '/promotions/new', element: <LazyWrapper><PromotionFormPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="promotion" operation="EDIT" />,
                  children: [
                    { path: '/promotions/:id/edit', element: <LazyWrapper><PromotionFormPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="sales_progress_rate_master" operation="READ" />,
                  children: [
                    { path: '/sales-progress-rate-masters', element: <LazyWrapper><SalesProgressRateMasterListPage /></LazyWrapper> },
                    { path: '/sales-progress-rate-masters/:id', element: <LazyWrapper><SalesProgressRateMasterDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="display_work_schedule" operation="READ" />,
                  children: [
                    { path: '/display-work-schedules', element: <LazyWrapper><DisplaySchedulePage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="female_employee" operation="READ" />,
                  children: [
                    { path: '/female-employee', element: <LazyWrapper><EmployeePage /></LazyWrapper> },
                    { path: '/female-employee/:employeeId', element: <LazyWrapper><EmployeeDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="employee" operation="READ" />,
                  children: [
                    { path: '/employee/:employeeId', element: <LazyWrapper><EmployeeDetailPage /></LazyWrapper> },
                    { path: '/settings/employees', element: <LazyWrapper><EmployeeListPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="team_member_schedule" operation="READ" />,
                  children: [
                    { path: '/safety-check', element: <LazyWrapper><SafetyCheckPage /></LazyWrapper> },
                    { path: '/female-employee-placement-check', element: <LazyWrapper><FemaleEmployeePlacementCheckPage /></LazyWrapper> },
                    { path: '/female-employee-work-history', element: <LazyWrapper><FemaleEmployeeWorkHistoryPage /></LazyWrapper> },
                    { path: '/female-employee-safety-check-report', element: <LazyWrapper><FemaleEmployeeSafetyCheckReportPage /></LazyWrapper> },
                    { path: '/female-employee-safety-check-report-rpa', element: <LazyWrapper><FemaleEmployeeSafetyCheckReportRpaPage /></LazyWrapper> },
                    { path: '/converted-headcount-report-permanent-temp-all', element: <LazyWrapper><ConvertedHeadcountReportPage variant="PERMANENT_TEMP_ALL" title="거래처유형별 환산인원 (상시·임시 전체)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-permanent-excl-consign', element: <LazyWrapper><ConvertedHeadcountReportPage variant="PERMANENT_ONLY_EXCL_CONSIGN" title="거래처유형별 환산인원 (상시, 위탁농협 제외)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-temp-all', element: <LazyWrapper><ConvertedHeadcountReportPage variant="TEMP_ALL" title="거래처유형별 환산인원 (임시 전체)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-temp-excl-consign', element: <LazyWrapper><ConvertedHeadcountReportPage variant="TEMP_ONLY_EXCL_CONSIGN" title="거래처유형별 환산인원 (임시 전체, 위탁농협 제외)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-team2-permanent-temp-all', element: <LazyWrapper><ConvertedHeadcountReportPage variant="TEAM2_PERMANENT_TEMP_ALL" title="(2팀) 거래처유형별 환산인원 (상시·임시 전체)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-agency-permanent-temp-all', element: <LazyWrapper><ConvertedHeadcountReportPage variant="AGENCY_PERMANENT_TEMP_ALL" title="대리점 환산인원 (상시·임시 전체)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-agency-permanent-only', element: <LazyWrapper><ConvertedHeadcountReportPage variant="AGENCY_PERMANENT_ONLY" title="대리점 환산인원 (only 상시)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-agency-temp-only', element: <LazyWrapper><ConvertedHeadcountReportPage variant="AGENCY_TEMP_ONLY" title="대리점 환산인원 (only 임시)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-hypermarket-permanent', element: <LazyWrapper><ConvertedHeadcountReportPage variant="HYPERMARKET_PERMANENT" title="대형마트 환산인원 (상시)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-hypermarket-permanent-wc3', element: <LazyWrapper><ConvertedHeadcountReportPage variant="HYPERMARKET_PERMANENT_WC3" title="대형마트 환산인원 (상시, 근무유형3 추가)" /></LazyWrapper> },
                    { path: '/converted-headcount-report-segmented', element: <LazyWrapper><ConvertedHeadcountReportPage variant="SEGMENTED_ALL" title="세분화 거래처유형별 환산인원" /></LazyWrapper> },
                    { path: '/converted-headcount-report-team2-split-check', element: <LazyWrapper><ConvertedHeadcountReportPage variant="TEAM2_SPLIT_CHECK" title="거래처유형별 환산인원 (상시·임시, 영업지원2팀 분리) 확인용" /></LazyWrapper> },
                    { path: '/valid-employee-confirmed-report', element: <LazyWrapper><ValidEmployeeConfirmedReportPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="claim" operation="READ" />,
                  children: [
                    { path: '/claim-period-report-packaging', element: <LazyWrapper><ClaimPeriodReportPage type="PACKAGING" /></LazyWrapper> },
                    { path: '/claim-period-report-all', element: <LazyWrapper><ClaimPeriodReportPage type="ALL" /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="suggestion" operation="READ" />,
                  children: [
                    { path: '/logistics-claim-report-period', element: <LazyWrapper><LogisticsClaimReportPage period="CUSTOM" /></LazyWrapper> },
                    { path: '/logistics-claim-report-this-month', element: <LazyWrapper><LogisticsClaimReportPage period="THIS_MONTH" /></LazyWrapper> },
                    { path: '/logistics-claim-report-last-month', element: <LazyWrapper><LogisticsClaimReportPage period="LAST_MONTH" /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="promotion" operation="READ" />,
                  children: [
                    { path: '/promotion-target-actual-report', element: <LazyWrapper><PromotionTargetActualReportPage /></LazyWrapper> },
                    { path: '/ppt-confirmed-members-report', element: <LazyWrapper><PptConfirmedReportPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="account" operation="READ" />,
                  children: [
                    { path: '/account', element: <LazyWrapper><AccountPage /></LazyWrapper> },
                    { path: '/account/:id', element: <LazyWrapper><AccountDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute systemPermission="MODIFY_ALL_DATA" />,
                  children: [
                    { path: '/admin/tools/external-api', element: <LazyWrapper><ExternalApiTestPage /></LazyWrapper> },
                    { path: '/admin/tools/naver-geocode', element: <LazyWrapper><NaverGeocodeTestPage /></LazyWrapper> },
                    { path: '/admin/app-packages', element: <LazyWrapper><AppPackagePage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute systemPermission="VIEW_ALL_DATA" />,
                  children: [
                    { path: '/admin/tools/scheduled-jobs', element: <LazyWrapper><ScheduledJobsPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute systemPermission="VIEW_ALL_DATA" />,
                  children: [
                    { path: '/admin/tools/sap-integration', element: <LazyWrapper><SapIntegrationPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute systemPermission="MODIFY_ALL_DATA" />,
                  children: [
                    { path: '/admin/tools/cache', element: <LazyWrapper><CacheManagementPage /></LazyWrapper> },
                  ],
                },
                // SF Migration Stage 1/2 — 런칭 전 일회성 운영 도구. 권한 부트스트랩 닭-달걀 회피 위해
                // PermissionRoute 가드 없이 ProtectedRoute(로그인) 아래 직접 배치 — 사이드 메뉴에서도 제외.
                // backend 도 @RequiresSfPermission 제거 (authenticated() 만 유지). 마이그레이션 완료 후 가드 복원 권장.
                // URL 은 기억·접근 용이성 위해 단축: Stage 1 적재 = sf-migration-1, Stage 2 FK = sf-migration-2.
                { path: '/admin/tools/sf-migration-2', element: <LazyWrapper><SfMigrationPage /></LazyWrapper> },
                { path: '/admin/tools/sf-migration-1', element: <LazyWrapper><SfMigrationStage1Page /></LazyWrapper> },
                { path: '/admin/tools/heroku-migration-2', element: <LazyWrapper><HerokuMigrationPage /></LazyWrapper> },
                { path: '/admin/tools/heroku-migration-1', element: <LazyWrapper><HerokuMigrationStage1Page /></LazyWrapper> },
                {
                  element: <PermissionRoute entity="agreement_word" operation="READ" />,
                  children: [
                    { path: '/admin/agreement-words', element: <LazyWrapper><AgreementWordsPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="working_day_master" operation="READ" />,
                  children: [
                    { path: '/admin/working-day-masters', element: <LazyWrapper><WorkingDayMastersPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="user" operation="READ" />,
                  children: [
                    { path: '/users', element: <LazyWrapper><UserListPage /></LazyWrapper> },
                    { path: '/users/:id', element: <LazyWrapper><UserDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="profile" operation="READ" />,
                  children: [
                    { path: '/admin/permissions/profiles', element: <LazyWrapper><ProfileListPage /></LazyWrapper> },
                    { path: '/admin/permissions/profiles/:profileId', element: <LazyWrapper><ProfileDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="permission_set" operation="READ" />,
                  children: [
                    { path: '/admin/permissions/permission-sets', element: <LazyWrapper><PermissionSetListPage /></LazyWrapper> },
                    { path: '/admin/permissions/permission-sets/:permissionSetId', element: <LazyWrapper><PermissionSetDetailPage /></LazyWrapper> },
                  ],
                },
                {
                  // Spec #837 — PS 등록/편집 페이지. MANAGE_USERS 가드 (PSA 부여와 동일 가드).
                  element: <PermissionRoute systemPermission="MANAGE_USERS" />,
                  children: [
                    { path: '/admin/permissions/permission-sets/new', element: <LazyWrapper><PermissionSetCreatePage /></LazyWrapper> },
                    { path: '/admin/permissions/permission-sets/:permissionSetId/edit', element: <LazyWrapper><PermissionSetEditPage /></LazyWrapper> },
                    { path: '/admin/permissions/profiles/:profileId/edit', element: <LazyWrapper><ProfileEditPage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute systemPermission="VIEW_ALL_DATA" />,
                  children: [
                    { path: '/admin/permissions/matrix', element: <LazyWrapper><PermissionMatrixPage /></LazyWrapper> },
                    { path: '/admin/permissions/page-access-guide', element: <LazyWrapper><PageAccessGuidePage /></LazyWrapper> },
                  ],
                },
                {
                  element: <PermissionRoute entity="user_role" operation="READ" />,
                  children: [
                    { path: '/admin/user-roles', element: <LazyWrapper><UserRoleTreePage /></LazyWrapper> },
                  ],
                },
                {
                  element: <RoleRoute allowedProfileNames={['시스템 관리자']} />,
                  children: [
                    { path: '/admin/permissions/guide', element: <LazyWrapper><PermissionGuidePage /></LazyWrapper> },
                    { path: '/admin/docs', element: <LazyWrapper><SystemDocsHomePage /></LazyWrapper> },
                    { path: '/admin/docs/overview', element: <LazyWrapper><SystemOverviewPage /></LazyWrapper> },
                    { path: '/admin/docs/domains', element: <LazyWrapper><DomainMapPage /></LazyWrapper> },
                    { path: '/admin/docs/api', element: <LazyWrapper><ApiCatalogPage /></LazyWrapper> },
                    { path: '/admin/docs/flows', element: <LazyWrapper><DataFlowPage /></LazyWrapper> },
                  ],
                },
                { path: '*', element: <Navigate to="/" replace /> },
              ],
            },
          ],
        },
      ],
    },
  ],
  {
    future: {
      v7_relativeSplatPath: true,
      v7_fetcherPersist: true,
      v7_normalizeFormMethod: true,
      v7_partialHydration: true,
      v7_skipActionErrorRevalidation: true,
    },
  },
);
