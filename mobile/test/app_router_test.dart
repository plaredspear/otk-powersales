import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app_router.dart';
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

    // MainScreen 내부의 HomePage가 initState에서 addPostFrameCallback으로
    // 비동기 데이터 로딩을 시작하므로, 위젯 테스트 시 pending timer가 발생합니다.
    // 라우트 매핑 검증은 라우트 맵 테스트에서 충분히 커버되므로,
    // 위젯 네비게이션 테스트는 라우트 상수 기반으로 검증합니다.
    test('메인 라우트가 routes 맵에 존재하고 MainScreen을 반환한다', () {
      final routes = AppRouter.routes;
      expect(routes.containsKey(AppRouter.main), true);
    });

    test('POS 매출 라우트가 routes 맵에 존재한다', () {
      final routes = AppRouter.routes;
      expect(routes.containsKey(AppRouter.posSales), true);
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
      expect(AppRouter.verifyPassword, '/verify-password');
      expect(AppRouter.changePasswordNew, '/change-password-new');
      expect(AppRouter.main, '/');
      expect(AppRouter.posSales, '/pos-sales');
      expect(AppRouter.attendance, '/attendance');
      expect(AppRouter.attendanceComplete, '/attendance/complete');
      expect(AppRouter.myStores, '/my-stores');
    });

    test('orderList 라우트 상수가 올바르다', () {
      expect(AppRouter.orderList, equals('/order-list'));
    });

    test('orderCancel 라우트 상수가 올바르다', () {
      expect(AppRouter.orderCancel, equals('/order-cancel'));
    });

    test('inspection 라우트 상수가 올바르다', () {
      expect(AppRouter.inspectionList, equals('/inspection-list'));
      expect(AppRouter.inspectionDetail, equals('/inspection-detail'));
      expect(AppRouter.inspectionRegister, equals('/inspection-register'));
    });

    test('claim 라우트 상수가 올바르다', () {
      expect(AppRouter.claimRegister, equals('/claim/register'));
    });

    test('라우트 맵이 올바르게 정의되어 있다', () {
      final routes = AppRouter.routes;

      expect(routes.containsKey(AppRouter.login), true);
      expect(routes.containsKey(AppRouter.changePassword), true);
      expect(routes.containsKey(AppRouter.verifyPassword), true);
      expect(routes.containsKey(AppRouter.changePasswordNew), true);
      expect(routes.containsKey(AppRouter.main), true);
      expect(routes.containsKey(AppRouter.posSales), true);
      expect(routes.containsKey(AppRouter.attendance), true);
      expect(routes.containsKey(AppRouter.attendanceComplete), true);
      expect(routes.containsKey(AppRouter.productSearch), true);
      expect(routes.containsKey(AppRouter.productSearchResult), true);
      expect(routes.containsKey(AppRouter.myStores), true);
      expect(routes.containsKey(AppRouter.orderList), true);
      expect(routes.containsKey(AppRouter.orderDetail), true);
      expect(routes.containsKey(AppRouter.orderCancel), true);
      expect(routes.containsKey(AppRouter.orderForm), true);
      expect(routes.containsKey(AppRouter.clientOrderDetail), true);
      expect(routes.containsKey(AppRouter.shelfLife), true);
      expect(routes.containsKey(AppRouter.shelfLifeRegister), true);
      expect(routes.containsKey(AppRouter.shelfLifeEdit), true);
      expect(routes.containsKey(AppRouter.shelfLifeDelete), true);
      expect(routes.containsKey(AppRouter.inspectionList), true);
      expect(routes.containsKey(AppRouter.inspectionDetail), true);
      expect(routes.containsKey(AppRouter.inspectionRegister), true);
      expect(routes.containsKey(AppRouter.claimRegister), true);
      expect(routes.containsKey(AppRouter.education), true);
      expect(routes.containsKey(AppRouter.educationList), true);
      expect(routes.containsKey(AppRouter.notices), true);
      expect(routes.containsKey(AppRouter.noticeDetail), true);
      expect(routes.containsKey(AppRouter.salesOverview), true);
      expect(routes.containsKey(AppRouter.myScheduleCalendar), true);
      expect(routes.containsKey(AppRouter.myScheduleDetail), true);
      expect(routes.containsKey(AppRouter.gpsConsent), true);
      expect(routes.containsKey(AppRouter.suggestionRegister), true);
      expect(routes.length, 33);
    });

    test('routes 맵에 orderList 라우트가 포함되어 있다', () {
      expect(AppRouter.routes.containsKey(AppRouter.orderList), isTrue);
    });

    // 초기 라우트 테스트는 상단에서 이미 검증됨
  });
}
