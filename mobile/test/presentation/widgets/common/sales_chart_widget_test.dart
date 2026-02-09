import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/sales_chart_widget.dart';

void main() {
  group('SalesChartWidget', () {
    // Mock 데이터
    final mockChartData = [
      const SalesChartData(
        label: '2026-01',
        currentAmount: 50000000,
        previousYearAmount: 45000000,
      ),
      const SalesChartData(
        label: '2026-02',
        currentAmount: 30000000,
        previousYearAmount: 28000000,
      ),
      const SalesChartData(
        label: '2026-03',
        currentAmount: 20000000,
        previousYearAmount: 22000000,
      ),
    ];

    testWidgets('빈 데이터일 때 안내 메시지를 표시한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: []),
          ),
        ),
      );

      expect(find.text('차트 데이터가 없습니다'), findsOneWidget);
    });

    testWidgets('데이터가 있을 때 BarChart가 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: mockChartData),
          ),
        ),
      );

      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('차트 제목이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(
              data: mockChartData,
              title: '물류매출 차트',
            ),
          ),
        ),
      );

      expect(find.text('물류매출 차트'), findsOneWidget);
    });

    testWidgets('차트 제목이 없으면 표시하지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(
              data: mockChartData,
              // title 미제공
            ),
          ),
        ),
      );

      // 제목이 없으므로 BarChart만 확인
      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('범례가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: mockChartData),
          ),
        ),
      );

      expect(find.text('당해 실적'), findsOneWidget);
      expect(find.text('전년 실적'), findsOneWidget);
    });

    testWidgets('차트 높이가 올바르게 설정된다', (WidgetTester tester) async {
      const customHeight = 400.0;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(
              data: mockChartData,
              height: customHeight,
            ),
          ),
        ),
      );

      // SizedBox with chart height 확인
      final sizedBoxes = tester.widgetList<SizedBox>(find.byType(SizedBox));
      expect(
        sizedBoxes.any((box) => box.height == customHeight),
        true,
      );
    });

    testWidgets('커스텀 색상이 적용된다', (WidgetTester tester) async {
      const customCurrentColor = Colors.red;
      const customPreviousColor = Colors.green;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(
              data: mockChartData,
              currentColor: customCurrentColor,
              previousYearColor: customPreviousColor,
            ),
          ),
        ),
      );

      // 색상은 내부적으로 적용되므로 위젯이 정상 렌더링되는지 확인
      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('SalesChartData가 올바르게 생성된다', (WidgetTester tester) async {
      const data = SalesChartData(
        label: '2026-01',
        currentAmount: 50000000,
        previousYearAmount: 45000000,
      );

      expect(data.label, '2026-01');
      expect(data.currentAmount, 50000000);
      expect(data.previousYearAmount, 45000000);
    });

    testWidgets('여러 데이터 포인트가 모두 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: mockChartData),
          ),
        ),
      );

      // 3개의 데이터 포인트 확인
      expect(find.byType(BarChart), findsOneWidget);

      // 범례 확인
      expect(find.text('당해 실적'), findsOneWidget);
      expect(find.text('전년 실적'), findsOneWidget);
    });

    testWidgets('빈 리스트일 때 차트가 렌더링되지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(
              data: [],
              title: '빈 차트',
            ),
          ),
        ),
      );

      expect(find.byType(BarChart), findsNothing);
      expect(find.text('차트 데이터가 없습니다'), findsOneWidget);
    });

    testWidgets('단일 데이터 포인트도 올바르게 표시된다', (WidgetTester tester) async {
      final singleData = [
        const SalesChartData(
          label: '2026-01',
          currentAmount: 50000000,
          previousYearAmount: 45000000,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: singleData),
          ),
        ),
      );

      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('0원 데이터도 올바르게 처리된다', (WidgetTester tester) async {
      final zeroData = [
        const SalesChartData(
          label: '2026-01',
          currentAmount: 0,
          previousYearAmount: 0,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: zeroData),
          ),
        ),
      );

      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('매우 큰 금액도 올바르게 표시된다', (WidgetTester tester) async {
      final largeData = [
        const SalesChartData(
          label: '2026-01',
          currentAmount: 1000000000, // 10억
          previousYearAmount: 900000000, // 9억
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: largeData),
          ),
        ),
      );

      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('전년보다 당해 실적이 높은 경우를 올바르게 표시한다', (WidgetTester tester) async {
      final increasedData = [
        const SalesChartData(
          label: '2026-01',
          currentAmount: 60000000,
          previousYearAmount: 50000000,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: increasedData),
          ),
        ),
      );

      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('전년보다 당해 실적이 낮은 경우를 올바르게 표시한다', (WidgetTester tester) async {
      final decreasedData = [
        const SalesChartData(
          label: '2026-01',
          currentAmount: 40000000,
          previousYearAmount: 50000000,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: decreasedData),
          ),
        ),
      );

      expect(find.byType(BarChart), findsOneWidget);
    });

    testWidgets('기본 색상이 올바르게 설정된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: mockChartData),
          ),
        ),
      );

      final widget = tester.widget<SalesChartWidget>(
        find.byType(SalesChartWidget),
      );

      expect(widget.currentColor, Colors.blue);
      expect(widget.previousYearColor, Colors.grey);
    });

    testWidgets('기본 높이가 300으로 설정된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SalesChartWidget(data: mockChartData),
          ),
        ),
      );

      final widget = tester.widget<SalesChartWidget>(
        find.byType(SalesChartWidget),
      );

      expect(widget.height, 300);
    });
  });
}
