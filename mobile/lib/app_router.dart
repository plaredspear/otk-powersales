import 'package:flutter/material.dart';
import 'presentation/pages/attendance_page.dart';
import 'presentation/pages/attendance_complete_page.dart';
import 'presentation/pages/order_cancel_page.dart';
import 'presentation/pages/order_detail_page.dart';
import 'presentation/pages/order_list_page.dart';
import 'presentation/pages/product_search_page.dart';
import 'presentation/pages/my_stores_page.dart';
import 'presentation/pages/product_search_result_page.dart';
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

  /// 라우트 맵
  static Map<String, WidgetBuilder> get routes => {
        login: (context) => const LoginScreen(),
        changePassword: (context) => const ChangePasswordScreen(),
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
