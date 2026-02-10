import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/rejected_item_list.dart';

void main() {
  group('RejectedItemList Widget Tests', () {
    testWidgets('반려 제품 개수 표시', (WidgetTester tester) async {
      // given
      final items = [
        const RejectedItem(
          productCode: '01101123',
          productName: '오감포차_크림새우180G',
          orderQuantityBoxes: 1,
          rejectionReason: '납품일자가 업무일이 아닙니다',
        ),
        const RejectedItem(
          productCode: '01101124',
          productName: '오감포차_마늘새우180G',
          orderQuantityBoxes: 2,
          rejectionReason: '재고 부족',
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('주문 반려 제품 (2)'), findsOneWidget);
    });

    testWidgets('제품명 + 제품코드 표시', (WidgetTester tester) async {
      // given
      final items = [
        const RejectedItem(
          productCode: '01101123',
          productName: '오감포차_크림새우180G',
          orderQuantityBoxes: 1,
          rejectionReason: '납품일자가 업무일이 아닙니다',
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('오감포차_크림새우180G'), findsOneWidget);
      expect(find.textContaining('(01101123)'), findsOneWidget);
    });

    testWidgets('주문수량 표시', (WidgetTester tester) async {
      // given
      final items = [
        const RejectedItem(
          productCode: '01101123',
          productName: '오감포차_크림새우180G',
          orderQuantityBoxes: 1,
          rejectionReason: '납품일자가 업무일이 아닙니다',
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('1 BOX'), findsOneWidget);
    });

    testWidgets('반려사유 표시', (WidgetTester tester) async {
      // given
      final items = [
        const RejectedItem(
          productCode: '01101123',
          productName: '오감포차_크림새우180G',
          orderQuantityBoxes: 1,
          rejectionReason: '납품일자가 업무일이 아닙니다',
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('반려사유: 납품일자가 업무일이 아닙니다'), findsOneWidget);
    });

    testWidgets('여러 제품 렌더링 확인', (WidgetTester tester) async {
      // given
      final items = [
        const RejectedItem(
          productCode: '01101123',
          productName: '오감포차_크림새우180G',
          orderQuantityBoxes: 1,
          rejectionReason: '납품일자가 업무일이 아닙니다',
        ),
        const RejectedItem(
          productCode: '01101124',
          productName: '오감포차_마늘새우180G',
          orderQuantityBoxes: 2,
          rejectionReason: '재고 부족',
        ),
        const RejectedItem(
          productCode: '01101125',
          productName: '오감포차_버터새우180G',
          orderQuantityBoxes: 3,
          rejectionReason: '주문 수량 초과',
        ),
      ];
      final widget = _buildWidget(items);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('오감포차_크림새우180G'), findsOneWidget);
      expect(find.textContaining('오감포차_마늘새우180G'), findsOneWidget);
      expect(find.textContaining('오감포차_버터새우180G'), findsOneWidget);
      expect(find.textContaining('1 BOX'), findsOneWidget);
      expect(find.textContaining('2 BOX'), findsOneWidget);
      expect(find.textContaining('3 BOX'), findsOneWidget);
      expect(find.textContaining('납품일자가 업무일이 아닙니다'), findsOneWidget);
      expect(find.textContaining('재고 부족'), findsOneWidget);
      expect(find.textContaining('주문 수량 초과'), findsOneWidget);
    });
  });
}

Widget _buildWidget(List<RejectedItem> items) {
  return MaterialApp(
    home: Scaffold(
      body: SingleChildScrollView(
        child: RejectedItemList(rejectedItems: items),
      ),
    ),
  );
}
