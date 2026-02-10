import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/app_router.dart';
import 'package:mobile/presentation/screens/main_screen.dart';
import 'package:mobile/presentation/screens/pos_sales_screen.dart';
import 'test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  group('AppRouter', () {
    test('초기 라우트가 로그인 경로이다', () {
      expect(AppRouter.initialRoute, AppRouter.login);
      expect(AppRouter.initialRoute, '/login');
    });

    testWidgets('메인 라우트로 이동하면 MainScreen이 표시된다',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            initialRoute: AppRouter.main,
            routes: AppRouter.routes,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // MainScreen이 표시됨
      expect(find.byType(MainScreen), findsOneWidget);
    });

    testWidgets('POS 매출 라우트로 이동할 수 있다', (WidgetTester tester) async {
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            initialRoute: AppRouter.main,
            routes: AppRouter.routes,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // POS 매출 라우트로 이동
      final context = tester.element(find.byType(MainScreen));
      AppRouter.navigateTo(context, AppRouter.posSales);

      await tester.pumpAndSettle();

      // PosSalesScreen이 표시됨 (최소 1개)
      expect(find.byType(PosSalesScreen), findsWidgets);
    });

    testWidgets('onUnknownRoute 핸들러가 정의되어 있다',
        (WidgetTester tester) async {
      // onUnknownRoute 핸들러가 null이 아님을 확인
      expect(AppRouter.onUnknownRoute, isNotNull);

      // 핸들러가 Route를 반환하는지 확인
      final route = AppRouter.onUnknownRoute(
        const RouteSettings(name: '/test-route'),
      );
      expect(route, isNotNull);
      expect(route, isA<MaterialPageRoute>());
    });

    test('라우트 이름 상수가 정의되어 있다', () {
      expect(AppRouter.login, '/login');
      expect(AppRouter.changePassword, '/change-password');
      expect(AppRouter.main, '/');
      expect(AppRouter.posSales, '/pos-sales');
      expect(AppRouter.attendance, '/attendance');
      expect(AppRouter.attendanceComplete, '/attendance/complete');
    });

    test('라우트 맵이 올바르게 정의되어 있다', () {
      final routes = AppRouter.routes;

      expect(routes.containsKey(AppRouter.login), true);
      expect(routes.containsKey(AppRouter.changePassword), true);
      expect(routes.containsKey(AppRouter.main), true);
      expect(routes.containsKey(AppRouter.posSales), true);
      expect(routes.containsKey(AppRouter.attendance), true);
      expect(routes.containsKey(AppRouter.attendanceComplete), true);
      expect(routes.containsKey(AppRouter.productSearch), true);
      expect(routes.containsKey(AppRouter.productSearchResult), true);
      expect(routes.length, 8);
    });

    // 초기 라우트 테스트는 상단에서 이미 검증됨
  });
}
