import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order.dart';

void main() {
  group('Order', () {
    test('should create Order entity with all required fields', () {
      final order = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      expect(order.id, equals(1));
      expect(order.orderRequestNumber, equals('OP00000074'));
      expect(order.clientId, equals(100));
      expect(order.clientName, equals('천사푸드'));
      expect(order.orderDate, equals(DateTime(2026, 2, 5)));
      expect(order.deliveryDate, equals(DateTime(2026, 2, 8)));
      expect(order.totalAmount, equals(612000000));
      expect(order.approvalStatus, equals(ApprovalStatus.approved));
      expect(order.isClosed, equals(true));
    });

    test('should create a copy with modified fields using copyWith', () {
      final original = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final modified = original.copyWith(
        totalAmount: 700000000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: false,
      );

      expect(modified.id, equals(original.id));
      expect(modified.orderRequestNumber, equals(original.orderRequestNumber));
      expect(modified.clientId, equals(original.clientId));
      expect(modified.clientName, equals(original.clientName));
      expect(modified.orderDate, equals(original.orderDate));
      expect(modified.deliveryDate, equals(original.deliveryDate));
      expect(modified.totalAmount, equals(700000000));
      expect(modified.approvalStatus, equals(ApprovalStatus.pending));
      expect(modified.isClosed, equals(false));
    });

    test('should serialize to JSON correctly', () {
      final order = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final json = order.toJson();

      expect(json['id'], equals(1));
      expect(json['orderRequestNumber'], equals('OP00000074'));
      expect(json['clientId'], equals(100));
      expect(json['clientName'], equals('천사푸드'));
      expect(json['orderDate'], equals('2026-02-05T00:00:00.000'));
      expect(json['deliveryDate'], equals('2026-02-08T00:00:00.000'));
      expect(json['totalAmount'], equals(612000000));
      expect(json['approvalStatus'], equals('APPROVED'));
      expect(json['isClosed'], equals(true));
    });

    test('should deserialize from JSON correctly', () {
      final json = {
        'id': 1,
        'orderRequestNumber': 'OP00000074',
        'clientId': 100,
        'clientName': '천사푸드',
        'orderDate': '2026-02-05T00:00:00.000',
        'deliveryDate': '2026-02-08T00:00:00.000',
        'totalAmount': 612000000,
        'approvalStatus': 'APPROVED',
        'isClosed': true,
      };

      final order = Order.fromJson(json);

      expect(order.id, equals(1));
      expect(order.orderRequestNumber, equals('OP00000074'));
      expect(order.clientId, equals(100));
      expect(order.clientName, equals('천사푸드'));
      expect(order.orderDate, equals(DateTime(2026, 2, 5)));
      expect(order.deliveryDate, equals(DateTime(2026, 2, 8)));
      expect(order.totalAmount, equals(612000000));
      expect(order.approvalStatus, equals(ApprovalStatus.approved));
      expect(order.isClosed, equals(true));
    });

    test('should support toJson/fromJson roundtrip', () {
      final original = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final json = original.toJson();
      final reconstructed = Order.fromJson(json);

      expect(reconstructed, equals(original));
    });

    test('should compare Orders correctly with equality operator', () {
      final order1 = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final order2 = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final order3 = Order(
        id: 2,
        orderRequestNumber: 'OP00000073',
        clientId: 200,
        clientName: '경산식품',
        orderDate: DateTime(2026, 2, 4),
        deliveryDate: DateTime(2026, 2, 7),
        totalAmount: 245000000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: false,
      );

      expect(order1, equals(order2));
      expect(order1, isNot(equals(order3)));
    });

    test('should generate consistent hashCode for equal Orders', () {
      final order1 = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final order2 = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      expect(order1.hashCode, equals(order2.hashCode));
    });

    test('should generate toString with all fields', () {
      final order = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final str = order.toString();

      expect(str, contains('Order('));
      expect(str, contains('id: 1'));
      expect(str, contains('orderRequestNumber: OP00000074'));
      expect(str, contains('clientId: 100'));
      expect(str, contains('clientName: 천사푸드'));
      expect(str, contains('totalAmount: 612000000'));
      expect(str, contains('approvalStatus: ApprovalStatus.approved'));
      expect(str, contains('isClosed: true'));
    });
  });

  group('ApprovalStatus', () {
    test('should have correct displayName for each status', () {
      expect(ApprovalStatus.approved.displayName, equals('승인완료'));
      expect(ApprovalStatus.pending.displayName, equals('승인상태'));
      expect(ApprovalStatus.sendFailed.displayName, equals('전송실패'));
      expect(ApprovalStatus.resend.displayName, equals('재전송'));
    });

    test('should have correct code for each status', () {
      expect(ApprovalStatus.approved.code, equals('APPROVED'));
      expect(ApprovalStatus.pending.code, equals('PENDING'));
      expect(ApprovalStatus.sendFailed.code, equals('SEND_FAILED'));
      expect(ApprovalStatus.resend.code, equals('RESEND'));
    });

    test('should have correct color for each status', () {
      expect(ApprovalStatus.approved.color, equals(Colors.green));
      expect(ApprovalStatus.pending.color, equals(Colors.amber));
      expect(ApprovalStatus.sendFailed.color, equals(Colors.red));
      expect(ApprovalStatus.resend.color, equals(Colors.orange));
    });

    test('should convert from valid code using fromCode', () {
      expect(ApprovalStatus.fromCode('APPROVED'), equals(ApprovalStatus.approved));
      expect(ApprovalStatus.fromCode('PENDING'), equals(ApprovalStatus.pending));
      expect(ApprovalStatus.fromCode('SEND_FAILED'), equals(ApprovalStatus.sendFailed));
      expect(ApprovalStatus.fromCode('RESEND'), equals(ApprovalStatus.resend));
    });

    test('should return pending for unknown code', () {
      expect(ApprovalStatus.fromCode('UNKNOWN'), equals(ApprovalStatus.pending));
      expect(ApprovalStatus.fromCode(''), equals(ApprovalStatus.pending));
      expect(ApprovalStatus.fromCode('INVALID_STATUS'), equals(ApprovalStatus.pending));
    });

    test('should serialize to JSON correctly', () {
      expect(ApprovalStatus.approved.toJson(), equals('APPROVED'));
      expect(ApprovalStatus.pending.toJson(), equals('PENDING'));
      expect(ApprovalStatus.sendFailed.toJson(), equals('SEND_FAILED'));
      expect(ApprovalStatus.resend.toJson(), equals('RESEND'));
    });

    test('should deserialize from JSON correctly', () {
      expect(ApprovalStatus.fromJson('APPROVED'), equals(ApprovalStatus.approved));
      expect(ApprovalStatus.fromJson('PENDING'), equals(ApprovalStatus.pending));
      expect(ApprovalStatus.fromJson('SEND_FAILED'), equals(ApprovalStatus.sendFailed));
      expect(ApprovalStatus.fromJson('RESEND'), equals(ApprovalStatus.resend));
    });
  });

  group('OrderSortType', () {
    test('should have correct displayName for each sort type', () {
      expect(OrderSortType.latestOrder.displayName, equals('최신주문일순'));
      expect(OrderSortType.oldestOrder.displayName, equals('오래된주문일순'));
      expect(OrderSortType.latestDelivery.displayName, equals('최신납기일순'));
      expect(OrderSortType.oldestDelivery.displayName, equals('오래된납기일순'));
      expect(OrderSortType.amountHigh.displayName, equals('금액높은순'));
      expect(OrderSortType.amountLow.displayName, equals('금액낮은순'));
    });

    test('should have correct sortBy field for each sort type', () {
      expect(OrderSortType.latestOrder.sortBy, equals('orderDate'));
      expect(OrderSortType.oldestOrder.sortBy, equals('orderDate'));
      expect(OrderSortType.latestDelivery.sortBy, equals('deliveryDate'));
      expect(OrderSortType.oldestDelivery.sortBy, equals('deliveryDate'));
      expect(OrderSortType.amountHigh.sortBy, equals('totalAmount'));
      expect(OrderSortType.amountLow.sortBy, equals('totalAmount'));
    });

    test('should have correct sortDir field for each sort type', () {
      expect(OrderSortType.latestOrder.sortDir, equals('DESC'));
      expect(OrderSortType.oldestOrder.sortDir, equals('ASC'));
      expect(OrderSortType.latestDelivery.sortDir, equals('DESC'));
      expect(OrderSortType.oldestDelivery.sortDir, equals('ASC'));
      expect(OrderSortType.amountHigh.sortDir, equals('DESC'));
      expect(OrderSortType.amountLow.sortDir, equals('ASC'));
    });

    test('should have exactly 6 sort type values', () {
      expect(OrderSortType.values.length, equals(6));
    });
  });
}
