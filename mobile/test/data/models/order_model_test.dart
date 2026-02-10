import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_model.dart';
import 'package:mobile/domain/entities/order.dart';

void main() {
  group('OrderModel', () {
    test('should create OrderModel from snake_case JSON', () {
      // Arrange
      final json = {
        'id': 1,
        'order_request_number': 'OP00000074',
        'client_id': 100,
        'client_name': '천사푸드',
        'order_date': '2026-02-05',
        'delivery_date': '2026-02-08',
        'total_amount': 612000000,
        'approval_status': 'APPROVED',
        'is_closed': true,
      };

      // Act
      final model = OrderModel.fromJson(json);

      // Assert
      expect(model.id, equals(1));
      expect(model.orderRequestNumber, equals('OP00000074'));
      expect(model.clientId, equals(100));
      expect(model.clientName, equals('천사푸드'));
      expect(model.orderDate, equals('2026-02-05'));
      expect(model.deliveryDate, equals('2026-02-08'));
      expect(model.totalAmount, equals(612000000));
      expect(model.approvalStatus, equals('APPROVED'));
      expect(model.isClosed, equals(true));
    });

    test('should serialize to snake_case JSON', () {
      // Arrange
      const model = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      // Act
      final json = model.toJson();

      // Assert
      expect(json['id'], equals(1));
      expect(json['order_request_number'], equals('OP00000074'));
      expect(json['client_id'], equals(100));
      expect(json['client_name'], equals('천사푸드'));
      expect(json['order_date'], equals('2026-02-05'));
      expect(json['delivery_date'], equals('2026-02-08'));
      expect(json['total_amount'], equals(612000000));
      expect(json['approval_status'], equals('APPROVED'));
      expect(json['is_closed'], equals(true));
    });

    test('should convert to Order entity correctly with DateTime parsing', () {
      // Arrange
      const model = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.id, equals(1));
      expect(entity.orderRequestNumber, equals('OP00000074'));
      expect(entity.clientId, equals(100));
      expect(entity.clientName, equals('천사푸드'));
      expect(entity.orderDate, equals(DateTime(2026, 2, 5)));
      expect(entity.deliveryDate, equals(DateTime(2026, 2, 8)));
      expect(entity.totalAmount, equals(612000000));
      expect(entity.approvalStatus, equals(ApprovalStatus.approved));
      expect(entity.isClosed, equals(true));
    });

    test('should convert to entity with correct ApprovalStatus enum', () {
      // Test all approval status values
      const testCases = [
        ('APPROVED', ApprovalStatus.approved),
        ('PENDING', ApprovalStatus.pending),
        ('SEND_FAILED', ApprovalStatus.sendFailed),
        ('RESEND', ApprovalStatus.resend),
      ];

      for (final (statusCode, expectedEnum) in testCases) {
        final model = OrderModel(
          id: 1,
          orderRequestNumber: 'OP00000074',
          clientId: 100,
          clientName: '천사푸드',
          orderDate: '2026-02-05',
          deliveryDate: '2026-02-08',
          totalAmount: 612000000,
          approvalStatus: statusCode,
          isClosed: true,
        );

        final entity = model.toEntity();

        expect(entity.approvalStatus, equals(expectedEnum),
            reason: 'Failed for status: $statusCode');
      }
    });

    test('should create model from Order entity', () {
      // Arrange
      final entity = Order(
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

      // Act
      final model = OrderModel.fromEntity(entity);

      // Assert
      expect(model.id, equals(1));
      expect(model.orderRequestNumber, equals('OP00000074'));
      expect(model.clientId, equals(100));
      expect(model.clientName, equals('천사푸드'));
      expect(model.orderDate, equals('2026-02-05'));
      expect(model.deliveryDate, equals('2026-02-08'));
      expect(model.totalAmount, equals(612000000));
      expect(model.approvalStatus, equals('APPROVED'));
      expect(model.isClosed, equals(true));
    });

    test('should extract date only (YYYY-MM-DD) when creating from entity', () {
      // Arrange - DateTime with time component
      final entity = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5, 14, 30, 45),
        deliveryDate: DateTime(2026, 2, 8, 9, 15, 30),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      // Act
      final model = OrderModel.fromEntity(entity);

      // Assert - Should only have date part, no time
      expect(model.orderDate, equals('2026-02-05'));
      expect(model.deliveryDate, equals('2026-02-08'));
    });

    test('should support entity roundtrip conversion', () {
      // Arrange
      final originalEntity = Order(
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

      // Act - entity -> model -> entity
      final model = OrderModel.fromEntity(originalEntity);
      final reconstructedEntity = model.toEntity();

      // Assert
      expect(reconstructedEntity, equals(originalEntity));
    });

    test('should compare OrderModels correctly with equality operator', () {
      // Arrange
      const model1 = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      const model2 = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      const model3 = OrderModel(
        id: 2,
        orderRequestNumber: 'OP00000073',
        clientId: 200,
        clientName: '경산식품',
        orderDate: '2026-02-04',
        deliveryDate: '2026-02-07',
        totalAmount: 245000000,
        approvalStatus: 'PENDING',
        isClosed: false,
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });

    test('should generate consistent hashCode for equal OrderModels', () {
      // Arrange
      const model1 = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      const model2 = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      // Assert
      expect(model1.hashCode, equals(model2.hashCode));
    });

    test('should generate toString with all fields', () {
      // Arrange
      const model = OrderModel(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: '2026-02-05',
        deliveryDate: '2026-02-08',
        totalAmount: 612000000,
        approvalStatus: 'APPROVED',
        isClosed: true,
      );

      // Act
      final str = model.toString();

      // Assert
      expect(str, contains('OrderModel('));
      expect(str, contains('id: 1'));
      expect(str, contains('orderRequestNumber: OP00000074'));
      expect(str, contains('clientId: 100'));
      expect(str, contains('clientName: 천사푸드'));
      expect(str, contains('orderDate: 2026-02-05'));
      expect(str, contains('deliveryDate: 2026-02-08'));
      expect(str, contains('totalAmount: 612000000'));
      expect(str, contains('approvalStatus: APPROVED'));
      expect(str, contains('isClosed: true'));
    });
  });
}
