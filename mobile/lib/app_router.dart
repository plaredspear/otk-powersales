import 'package:flutter/material.dart';
import 'domain/entities/attendance_result.dart';
import 'domain/entities/education_category.dart';
import 'domain/entities/product_expiration_item.dart';
import 'presentation/pages/app_info_page.dart';
import 'presentation/pages/attendance_page.dart';
import 'presentation/pages/attendance_complete_page.dart';
import 'presentation/pages/claim_detail_page.dart';
import 'presentation/pages/claim_list_page.dart';
import 'presentation/pages/claim_register_page.dart';
import 'presentation/pages/client_order_detail_page.dart';
import 'presentation/pages/education_detail_page.dart';
import 'presentation/pages/education_list_page.dart';
import 'presentation/pages/education_main_page.dart';
import 'presentation/pages/inspection_detail_page.dart';
import 'presentation/pages/notice_list_page.dart';
import 'presentation/pages/notice_detail_page.dart';
import 'presentation/pages/inspection_list_page.dart';
import 'presentation/pages/inspection/inspection_register_page.dart';
import 'presentation/pages/order_cancel_page.dart';
import 'presentation/pages/order_detail_page.dart';
import 'presentation/pages/order_list_page.dart';
import 'presentation/pages/order_form_page.dart';
import 'presentation/pages/product_detail_page.dart';
import 'presentation/pages/product_search_page.dart';
import 'presentation/pages/my_accounts_page.dart';
import 'presentation/pages/product_expiration_list_page.dart';
import 'presentation/pages/suggestion_detail_page.dart';
import 'presentation/pages/suggestion_list_page.dart';
import 'presentation/pages/suggestion_register_page.dart';
import 'presentation/pages/product_expiration_delete_page.dart';
import 'presentation/pages/product_expiration_register_page.dart';
import 'presentation/pages/product_expiration_edit_page.dart';
import 'presentation/pages/change_password_page.dart';
import 'presentation/pages/verify_password_page.dart';
import 'presentation/pages/my_schedule_calendar_page.dart';
import 'presentation/pages/my_schedule_detail_page.dart';
import 'presentation/pages/profile_page.dart';
import 'presentation/pages/staff_evaluation_page.dart';
import 'presentation/pages/promotion_list_page.dart';
import 'presentation/pages/monthly_sales_page.dart';
import 'presentation/pages/sales_status_page.dart';
import 'presentation/screens/electronic_sales_screen.dart';
import 'presentation/pages/promotion_detail_page.dart';
import 'presentation/pages/promotion_daily_sales_page.dart';
import 'presentation/pages/daily_sales_entry_page.dart';
import 'presentation/pages/safety_check_page.dart';
import 'presentation/screens/change_password_screen.dart';
import 'presentation/screens/gps_consent_screen.dart';
import 'presentation/screens/leader_schedule/leader_female_staff_screen.dart';
import 'presentation/screens/leader_schedule/leader_team_member_schedule_screen.dart';
import 'presentation/screens/logistics_sales_screen.dart';
import 'presentation/screens/login_screen.dart';
import 'presentation/screens/main_screen.dart';
import 'presentation/screens/pos_sales_screen.dart';
import 'presentation/screens/sales_overview_screen.dart';
import 'presentation/screens/splash_screen.dart';

/// 앱 라우터
///
/// 앱 전체의 라우팅을 관리합니다.
class AppRouter {
  /// 라우트 이름 상수
  static const String splash = '/splash';
  static const String login = '/login';
  static const String changePassword = '/change-password';
  static const String verifyPassword = '/verify-password'; // F54: 현재 비밀번호 확인
  static const String changePasswordNew = '/change-password-new'; // F54: 새 비밀번호 입력
  static const String gpsConsent = '/gps-consent'; // F62: GPS 사용 동의
  static const String main = '/';
  static const String posSales = '/pos-sales';
  static const String logisticsSales = '/logistics-sales';
  static const String electronicSales = '/electronic-sales'; // 전산 매출 조회 (레거시 abcMain)
  static const String attendance = '/attendance';
  static const String attendanceComplete = '/attendance/complete';
  static const String myAccounts = '/my-accounts';
  static const String productSearch = '/product-search';
  static const String productDetail = '/product/detail';
  static const String orderList = '/order-list';
  static const String orderDetail = '/order-detail';
  static const String orderCancel = '/order-cancel';
  static const String orderForm = '/order-form';
  static const String clientOrderDetail = '/client-order-detail';
  static const String productExpiration = '/product-expiration';
  static const String productExpirationRegister = '/product-expiration/register';
  static const String productExpirationEdit = '/product-expiration/edit';
  static const String productExpirationDelete = '/product-expiration/delete';
  static const String inspectionList = '/inspection-list';
  static const String inspectionDetail = '/inspection-detail';
  static const String inspectionRegister = '/inspection-register';
  static const String claimList = '/claim/list';
  static const String claimDetail = '/claim/detail';
  static const String claimRegister = '/claim/register';
  static const String suggestionRegister = '/suggestion/register';
  static const String suggestionList = '/suggestion/list'; // 내 제안/물류클레임 목록
  static const String suggestionDetail = '/suggestion/detail'; // 제안/물류클레임 상세
  static const String education = '/education';
  static const String educationList = '/education/list';
  static const String educationDetail = '/education/detail';
  static const String notices = '/notices';
  static const String noticeDetail = '/notices/detail';
  static const String salesOverview = '/sales-overview';
  static const String monthlySales = '/monthly-sales';
  static const String salesStatus = '/sales-status'; // 레거시 매출 현황(행사 매출 + 월 매출 탭)
  static const String safetyCheck = '/safety-check';
  static const String promotionList = '/promotions';
  static const String promotionDetail = '/promotions/detail';
  static const String promotionDailySales = '/promotions/daily-sales'; // P4: 여사원 일매출 마감
  static const String promotionDailySalesEntry =
      '/promotions/daily-sales-entry'; // 일 매출 등록 진입(담당 행사 선택)
  static const String myScheduleCalendar = '/my-schedule'; // F56: 마이페이지 일정 캘린더
  static const String myScheduleDetail = '/my-schedule/detail'; // F56: 일정 상세
  static const String leaderFemaleStaff = '/leader/female-staff'; // 레거시 /employee/main: 조장 — 여사원 명단
  static const String leaderSchedule = '/leader/schedule'; // 레거시 /employee/mgnSchedule: 조장 — 일정/등록(월간 캘린더 → 일별현황)
  static const String appInfo = '/app-info'; // 앱 정보 / 오픈소스 라이선스
  static const String profile = '/profile'; // 내 정보(프로필)
  static const String staffEvaluation = '/staff-evaluation'; // 레거시 /employee/evaluationList: 여사원 평가조회

  /// 라우트 맵
  static Map<String, WidgetBuilder> get routes => {
        splash: (context) => const SplashScreen(),
        login: (context) => const LoginScreen(),
        changePassword: (context) => const ChangePasswordScreen(),
        gpsConsent: (context) => const GpsConsentScreen(), // F62: GPS 동의
        verifyPassword: (context) => const VerifyPasswordPage(), // F54: 현재 비밀번호 확인
        changePasswordNew: (context) { // F54: 새 비밀번호 입력
          final currentPassword =
              ModalRoute.of(context)!.settings.arguments as String;
          return ChangePasswordPage(currentPassword: currentPassword);
        },
        main: (context) => const MainScreen(),
        posSales: (context) => const PosSalesScreen(),
        logisticsSales: (context) => const LogisticsSalesScreen(),
        electronicSales: (context) => const ElectronicSalesScreen(),
        attendance: (context) => const AttendancePage(),
        attendanceComplete: (context) {
          final result =
              ModalRoute.of(context)!.settings.arguments as AttendanceResult;
          return AttendanceCompletePage(result: result);
        },
        myAccounts: (context) => const MyAccountsPage(),
        productSearch: (context) {
          final selectionMode =
              ModalRoute.of(context)?.settings.arguments as bool? ?? false;
          return ProductSearchPage(selectionMode: selectionMode);
        },
        productDetail: (context) {
          final productCode =
              ModalRoute.of(context)!.settings.arguments as String;
          return ProductDetailPage(productCode: productCode);
        },
        orderList: (context) {
          final initialTabIndex =
              ModalRoute.of(context)?.settings.arguments as int?;
          return OrderListPage(initialTabIndex: initialTabIndex ?? 0);
        },
        orderDetail: (context) {
          final orderId = ModalRoute.of(context)!.settings.arguments as int;
          return OrderDetailPage(orderId: orderId);
        },
        orderCancel: (context) {
          final args = ModalRoute.of(context)!.settings.arguments
              as OrderCancelPageArgs;
          return OrderCancelPage(args: args);
        },
        orderForm: (context) {
          final args = ModalRoute.of(context)?.settings.arguments;
          // int = 수정 모드(orderId), String = 제품검색에서 전달된 제품코드(신규)
          if (args is String) {
            return OrderFormPage(initialProductCode: args);
          }
          return OrderFormPage(orderId: args as int?);
        },
        clientOrderDetail: (context) {
          final sapOrderNumber =
              ModalRoute.of(context)!.settings.arguments as String;
          return ClientOrderDetailPage(sapOrderNumber: sapOrderNumber);
        },
        productExpiration: (context) => const ProductExpirationListPage(),
        productExpirationRegister: (context) => const ProductExpirationRegisterPage(),
        productExpirationEdit: (context) {
          final item = ModalRoute.of(context)!.settings.arguments
              as ProductExpirationItem;
          return ProductExpirationEditPage(item: item);
        },
        productExpirationDelete: (context) {
          final items = ModalRoute.of(context)!.settings.arguments
              as List<ProductExpirationItem>;
          return ProductExpirationDeletePage(items: items);
        },
        inspectionList: (context) => const InspectionListPage(),
        inspectionDetail: (context) {
          final inspectionId =
              ModalRoute.of(context)!.settings.arguments as int;
          return InspectionDetailPage(inspectionId: inspectionId);
        },
        inspectionRegister: (context) => const InspectionRegisterPage(),
        claimList: (context) => const ClaimListPage(),
        claimDetail: (context) {
          final claimId = ModalRoute.of(context)!.settings.arguments as int;
          return ClaimDetailPage(claimId: claimId);
        },
        claimRegister: (context) {
          // 제품검색 결과에서 제품 정보를 전달받아 진입할 수 있음
          final args = ModalRoute.of(context)?.settings.arguments;
          if (args is ({String productCode, String productName})) {
            return ClaimRegisterPage(
              presetProductCode: args.productCode,
              presetProductName: args.productName,
            );
          }
          return const ClaimRegisterPage();
        },
        suggestionRegister: (context) => const SuggestionRegisterPage(),
        suggestionList: (context) => const SuggestionListPage(),
        suggestionDetail: (context) {
          final suggestionId =
              ModalRoute.of(context)!.settings.arguments as int;
          return SuggestionDetailPage(suggestionId: suggestionId);
        },
        education: (context) => const EducationMainPage(),
        educationList: (context) {
          final category =
              ModalRoute.of(context)?.settings.arguments as EducationCategory?;
          return EducationListPage(category: category);
        },
        educationDetail: (context) {
          final postId = ModalRoute.of(context)!.settings.arguments as String;
          return EducationDetailPage(postId: postId);
        },
        notices: (context) => const NoticeListPage(),
        noticeDetail: (context) {
          final noticeId = ModalRoute.of(context)!.settings.arguments as int;
          return NoticeDetailPage(noticeId: noticeId);
        },
        salesOverview: (context) => const SalesOverviewScreen(),
        monthlySales: (context) => const MonthlySalesPage(),
        salesStatus: (context) {
          final args = ModalRoute.of(context)?.settings.arguments;
          // 내 거래처 팝업: 거래처 사전 지정 (SalesStatusArgs)
          if (args is SalesStatusArgs) {
            return SalesStatusPage(
              initialTabIndex: args.initialTabIndex,
              presetAccountId: args.presetAccountId,
              presetAccountName: args.presetAccountName,
            );
          }
          // 드로어: 탭 인덱스만 전달 → 거래처 전체로 초기화
          final initialTabIndex = args as int?;
          return SalesStatusPage(initialTabIndex: initialTabIndex ?? 0);
        },
        safetyCheck: (context) => const SafetyCheckPage(),
        promotionList: (context) => const PromotionListPage(),
        promotionDetail: (context) {
          final promotionId =
              ModalRoute.of(context)!.settings.arguments as int;
          return PromotionDetailPage(promotionId: promotionId);
        },
        promotionDailySales: (context) {
          final promotionEmployeeId =
              ModalRoute.of(context)!.settings.arguments as int;
          return PromotionDailySalesPage(
              promotionEmployeeId: promotionEmployeeId);
        },
        promotionDailySalesEntry: (context) => const DailySalesEntryPage(),
        leaderFemaleStaff: (context) => const LeaderFemaleStaffScreen(), // 레거시 /employee/main: 여사원 명단
        leaderSchedule: (context) => const LeaderTeamMemberScheduleScreen(), // 레거시 /employee/mgnSchedule: 일정/등록(월간 캘린더)
        appInfo: (context) => const AppInfoPage(),
        profile: (context) => const ProfilePage(), // 내 정보(프로필)
        staffEvaluation: (context) => const StaffEvaluationPage(), // 레거시 /employee/evaluationList: 여사원 평가조회
        myScheduleCalendar: (context) => const MyScheduleCalendarPage(), // F56: 일정 캘린더
        myScheduleDetail: (context) {
          // F56: 일정 상세
          final date = ModalRoute.of(context)!.settings.arguments as DateTime;
          return MyScheduleDetailPage(selectedDate: date);
        },
      };

  /// 초기 라우트 - 스플래시(버전 게이트 + 자동 로그인)에서 시작
  static String get initialRoute => splash;

  /// 알 수 없는 라우트 처리
  static Route<dynamic>? onUnknownRoute(RouteSettings settings) {
    return MaterialPageRoute(
      builder: (context) => Scaffold(
        appBar: AppBar(
          title: const Text('페이지를 찾을 수 없습니다'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.error_outline,
                size: 80,
                color: Colors.grey[400],
              ),
              const SizedBox(height: 24),
              Text(
                '페이지를 찾을 수 없습니다',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: Colors.grey[700],
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '요청한 페이지: ${settings.name}',
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.grey[600],
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: () {
                  Navigator.of(context).pushNamedAndRemoveUntil(
                    main,
                    (route) => false,
                  );
                },
                icon: const Icon(Icons.home),
                label: const Text('홈으로 돌아가기'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 네비게이션 헬퍼 메서드
  ///
  /// 라우트는 `routes:` 테이블로 정의되어 항상 `MaterialPageRoute<dynamic>`로
  /// 생성된다. 따라서 `pushNamed<T>`에 구체 타입(T=Product 등)을 넘기면
  /// `Route<dynamic>` → `Route<T?>` 캐스트가 실패한다.
  /// route 는 dynamic 으로 push 하고 pop 결과값만 T 로 캐스트한다.
  static Future<T?> navigateTo<T>(
    BuildContext context,
    String routeName, {
    Object? arguments,
  }) async {
    final result = await Navigator.of(context).pushNamed<dynamic>(
      routeName,
      arguments: arguments,
    );
    return result as T?;
  }

  /// 네비게이션 교체 (뒤로가기 불가)
  static Future<T?> navigateToAndReplace<T>(
    BuildContext context,
    String routeName, {
    Object? arguments,
  }) {
    return Navigator.of(context).pushReplacementNamed<T, dynamic>(
      routeName,
      arguments: arguments,
    );
  }

  /// 모든 라우트 제거하고 이동
  static Future<T?> navigateToAndRemoveAll<T>(
    BuildContext context,
    String routeName, {
    Object? arguments,
  }) {
    return Navigator.of(context).pushNamedAndRemoveUntil<T>(
      routeName,
      (route) => false,
      arguments: arguments,
    );
  }

  /// 뒤로가기
  static void goBack(BuildContext context, {dynamic result}) {
    Navigator.of(context).pop(result);
  }
}
