import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/electronic_sales.dart';
import 'package:mobile/presentation/widgets/electronic/electronic_sales_table.dart';

void main() {
  group('ElectronicSalesTable', () {
    // 테스트용 Mock 데이터
    final mockSalesList = [
      const ElectronicSales(
        yearMonth: '202602',
        customerName: '농협',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 100,
        previousYearAmount: 900000,
        growthRate: 11.11,
      ),
      const ElectronicSales(
        yearMonth: '202602',
        customerName: '농협',
        productName: '케첩',
        productCode: 'P002',
        amount: 500000,
        quantity: 50,
        previousYearAmount: 550000,
        growthRate: -9.09,
      ),
      const ElectronicSales(
        yearMonth: '202602',
        customerName: 'GS25',
        productName: '진라면',
        productCode: 'P001',
        amount: 800000,
        quantity: 80,
        previousYearAmount: 700000,
        growthRate: 14.29,
      ),
    ];

    testWidgets('빈 리스트일 때 안내 메시지가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: []),
          ),
        ),
      );

      expect(find.text('조회된 전산매출이 없습니다'), findsOneWidget);
    });

    testWidgets('데이터가 있을 때 DataTable이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      expect(find.byType(DataTable), findsOneWidget);
    });

    testWidgets('테이블 헤더가 올바르게 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 테이블 헤더 확인
      expect(find.text('거래처'), findsOneWidget);
      expect(find.text('제품명'), findsOneWidget);
      expect(find.text('제품코드'), findsOneWidget);
      expect(find.text('수량'), findsOneWidget);
      expect(find.text('금액'), findsOneWidget);
      expect(find.text('증감율'), findsOneWidget);
    });

    testWidgets('거래처명이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 거래처명 확인
      expect(find.text('농협'), findsNWidgets(2)); // 2개 row
      expect(find.text('GS25'), findsOneWidget);
    });

    testWidgets('제품명이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 제품명 확인
      expect(find.text('진라면'), findsNWidgets(2)); // 2개 row
      expect(find.text('케첩'), findsOneWidget);
    });

    testWidgets('제품코드가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 제품코드 확인
      expect(find.text('P001'), findsNWidgets(2)); // 2개 row
      expect(find.text('P002'), findsOneWidget);
    });

    testWidgets('수량이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 수량 확인
      expect(find.text('100'), findsOneWidget);
      expect(find.text('50'), findsOneWidget);
      expect(find.text('80'), findsOneWidget);
    });

    testWidgets('금액이 포맷되어 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 금액이 천 단위 콤마로 포맷되어 표시되는지 확인
      expect(find.text('1,000,000'), findsOneWidget);
      expect(find.text('500,000'), findsOneWidget);
      expect(find.text('800,000'), findsOneWidget);
    });

    testWidgets('양수 증감율이 녹색 화살표와 함께 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 양수 증감율 텍스트 확인
      expect(find.text('11.1%'), findsOneWidget);
      expect(find.text('14.3%'), findsOneWidget);

      // 위쪽 화살표 아이콘 확인 (양수 증감율에 사용)
      expect(find.byIcon(Icons.arrow_upward), findsNWidgets(2));
    });

    testWidgets('음수 증감율이 빨간색 화살표와 함께 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // 음수 증감율 텍스트 확인
      expect(find.text('-9.1%'), findsOneWidget);

      // 아래쪽 화살표 아이콘 확인 (음수 증감율에 사용)
      expect(find.byIcon(Icons.arrow_downward), findsOneWidget);
    });

    testWidgets('증감율이 null일 때 하이픈이 표시된다', (WidgetTester tester) async {
      final salesWithNullGrowth = [
        const ElectronicSales(
          yearMonth: '202602',
          customerName: '테스트',
          productName: '테스트 제품',
          productCode: 'TEST',
          amount: 100000,
          quantity: 10,
          growthRate: null,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: salesWithNullGrowth),
          ),
        ),
      );

      // 증감율이 null일 때 '-'가 표시되는지 확인
      expect(find.text('-'), findsWidgets);
    });

    testWidgets('행을 탭하면 onTap 콜백이 호출된다', (WidgetTester tester) async {
      ElectronicSales? tappedSales;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(
              salesList: mockSalesList,
              onTap: (sales) {
                tappedSales = sales;
              },
            ),
          ),
        ),
      );

      // 첫 번째 행 탭
      await tester.tap(find.text('농협').first);
      await tester.pumpAndSettle();

      // 콜백이 호출되었는지 확인
      expect(tappedSales, isNotNull);
      expect(tappedSales?.customerName, '농협');
      expect(tappedSales?.productName, '진라면');
    });

    testWidgets('onTap이 null이면 행을 탭할 수 없다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(
              salesList: mockSalesList,
              onTap: null,
            ),
          ),
        ),
      );

      // DataTable이 표시되지만 onSelectChanged가 null이므로 탭이 동작하지 않음
      expect(find.byType(DataTable), findsOneWidget);

      // DataRow를 찾아서 onSelectChanged가 null인지 확인
      final dataTable = tester.widget<DataTable>(find.byType(DataTable));
      expect(dataTable.rows.first.onSelectChanged, isNull);
    });

    testWidgets('테이블이 수평 스크롤 가능하다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // SingleChildScrollView가 수평 스크롤을 지원하는지 확인
      final scrollViews = tester.widgetList<SingleChildScrollView>(
        find.byType(SingleChildScrollView),
      );

      // 수평 스크롤뷰가 있는지 확인
      expect(
        scrollViews.any((widget) => widget.scrollDirection == Axis.horizontal),
        true,
      );
    });

    testWidgets('3개 항목이 모두 테이블에 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ElectronicSalesTable(salesList: mockSalesList),
          ),
        ),
      );

      // DataTable의 rows 개수 확인
      final dataTable = tester.widget<DataTable>(find.byType(DataTable));
      expect(dataTable.rows.length, 3);
    });
  });
}
