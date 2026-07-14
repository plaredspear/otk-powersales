import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/client_order_item_table.dart';

void main() {
  Widget buildTable({
    required List<ClientOrderItem> items,
    void Function(ClientOrderItem)? onItemTap,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: ClientOrderItemTable(items: items, onItemTap: onItemTap),
      ),
    );
  }

  group('ClientOrderItemTable - 4종 분기', () {
    testWidgets('4종 displayName 모두 표시 (대기/배송중/배송 완료/결품)', (tester) async {
      await tester.pumpWidget(buildTable(items: _items4));

      expect(find.text('대기'), findsOneWidget);
      expect(find.text('배송중'), findsOneWidget);
      expect(find.text('배송 완료'), findsOneWidget);
      expect(find.text('결품'), findsOneWidget);
    });

    testWidgets('SHIPPING/DELIVERED 만 onItemTap 활성', (tester) async {
      final tapped = <String>[];
      await tester.pumpWidget(buildTable(
        items: _items4,
        onItemTap: (item) => tapped.add(item.deliveryStatus),
      ));

      await tester.tap(find.text('대기'), warnIfMissed: false);
      await tester.tap(find.text('배송중'));
      await tester.tap(find.text('배송 완료'));
      await tester.tap(find.text('결품'), warnIfMissed: false);
      await tester.pump();

      expect(
        tapped,
        containsAllInOrder(
            [OrderDeliveryStatus.shipping, OrderDeliveryStatus.delivered]),
      );
      expect(tapped, hasLength(2));
    });
  });
}

const _items4 = [
  ClientOrderItem(
    productCode: 'P001',
    productName: '대기 상품',
    deliveredQuantity: '0 BOX',
    deliveryStatus: OrderDeliveryStatus.pending,
  ),
  ClientOrderItem(
    productCode: 'P002',
    productName: '배송중 상품',
    deliveredQuantity: '5 BOX',
    deliveryStatus: OrderDeliveryStatus.shipping,
  ),
  ClientOrderItem(
    productCode: 'P003',
    productName: '배송완료 상품',
    deliveredQuantity: '10 BOX',
    deliveryStatus: OrderDeliveryStatus.delivered,
  ),
  ClientOrderItem(
    productCode: 'P004',
    productName: '결품 상품',
    deliveredQuantity: '0 BOX',
    deliveryStatus: OrderDeliveryStatus.outOfStock,
  ),
];
