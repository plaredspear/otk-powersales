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
const SchedulePage = lazy(() => import('@/pages/schedule/SchedulePage'));
const DeploymentPage = lazy(() => import('@/pages/DeploymentPage'));
const AttendancePage = lazy(() => import('@/pages/AttendancePage'));
const ClaimListPage = lazy(() => import('@/pages/claims/ClaimListPage'));
const ClaimDetailPage = lazy(() => import('@/pages/claims/ClaimDetailPage'));
const SuggestionPage = lazy(() => import('@/pages/SuggestionPage'));
const LeavePage = lazy(() => import('@/pages/LeavePage'));
const SafetyCheckPage = lazy(() => import('@/pages/SafetyCheckPage'));
const ProductPage = lazy(() => import('@/pages/ProductPage'));
const AccountPage = lazy(() => import('@/pages/AccountPage'));
const EmployeePage = lazy(() => import('@/pages/EmployeePage'));
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
const MonthlyIntegrationSchedulePage = lazy(() => import('@/pages/schedules/MonthlyIntegrationSchedulePage'));
const CategorySchedulePage = lazy(() => import('@/pages/schedules/CategorySchedulePage'));
const MonthlyInputAdequacyPage = lazy(() => import('@/pages/MonthlyInputAdequacyPage'));
const PPTMasterPage = lazy(() => import('@/pages/promotion/PPTMasterPage'));
const PPTHistoryPage = lazy(() => import('@/pages/promotion/PPTHistoryPage'));
const PermissionMatrixPage = lazy(() => import('@/pages/settings/PermissionMatrixPage'));
const ProductExpirationPage = lazy(() => import('@/pages/ProductExpirationPage'));
const EmployeePermissionPage = lazy(() => import('@/pages/settings/EmployeePermissionPage'));
const AdminAccountRegisterPage = lazy(() => import('@/pages/settings/AdminAccountRegisterPage'));
const NaverGeocodeTestPage = lazy(() => import('@/pages/admin/NaverGeocodeTestPage'));
const ScheduledJobsPage = lazy(() => import('@/pages/admin/scheduled-jobs/ScheduledJobsPage'));
const AgreementWordsPage = lazy(() => import('@/pages/admin/agreement-words/AgreementWordsPage'));
const UserListPage = lazy(() => import('@/pages/users/UserListPage'));
const UserDetailPage = lazy(() => import('@/pages/users/UserDetailPage'));

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
            { path: '/sales/monthly', element: <LazyWrapper><SalesQueryPage /></LazyWrapper> },
            { path: '/sales/electronic', element: <LazyWrapper><SalesQueryPage /></LazyWrapper> },
            { path: '/sales/pos', element: <LazyWrapper><SalesQueryPage /></LazyWrapper> },
            { path: '/schedule', element: <LazyWrapper><SchedulePage /></LazyWrapper> },
            { path: '/deployment', element: <LazyWrapper><DeploymentPage /></LazyWrapper> },
            { path: '/attendance', element: <LazyWrapper><AttendancePage /></LazyWrapper> },
            { path: '/claims', element: <LazyWrapper><ClaimListPage /></LazyWrapper> },
            { path: '/claims/:claimId', element: <LazyWrapper><ClaimDetailPage /></LazyWrapper> },
            { path: '/suggestion', element: <LazyWrapper><SuggestionPage /></LazyWrapper> },
            { path: '/leave', element: <LazyWrapper><LeavePage /></LazyWrapper> },
            { path: '/product', element: <LazyWrapper><ProductPage /></LazyWrapper> },
            { path: '/field-inspection', element: <LazyWrapper><FieldInspectionPage /></LazyWrapper> },
            { path: '/report', element: <LazyWrapper><ReportPage /></LazyWrapper> },
            { path: '/notices', element: <LazyWrapper><NoticeListPage /></LazyWrapper> },
            { path: '/notices/new', element: <LazyWrapper><NoticeFormPage /></LazyWrapper> },
            { path: '/notices/:id', element: <LazyWrapper><NoticeDetailPage /></LazyWrapper> },
            { path: '/notices/:id/edit', element: <LazyWrapper><NoticeFormPage /></LazyWrapper> },
            { path: '/education', element: <LazyWrapper><EducationListPage /></LazyWrapper> },
            { path: '/education/new', element: <LazyWrapper><EducationFormPage /></LazyWrapper> },
            { path: '/education/:id', element: <LazyWrapper><EducationDetailPage /></LazyWrapper> },
            { path: '/education/:id/edit', element: <LazyWrapper><EducationFormPage /></LazyWrapper> },
            { path: '/alternative-holidays', element: <LazyWrapper><AlternativeHolidayPage /></LazyWrapper> },
            { path: '/settings/organizations', element: <LazyWrapper><OrganizationPage /></LazyWrapper> },
            { path: '/settings/holiday-masters', element: <LazyWrapper><HolidayMasterListPage /></LazyWrapper> },
            { path: '/settings/permissions', element: <LazyWrapper><PermissionMatrixPage /></LazyWrapper> },
            { path: '/settings/permissions/employees', element: <LazyWrapper><EmployeePermissionPage /></LazyWrapper> },
            {
              element: <RoleRoute allowedRoles={['SYSTEM_ADMIN']} />,
              children: [
                { path: '/settings/admin-accounts/new', element: <LazyWrapper><AdminAccountRegisterPage /></LazyWrapper> },
              ],
            },
            { path: '/work-type-headcount', element: <LazyWrapper><CategorySchedulePage /></LazyWrapper> },
            { path: '/monthly-integration', element: <LazyWrapper><MonthlyIntegrationSchedulePage /></LazyWrapper> },
            {
              element: <PermissionRoute requiredPermission="MONTHLY_INPUT_ADEQUACY_READ" />,
              children: [
                { path: '/monthly-input-adequacy', element: <LazyWrapper><MonthlyInputAdequacyPage /></LazyWrapper> },
              ],
            },
            { path: '/promotion/ppt-masters', element: <LazyWrapper><PPTMasterPage /></LazyWrapper> },
            { path: '/promotion/ppt-master-history', element: <LazyWrapper><PPTHistoryPage /></LazyWrapper> },
            {
              element: <PermissionRoute requiredPermission="PRODUCT_EXPIRATION_READ" />,
              children: [
                { path: '/product-expiration', element: <LazyWrapper><ProductExpirationPage /></LazyWrapper> },
              ],
            },
            // Permission-guarded routes
            {
              element: <PermissionRoute requiredPermission="PROMOTION_READ" />,
              children: [
                { path: '/promotions', element: <LazyWrapper><PromotionListPage /></LazyWrapper> },
                { path: '/promotions/:id', element: <LazyWrapper><PromotionDetailPage /></LazyWrapper> },
                { path: '/promotions/new', element: <LazyWrapper><PromotionFormPage /></LazyWrapper> },
                { path: '/promotions/:id/edit', element: <LazyWrapper><PromotionFormPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="PROMOTION_WRITE" />,
              children: [
                { path: '/display-schedules', element: <LazyWrapper><DisplaySchedulePage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="EMPLOYEE_READ" />,
              children: [
                { path: '/employee', element: <LazyWrapper><EmployeePage /></LazyWrapper> },
                { path: '/employee/:employeeId', element: <LazyWrapper><EmployeeDetailPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="SAFETY_CHECK_READ" />,
              children: [
                { path: '/safety-check', element: <LazyWrapper><SafetyCheckPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="ACCOUNT_READ" />,
              children: [
                { path: '/account', element: <LazyWrapper><AccountPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="NAVER_GEOCODE_TEST" />,
              children: [
                { path: '/admin/tools/naver-geocode', element: <LazyWrapper><NaverGeocodeTestPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="SCHEDULED_JOB_READ" />,
              children: [
                { path: '/admin/tools/scheduled-jobs', element: <LazyWrapper><ScheduledJobsPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="AGREEMENT_READ" />,
              children: [
                { path: '/admin/agreement-words', element: <LazyWrapper><AgreementWordsPage /></LazyWrapper> },
              ],
            },
            {
              element: <PermissionRoute requiredPermission="USER_READ" />,
              children: [
                { path: '/users', element: <LazyWrapper><UserListPage /></LazyWrapper> },
                { path: '/users/:id', element: <LazyWrapper><UserDetailPage /></LazyWrapper> },
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
