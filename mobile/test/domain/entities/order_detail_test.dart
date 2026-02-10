import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order.dart';

void main() {
  group('DeliveryStatus', () {
    test('should have correct values', () {
      expect(DeliveryStatus.waiting.displayName, '대기');
      expect(DeliveryStatus.waiting.code, 'WAITING');
      expect(DeliveryStatus.shipping.displayName, '배송중');
      expect(DeliveryStatus.shipping.code, 'SHIPPING');
      expect(DeliveryStatus.delivered.displayName, '배송완료');
      expect(DeliveryStatus.delivered.code, 'DELIVERED');
    });

    test('fromCode should return correct status for valid codes', () {
      expect(DeliveryStatus.fromCode('WAITING'), DeliveryStatus.waiting);
      expect(DeliveryStatus.fromCode('SHIPPING'), DeliveryStatus.shipping);
      expect(DeliveryStatus.fromCode('DELIVERED'), DeliveryStatus.delivered);
    });

    test('fromCode should return default (waiting) for invalid code', () {
      expect(DeliveryStatus.fromCode('INVALID'), DeliveryStatus.waiting);
      expect(DeliveryStatus.fromCode(''), DeliveryStatus.waiting);
      expect(DeliveryStatus.fromCode('unknown'), DeliveryStatus.waiting);
    });

    test('toJson should return code value', () {
      expect(DeliveryStatus.waiting.toJson(), 'WAITING');
      expect(DeliveryStatus.shipping.toJson(), 'SHIPPING');
      expect(DeliveryStatus.delivered.toJson(), 'DELIVERED');
    });

    test('fromJson should return correct status', () {
      expect(DeliveryStatus.fromJson('WAITING'), DeliveryStatus.waiting);
      expect(DeliveryStatus.fromJson('SHIPPING'), DeliveryStatus.shipping);
      expect(DeliveryStatus.fromJson('DELIVERED'), DeliveryStatus.delivered);
    });

    test('fromJson should return default for invalid value', () {
      expect(DeliveryStatus.fromJson('INVALID'), DeliveryStatus.waiting);
    });
  });

  group('OrderedItem', () {
    const testItem = OrderedItem(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      totalQuantityBoxes: 5.0,
      totalQuantityPieces: 100,
      isCancelled: false,
    );

    test('should create instance with required fields', () {
      expect(testItem.productCode, '01101123');
      expect(testItem.productName, '갈릭 아이올리소스 240g');
      expect(testItem.totalQuantityBoxes, 5.0);
      expect(testItem.totalQuantityPieces, 100);
      expect(testItem.isCancelled, false);
    });

    test('copyWith should create new instance with updated fields', () {
      final updated = testItem.copyWith(
        productCode: '01101124',
        totalQuantityBoxes: 10.0,
      );

      expect(updated.productCode, '01101124');
      expect(updated.productName, '갈릭 아이올리소스 240g');
      expect(updated.totalQuantityBoxes, 10.0);
      expect(updated.totalQuantityPieces, 100);
      expect(updated.isCancelled, false);
    });

    test('copyWith should return same values when no params provided', () {
      final copied = testItem.copyWith();

      expect(copied.productCode, testItem.productCode);
      expect(copied.productName, testItem.productName);
      expect(copied.totalQuantityBoxes, testItem.totalQuantityBoxes);
      expect(copied.totalQuantityPieces, testItem.totalQuantityPieces);
      expect(copied.isCancelled, testItem.isCancelled);
    });

    test('toJson should serialize correctly', () {
      final json = testItem.toJson();

      expect(json['productCode'], '01101123');
      expect(json['productName'], '갈릭 아이올리소스 240g');
      expect(json['totalQuantityBoxes'], 5.0);
      expect(json['totalQuantityPieces'], 100);
      expect(json['isCancelled'], false);
    });

    test('fromJson should deserialize correctly', () {
      final json = {
        'productCode': '01101123',
        'productName': '갈릭 아이올리소스 240g',
        'totalQuantityBoxes': 5.0,
        'totalQuantityPieces': 100,
        'isCancelled': false,
      };

      final item = OrderedItem.fromJson(json);

      expect(item.productCode, '01101123');
      expect(item.productName, '갈릭 아이올리소스 240g');
      expect(item.totalQuantityBoxes, 5.0);
      expect(item.totalQuantityPieces, 100);
      expect(item.isCancelled, false);
    });

    test('fromJson should handle int to double conversion', () {
      final json = {
        'productCode': '01101123',
        'productName': '갈릭 아이올리소스 240g',
        'totalQuantityBoxes': 5, // int instead of double
        'totalQuantityPieces': 100,
        'isCancelled': false,
      };

      final item = OrderedItem.fromJson(json);

      expect(item.totalQuantityBoxes, 5.0);
    });

    test('equality should work correctly', () {
      const item1 = OrderedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        totalQuantityBoxes: 5.0,
        totalQuantityPieces: 100,
        isCancelled: false,
      );

      const item2 = OrderedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        totalQuantityBoxes: 5.0,
        totalQuantityPieces: 100,
        isCancelled: false,
      );

      const item3 = OrderedItem(
        productCode: '01101124',
        productName: '갈릭 아이올리소스 240g',
        totalQuantityBoxes: 5.0,
        totalQuantityPieces: 100,
        isCancelled: false,
      );

      expect(item1, equals(item2));
      expect(item1, isNot(equals(item3)));
    });

    test('hashCode should be consistent', () {
      const item1 = OrderedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        totalQuantityBoxes: 5.0,
        totalQuantityPieces: 100,
        isCancelled: false,
      );

      const item2 = OrderedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        totalQuantityBoxes: 5.0,
        totalQuantityPieces: 100,
        isCancelled: false,
      );

      expect(item1.hashCode, equals(item2.hashCode));
    });

    test('toString should return formatted string', () {
      final str = testItem.toString();

      expect(str, contains('OrderedItem'));
      expect(str, contains('01101123'));
      expect(str, contains('갈릭 아이올리소스 240g'));
    });
  });

  group('ProcessingItem', () {
    const testItem = ProcessingItem(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      deliveredQuantity: '0 EA',
      deliveryStatus: DeliveryStatus.waiting,
    );

    test('should create instance with required fields', () {
      expect(testItem.productCode, '01101123');
      expect(testItem.productName, '갈릭 아이올리소스 240g');
      expect(testItem.deliveredQuantity, '0 EA');
      expect(testItem.deliveryStatus, DeliveryStatus.waiting);
    });

    test('copyWith should create new instance with updated fields', () {
      final updated = testItem.copyWith(
        deliveredQuantity: '50 EA',
        deliveryStatus: DeliveryStatus.shipping,
      );

      expect(updated.productCode, '01101123');
      expect(updated.productName, '갈릭 아이올리소스 240g');
      expect(updated.deliveredQuantity, '50 EA');
      expect(updated.deliveryStatus, DeliveryStatus.shipping);
    });

    test('copyWith should return same values when no params provided', () {
      final copied = testItem.copyWith();

      expect(copied.productCode, testItem.productCode);
      expect(copied.productName, testItem.productName);
      expect(copied.deliveredQuantity, testItem.deliveredQuantity);
      expect(copied.deliveryStatus, testItem.deliveryStatus);
    });

    test('toJson should serialize correctly', () {
      final json = testItem.toJson();

      expect(json['productCode'], '01101123');
      expect(json['productName'], '갈릭 아이올리소스 240g');
      expect(json['deliveredQuantity'], '0 EA');
      expect(json['deliveryStatus'], 'WAITING');
    });

    test('fromJson should deserialize correctly', () {
      final json = {
        'productCode': '01101123',
        'productName': '갈릭 아이올리소스 240g',
        'deliveredQuantity': '0 EA',
        'deliveryStatus': 'WAITING',
      };

      final item = ProcessingItem.fromJson(json);

      expect(item.productCode, '01101123');
      expect(item.productName, '갈릭 아이올리소스 240g');
      expect(item.deliveredQuantity, '0 EA');
      expect(item.deliveryStatus, DeliveryStatus.waiting);
    });

    test('fromJson should handle different delivery statuses', () {
      final jsonShipping = {
        'productCode': '01101123',
        'productName': '갈릭 아이올리소스 240g',
        'deliveredQuantity': '50 EA',
        'deliveryStatus': 'SHIPPING',
      };

      final itemShipping = ProcessingItem.fromJson(jsonShipping);
      expect(itemShipping.deliveryStatus, DeliveryStatus.shipping);

      final jsonDelivered = {
        'productCode': '01101123',
        'productName': '갈릭 아이올리소스 240g',
        'deliveredQuantity': '100 EA',
        'deliveryStatus': 'DELIVERED',
      };

      final itemDelivered = ProcessingItem.fromJson(jsonDelivered);
      expect(itemDelivered.deliveryStatus, DeliveryStatus.delivered);
    });

    test('equality should work correctly', () {
      const item1 = ProcessingItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '0 EA',
        deliveryStatus: DeliveryStatus.waiting,
      );

      const item2 = ProcessingItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '0 EA',
        deliveryStatus: DeliveryStatus.waiting,
      );

      const item3 = ProcessingItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '50 EA',
        deliveryStatus: DeliveryStatus.shipping,
      );

      expect(item1, equals(item2));
      expect(item1, isNot(equals(item3)));
    });

    test('hashCode should be consistent', () {
      const item1 = ProcessingItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '0 EA',
        deliveryStatus: DeliveryStatus.waiting,
      );

      const item2 = ProcessingItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '0 EA',
        deliveryStatus: DeliveryStatus.waiting,
      );

      expect(item1.hashCode, equals(item2.hashCode));
    });

    test('toString should return formatted string', () {
      final str = testItem.toString();

      expect(str, contains('ProcessingItem'));
      expect(str, contains('01101123'));
      expect(str, contains('갈릭 아이올리소스 240g'));
    });
  });

  group('OrderProcessingStatus', () {
    const testItem1 = ProcessingItem(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      deliveredQuantity: '0 EA',
      deliveryStatus: DeliveryStatus.waiting,
    );

    const testItem2 = ProcessingItem(
      productCode: '01101124',
      productName: '스위트칠리소스 300g',
      deliveredQuantity: '50 EA',
      deliveryStatus: DeliveryStatus.shipping,
    );

    const testStatus = OrderProcessingStatus(
      sapOrderNumber: '0300013650',
      items: [testItem1, testItem2],
    );

    test('should create instance with required fields', () {
      expect(testStatus.sapOrderNumber, '0300013650');
      expect(testStatus.items.length, 2);
      expect(testStatus.items[0], testItem1);
      expect(testStatus.items[1], testItem2);
    });

    test('copyWith should create new instance with updated fields', () {
      const newItem = ProcessingItem(
        productCode: '01101125',
        productName: '테스트 제품',
        deliveredQuantity: '100 EA',
        deliveryStatus: DeliveryStatus.delivered,
      );

      final updated = testStatus.copyWith(
        sapOrderNumber: '0300013651',
        items: [newItem],
      );

      expect(updated.sapOrderNumber, '0300013651');
      expect(updated.items.length, 1);
      expect(updated.items[0], newItem);
    });

    test('copyWith should return same values when no params provided', () {
      final copied = testStatus.copyWith();

      expect(copied.sapOrderNumber, testStatus.sapOrderNumber);
      expect(copied.items, testStatus.items);
    });

    test('toJson should serialize correctly', () {
      final json = testStatus.toJson();

      expect(json['sapOrderNumber'], '0300013650');
      expect(json['items'], isA<List>());
      expect(json['items'].length, 2);
      expect(json['items'][0]['productCode'], '01101123');
      expect(json['items'][1]['productCode'], '01101124');
    });

    test('fromJson should deserialize correctly', () {
      final json = {
        'sapOrderNumber': '0300013650',
        'items': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'deliveredQuantity': '0 EA',
            'deliveryStatus': 'WAITING',
          },
          {
            'productCode': '01101124',
            'productName': '스위트칠리소스 300g',
            'deliveredQuantity': '50 EA',
            'deliveryStatus': 'SHIPPING',
          },
        ],
      };

      final status = OrderProcessingStatus.fromJson(json);

      expect(status.sapOrderNumber, '0300013650');
      expect(status.items.length, 2);
      expect(status.items[0].productCode, '01101123');
      expect(status.items[1].productCode, '01101124');
    });

    test('fromJson should handle empty items list', () {
      final json = {
        'sapOrderNumber': '0300013650',
        'items': [],
      };

      final status = OrderProcessingStatus.fromJson(json);

      expect(status.sapOrderNumber, '0300013650');
      expect(status.items.length, 0);
    });

    test('equality should work correctly with list comparison', () {
      const status1 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem1, testItem2],
      );

      const status2 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem1, testItem2],
      );

      const status3 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem1],
      );

      const status4 = OrderProcessingStatus(
        sapOrderNumber: '0300013651',
        items: [testItem1, testItem2],
      );

      expect(status1, equals(status2));
      expect(status1, isNot(equals(status3)));
      expect(status1, isNot(equals(status4)));
    });

    test('equality should return false for different list order', () {
      const status1 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem1, testItem2],
      );

      const status2 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem2, testItem1],
      );

      expect(status1, isNot(equals(status2)));
    });

    test('hashCode should be consistent', () {
      const status1 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem1, testItem2],
      );

      const status2 = OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [testItem1, testItem2],
      );

      expect(status1.hashCode, equals(status2.hashCode));
    });

    test('toString should return formatted string', () {
      final str = testStatus.toString();

      expect(str, contains('OrderProcessingStatus'));
      expect(str, contains('0300013650'));
      expect(str, contains('2'));
    });
  });

  group('RejectedItem', () {
    const testItem = RejectedItem(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      orderQuantityBoxes: 5,
      rejectionReason: '재고부족',
    );

    test('should create instance with required fields', () {
      expect(testItem.productCode, '01101123');
      expect(testItem.productName, '갈릭 아이올리소스 240g');
      expect(testItem.orderQuantityBoxes, 5);
      expect(testItem.rejectionReason, '재고부족');
    });

    test('copyWith should create new instance with updated fields', () {
      final updated = testItem.copyWith(
        orderQuantityBoxes: 10,
        rejectionReason: '단종',
      );

      expect(updated.productCode, '01101123');
      expect(updated.productName, '갈릭 아이올리소스 240g');
      expect(updated.orderQuantityBoxes, 10);
      expect(updated.rejectionReason, '단종');
    });

    test('copyWith should return same values when no params provided', () {
      final copied = testItem.copyWith();

      expect(copied.productCode, testItem.productCode);
      expect(copied.productName, testItem.productName);
      expect(copied.orderQuantityBoxes, testItem.orderQuantityBoxes);
      expect(copied.rejectionReason, testItem.rejectionReason);
    });

    test('toJson should serialize correctly', () {
      final json = testItem.toJson();

      expect(json['productCode'], '01101123');
      expect(json['productName'], '갈릭 아이올리소스 240g');
      expect(json['orderQuantityBoxes'], 5);
      expect(json['rejectionReason'], '재고부족');
    });

    test('fromJson should deserialize correctly', () {
      final json = {
        'productCode': '01101123',
        'productName': '갈릭 아이올리소스 240g',
        'orderQuantityBoxes': 5,
        'rejectionReason': '재고부족',
      };

      final item = RejectedItem.fromJson(json);

      expect(item.productCode, '01101123');
      expect(item.productName, '갈릭 아이올리소스 240g');
      expect(item.orderQuantityBoxes, 5);
      expect(item.rejectionReason, '재고부족');
    });

    test('equality should work correctly', () {
      const item1 = RejectedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        orderQuantityBoxes: 5,
        rejectionReason: '재고부족',
      );

      const item2 = RejectedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        orderQuantityBoxes: 5,
        rejectionReason: '재고부족',
      );

      const item3 = RejectedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        orderQuantityBoxes: 10,
        rejectionReason: '단종',
      );

      expect(item1, equals(item2));
      expect(item1, isNot(equals(item3)));
    });

    test('hashCode should be consistent', () {
      const item1 = RejectedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        orderQuantityBoxes: 5,
        rejectionReason: '재고부족',
      );

      const item2 = RejectedItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        orderQuantityBoxes: 5,
        rejectionReason: '재고부족',
      );

      expect(item1.hashCode, equals(item2.hashCode));
    });

    test('toString should return formatted string', () {
      final str = testItem.toString();

      expect(str, contains('RejectedItem'));
      expect(str, contains('01101123'));
      expect(str, contains('갈릭 아이올리소스 240g'));
    });
  });

  group('OrderDetail', () {
    final testOrderDate = DateTime(2025, 1, 15);
    final testDeliveryDate = DateTime(2025, 1, 20);

    const testOrderedItem1 = OrderedItem(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      totalQuantityBoxes: 5.0,
      totalQuantityPieces: 100,
      isCancelled: false,
    );

    const testOrderedItem2 = OrderedItem(
      productCode: '01101124',
      productName: '스위트칠리소스 300g',
      totalQuantityBoxes: 3.0,
      totalQuantityPieces: 60,
      isCancelled: false,
    );

    final testOrderDetail = OrderDetail(
      id: 1,
      orderRequestNumber: 'OP00000001',
      clientId: 100,
      clientName: '테스트 거래처',
      clientDeadlineTime: '15:00',
      orderDate: testOrderDate,
      deliveryDate: testDeliveryDate,
      totalAmount: 500000,
      totalApprovedAmount: 480000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
      orderedItemCount: 2,
      orderedItems: const [testOrderedItem1, testOrderedItem2],
      orderProcessingStatus: const OrderProcessingStatus(
        sapOrderNumber: '0300013650',
        items: [],
      ),
      rejectedItems: const [
        RejectedItem(
          productCode: '01101125',
          productName: '테스트 제품',
          orderQuantityBoxes: 2,
          rejectionReason: '재고부족',
        ),
      ],
    );

    test('should create instance with required fields', () {
      expect(testOrderDetail.id, 1);
      expect(testOrderDetail.orderRequestNumber, 'OP00000001');
      expect(testOrderDetail.clientId, 100);
      expect(testOrderDetail.clientName, '테스트 거래처');
      expect(testOrderDetail.clientDeadlineTime, '15:00');
      expect(testOrderDetail.orderDate, testOrderDate);
      expect(testOrderDetail.deliveryDate, testDeliveryDate);
      expect(testOrderDetail.totalAmount, 500000);
      expect(testOrderDetail.totalApprovedAmount, 480000);
      expect(testOrderDetail.approvalStatus, ApprovalStatus.approved);
      expect(testOrderDetail.isClosed, true);
      expect(testOrderDetail.orderedItemCount, 2);
      expect(testOrderDetail.orderedItems.length, 2);
      expect(testOrderDetail.orderProcessingStatus, isNotNull);
      expect(testOrderDetail.rejectedItems, isNotNull);
    });

    test('should create instance with nullable fields as null', () {
      final orderDetail = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: false,
        orderedItemCount: 2,
        orderedItems: const [testOrderedItem1, testOrderedItem2],
      );

      expect(orderDetail.clientDeadlineTime, isNull);
      expect(orderDetail.totalApprovedAmount, isNull);
      expect(orderDetail.orderProcessingStatus, isNull);
      expect(orderDetail.rejectedItems, isNull);
    });

    test('hasRejectedItems should return true when rejectedItems exist', () {
      expect(testOrderDetail.hasRejectedItems, true);
    });

    test('hasRejectedItems should return false when rejectedItems is null', () {
      // Note: copyWith with null doesn't actually set the field to null
      // due to the ?? operator in the implementation.
      // Testing with a fresh instance instead.
      final orderDetail = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 2,
        orderedItems: const [testOrderedItem1, testOrderedItem2],
        rejectedItems: null,
      );

      expect(orderDetail.hasRejectedItems, false);
    });

    test('hasRejectedItems should return false when rejectedItems is empty', () {
      final orderDetail = testOrderDetail.copyWith(
        rejectedItems: const [],
      );

      expect(orderDetail.hasRejectedItems, false);
    });

    test('allItemsCancelled should return true when all items are cancelled',
        () {
      final orderDetail = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 2,
        orderedItems: const [
          OrderedItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            totalQuantityBoxes: 5.0,
            totalQuantityPieces: 100,
            isCancelled: true,
          ),
          OrderedItem(
            productCode: '01101124',
            productName: '스위트칠리소스 300g',
            totalQuantityBoxes: 3.0,
            totalQuantityPieces: 60,
            isCancelled: true,
          ),
        ],
      );

      expect(orderDetail.allItemsCancelled, true);
    });

    test('allItemsCancelled should return false when some items are not cancelled',
        () {
      expect(testOrderDetail.allItemsCancelled, false);
    });

    test('allItemsCancelled should return false when orderedItems is empty',
        () {
      final orderDetail = testOrderDetail.copyWith(
        orderedItems: const [],
      );

      expect(orderDetail.allItemsCancelled, false);
    });

    test('copyWith should create new instance with updated fields', () {
      final updated = testOrderDetail.copyWith(
        id: 2,
        clientName: '새로운 거래처',
        totalAmount: 600000,
      );

      expect(updated.id, 2);
      expect(updated.clientName, '새로운 거래처');
      expect(updated.totalAmount, 600000);
      expect(updated.orderRequestNumber, 'OP00000001');
      expect(updated.clientId, 100);
    });

    test('copyWith should return same values when no params provided', () {
      final copied = testOrderDetail.copyWith();

      expect(copied.id, testOrderDetail.id);
      expect(copied.orderRequestNumber, testOrderDetail.orderRequestNumber);
      expect(copied.clientId, testOrderDetail.clientId);
      expect(copied.clientName, testOrderDetail.clientName);
    });

    test('toJson should serialize correctly', () {
      final json = testOrderDetail.toJson();

      expect(json['id'], 1);
      expect(json['orderRequestNumber'], 'OP00000001');
      expect(json['clientId'], 100);
      expect(json['clientName'], '테스트 거래처');
      expect(json['clientDeadlineTime'], '15:00');
      expect(json['orderDate'], testOrderDate.toIso8601String());
      expect(json['deliveryDate'], testDeliveryDate.toIso8601String());
      expect(json['totalAmount'], 500000);
      expect(json['totalApprovedAmount'], 480000);
      expect(json['approvalStatus'], 'APPROVED');
      expect(json['isClosed'], true);
      expect(json['orderedItemCount'], 2);
      expect(json['orderedItems'], isA<List>());
      expect(json['orderProcessingStatus'], isA<Map>());
      expect(json['rejectedItems'], isA<List>());
    });

    test('toJson should serialize nullable fields as null', () {
      final orderDetail = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: false,
        orderedItemCount: 2,
        orderedItems: const [testOrderedItem1],
      );

      final json = orderDetail.toJson();

      expect(json['clientDeadlineTime'], isNull);
      expect(json['totalApprovedAmount'], isNull);
      expect(json['orderProcessingStatus'], isNull);
      expect(json['rejectedItems'], isNull);
    });

    test('fromJson should deserialize correctly', () {
      final json = {
        'id': 1,
        'orderRequestNumber': 'OP00000001',
        'clientId': 100,
        'clientName': '테스트 거래처',
        'clientDeadlineTime': '15:00',
        'orderDate': testOrderDate.toIso8601String(),
        'deliveryDate': testDeliveryDate.toIso8601String(),
        'totalAmount': 500000,
        'totalApprovedAmount': 480000,
        'approvalStatus': 'APPROVED',
        'isClosed': true,
        'orderedItemCount': 2,
        'orderedItems': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'totalQuantityBoxes': 5.0,
            'totalQuantityPieces': 100,
            'isCancelled': false,
          },
        ],
        'orderProcessingStatus': {
          'sapOrderNumber': '0300013650',
          'items': [],
        },
        'rejectedItems': [
          {
            'productCode': '01101125',
            'productName': '테스트 제품',
            'orderQuantityBoxes': 2,
            'rejectionReason': '재고부족',
          },
        ],
      };

      final orderDetail = OrderDetail.fromJson(json);

      expect(orderDetail.id, 1);
      expect(orderDetail.orderRequestNumber, 'OP00000001');
      expect(orderDetail.clientId, 100);
      expect(orderDetail.clientName, '테스트 거래처');
      expect(orderDetail.clientDeadlineTime, '15:00');
      expect(orderDetail.orderDate, testOrderDate);
      expect(orderDetail.deliveryDate, testDeliveryDate);
      expect(orderDetail.totalAmount, 500000);
      expect(orderDetail.totalApprovedAmount, 480000);
      expect(orderDetail.approvalStatus, ApprovalStatus.approved);
      expect(orderDetail.isClosed, true);
      expect(orderDetail.orderedItemCount, 2);
      expect(orderDetail.orderedItems.length, 1);
      expect(orderDetail.orderProcessingStatus, isNotNull);
      expect(orderDetail.rejectedItems, isNotNull);
      expect(orderDetail.rejectedItems!.length, 1);
    });

    test('fromJson should handle nullable fields', () {
      final json = {
        'id': 1,
        'orderRequestNumber': 'OP00000001',
        'clientId': 100,
        'clientName': '테스트 거래처',
        'clientDeadlineTime': null,
        'orderDate': testOrderDate.toIso8601String(),
        'deliveryDate': testDeliveryDate.toIso8601String(),
        'totalAmount': 500000,
        'totalApprovedAmount': null,
        'approvalStatus': 'PENDING',
        'isClosed': false,
        'orderedItemCount': 1,
        'orderedItems': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'totalQuantityBoxes': 5.0,
            'totalQuantityPieces': 100,
            'isCancelled': false,
          },
        ],
        'orderProcessingStatus': null,
        'rejectedItems': null,
      };

      final orderDetail = OrderDetail.fromJson(json);

      expect(orderDetail.clientDeadlineTime, isNull);
      expect(orderDetail.totalApprovedAmount, isNull);
      expect(orderDetail.orderProcessingStatus, isNull);
      expect(orderDetail.rejectedItems, isNull);
    });

    test('fromJson should handle different approval statuses', () {
      final jsonApproved = {
        'id': 1,
        'orderRequestNumber': 'OP00000001',
        'clientId': 100,
        'clientName': '테스트 거래처',
        'orderDate': testOrderDate.toIso8601String(),
        'deliveryDate': testDeliveryDate.toIso8601String(),
        'totalAmount': 500000,
        'approvalStatus': 'APPROVED',
        'isClosed': true,
        'orderedItemCount': 1,
        'orderedItems': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'totalQuantityBoxes': 5.0,
            'totalQuantityPieces': 100,
            'isCancelled': false,
          },
        ],
      };

      final orderApproved = OrderDetail.fromJson(jsonApproved);
      expect(orderApproved.approvalStatus, ApprovalStatus.approved);

      final jsonPending = {
        ...jsonApproved,
        'approvalStatus': 'PENDING',
        'isClosed': false,
      };

      final orderPending = OrderDetail.fromJson(jsonPending);
      expect(orderPending.approvalStatus, ApprovalStatus.pending);

      final jsonFailed = {
        ...jsonApproved,
        'approvalStatus': 'SEND_FAILED',
      };

      final orderFailed = OrderDetail.fromJson(jsonFailed);
      expect(orderFailed.approvalStatus, ApprovalStatus.sendFailed);
    });

    test('equality should work correctly', () {
      final orderDetail1 = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [testOrderedItem1],
      );

      final orderDetail2 = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [testOrderedItem1],
      );

      final orderDetail3 = OrderDetail(
        id: 2,
        orderRequestNumber: 'OP00000002',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [testOrderedItem1],
      );

      expect(orderDetail1, equals(orderDetail2));
      expect(orderDetail1, isNot(equals(orderDetail3)));
    });

    test('equality should compare orderedItems lists', () {
      final orderDetail1 = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 2,
        orderedItems: const [testOrderedItem1, testOrderedItem2],
      );

      final orderDetail2 = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [testOrderedItem1],
      );

      expect(orderDetail1, isNot(equals(orderDetail2)));
    });

    test('equality should compare rejectedItems lists when present', () {
      final orderDetail1 = testOrderDetail;

      final orderDetail2 = testOrderDetail.copyWith(
        rejectedItems: const [
          RejectedItem(
            productCode: '01101126',
            productName: '다른 제품',
            orderQuantityBoxes: 3,
            rejectionReason: '단종',
          ),
        ],
      );

      expect(orderDetail1, isNot(equals(orderDetail2)));
    });

    test('hashCode should be consistent', () {
      final orderDetail1 = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [testOrderedItem1],
      );

      final orderDetail2 = OrderDetail(
        id: 1,
        orderRequestNumber: 'OP00000001',
        clientId: 100,
        clientName: '테스트 거래처',
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalAmount: 500000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [testOrderedItem1],
      );

      expect(orderDetail1.hashCode, equals(orderDetail2.hashCode));
    });

    test('toString should return formatted string', () {
      final str = testOrderDetail.toString();

      expect(str, contains('OrderDetail'));
      expect(str, contains('id: 1'));
      expect(str, contains('OP00000001'));
      expect(str, contains('테스트 거래처'));
      expect(str, contains('isClosed: true'));
    });
  });
}
