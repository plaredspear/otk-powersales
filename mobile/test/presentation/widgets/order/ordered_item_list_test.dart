import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/ordered_item_list.dart';

void main() {
  Widget buildList(List<OrderedItem> items) {
    return MaterialApp(
      home: Scaffold(body: OrderedItemList(items: items)),
    );
  }

  OrderedItem item({
    bool isCancelled = false,
    bool isCancelRequested = false,
    bool isOutOfStock = false,
    String? outOfStockReason,
    bool isCancelledBySap = false,
    String? cancelReason,
  }) {
    return OrderedItem(
      orderProductId: 101,
      productCode: '26010007',
      productName: '콩기름 0.9L',
      totalQuantityBoxes: 1,
      totalQuantityPieces: 12,
      isCancelled: isCancelled,
      isCancelRequested: isCancelRequested,
      isOutOfStock: isOutOfStock,
      outOfStockReason: outOfStockReason,
      isCancelledBySap: isCancelledBySap,
      cancelReason: cancelReason,
    );
  }

  group('OrderedItemList - 취소요청/실제취소 비교 배지 (Spec #845 P2-M §4)', () {
    testWidgets('요청+SAP취소 반영 → 취소요청+SAP취소됨 배지, 취소사유 표시', (tester) async {
      await tester.pumpWidget(buildList([
        item(
          isCancelRequested: true,
          isCancelledBySap: true,
          cancelReason: 'S2 [영업] 고객사정에 의한 취소',
        ),
      ]));

      expect(find.text('취소요청'), findsOneWidget);
      expect(find.text('SAP취소됨'), findsOneWidget);
      expect(find.text('취소사유: S2 [영업] 고객사정에 의한 취소'), findsOneWidget);
      expect(find.text('결품'), findsNothing);
    });

    testWidgets('결품 → 결품 배지, 결품사유 표시', (tester) async {
      await tester.pumpWidget(buildList([
        item(isOutOfStock: true, outOfStockReason: 'L1 [물류] 재고부족'),
      ]));

      expect(find.text('결품'), findsOneWidget);
      expect(find.text('결품사유: L1 [물류] 재고부족'), findsOneWidget);
      expect(find.text('SAP취소됨'), findsNothing);
      expect(find.text('취소요청'), findsNothing);
    });

    testWidgets('요청만(미반영) → 취소요청 배지만, 사유줄 없음', (tester) async {
      await tester.pumpWidget(buildList([
        item(isCancelRequested: true),
      ]));

      expect(find.text('취소요청'), findsOneWidget);
      expect(find.text('SAP취소됨'), findsNothing);
      expect(find.text('결품'), findsNothing);
      expect(find.textContaining('취소사유:'), findsNothing);
      expect(find.textContaining('결품사유:'), findsNothing);
    });

    testWidgets('정상 → 배지/사유 없음', (tester) async {
      await tester.pumpWidget(buildList([item()]));

      expect(find.text('취소요청'), findsNothing);
      expect(find.text('SAP취소됨'), findsNothing);
      expect(find.text('결품'), findsNothing);
      expect(find.text('[주문 취소] '), findsNothing);
      expect(find.textContaining('사유:'), findsNothing);
    });

    testWidgets('마이그레이션 취소(isCancelled) → [주문 취소] 접두 유지', (tester) async {
      await tester.pumpWidget(buildList([item(isCancelled: true)]));

      // '[주문 취소]' 는 제품명과 함께 RichText 로 렌더 → RichText 존재 확인.
      final richTexts = tester.widgetList<RichText>(find.byType(RichText));
      final hasPrefix = richTexts.any(
        (rt) => rt.text.toPlainText().contains('[주문 취소]'),
      );
      expect(hasPrefix, isTrue);
      expect(find.text('SAP취소됨'), findsNothing);
    });

    testWidgets('방어적 결품 우선 — 결품+SAP취소 동시 true → 결품만', (tester) async {
      await tester.pumpWidget(buildList([
        item(
          isOutOfStock: true,
          outOfStockReason: 'L1 [물류] 재고부족',
          isCancelledBySap: true,
          cancelReason: 'S2 [영업] 고객사정에 의한 취소',
        ),
      ]));

      expect(find.text('결품'), findsOneWidget);
      expect(find.text('SAP취소됨'), findsNothing);
      expect(find.text('결품사유: L1 [물류] 재고부족'), findsOneWidget);
      expect(find.textContaining('취소사유:'), findsNothing);
    });
  });
}
