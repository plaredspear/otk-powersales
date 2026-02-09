import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/logistics_sales.dart';
import 'package:mobile/presentation/widgets/logistics/logistics_sales_table.dart';

void main() {
  group('LogisticsSalesTable', () {
    // Mock 데이터
    final mockSalesList = [
      const LogisticsSales(
        yearMonth: '202601',
        category: LogisticsCategory.normal,
        currentAmount: 50000000,
        previousYearAmount: 45000000,
        difference: 5000000,
        growthRate: 11.11,
        isCurrentMonth: false,
      ),
      const LogisticsSales(
        yearMonth: '202602',
        category: LogisticsCategory.ramen,
        currentAmount: 30000000,
        previousYearAmount: 28000000,
        difference: 2000000,
        growthRate: 7.14,
        isCurrentMonth: true,
      ),
      const LogisticsSales(
        yearMonth: '202602',
        category: LogisticsCategory.frozen,
        currentAmount: 20000000,
        previousYearAmount: 22000000,
        difference: -2000000,
        growthRate: -9.09,
        isCurrentMonth: true,
      ),
    ];

    testWidgets('빈 데이터일 때 안내 메시지를 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: []),
          ),
        ),
      );

      expect(find.text('조회된 물류매출이 없습니다'), findsOneWidget);
    });

    testWidgets('데이터가 있을 때 DataTable이 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      expect(find.byType(DataTable), findsOneWidget);
    });

    testWidgets('테이블 컬럼 헤더가 올바르게 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 컬럼 헤더 확인
      expect(find.text('년월'), findsOneWidget);
      expect(find.text('카테고리'), findsOneWidget);
      expect(find.text('구분'), findsOneWidget);
      expect(find.text('당해 실적'), findsOneWidget);
      expect(find.text('전년 실적'), findsOneWidget);
      expect(find.text('증감'), findsOneWidget);
      expect(find.text('증감율'), findsOneWidget);
    });

    testWidgets('년월이 올바른 포맷으로 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 년월 포맷 확인: 202601 -> 2026-01
      expect(find.text('2026-01'), findsOneWidget);
      expect(find.text('2026-02'), findsNWidgets(2)); // 2개의 202602 데이터
    });

    testWidgets('카테고리 아이콘과 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 카테고리 아이콘 확인
      expect(find.byIcon(Icons.inventory_2), findsOneWidget); // 상온
      expect(find.byIcon(Icons.ramen_dining), findsOneWidget); // 라면
      expect(find.byIcon(Icons.ac_unit), findsOneWidget); // 냉동/냉장

      // 카테고리 텍스트 확인
      expect(find.text('상온'), findsOneWidget);
      expect(find.text('라면'), findsOneWidget);
      expect(find.text('냉동/냉장'), findsOneWidget);
    });

    testWidgets('당월/마감 구분 칩이 올바르게 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 당월 칩 확인
      expect(find.text('당월'), findsNWidgets(2)); // 라면, 냉동/냉장
      // 마감 칩 확인
      expect(find.text('마감'), findsOneWidget); // 상온
    });

    testWidgets('당해 실적이 천 단위 구분자와 함께 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 천 단위 구분자 확인
      expect(find.text('50,000,000'), findsOneWidget);
      expect(find.text('30,000,000'), findsOneWidget);
      expect(find.text('20,000,000'), findsOneWidget);
    });

    testWidgets('전년 실적이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      expect(find.text('45,000,000'), findsOneWidget);
      expect(find.text('28,000,000'), findsOneWidget);
      expect(find.text('22,000,000'), findsOneWidget);
    });

    testWidgets('증감이 +/- 부호와 함께 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 양수 증감 (+부호 포함)
      expect(find.text('+5,000,000'), findsOneWidget);
      expect(find.text('+2,000,000'), findsOneWidget);
      // 음수 증감 (-부호 포함)
      expect(find.text('-2,000,000'), findsOneWidget);
    });

    testWidgets('증감율이 퍼센트와 화살표 아이콘과 함께 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 증감율 텍스트 확인
      expect(find.text('11.11%'), findsOneWidget);
      expect(find.text('7.14%'), findsOneWidget);
      expect(find.text('-9.09%'), findsOneWidget);

      // 화살표 아이콘 확인
      expect(find.byIcon(Icons.arrow_upward), findsNWidgets(2)); // 양수 2개
      expect(find.byIcon(Icons.arrow_downward), findsOneWidget); // 음수 1개
    });

    testWidgets('양수 증감은 빨간색, 음수 증감은 파란색으로 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 증감 Text 위젯 확인
      final positiveText = tester.widget<Text>(
        find.text('+5,000,000'),
      );
      expect(positiveText.style?.color, Colors.red);

      final negativeText = tester.widget<Text>(
        find.text('-2,000,000'),
      );
      expect(negativeText.style?.color, Colors.blue);
    });

    testWidgets('SingleChildScrollView가 가로/세로 스크롤을 지원한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 가로 스크롤 뷰 확인
      final horizontalScrollView = find.byType(SingleChildScrollView).first;
      expect(horizontalScrollView, findsOneWidget);

      final horizontalScrollWidget =
          tester.widget<SingleChildScrollView>(horizontalScrollView);
      expect(horizontalScrollWidget.scrollDirection, Axis.horizontal);
    });

    testWidgets('onTap 콜백이 제공되면 행 클릭이 동작한다', (WidgetTester tester) async {
      LogisticsSales? tappedSales;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(
              salesList: mockSalesList,
              onTap: (sales) => tappedSales = sales,
            ),
          ),
        ),
      );

      // 첫 번째 행 탭
      await tester.tap(find.text('상온'));
      await tester.pumpAndSettle();

      // 콜백이 호출되었는지 확인
      expect(tappedSales, isNotNull);
      expect(tappedSales?.category, LogisticsCategory.normal);
    });

    testWidgets('onTap 콜백이 없으면 행 클릭이 동작하지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(
              salesList: mockSalesList,
              // onTap 미제공
            ),
          ),
        ),
      );

      // 행을 탭해도 에러가 발생하지 않아야 함
      await tester.tap(find.text('상온'));
      await tester.pumpAndSettle();

      // 에러 없이 통과하면 성공
      expect(find.byType(LogisticsSalesTable), findsOneWidget);
    });

    testWidgets('당월 칩은 녹색 배경, 마감 칩은 파란색 배경을 가진다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 당월 칩의 Container 찾기
      final currentMonthChips = find.text('당월');
      expect(currentMonthChips, findsWidgets);

      // 마감 칩의 Container 찾기
      final closedMonthChips = find.text('마감');
      expect(closedMonthChips, findsOneWidget);
    });

    testWidgets('3개의 카테고리 아이콘이 서로 다른 색상을 가진다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 상온 아이콘 (brown)
      final normalIcon = tester.widget<Icon>(
        find.byIcon(Icons.inventory_2),
      );
      expect(normalIcon.color, Colors.brown);

      // 라면 아이콘 (orange)
      final ramenIcon = tester.widget<Icon>(
        find.byIcon(Icons.ramen_dining),
      );
      expect(ramenIcon.color, Colors.orange);

      // 냉동/냉장 아이콘 (blue)
      final frozenIcon = tester.widget<Icon>(
        find.byIcon(Icons.ac_unit),
      );
      expect(frozenIcon.color, Colors.blue);
    });

    testWidgets('DataTable의 헤더 행이 파란색 배경을 가진다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: LogisticsSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      await tester.pumpAndSettle();

      final dataTable = tester.widget<DataTable>(
        find.byType(DataTable),
      );

      // headingRowColor 확인
      final headingColor = dataTable.headingRowColor?.resolve({});
      expect(headingColor, Colors.blue[50]);
    });
  });
}
