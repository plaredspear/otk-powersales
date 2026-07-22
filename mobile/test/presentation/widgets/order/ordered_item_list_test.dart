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

  group('OrderedItemList - 취소요청/주문취소 비교 배지 2종 (Spec #845 / 2026-07-22 통합)', () {
    testWidgets('요청+SAP취소 반영 → 취소요청+주문 취소 배지, 취소사유 표시', (tester) async {
      await tester.pumpWidget(buildList([
        item(
          isCancelRequested: true,
          isCancelledBySap: true,
          cancelReason: 'S2 [영업] 고객사정에 의한 취소',
        ),
      ]));

      // 취소요청은 문구 없는 주황 동그라미(dot) — 텍스트가 아니라 key 로 확인.
      expect(find.byKey(const ValueKey('cancelRequestedDot')), findsOneWidget);
      expect(find.text('주문 취소'), findsOneWidget);
      expect(find.text('취소사유: S2 [영업] 고객사정에 의한 취소'), findsOneWidget);
      expect(find.text('결품'), findsNothing);
    });

    testWidgets('결품 → 결품 배지, 결품사유 표시', (tester) async {
      await tester.pumpWidget(buildList([
        item(isOutOfStock: true, outOfStockReason: 'L1 [물류] 재고부족'),
      ]));

      expect(find.text('결품'), findsOneWidget);
      expect(find.text('결품사유: L1 [물류] 재고부족'), findsOneWidget);
      expect(find.text('주문 취소'), findsNothing);
      expect(find.byKey(const ValueKey('cancelRequestedDot')), findsNothing);
    });

    testWidgets('요청만(미반영) → 취소요청 동그라미만, 사유줄 없음', (tester) async {
      await tester.pumpWidget(buildList([
        item(isCancelRequested: true),
      ]));

      expect(find.byKey(const ValueKey('cancelRequestedDot')), findsOneWidget);
      expect(find.text('주문 취소'), findsNothing);
      expect(find.text('결품'), findsNothing);
      expect(find.textContaining('취소사유:'), findsNothing);
      expect(find.textContaining('결품사유:'), findsNothing);
    });

    testWidgets('정상 → 배지/사유 없음', (tester) async {
      await tester.pumpWidget(buildList([item()]));

      expect(find.byKey(const ValueKey('cancelRequestedDot')), findsNothing);
      expect(find.text('주문 취소'), findsNothing);
      expect(find.text('결품'), findsNothing);
      expect(find.textContaining('사유:'), findsNothing);
    });

    testWidgets('SAP 상세 취소(isCancelledBySap) → 주문 취소 배지', (tester) async {
      await tester.pumpWidget(buildList([
        item(isCancelledBySap: true, cancelReason: 'S1 [영업] 주문오류분 취소'),
      ]));

      expect(find.text('주문 취소'), findsOneWidget);
      expect(find.text('취소사유: S1 [영업] 주문오류분 취소'), findsOneWidget);
      // 구 '[주문 취소]' 접두는 더 이상 렌더하지 않는다.
      final richTexts = tester.widgetList<RichText>(find.byType(RichText));
      expect(
        richTexts.any((rt) => rt.text.toPlainText().contains('[주문 취소]')),
        isFalse,
      );
    });

    testWidgets('마이그레이션 취소(isCancelled) → 주문 취소 배지(접두 아님)', (tester) async {
      await tester.pumpWidget(buildList([item(isCancelled: true)]));

      expect(find.text('주문 취소'), findsOneWidget);
      final richTexts = tester.widgetList<RichText>(find.byType(RichText));
      expect(
        richTexts.any((rt) => rt.text.toPlainText().contains('[주문 취소]')),
        isFalse,
      );
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
      expect(find.text('주문 취소'), findsNothing);
      expect(find.text('결품사유: L1 [물류] 재고부족'), findsOneWidget);
      expect(find.textContaining('취소사유:'), findsNothing);
    });
  });

  group('OrderedItemList - 취소 배지 설명 info 아이콘', () {
    testWidgets('취소요청/주문취소 배지가 없으면 info 아이콘 미노출', (tester) async {
      await tester.pumpWidget(buildList([item()]));
      expect(find.byIcon(Icons.info_outline), findsNothing);
    });

    testWidgets('결품만 있으면(취소 아님) info 아이콘 미노출', (tester) async {
      await tester.pumpWidget(buildList([
        item(isOutOfStock: true, outOfStockReason: 'L1 [물류] 재고부족'),
      ]));
      expect(find.byIcon(Icons.info_outline), findsNothing);
    });

    testWidgets('취소요청 배지가 있으면 info 아이콘 노출', (tester) async {
      await tester.pumpWidget(buildList([item(isCancelRequested: true)]));
      expect(find.byIcon(Icons.info_outline), findsOneWidget);
    });

    testWidgets('info 아이콘 탭 → 취소 상태 안내 시트(취소요청/주문 취소 설명)', (tester) async {
      await tester.pumpWidget(buildList([
        item(isCancelRequested: true, isCancelledBySap: true),
      ]));

      await tester.tap(find.byIcon(Icons.info_outline));
      await tester.pumpAndSettle();

      expect(find.text('취소 상태 안내'), findsOneWidget);
      // 시트 안에 두 배지 라벨 + 설명 문구.
      expect(find.text('취소를 요청해 정상적으로 접수된 상태입니다. 실제 취소 처리는 이후 진행됩니다.'),
          findsOneWidget);
      expect(find.text('주문 취소 처리가 완료된 상태입니다.'), findsOneWidget);
    });
  });
}
