import 'package:flutter/material.dart';
import 'domain/entities/education_category.dart';
import 'domain/entities/shelf_life_item.dart';
import 'presentation/pages/attendance_page.dart';
import 'presentation/pages/attendance_complete_page.dart';
import 'presentation/pages/claim_register_page.dart';
import 'presentation/pages/client_order_detail_page.dart';
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
import 'presentation/pages/product_search_page.dart';
import 'presentation/pages/my_stores_page.dart';
import 'presentation/pages/product_search_result_page.dart';
import 'presentation/pages/shelf_life_list_page.dart';
import 'presentation/pages/shelf_life_delete_page.dart';
import 'presentation/pages/shelf_life_register_page.dart';
import 'presentation/pages/shelf_life_edit_page.dart';
import 'presentation/pages/change_password_page.dart';
import 'presentation/pages/verify_password_page.dart';
import 'presentation/pages/my_schedule_calendar_page.dart';
import 'presentation/screens/change_password_screen.dart';
import 'presentation/screens/login_screen.dart';
import 'presentation/screens/main_screen.dart';
import 'presentation/screens/pos_sales_screen.dart';

/// 앱 라우터
///
/// 앱 전체의 라우팅을 관리합니다.
class AppRouter {
  /// 라우트 이름 상수
  static const String login = '/login';
  static const String changePassword = '/change-password';
  static const String verifyPassword = '/verify-password'; // F54: 현재 비밀번호 확인
  static const String changePasswordNew = '/change-password-new'; // F54: 새 비밀번호 입력
  static const String main = '/';
  static const String posSales = '/pos-sales';
  static const String attendance = '/attendance';
  static const String attendanceComplete = '/attendance/complete';
  static const String myStores = '/my-stores';
  static const String productSearch = '/product-search';
  static const String productSearchResult = '/product-search/result';
  static const String orderList = '/order-list';
  static const String orderDetail = '/order-detail';
  static const String orderCancel = '/order-cancel';
  static const String orderForm = '/order-form';
  static const String clientOrderDetail = '/client-order-detail';
  static const String shelfLife = '/shelf-life';
  static const String shelfLifeRegister = '/shelf-life/register';
  static const String shelfLifeEdit = '/shelf-life/edit';
  static const String shelfLifeDelete = '/shelf-life/delete';
  static const String inspectionList = '/inspection-list';
  static const String inspectionDetail = '/inspection-detail';
  static const String inspectionRegister = '/inspection-register';
  static const String claimRegister = '/claim/register';
  static const String education = '/education';
  static const String educationList = '/education/list';
  static const String notices = '/notices';
  static const String noticeDetail = '/notices/detail';
  static const String myScheduleCalendar = '/my-schedule'; // F56: 마이페이지 일정 캘린더
  static const String myScheduleDetail = '/my-schedule/detail'; // F56: 일정 상세

  /// 라우트 맵
  static Map<String, WidgetBuilder> get routes => {
        login: (context) => const LoginScreen(),
        changePassword: (context) => const ChangePasswordScreen(),
        verifyPassword: (context) => const VerifyPasswordPage(), // F54: 현재 비밀번호 확인
        changePasswordNew: (context) { // F54: 새 비밀번호 입력
          final currentPassword =
              ModalRoute.of(context)!.settings.arguments as String;
          return ChangePasswordPage(currentPassword: currentPassword);
        },
        main: (context) => const MainScreen(),
        posSales: (context) => const PosSalesScreen(),
        attendance: (context) => const AttendancePage(),
        attendanceComplete: (context) => const AttendanceCompletePage(),
        myStores: (context) => const MyStoresPage(),
        productSearch: (context) => const ProductSearchPage(),
        productSearchResult: (context) => const ProductSearchResultPage(),
        orderList: (context) => const OrderListPage(),
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
          final orderId =
              ModalRoute.of(context)?.settings.arguments as int?;
          return OrderFormPage(orderId: orderId);
        },
        clientOrderDetail: (context) {
          final sapOrderNumber =
              ModalRoute.of(context)!.settings.arguments as String;
          return ClientOrderDetailPage(sapOrderNumber: sapOrderNumber);
        },
        shelfLife: (context) => const ShelfLifeListPage(),
        shelfLifeRegister: (context) => const ShelfLifeRegisterPage(),
        shelfLifeEdit: (context) {
          final item = ModalRoute.of(context)!.settings.arguments
              as ShelfLifeItem;
          return ShelfLifeEditPage(item: item);
        },
        shelfLifeDelete: (context) {
          final items = ModalRoute.of(context)!.settings.arguments
              as List<ShelfLifeItem>;
          return ShelfLifeDeletePage(items: items);
        },
        inspectionList: (context) => const InspectionListPage(),
        inspectionDetail: (context) {
          final inspectionId =
              ModalRoute.of(context)!.settings.arguments as int;
          return InspectionDetailPage(inspectionId: inspectionId);
        },
        inspectionRegister: (context) => const InspectionRegisterPage(),
        claimRegister: (context) => const ClaimRegisterPage(),
        education: (context) => const EducationMainPage(),
        educationList: (context) {
          final category =
              ModalRoute.of(context)?.settings.arguments as EducationCategory?;
          return EducationListPage(category: category);
        },
        notices: (context) => const NoticeListPage(),
        noticeDetail: (context) {
          final noticeId = ModalRoute.of(context)!.settings.arguments as int;
          return NoticeDetailPage(noticeId: noticeId);
        },
        myScheduleCalendar: (context) => const MyScheduleCalendarPage(), // F56: 일정 캘린더
        myScheduleDetail: (context) {
          // F56: 일정 상세 (Task #10에서 구현 예정)
          final date = ModalRoute.of(context)!.settings.arguments as DateTime;
          // TODO: MyScheduleDetailPage 구현 후 교체
          return Scaffold(
            appBar: AppBar(title: const Text('일정 상세 (준비 중)')),
            body: Center(child: Text('선택 날짜: ${date.toString()}')),
          );
        },
      };

  /// 초기 라우트 - 로그인 화면에서 시작
  static String get initialRoute => login;

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
                  fontWeight: FontWeight.w600,
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
  static Future<T?> navigateTo<T>(
    BuildContext context,
    String routeName, {
    Object? arguments,
  }) {
    return Navigator.of(context).pushNamed<T>(
      routeName,
      arguments: arguments,
    );
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
