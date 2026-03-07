import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import ProtectedRoute from '@/components/ProtectedRoute';
import AdminLayout from '@/layouts/AdminLayout';

const LoginPage = lazy(() => import('@/pages/LoginPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const SalesQueryPage = lazy(() => import('@/pages/SalesQueryPage'));
const SchedulePage = lazy(() => import('@/pages/SchedulePage'));
const DeploymentPage = lazy(() => import('@/pages/DeploymentPage'));
const AttendancePage = lazy(() => import('@/pages/AttendancePage'));
const EventTeamPage = lazy(() => import('@/pages/EventTeamPage'));
const ClaimPage = lazy(() => import('@/pages/ClaimPage'));
const SuggestionPage = lazy(() => import('@/pages/SuggestionPage'));
const LeavePage = lazy(() => import('@/pages/LeavePage'));
const SafetyCheckPage = lazy(() => import('@/pages/SafetyCheckPage'));
const ProductPage = lazy(() => import('@/pages/ProductPage'));
const EmployeePage = lazy(() => import('@/pages/EmployeePage'));
const FieldInspectionPage = lazy(() => import('@/pages/FieldInspectionPage'));
const ReportPage = lazy(() => import('@/pages/ReportPage'));
const NoticeListPage = lazy(() => import('@/pages/notice/NoticeListPage'));
const NoticeDetailPage = lazy(() => import('@/pages/notice/NoticeDetailPage'));
const NoticeFormPage = lazy(() => import('@/pages/notice/NoticeFormPage'));

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

export const router = createBrowserRouter([
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
          { path: '/event-team', element: <LazyWrapper><EventTeamPage /></LazyWrapper> },
          { path: '/claim', element: <LazyWrapper><ClaimPage /></LazyWrapper> },
          { path: '/suggestion', element: <LazyWrapper><SuggestionPage /></LazyWrapper> },
          { path: '/leave', element: <LazyWrapper><LeavePage /></LazyWrapper> },
          { path: '/safety-check', element: <LazyWrapper><SafetyCheckPage /></LazyWrapper> },
          { path: '/product', element: <LazyWrapper><ProductPage /></LazyWrapper> },
          { path: '/employee', element: <LazyWrapper><EmployeePage /></LazyWrapper> },
          { path: '/field-inspection', element: <LazyWrapper><FieldInspectionPage /></LazyWrapper> },
          { path: '/report', element: <LazyWrapper><ReportPage /></LazyWrapper> },
          { path: '/notices', element: <LazyWrapper><NoticeListPage /></LazyWrapper> },
          { path: '/notices/new', element: <LazyWrapper><NoticeFormPage /></LazyWrapper> },
          { path: '/notices/:id', element: <LazyWrapper><NoticeDetailPage /></LazyWrapper> },
          { path: '/notices/:id/edit', element: <LazyWrapper><NoticeFormPage /></LazyWrapper> },
          { path: '*', element: <Navigate to="/" replace /> },
        ],
      },
    ],
  },
]);
