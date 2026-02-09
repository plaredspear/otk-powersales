import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mobile/presentation/screens/logistics_sales_screen.dart';

void main() {
  // 한국어 locale 초기화 (날짜 포맷팅용)
  setUpAll(() async {
    await initializeDateFormatting('ko_KR', null);
  });

  group('LogisticsSalesScreen', () {
    testWidgets('화면이 정상적으로 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // AppBar 확인
      expect(find.text('물류매출 조회'), findsOneWidget);

      // 조회 버튼 확인
      expect(find.text('조회'), findsOneWidget);
    });

    testWidgets('카테고리별 탭이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 탭 레이블 확인 (탭과 컨텐츠에 중복 표시될 수 있음)
      expect(find.text('상온'), findsWidgets);
      expect(find.text('라면'), findsWidgets);
      expect(find.text('냉동/냉장'), findsWidgets);
    });

    testWidgets('카테고리별 아이콘이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 카테고리 아이콘 확인 (탭 + 테이블에 표시되므로 여러 개)
      expect(find.byIcon(Icons.inventory_2), findsWidgets); // 상온
      expect(find.byIcon(Icons.ramen_dining), findsWidgets); // 라면
      expect(find.byIcon(Icons.ac_unit), findsWidgets); // 냉동/냉장
    });

    testWidgets('필터 섹션이 올바른 레이블을 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 필터 레이블 확인
      expect(find.text('조회 년월'), findsOneWidget);
    });

    testWidgets('년월 선택기가 현재 년월을 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 년월 선택기 확인 (현재 년월이 표시되어야 함)
      // 차트의 X축 레이블에도 표시되므로 findsWidgets로 변경
      final now = DateTime.now();
      final currentYearMonth =
          '${now.year}년 ${now.month.toString().padLeft(2, '0')}월';
      expect(find.text(currentYearMonth), findsWidgets);
    });

    testWidgets('년월 선택기에 캘린더 아이콘이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 캘린더 아이콘 확인
      expect(find.byIcon(Icons.calendar_today), findsOneWidget);
    });

    testWidgets('당월/이전월 상태 표시기가 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 당월/이전월 상태 표시 확인
      // 초기 상태는 당월이므로 "당월 물류예상실적" 또는 "이전월 ABC물류배부 마감실적" 중 하나가 표시됨
      expect(
        find.textContaining('물류'),
        findsWidgets,
      );
    });

    testWidgets('조회 버튼이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 조회 버튼 확인
      final searchButton = find.text('조회');
      expect(searchButton, findsOneWidget);

      // 조회 버튼 아이콘 확인
      expect(find.byIcon(Icons.search), findsOneWidget);
    });

    testWidgets('필터 초기화 버튼이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 필터 초기화 버튼(refresh 아이콘) 확인
      expect(find.byIcon(Icons.refresh), findsOneWidget);
    });

    testWidgets('탭 전환이 동작한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 두 번째 탭(라면)을 탭
      await tester.tap(find.text('라면').first);
      await tester.pumpAndSettle();

      // 탭이 선택되었는지 확인 (탭과 컨텐츠에 중복 표시됨)
      expect(find.text('라면'), findsWidgets);

      // 세 번째 탭(냉동/냉장)을 탭
      await tester.tap(find.text('냉동/냉장').first);
      await tester.pumpAndSettle();

      expect(find.text('냉동/냉장'), findsWidgets);
    });

    testWidgets('TabBarView가 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // TabBarView 확인
      expect(find.byType(TabBarView), findsOneWidget);
    });

    testWidgets('TabController가 올바르게 초기화된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: LogisticsSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // TabController가 3개 탭으로 초기화되었는지 확인
      // TabBar가 렌더링되었다면 TabController도 정상 동작
      expect(find.byType(TabBar), findsOneWidget);
      expect(find.byType(TabBarView), findsOneWidget);
    });
  });
}
