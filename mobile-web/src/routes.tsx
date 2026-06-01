import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import RootShell from '@/layouts/RootShell';
import TabLayout from '@/layouts/TabLayout';
import SubLayout from '@/layouts/SubLayout';
import AuthGuard from '@/components/AuthGuard';
import { LoadingState } from '@/components/PageStates';

// 라우트 코드분할 — 모바일 WebView 콜드스타트 번들 최소화
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'));
const HomePage = lazy(() => import('@/pages/HomePage'));
const MenuPage = lazy(() => import('@/pages/MenuPage'));
const NoticeListPage = lazy(() => import('@/pages/notice/NoticeListPage'));
const NoticeDetailPage = lazy(() => import('@/pages/notice/NoticeDetailPage'));
const EducationMainPage = lazy(() => import('@/pages/education/EducationMainPage'));
const EducationListPage = lazy(() => import('@/pages/education/EducationListPage'));
const EducationDetailPage = lazy(() => import('@/pages/education/EducationDetailPage'));
const AccountListPage = lazy(() => import('@/pages/account/AccountListPage'));
const ProductDetailPage = lazy(() => import('@/pages/product/ProductDetailPage'));
const ProductExpirationListPage = lazy(() => import('@/pages/product/ProductExpirationListPage'));
const ClaimListPage = lazy(() => import('@/pages/claim/ClaimListPage'));
const ClaimDetailPage = lazy(() => import('@/pages/claim/ClaimDetailPage'));
const LogisticsClaimListPage = lazy(() => import('@/pages/logistics/LogisticsClaimListPage'));
const LogisticsClaimDetailPage = lazy(() => import('@/pages/logistics/LogisticsClaimDetailPage'));
const SalesHubPage = lazy(() => import('@/pages/sales/SalesHubPage'));
const MonthlySalesPage = lazy(() => import('@/pages/sales/MonthlySalesPage'));
const PromotionListPage = lazy(() => import('@/pages/promotion/PromotionListPage'));
const PromotionDetailPage = lazy(() => import('@/pages/promotion/PromotionDetailPage'));
const SettingPage = lazy(() => import('@/pages/setting/SettingPage'));
// Wave 2
const VerifyPasswordPage = lazy(() => import('@/pages/mypage/VerifyPasswordPage'));
const ChangePasswordPage = lazy(() => import('@/pages/mypage/ChangePasswordPage'));
const SafetyCheckPage = lazy(() => import('@/pages/safetycheck/SafetyCheckPage'));
const MyScheduleCalendarPage = lazy(() => import('@/pages/mypage/MyScheduleCalendarPage'));
const MyDailySchedulePage = lazy(() => import('@/pages/mypage/MyDailySchedulePage'));

export default function AppRoutes() {
  return (
    <Suspense fallback={<LoadingState />}>
      <Routes>
        <Route element={<RootShell />}>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<AuthGuard />}>
            {/* 하단 탭 진입점 */}
            <Route element={<TabLayout />}>
              <Route path="/" element={<HomePage />} />
              <Route path="/notices" element={<NoticeListPage />} />
              <Route path="/sales" element={<SalesHubPage />} />
              <Route path="/claims" element={<ClaimListPage />} />
              <Route path="/menu" element={<MenuPage />} />
            </Route>

            {/* 상세/하위 페이지 */}
            <Route element={<SubLayout />}>
              <Route path="/notices/:id" element={<NoticeDetailPage />} />
              <Route path="/education" element={<EducationMainPage />} />
              <Route path="/education/list/:category" element={<EducationListPage />} />
              <Route path="/education/:id" element={<EducationDetailPage />} />
              <Route path="/accounts" element={<AccountListPage />} />
              <Route path="/products/:productCode" element={<ProductDetailPage />} />
              <Route path="/product-expiration" element={<ProductExpirationListPage />} />
              <Route path="/claims/:id" element={<ClaimDetailPage />} />
              <Route path="/logistics-claims" element={<LogisticsClaimListPage />} />
              <Route path="/logistics-claims/:id" element={<LogisticsClaimDetailPage />} />
              <Route path="/promotions" element={<PromotionListPage />} />
              <Route path="/promotions/:id" element={<PromotionDetailPage />} />
              <Route path="/sales/monthly" element={<MonthlySalesPage />} />
              <Route path="/safety-check" element={<SafetyCheckPage />} />
              <Route path="/mypage/schedule" element={<MyScheduleCalendarPage />} />
              <Route path="/mypage/daily/:date" element={<MyDailySchedulePage />} />
              <Route path="/password/verify" element={<VerifyPasswordPage />} />
              <Route path="/password/change" element={<ChangePasswordPage />} />
              <Route path="/settings" element={<SettingPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
