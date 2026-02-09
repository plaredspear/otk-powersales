import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/presentation/screens/main_screen.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  group('MainScreen', () {
    testWidgets('화면이 정상적으로 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // BottomNavigationBar 확인
      expect(find.byType(BottomNavigationBar), findsOneWidget);

      // 4개 탭 확인
      expect(find.text('POS 매출'), findsOneWidget);
      expect(find.text('전산매출'), findsOneWidget);
      expect(find.text('물류매출'), findsOneWidget);
      expect(find.text('목표/진도율'), findsOneWidget);
    });

    testWidgets('BottomNavigationBar 아이콘이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 각 탭의 아이콘 확인
      expect(find.byIcon(Icons.point_of_sale), findsOneWidget);
      expect(find.byIcon(Icons.computer), findsOneWidget);
      expect(find.byIcon(Icons.local_shipping), findsOneWidget);
      expect(find.byIcon(Icons.trending_up), findsOneWidget);
    });

    testWidgets('초기 화면은 POS 매출 조회 화면이다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // POS 매출 조회 AppBar 텍스트 확인
      expect(find.text('POS 매출 조회'), findsOneWidget);
    });

    testWidgets('탭을 클릭하면 해당 화면으로 전환된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 전산매출 탭 클릭
      await tester.tap(find.text('전산매출'));
      await tester.pumpAndSettle();

      // 전산매출 화면 확인 (실제 구현된 화면)
      expect(find.text('전산매출 조회'), findsOneWidget);

      // 물류매출 탭 클릭
      await tester.tap(find.text('물류매출'));
      await tester.pumpAndSettle();

      // 물류매출 화면 확인 (실제 구현된 화면)
      expect(find.text('물류매출 조회'), findsOneWidget);

      // 목표/진도율 탭 클릭
      await tester.tap(find.text('목표/진도율'));
      await tester.pumpAndSettle();

      // 목표/진도율 화면 확인 (실제 구현된 화면)
      expect(find.text('목표/진도율'), findsNWidgets(2)); // 탭 + AppBar

      // POS 매출 탭으로 다시 돌아가기
      await tester.tap(find.text('POS 매출'));
      await tester.pumpAndSettle();

      // POS 매출 조회 화면 확인
      expect(find.text('POS 매출 조회'), findsOneWidget);
    });

    testWidgets('IndexedStack을 사용하여 화면 상태를 유지한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // IndexedStack 확인
      expect(find.byType(IndexedStack), findsOneWidget);
    });

    testWidgets('전산매출 화면이 올바르게 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 전산매출 탭으로 이동
      await tester.tap(find.text('전산매출'));
      await tester.pumpAndSettle();

      // 전산매출 조회 화면 확인 (실제 구현된 화면)
      expect(find.text('전산매출 조회'), findsOneWidget);
    });

    testWidgets('BottomNavigationBar 타입이 fixed다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      final bottomNavBar = tester.widget<BottomNavigationBar>(
        find.byType(BottomNavigationBar),
      );

      expect(bottomNavBar.type, BottomNavigationBarType.fixed);
    });

    testWidgets('선택되지 않은 라벨도 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      final bottomNavBar = tester.widget<BottomNavigationBar>(
        find.byType(BottomNavigationBar),
      );

      expect(bottomNavBar.showUnselectedLabels, true);
    });
  });
}
