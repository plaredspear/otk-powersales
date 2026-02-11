import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_detail.dart';

void main() {
  group('ClientOrder', () {
    // Test data
    const testSapOrderNumber = '300011396';
    const testClientId = 12345;
    const testClientName = '오뚜기식품점';
    const testTotalAmount = 1500000;

    test('should create instance with all required fields', () {
      // Act
      const order = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Assert
      expect(order.sapOrderNumber, testSapOrderNumber);
      expect(order.clientId, testClientId);
      expect(order.clientName, testClientName);
      expect(order.totalAmount, testTotalAmount);
    });

    test('copyWith should create new instance with updated fields', () {
      // Arrange
      const original = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Act
      final updated = original.copyWith(
        sapOrderNumber: '300011397',
        totalAmount: 2000000,
      );

      // Assert
      expect(updated.sapOrderNumber, '300011397');
      expect(updated.clientId, testClientId); // unchanged
      expect(updated.clientName, testClientName); // unchanged
      expect(updated.totalAmount, 2000000);
      expect(original.sapOrderNumber, testSapOrderNumber); // original unchanged
    });

    test('copyWith with no parameters should return identical copy', () {
      // Arrange
      const original = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Act
      final copy = original.copyWith();

      // Assert
      expect(copy.sapOrderNumber, original.sapOrderNumber);
      expect(copy.clientId, original.clientId);
      expect(copy.clientName, original.clientName);
      expect(copy.totalAmount, original.totalAmount);
    });

    test('toJson should serialize to Map correctly', () {
      // Arrange
      const order = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Act
      final json = order.toJson();

      // Assert
      expect(json, {
        'sapOrderNumber': testSapOrderNumber,
        'clientId': testClientId,
        'clientName': testClientName,
        'totalAmount': testTotalAmount,
      });
    });

    test('fromJson should deserialize from Map correctly', () {
      // Arrange
      final json = {
        'sapOrderNumber': testSapOrderNumber,
        'clientId': testClientId,
        'clientName': testClientName,
        'totalAmount': testTotalAmount,
      };

      // Act
      final order = ClientOrder.fromJson(json);

      // Assert
      expect(order.sapOrderNumber, testSapOrderNumber);
      expect(order.clientId, testClientId);
      expect(order.clientName, testClientName);
      expect(order.totalAmount, testTotalAmount);
    });

    test('toJson and fromJson should be reversible', () {
      // Arrange
      const original = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Act
      final json = original.toJson();
      final restored = ClientOrder.fromJson(json);

      // Assert
      expect(restored, original);
    });

    test('equality operator should return true for same values', () {
      // Arrange
      const order1 = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );
      const order2 = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Assert
      expect(order1, order2);
      expect(order1 == order2, true);
    });

    test('equality operator should return false for different values', () {
      // Arrange
      const order1 = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );
      const order2 = ClientOrder(
        sapOrderNumber: '300011397', // different
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Assert
      expect(order1 == order2, false);
    });

    test('hashCode should be same for equal objects', () {
      // Arrange
      const order1 = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );
      const order2 = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Assert
      expect(order1.hashCode, order2.hashCode);
    });

    test('toString should include all field values', () {
      // Arrange
      const order = ClientOrder(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      // Act
      final string = order.toString();

      // Assert
      expect(string, contains('ClientOrder'));
      expect(string, contains(testSapOrderNumber));
      expect(string, contains(testClientId.toString()));
      expect(string, contains(testClientName));
      expect(string, contains(testTotalAmount.toString()));
    });
  });

  group('ClientOrderItem', () {
    // Test data
    const testProductCode = '01101123';
    const testProductName = '갈릭 아이올리소스 240g';
    const testDeliveredQuantity = '0 BOX';
    const testDeliveryStatus = DeliveryStatus.waiting;

    test('should create instance with all required fields', () {
      // Act
      const item = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Assert
      expect(item.productCode, testProductCode);
      expect(item.productName, testProductName);
      expect(item.deliveredQuantity, testDeliveredQuantity);
      expect(item.deliveryStatus, testDeliveryStatus);
    });

    test('copyWith should create new instance with updated fields', () {
      // Arrange
      const original = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Act
      final updated = original.copyWith(
        deliveredQuantity: '5 BOX',
        deliveryStatus: DeliveryStatus.delivered,
      );

      // Assert
      expect(updated.productCode, testProductCode); // unchanged
      expect(updated.productName, testProductName); // unchanged
      expect(updated.deliveredQuantity, '5 BOX');
      expect(updated.deliveryStatus, DeliveryStatus.delivered);
      expect(original.deliveredQuantity, testDeliveredQuantity); // original unchanged
    });

    test('copyWith with no parameters should return identical copy', () {
      // Arrange
      const original = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Act
      final copy = original.copyWith();

      // Assert
      expect(copy.productCode, original.productCode);
      expect(copy.productName, original.productName);
      expect(copy.deliveredQuantity, original.deliveredQuantity);
      expect(copy.deliveryStatus, original.deliveryStatus);
    });

    test('toJson should serialize to Map correctly', () {
      // Arrange
      const item = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Act
      final json = item.toJson();

      // Assert
      expect(json, {
        'productCode': testProductCode,
        'productName': testProductName,
        'deliveredQuantity': testDeliveredQuantity,
        'deliveryStatus': testDeliveryStatus.code,
      });
    });

    test('fromJson should deserialize from Map correctly', () {
      // Arrange
      final json = {
        'productCode': testProductCode,
        'productName': testProductName,
        'deliveredQuantity': testDeliveredQuantity,
        'deliveryStatus': 'WAITING',
      };

      // Act
      final item = ClientOrderItem.fromJson(json);

      // Assert
      expect(item.productCode, testProductCode);
      expect(item.productName, testProductName);
      expect(item.deliveredQuantity, testDeliveredQuantity);
      expect(item.deliveryStatus, DeliveryStatus.waiting);
    });

    test('fromJson should handle all DeliveryStatus values', () {
      // Arrange & Act & Assert
      final waitingItem = ClientOrderItem.fromJson({
        'productCode': testProductCode,
        'productName': testProductName,
        'deliveredQuantity': testDeliveredQuantity,
        'deliveryStatus': 'WAITING',
      });
      expect(waitingItem.deliveryStatus, DeliveryStatus.waiting);

      final shippingItem = ClientOrderItem.fromJson({
        'productCode': testProductCode,
        'productName': testProductName,
        'deliveredQuantity': testDeliveredQuantity,
        'deliveryStatus': 'SHIPPING',
      });
      expect(shippingItem.deliveryStatus, DeliveryStatus.shipping);

      final deliveredItem = ClientOrderItem.fromJson({
        'productCode': testProductCode,
        'productName': testProductName,
        'deliveredQuantity': testDeliveredQuantity,
        'deliveryStatus': 'DELIVERED',
      });
      expect(deliveredItem.deliveryStatus, DeliveryStatus.delivered);
    });

    test('toJson and fromJson should be reversible', () {
      // Arrange
      const original = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Act
      final json = original.toJson();
      final restored = ClientOrderItem.fromJson(json);

      // Assert
      expect(restored, original);
    });

    test('equality operator should return true for same values', () {
      // Arrange
      const item1 = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );
      const item2 = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Assert
      expect(item1, item2);
      expect(item1 == item2, true);
    });

    test('equality operator should return false for different values', () {
      // Arrange
      const item1 = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );
      const item2 = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: '5 BOX', // different
        deliveryStatus: testDeliveryStatus,
      );

      // Assert
      expect(item1 == item2, false);
    });

    test('hashCode should be same for equal objects', () {
      // Arrange
      const item1 = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );
      const item2 = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Assert
      expect(item1.hashCode, item2.hashCode);
    });

    test('toString should include all field values', () {
      // Arrange
      const item = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      // Act
      final string = item.toString();

      // Assert
      expect(string, contains('ClientOrderItem'));
      expect(string, contains(testProductCode));
      expect(string, contains(testProductName));
      expect(string, contains(testDeliveredQuantity));
      expect(string, contains('DeliveryStatus.waiting'));
    });
  });

  group('ClientOrderDetail', () {
    // Test data
    const testSapOrderNumber = '300011396';
    const testClientId = 12345;
    const testClientName = '오뚜기식품점';
    const testClientDeadlineTime = '14:00';
    final testOrderDate = DateTime(2025, 1, 15, 10, 30);
    final testDeliveryDate = DateTime(2025, 1, 17);
    const testTotalApprovedAmount = 1500000;
    const testOrderedItemCount = 3;
    final testOrderedItems = const [
      ClientOrderItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '5 BOX',
        deliveryStatus: DeliveryStatus.delivered,
      ),
      ClientOrderItem(
        productCode: '01101124',
        productName: '진간장 500ml',
        deliveredQuantity: '0 BOX',
        deliveryStatus: DeliveryStatus.waiting,
      ),
    ];

    test('should create instance with all required fields', () {
      // Act
      final detail = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Assert
      expect(detail.sapOrderNumber, testSapOrderNumber);
      expect(detail.clientId, testClientId);
      expect(detail.clientName, testClientName);
      expect(detail.clientDeadlineTime, testClientDeadlineTime);
      expect(detail.orderDate, testOrderDate);
      expect(detail.deliveryDate, testDeliveryDate);
      expect(detail.totalApprovedAmount, testTotalApprovedAmount);
      expect(detail.orderedItemCount, testOrderedItemCount);
      expect(detail.orderedItems, testOrderedItems);
    });

    test('should create instance with null clientDeadlineTime', () {
      // Act
      final detail = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: null,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Assert
      expect(detail.clientDeadlineTime, null);
    });

    test('copyWith should create new instance with updated fields', () {
      // Arrange
      final original = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Act
      final newDeliveryDate = DateTime(2025, 1, 18);
      final updated = original.copyWith(
        deliveryDate: newDeliveryDate,
        totalApprovedAmount: 2000000,
      );

      // Assert
      expect(updated.sapOrderNumber, testSapOrderNumber); // unchanged
      expect(updated.deliveryDate, newDeliveryDate);
      expect(updated.totalApprovedAmount, 2000000);
      expect(original.deliveryDate, testDeliveryDate); // original unchanged
    });

    test('copyWith with no parameters should return identical copy', () {
      // Arrange
      final original = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Act
      final copy = original.copyWith();

      // Assert
      expect(copy.sapOrderNumber, original.sapOrderNumber);
      expect(copy.clientId, original.clientId);
      expect(copy.clientName, original.clientName);
      expect(copy.clientDeadlineTime, original.clientDeadlineTime);
      expect(copy.orderDate, original.orderDate);
      expect(copy.deliveryDate, original.deliveryDate);
      expect(copy.totalApprovedAmount, original.totalApprovedAmount);
      expect(copy.orderedItemCount, original.orderedItemCount);
      expect(copy.orderedItems, original.orderedItems);
    });

    test('toJson should serialize to Map correctly', () {
      // Arrange
      final detail = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Act
      final json = detail.toJson();

      // Assert
      expect(json['sapOrderNumber'], testSapOrderNumber);
      expect(json['clientId'], testClientId);
      expect(json['clientName'], testClientName);
      expect(json['clientDeadlineTime'], testClientDeadlineTime);
      expect(json['orderDate'], testOrderDate.toIso8601String());
      expect(json['deliveryDate'], testDeliveryDate.toIso8601String());
      expect(json['totalApprovedAmount'], testTotalApprovedAmount);
      expect(json['orderedItemCount'], testOrderedItemCount);
      expect(json['orderedItems'], isA<List>());
      expect((json['orderedItems'] as List).length, testOrderedItems.length);
    });

    test('fromJson should deserialize from Map correctly', () {
      // Arrange
      final json = {
        'sapOrderNumber': testSapOrderNumber,
        'clientId': testClientId,
        'clientName': testClientName,
        'clientDeadlineTime': testClientDeadlineTime,
        'orderDate': testOrderDate.toIso8601String(),
        'deliveryDate': testDeliveryDate.toIso8601String(),
        'totalApprovedAmount': testTotalApprovedAmount,
        'orderedItemCount': testOrderedItemCount,
        'orderedItems': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'deliveredQuantity': '5 BOX',
            'deliveryStatus': 'DELIVERED',
          },
          {
            'productCode': '01101124',
            'productName': '진간장 500ml',
            'deliveredQuantity': '0 BOX',
            'deliveryStatus': 'WAITING',
          },
        ],
      };

      // Act
      final detail = ClientOrderDetail.fromJson(json);

      // Assert
      expect(detail.sapOrderNumber, testSapOrderNumber);
      expect(detail.clientId, testClientId);
      expect(detail.clientName, testClientName);
      expect(detail.clientDeadlineTime, testClientDeadlineTime);
      expect(detail.orderDate, testOrderDate);
      expect(detail.deliveryDate, testDeliveryDate);
      expect(detail.totalApprovedAmount, testTotalApprovedAmount);
      expect(detail.orderedItemCount, testOrderedItemCount);
      expect(detail.orderedItems.length, 2);
      expect(detail.orderedItems[0].productCode, '01101123');
      expect(detail.orderedItems[1].productCode, '01101124');
    });

    test('fromJson should handle null clientDeadlineTime', () {
      // Arrange
      final json = {
        'sapOrderNumber': testSapOrderNumber,
        'clientId': testClientId,
        'clientName': testClientName,
        'clientDeadlineTime': null,
        'orderDate': testOrderDate.toIso8601String(),
        'deliveryDate': testDeliveryDate.toIso8601String(),
        'totalApprovedAmount': testTotalApprovedAmount,
        'orderedItemCount': testOrderedItemCount,
        'orderedItems': [],
      };

      // Act
      final detail = ClientOrderDetail.fromJson(json);

      // Assert
      expect(detail.clientDeadlineTime, null);
    });

    test('toJson and fromJson should be reversible', () {
      // Arrange
      final original = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Act
      final json = original.toJson();
      final restored = ClientOrderDetail.fromJson(json);

      // Assert
      expect(restored, original);
    });

    test('equality operator should return true for same values', () {
      // Arrange
      final detail1 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );
      final detail2 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Assert
      expect(detail1, detail2);
      expect(detail1 == detail2, true);
    });

    test('equality operator should return false for different values', () {
      // Arrange
      final detail1 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );
      final detail2 = ClientOrderDetail(
        sapOrderNumber: '300011397', // different
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Assert
      expect(detail1 == detail2, false);
    });

    test('equality operator should handle different orderedItems lists', () {
      // Arrange
      final detail1 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );
      final detail2 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: const [], // different
      );

      // Assert
      expect(detail1 == detail2, false);
    });

    test('hashCode should be same for equal objects', () {
      // Arrange
      final detail1 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );
      final detail2 = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Assert
      expect(detail1.hashCode, detail2.hashCode);
    });

    test('toString should include key field values', () {
      // Arrange
      final detail = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: testOrderedItems,
      );

      // Act
      final string = detail.toString();

      // Assert
      expect(string, contains('ClientOrderDetail'));
      expect(string, contains(testSapOrderNumber));
      expect(string, contains(testClientName));
      expect(string, contains(testOrderedItemCount.toString()));
    });
  });

  group('ClientOrderListResult', () {
    // Test data
    final testOrders = const [
      ClientOrder(
        sapOrderNumber: '300011396',
        clientId: 12345,
        clientName: '오뚜기식품점',
        totalAmount: 1500000,
      ),
      ClientOrder(
        sapOrderNumber: '300011397',
        clientId: 12346,
        clientName: '신세계마트',
        totalAmount: 2000000,
      ),
    ];

    test('should create instance with all required fields', () {
      // Act
      final result = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Assert
      expect(result.orders, testOrders);
      expect(result.totalElements, 25);
      expect(result.totalPages, 3);
      expect(result.currentPage, 0);
      expect(result.pageSize, 10);
      expect(result.isFirst, true);
      expect(result.isLast, false);
    });

    test('hasNextPage should return true when not on last page', () {
      // Arrange
      const result = ClientOrderListResult(
        orders: [],
        totalElements: 25,
        totalPages: 3,
        currentPage: 1,
        pageSize: 10,
        isFirst: false,
        isLast: false,
      );

      // Assert
      expect(result.hasNextPage, true);
    });

    test('hasNextPage should return false when on last page', () {
      // Arrange
      const result = ClientOrderListResult(
        orders: [],
        totalElements: 25,
        totalPages: 3,
        currentPage: 2,
        pageSize: 10,
        isFirst: false,
        isLast: true,
      );

      // Assert
      expect(result.hasNextPage, false);
    });

    test('hasPreviousPage should return true when not on first page', () {
      // Arrange
      const result = ClientOrderListResult(
        orders: [],
        totalElements: 25,
        totalPages: 3,
        currentPage: 1,
        pageSize: 10,
        isFirst: false,
        isLast: false,
      );

      // Assert
      expect(result.hasPreviousPage, true);
    });

    test('hasPreviousPage should return false when on first page', () {
      // Arrange
      const result = ClientOrderListResult(
        orders: [],
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Assert
      expect(result.hasPreviousPage, false);
    });

    test('equality operator should return true for same values', () {
      // Arrange
      final result1 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );
      final result2 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Assert
      expect(result1, result2);
      expect(result1 == result2, true);
    });

    test('equality operator should return false for different values', () {
      // Arrange
      final result1 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );
      final result2 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 30, // different
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Assert
      expect(result1 == result2, false);
    });

    test('equality operator should handle different orders lists', () {
      // Arrange
      final result1 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );
      const result2 = ClientOrderListResult(
        orders: [], // different
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Assert
      expect(result1 == result2, false);
    });

    test('hashCode should be same for equal objects', () {
      // Arrange
      final result1 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );
      final result2 = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Assert
      expect(result1.hashCode, result2.hashCode);
    });

    test('toString should include pagination information', () {
      // Arrange
      final result = ClientOrderListResult(
        orders: testOrders,
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );

      // Act
      final string = result.toString();

      // Assert
      expect(string, contains('ClientOrderListResult'));
      expect(string, contains('2')); // orders.length
      expect(string, contains('25')); // totalElements
      expect(string, contains('3')); // totalPages
      expect(string, contains('0')); // currentPage
      expect(string, contains('10')); // pageSize
      expect(string, contains('true')); // isFirst
      expect(string, contains('false')); // isLast
    });

    test('should handle single page result', () {
      // Arrange
      final result = ClientOrderListResult(
        orders: testOrders,
        totalElements: 2,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: true,
      );

      // Assert
      expect(result.hasNextPage, false);
      expect(result.hasPreviousPage, false);
    });

    test('should handle empty result', () {
      // Arrange
      const result = ClientOrderListResult(
        orders: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: true,
      );

      // Assert
      expect(result.orders.isEmpty, true);
      expect(result.totalElements, 0);
      expect(result.hasNextPage, false);
      expect(result.hasPreviousPage, false);
    });
  });
}
