import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import ProtectedRoute from '@/components/ProtectedRoute';
import PermissionRoute from '@/components/PermissionRoute';
import RoleRoute from '@/components/RoleRoute';
import AdminLayout from '@/layouts/AdminLayout';

const LoginPage = lazy(() => import('@/pages/LoginPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const SalesQueryPage = lazy(() => import('@/pages/SalesQueryPage'));
const MonthlySalesDashboardPage = lazy(() => import('@/pages/MonthlySalesDashboardPage'));
const SchedulePage = lazy(() => import('@/pages/schedule/SchedulePage'));
const DeploymentPage = lazy(() => import('@/pages/DeploymentPage'));
const AttendancePage = lazy(() => import('@/pages/AttendancePage'));
const ClaimListPage = lazy(() => import('@/pages/claims/ClaimListPage'));
const ClaimDetailPage = lazy(() => import('@/pages/claims/ClaimDetailPage'));
const ClaimCreatePage = lazy(() => import('@/pages/claims/ClaimCreatePage'));
const SuggestionListPage = lazy(() => import('@/pages/suggestion/SuggestionListPage'));
const SuggestionDetailPage = lazy(() => import('@/pages/suggestion/SuggestionDetailPage'));
const SuggestionCreatePage = lazy(() => import('@/pages/suggestion/SuggestionCreatePage'));
const LeavePage = lazy(() => import('@/pages/LeavePage'));
const SafetyCheckPage = lazy(() => import('@/pages/SafetyCheckPage'));
const ProductPage = lazy(() => import('@/pages/ProductPage'));
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage'));
const AccountPage = lazy(() => import('@/pages/AccountPage'));
const EmployeePage = lazy(() => import('@/pages/EmployeePage'));
const EmployeeListPage = lazy(() => import('@/pages/settings/EmployeeListPage'));
const EmployeeDetailPage = lazy(() => import('@/pages/EmployeeDetailPage'));
const FieldInspectionPage = lazy(() => import('@/pages/FieldInspectionPage'));
const ReportPage = lazy(() => import('@/pages/ReportPage'));
const NoticeListPage = lazy(() => import('@/pages/notice/NoticeListPage'));
const NoticeDetailPage = lazy(() => import('@/pages/notice/NoticeDetailPage'));
const NoticeFormPage = lazy(() => import('@/pages/notice/NoticeFormPage'));
const PromotionListPage = lazy(() => import('@/pages/promotion/PromotionListPage'));
const PromotionDetailPage = lazy(() => import('@/pages/promotion/PromotionDetailPage'));
const PromotionFormPage = lazy(() => import('@/pages/promotion/PromotionFormPage'));
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
const PPTMasterPage = lazy(() => import('@/pages/promotion/PPTMasterPage'));
const PPTHistoryPage = lazy(() => import('@/pages/promotion/PPTHistoryPage'));
const ProductExpirationPage = lazy(() => import('@/pages/ProductExpirationPage'));
const AdminAccountRegisterPage = lazy(() => import('@/pages/settings/AdminAccountRegisterPage'));
const NaverGeocodeTestPage = lazy(() => import('@/pages/admin/NaverGeocodeTestPage'));
const ScheduledJobsPage = lazy(() => import('@/pages/admin/scheduled-jobs/ScheduledJobsPage'));
const SapInboundPage = lazy(() => import('@/pages/admin/tools/sap-inbound/SapInboundPage'));
const SapOutboundPage = lazy(() => import('@/pages/admin/tools/sap-outbound/SapOutboundPage'));
const SfMigrationPage = lazy(() => import('@/pages/admin/tools/sf-migration/SfMigrationPage'));
const SfMigrationStage1Page = lazy(() => import('@/pages/admin/tools/sf-migration-stage1/SfMigrationStage1Page'));
const CacheManagementPage = lazy(() => import('@/pages/admin/cache/CacheManagementPage'));
const AgreementWordsPage = lazy(() => import('@/pages/admin/agreement-words/AgreementWordsPage'));
const UserListPage = lazy(() => import('@/pages/users/UserListPage'));
const UserDetailPage = lazy(() => import('@/pages/users/UserDetailPage'));
const ProfileListPage = lazy(() => import('@/pages/admin/permissions/ProfileListPage'));
const ProfileDetailPage = lazy(() => import('@/pages/admin/permissions/ProfileDetailPage'));
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

// eslint-disable-next-line react-refresh/only-export-components
function LazyWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Suspense
      fallback={
        <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      }
    >
      {children}
    </Suspense>
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
    },
    {
      element: <ProtectedRoute />,
      children: [
        {
          element: <AdminLayout />,
          children: [
            { path: '/', element: <LazyWrapper><DashboardPage /></LazyWrapper> },
            {
              element: <PermissionRoute entity="monthly_sales_history" operation="READ" />,
              children: [
                { path: '/sales/monthly', element: <LazyWrapper><MonthlySalesDashboardPage /></LazyWrapper> },
              ],
            },
            { path: '/sales/electronic', element: <LazyWrapper><SalesQueryPage /></LazyWrapper> },
            { path: '/sales/pos', element: <LazyWrapper><SalesQueryPage /></LazyWrapper> },
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
            { path: '/leave', element: <LazyWrapper><LeavePage /></LazyWrapper> },
            { path: '/product', element: <LazyWrapper><ProductPage /></LazyWrapper> },
            { path: '/product/:productCode', element: <LazyWrapper><ProductDetailPage /></LazyWrapper> },
            { path: '/field-inspection', element: <LazyWrapper><FieldInspectionPage /></LazyWrapper> },
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
              element: <PermissionRoute entity="promotion" operation="EDIT" />,
              children: [
                { path: '/display-schedules', element: <LazyWrapper><DisplaySchedulePage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute entity="employee" operation="READ" />,
              children: [
                { path: '/employee', element: <LazyWrapper><EmployeePage /></LazyWrapper> },
                { path: '/employee/:employeeId', element: <LazyWrapper><EmployeeDetailPage /></LazyWrapper> },
                { path: '/settings/employees', element: <LazyWrapper><EmployeeListPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute entity="team_member_schedule" operation="READ" />,
              children: [
                { path: '/safety-check', element: <LazyWrapper><SafetyCheckPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute entity="account" operation="READ" />,
              children: [
                { path: '/account', element: <LazyWrapper><AccountPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute systemPermission="MODIFY_ALL_DATA" />,
              children: [
                { path: '/admin/tools/naver-geocode', element: <LazyWrapper><NaverGeocodeTestPage /></LazyWrapper> },
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
                { path: '/admin/tools/sap-inbound', element: <LazyWrapper><SapInboundPage /></LazyWrapper> },
                { path: '/admin/tools/sap-outbound', element: <LazyWrapper><SapOutboundPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute systemPermission="MODIFY_ALL_DATA" />,
              children: [
                { path: '/admin/tools/sf-migration', element: <LazyWrapper><SfMigrationPage /></LazyWrapper> },
                { path: '/admin/tools/sf-migration-stage1', element: <LazyWrapper><SfMigrationStage1Page /></LazyWrapper> },
                { path: '/admin/tools/cache', element: <LazyWrapper><CacheManagementPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute entity="agreement_word" operation="READ" />,
              children: [
                { path: '/admin/agreement-words', element: <LazyWrapper><AgreementWordsPage /></LazyWrapper> },
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
              ],
            },
            { path: '*', element: <Navigate to="/" replace /> },
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
