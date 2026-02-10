import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/ordered_item_list.dart';

void main() {
  group('OrderedItemList Widget Tests', () {
    testWidgets('빈 목록', (WidgetTester tester) async {
      // given
      final widget = _buildWidget([]);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('주문한 제품이 없습니다'), findsOneWidget);
    });

    testWidgets('제품 개수 표시', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 5,
          totalQuantityPieces: 100,
          isCancelled: false,
        ),
        const OrderedItem(
          productCode: '01101124',
          productName: '허니 머스타드소스 240g',
          totalQuantityBoxes: 3,
          totalQuantityPieces: 60,
          isCancelled: false,
        ),
        const OrderedItem(
          productCode: '01101125',
          productName: '스위트 칠리소스 240g',
          totalQuantityBoxes: 2,
          totalQuantityPieces: 40,
          isCancelled: false,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('주문한 제품 (3)'), findsOneWidget);
    });

    testWidgets('제품코드 + 제품명 표시', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 5,
          totalQuantityPieces: 100,
          isCancelled: false,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.byWidgetPredicate((widget) {
        if (widget is RichText) {
          final text = widget.text.toPlainText();
          return text.contains('(01101123)') && text.contains('갈릭 아이올리소스 240g');
        }
        return false;
      }), findsOneWidget);
    });

    testWidgets('박스수량 표시 (정수)', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 5,
          totalQuantityPieces: 100,
          isCancelled: false,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('5박스'), findsOneWidget);
    });

    testWidgets('박스수량 표시 (소수)', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 60.5,
          totalQuantityPieces: 1150,
          isCancelled: false,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('60.5박스'), findsOneWidget);
    });

    testWidgets('낱개수량 표시 (콤마)', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 60.5,
          totalQuantityPieces: 1150,
          isCancelled: false,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('1,150개'), findsOneWidget);
    });

    testWidgets('취소된 제품 [취소] 라벨 표시', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 5,
          totalQuantityPieces: 100,
          isCancelled: true,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.byWidgetPredicate((widget) {
        if (widget is RichText) {
          final text = widget.text.toPlainText();
          return text.contains('[취소]');
        }
        return false;
      }), findsOneWidget);
    });

    testWidgets('취소되지 않은 제품에는 [취소] 미표시', (WidgetTester tester) async {
      // given
      final items = [
        const OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          totalQuantityBoxes: 5,
          totalQuantityPieces: 100,
          isCancelled: false,
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.byWidgetPredicate((widget) {
        if (widget is RichText) {
          final text = widget.text.toPlainText();
          return text.contains('[취소]');
        }
        return false;
      }), findsNothing);
    });
  });
}

Widget _buildWidget(List<OrderedItem> items) {
  return MaterialApp(
    home: Scaffold(
      body: SingleChildScrollView(
        child: OrderedItemList(items: items),
      ),
    ),
  );
}
