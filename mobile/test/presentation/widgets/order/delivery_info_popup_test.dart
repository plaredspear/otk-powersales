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
      await tester.pumpWidget(buildPopup(_item(OrderDeliveryStatus.pending)));
      expect(find.text('배송 대기 중입니다.'), findsOneWidget);
      expect(find.text('대기'), findsOneWidget);
    });

    testWidgets('SHIPPING — 배송중 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(OrderDeliveryStatus.shipping)));
      expect(find.text('배송이 진행 중입니다.'), findsOneWidget);
      expect(find.text('배송중'), findsOneWidget);
    });

    testWidgets('DELIVERED — 배송 완료 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(OrderDeliveryStatus.delivered)));
      expect(find.text('배송이 완료되었습니다.'), findsOneWidget);
      expect(find.text('배송 완료'), findsOneWidget);
    });

    testWidgets('OUT_OF_STOCK — 결품 메시지 표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(OrderDeliveryStatus.outOfStock)));
      expect(find.text('결품으로 인해 배송할 수 없습니다.'), findsOneWidget);
      expect(find.text('결품'), findsOneWidget);
    });
  });

  group('DeliveryInfoPopup - 차량/기사 5필드 (Spec #595 Q5)', () {
    testWidgets('SHIPPING — 4행 표시 (차량/기사/연락처/예정시각)', (tester) async {
      final item = ProcessingItem(
        productCode: 'P001',
        productName: '진라면 매운맛',
        deliveredQuantity: '10 BOX (300 EA)',
        deliveryStatus: OrderDeliveryStatus.shipping,
        driverName: '홍길동',
        vehicle: '12가3456',
        driverPhone: '010-1234-5678',
        scheduleTime: '12:00',
        completeTime: null,
      );
      await tester.pumpWidget(buildPopup(item));

      expect(find.text('차량번호'), findsOneWidget);
      expect(find.text('12가3456'), findsOneWidget);
      expect(find.text('기사명'), findsOneWidget);
      expect(find.text('홍길동'), findsOneWidget);
      expect(find.text('연락처'), findsOneWidget);
      expect(find.text('010-1234-5678'), findsOneWidget);
      expect(find.text('배송 예정'), findsOneWidget);
      expect(find.text('12:00'), findsOneWidget);
      // completeTime null → 행 미표시
      expect(find.text('배송 완료'), findsNothing);
    });

    testWidgets('DELIVERED — 5행 모두 표시 (배송 완료 시각 포함)', (tester) async {
      final item = ProcessingItem(
        productCode: 'P001',
        productName: '진라면 매운맛',
        deliveredQuantity: '10 BOX (300 EA)',
        deliveryStatus: OrderDeliveryStatus.delivered,
        driverName: '홍길동',
        vehicle: '12가3456',
        driverPhone: '010-1234-5678',
        scheduleTime: '12:00',
        completeTime: '14:30',
      );
      await tester.pumpWidget(buildPopup(item));

      expect(find.text('차량번호'), findsOneWidget);
      expect(find.text('기사명'), findsOneWidget);
      expect(find.text('연락처'), findsOneWidget);
      expect(find.text('배송 예정'), findsOneWidget);
      expect(find.text('12:00'), findsOneWidget);
      // '배송 완료' 텍스트는 행 라벨 + 상태 표시 두 곳에서 등장 (status displayName 충돌).
      expect(find.text('배송 완료'), findsNWidgets(2));
      expect(find.text('14:30'), findsOneWidget);
    });

    testWidgets('차량/기사 5필드 모두 null — 차량/기사 행 모두 미표시', (tester) async {
      await tester.pumpWidget(buildPopup(_item(OrderDeliveryStatus.delivered)));

      expect(find.text('차량번호'), findsNothing);
      expect(find.text('기사명'), findsNothing);
      expect(find.text('연락처'), findsNothing);
      expect(find.text('배송 예정'), findsNothing);
      // 단, 기존 '배송 완료' 메시지(상태 메시지)는 여전히 표시됨 — 행 라벨로는 없음
      expect(find.text('배송이 완료되었습니다.'), findsOneWidget);
    });
  });

  group('ProcessingItem.hasNoDeliveryDetail (Spec #595 Q5)', () {
    test('5필드 모두 null → true', () {
      const item = ProcessingItem(
        productCode: 'P001',
        productName: 'n',
        deliveredQuantity: '0 BOX',
        deliveryStatus: OrderDeliveryStatus.pending,
      );
      expect(item.hasNoDeliveryDetail, isTrue);
    });

    test('한 필드라도 채워지면 false', () {
      const item = ProcessingItem(
        productCode: 'P001',
        productName: 'n',
        deliveredQuantity: '0 BOX',
        deliveryStatus: OrderDeliveryStatus.shipping,
        driverName: '홍길동',
      );
      expect(item.hasNoDeliveryDetail, isFalse);
    });
  });
}

ProcessingItem _item(String status) => ProcessingItem(
      productCode: 'P001',
      productName: '예시 상품',
      deliveredQuantity: '0 EA',
      deliveryStatus: status,
    );
