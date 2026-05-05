import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/delivery_info_popup.dart';

void main() {
  Widget buildPopup(ProcessingItem item) {
    return MaterialApp(
      home: Scaffold(body: DeliveryInfoPopup(processingItem: item)),
    );
  }

  group('DeliveryInfoPopup - 4종 메시지 분기', () {
    testWidgets('PENDING — 대기 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(DeliveryStatus.pending)));
      expect(find.text('배송 대기 중입니다.'), findsOneWidget);
      expect(find.text('대기'), findsOneWidget);
    });

    testWidgets('SHIPPING — 배송중 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(DeliveryStatus.shipping)));
      expect(find.text('배송이 진행 중입니다.'), findsOneWidget);
      expect(find.text('배송중'), findsOneWidget);
    });

    testWidgets('DELIVERED — 배송 완료 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(DeliveryStatus.delivered)));
      expect(find.text('배송이 완료되었습니다.'), findsOneWidget);
      expect(find.text('배송 완료'), findsOneWidget);
    });

    testWidgets('OUT_OF_STOCK — 결품 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(DeliveryStatus.outOfStock)));
      expect(find.text('결품으로 인해 배송할 수 없습니다.'), findsOneWidget);
      expect(find.text('결품'), findsOneWidget);
    });
  });
}

ProcessingItem _item(DeliveryStatus status) => ProcessingItem(
      productCode: 'P001',
      productName: '예시 상품',
      deliveredQuantity: '0 EA',
      deliveryStatus: status,
    );
