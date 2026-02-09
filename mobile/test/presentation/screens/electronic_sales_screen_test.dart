import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mobile/presentation/screens/electronic_sales_screen.dart';
import 'package:mobile/presentation/widgets/common/search_filter_widget.dart';

void main() {
  // 한국어 locale 초기화 (날짜 포맷팅용)
  setUpAll(() async {
    await initializeDateFormatting('ko_KR', null);
  });

  group('ElectronicSalesScreen', () {
    testWidgets('화면이 정상적으로 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // AppBar 확인
      expect(find.text('전산매출 조회'), findsOneWidget);

      // 필터 영역 확인
      expect(find.text('년월'), findsOneWidget);
      expect(find.byType(SearchFilterWidget), findsNWidgets(3)); // 거래처, 제품명, 제품코드
      expect(find.text('조회'), findsOneWidget);
    });

    testWidgets('필터 섹션이 올바른 레이블을 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 필터 레이블 확인 (테이블 헤더에도 같은 텍스트가 있을 수 있으므로 findsWidgets 사용)
      expect(find.text('년월'), findsOneWidget);
      expect(find.text('거래처명'), findsOneWidget);
      expect(find.text('제품명'), findsWidgets); // 필터와 테이블 헤더
      expect(find.text('제품 코드'), findsOneWidget);
    });

    testWidgets('년월 선택기가 현재 년월을 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 년월 선택기 확인 (현재 년월이 표시되어야 함)
      final now = DateTime.now();
      final currentYearMonth = '${now.year}년 ${now.month.toString().padLeft(2, '0')}월';
      expect(find.text(currentYearMonth), findsOneWidget);
    });

    testWidgets('년월 선택기에 캘린더 아이콘이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 캘린더 아이콘 확인
      expect(find.byIcon(Icons.calendar_today), findsOneWidget);
      expect(find.byIcon(Icons.arrow_drop_down), findsOneWidget);
    });

    testWidgets('조회 버튼이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 조회 버튼 텍스트 확인
      final searchButton = find.text('조회');
      expect(searchButton, findsOneWidget);

      // 검색 아이콘 확인
      expect(find.byIcon(Icons.search), findsWidgets);
    });

    testWidgets('필터 초기화 버튼이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // AppBar의 새로고침 버튼 확인
      expect(find.byIcon(Icons.refresh), findsOneWidget);
    });

    testWidgets('SearchFilterWidget이 올바른 타입으로 생성된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // SearchFilterWidget 찾기
      final searchFilters = tester.widgetList<SearchFilterWidget>(
        find.byType(SearchFilterWidget),
      );

      expect(searchFilters.length, 3); // 거래처명, 제품명, 제품 코드

      // 모든 필터가 textInput 타입인지 확인
      for (final filter in searchFilters) {
        expect(filter.filterType, FilterType.textInput);
        expect(filter.onChanged, isNotNull);
      }
    });

    testWidgets('거래처명 입력 필드에 힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 힌트 텍스트 확인
      expect(find.text('거래처명 입력 (예: 농협)'), findsOneWidget);
    });

    testWidgets('제품명 입력 필드에 힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 힌트 텍스트 확인
      expect(find.text('제품명 입력 (예: 진라면)'), findsOneWidget);
    });

    testWidgets('제품 코드 입력 필드에 힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 힌트 텍스트 확인
      expect(find.text('제품 코드 입력 (예: P001)'), findsOneWidget);
    });

    testWidgets('합계 정보 라벨이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 합계 정보 라벨 확인
      expect(find.text('총 건수'), findsOneWidget);
      expect(find.text('총 수량'), findsOneWidget);
      expect(find.text('총 금액'), findsOneWidget);
    });

    testWidgets('합계 정보 아이콘이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 합계 아이콘 확인
      expect(find.byIcon(Icons.receipt_long), findsWidgets);
      expect(find.byIcon(Icons.inventory_2), findsWidgets);
      expect(find.byIcon(Icons.payments), findsWidgets);
    });

    testWidgets('평균 증감율 정보가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 평균 증감율 라벨 확인 (Mock 데이터에 증감율이 있으므로 표시되어야 함)
      expect(find.textContaining('평균 증감율'), findsOneWidget);

      // 증감율 아이콘 확인 (증가 또는 감소 아이콘 중 하나)
      final trendIcons = find.byWidgetPredicate(
        (widget) =>
            widget is Icon &&
            (widget.icon == Icons.trending_up ||
                widget.icon == Icons.trending_down),
      );
      expect(trendIcons, findsOneWidget);
    });

    testWidgets('매출 테이블이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // DataTable이 표시되는지 확인 (Mock 데이터가 있으므로)
      expect(find.byType(DataTable), findsOneWidget);
    });

    testWidgets('테이블 헤더가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ElectronicSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 테이블 헤더 확인
      expect(find.text('거래처'), findsOneWidget);
      expect(find.text('수량'), findsOneWidget);
      expect(find.text('금액'), findsOneWidget);
      expect(find.text('증감율'), findsOneWidget);
    });
  });
}
