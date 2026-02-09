import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mobile/presentation/screens/pos_sales_screen.dart';
import 'package:mobile/presentation/widgets/common/date_picker_widget.dart';
import 'package:mobile/presentation/widgets/common/search_filter_widget.dart';

void main() {
  // 한국어 locale 초기화 (날짜 포맷팅용)
  setUpAll(() async {
    await initializeDateFormatting('ko_KR', null);
  });

  group('PosSalesScreen', () {
    testWidgets('화면이 정상적으로 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // AppBar 확인
      expect(find.text('POS 매출 조회'), findsOneWidget);

      // 필터 영역 확인
      expect(find.byType(DatePickerWidget), findsOneWidget);
      expect(find.byType(SearchFilterWidget), findsNWidgets(2)); // 매장명, 제품명
      expect(find.text('조회'), findsOneWidget);
    });

    testWidgets('필터 섹션이 올바른 레이블을 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 필터 레이블 확인
      expect(find.text('매장명'), findsOneWidget);
      expect(find.text('제품명'), findsOneWidget);
    });

    testWidgets('조회 버튼이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 조회 버튼 텍스트 확인
      final searchButton = find.text('조회');
      expect(searchButton, findsOneWidget);

      // 검색 아이콘이 화면에 표시됨 (여러 개 있을 수 있음)
      expect(find.byIcon(Icons.search), findsWidgets);
    });

    testWidgets('DatePickerWidget이 올바른 파라미터로 생성된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // DatePickerWidget 찾기
      final datePickerFinder = find.byType(DatePickerWidget);
      expect(datePickerFinder, findsOneWidget);

      final datePickerWidget = tester.widget<DatePickerWidget>(datePickerFinder);

      // 콜백이 설정되었는지 확인
      expect(datePickerWidget.onStartDateChanged, isNotNull);
      expect(datePickerWidget.onEndDateChanged, isNotNull);
    });

    testWidgets('SearchFilterWidget이 올바른 타입으로 생성된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // SearchFilterWidget 찾기
      final searchFilters = tester.widgetList<SearchFilterWidget>(
        find.byType(SearchFilterWidget),
      );

      expect(searchFilters.length, 2);

      // 모든 필터가 textInput 타입인지 확인
      for (final filter in searchFilters) {
        expect(filter.filterType, FilterType.textInput);
        expect(filter.onChanged, isNotNull);
      }
    });

    testWidgets('매장명 입력 필드에 힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 힌트 텍스트 확인
      expect(find.text('매장명 입력 (예: 이마트)'), findsOneWidget);
    });

    testWidgets('제품명 입력 필드에 힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 힌트 텍스트 확인
      expect(find.text('제품명 입력 (예: 진라면)'), findsOneWidget);
    });

    testWidgets('합계 정보 라벨이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
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
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 합계 아이콘 확인 (리스트 아이템에도 같은 아이콘이 있을 수 있음)
      expect(find.byIcon(Icons.receipt_long), findsWidgets);
      expect(find.byIcon(Icons.inventory_2), findsWidgets);
      expect(find.byIcon(Icons.payments), findsWidgets);
    });

    testWidgets('매출 리스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: PosSalesScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // ListView가 표시되는지 확인 (Mock 데이터가 있으므로)
      expect(find.byType(ListView), findsOneWidget);
    });
  });
}
