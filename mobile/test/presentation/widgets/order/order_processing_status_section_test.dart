import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/order_processing_status_section.dart';

void main() {
  Widget buildSection(List<ProcessingItem> items) {
    return MaterialApp(
      home: Scaffold(
        body: OrderProcessingStatusSection(
          processingStatus: OrderProcessingStatus(
            sapOrderNumber: '0300013650',
            items: items,
          ),
        ),
      ),
    );
  }

  ProcessingItem item(String status) => ProcessingItem(
        productCode: 'P001',
        productName: '진라면 매운맛',
        deliveredQuantity: '10 BOX (300 EA)',
        deliveryStatus: status,
      );

  group('OrderProcessingStatusSection - 상태 라벨', () {
    testWidgets('PENDING — 대기 표시', (tester) async {
      await tester.pumpWidget(buildSection([item(OrderDeliveryStatus.pending)]));
      expect(find.text('대기'), findsOneWidget);
    });

    testWidgets('SHIPPING — 배송중 표시', (tester) async {
      await tester.pumpWidget(buildSection([item(OrderDeliveryStatus.shipping)]));
      expect(find.text('배송중'), findsOneWidget);
    });

    testWidgets('DELIVERED — 공백 없는 배송완료 표시 (SF 조회 클래스 cls:157 정합)', (tester) async {
      await tester.pumpWidget(buildSection([item(OrderDeliveryStatus.delivered)]));
      expect(find.text('배송완료'), findsOneWidget);
      expect(find.text('배송 완료'), findsNothing);
    });

    testWidgets('OUT_OF_STOCK — 결품 표시 (DefaultReason 결품셋, 2026-07-23)', (tester) async {
      await tester.pumpWidget(buildSection([item(OrderDeliveryStatus.outOfStock)]));
      expect(find.text('결품'), findsOneWidget);
      expect(find.text('미납'), findsNothing);
      expect(find.text('대기'), findsNothing);
    });

    testWidgets('CANCELLED — 취소 표시 (DefaultReason 취소셋, 2026-07-23)', (tester) async {
      await tester.pumpWidget(buildSection([item(OrderDeliveryStatus.cancelled)]));
      expect(find.text('취소'), findsOneWidget);
      expect(find.text('미납'), findsNothing);
      expect(find.text('결품'), findsNothing);
    });

    testWidgets('UNKNOWN — 빈 라벨 (레거시 status=empty 정합)', (tester) async {
      await tester.pumpWidget(buildSection([item(OrderDeliveryStatus.unknown)]));
      expect(find.text('대기'), findsNothing);
      expect(find.text('배송중'), findsNothing);
      expect(find.text('배송완료'), findsNothing);
    });
  });
}
