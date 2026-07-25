import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/out_of_stock_item_list.dart';

void main() {
  Widget buildList(List<OutOfStockItem> items) {
    return MaterialApp(
      home: Scaffold(body: OutOfStockItemList(outOfStockItems: items)),
    );
  }

  OutOfStockItem item({
    String productCode = '11910112',
    String productName = '타바스코_스콜피온소스 150ML',
    double orderQuantityBoxes = 1,
    String reason = 'L1 [물류] 재고부족',
  }) {
    return OutOfStockItem(
      productCode: productCode,
      productName: productName,
      orderQuantityBoxes: orderQuantityBoxes,
      reason: reason,
    );
  }

  testWidgets('미납 품목 섹션 — 헤더(건수)/품목명(코드)/수량/미납사유 표시', (tester) async {
    await tester.pumpWidget(buildList([item()]));

    expect(find.text('미납 품목 (1)'), findsOneWidget);
    expect(find.text('타바스코_스콜피온소스 150ML (11910112)'), findsOneWidget);
    expect(find.text('1 BOX'), findsOneWidget);
    expect(find.text('미납사유: L1 [물류] 재고부족'), findsOneWidget);
  });

  testWidgets('여러 미납 품목 — 건수 반영', (tester) async {
    await tester.pumpWidget(buildList([
      item(productCode: '11910112', productName: 'A'),
      item(productCode: '11910111', productName: 'B'),
    ]));

    expect(find.text('미납 품목 (2)'), findsOneWidget);
    expect(find.text('A (11910112)'), findsOneWidget);
    expect(find.text('B (11910111)'), findsOneWidget);
  });
}
