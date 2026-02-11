import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/client_order_model.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_detail.dart';

void main() {
  group('ClientOrderModel', () {
    const testSapOrderNumber = '300011396';
    const testClientId = 12345;
    const testClientName = '서울마트';
    const testTotalAmount = 1500000;

    final testJson = {
      'sap_order_number': testSapOrderNumber,
      'client_id': testClientId,
      'client_name': testClientName,
      'total_amount': testTotalAmount,
    };

    final testModel = ClientOrderModel(
      sapOrderNumber: testSapOrderNumber,
      clientId: testClientId,
      clientName: testClientName,
      totalAmount: testTotalAmount,
    );

    final testEntity = ClientOrder(
      sapOrderNumber: testSapOrderNumber,
      clientId: testClientId,
      clientName: testClientName,
      totalAmount: testTotalAmount,
    );

    test('fromJson should parse snake_case JSON correctly', () {
      final result = ClientOrderModel.fromJson(testJson);

      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.totalAmount, testTotalAmount);
    });

    test('toJson should output snake_case JSON correctly', () {
      final result = testModel.toJson();

      expect(result['sap_order_number'], testSapOrderNumber);
      expect(result['client_id'], testClientId);
      expect(result['client_name'], testClientName);
      expect(result['total_amount'], testTotalAmount);
    });

    test('toEntity should convert to domain entity correctly', () {
      final result = testModel.toEntity();

      expect(result, isA<ClientOrder>());
      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.totalAmount, testTotalAmount);
    });

    test('fromEntity should convert from domain entity correctly', () {
      final result = ClientOrderModel.fromEntity(testEntity);

      expect(result, isA<ClientOrderModel>());
      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.totalAmount, testTotalAmount);
    });

    test('round-trip: entity -> model -> json -> model -> entity should equal',
        () {
      final model1 = ClientOrderModel.fromEntity(testEntity);
      final json = model1.toJson();
      final model2 = ClientOrderModel.fromJson(json);
      final entity2 = model2.toEntity();

      expect(entity2, testEntity);
    });

    test('equality should work correctly', () {
      final model1 = ClientOrderModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      final model2 = ClientOrderModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      final model3 = ClientOrderModel(
        sapOrderNumber: 'different',
        clientId: testClientId,
        clientName: testClientName,
        totalAmount: testTotalAmount,
      );

      expect(model1, model2);
      expect(model1, isNot(model3));
      expect(model1.hashCode, model2.hashCode);
      expect(model1.hashCode, isNot(model3.hashCode));
    });

    test('toString should return formatted string', () {
      final result = testModel.toString();

      expect(result, contains('ClientOrderModel'));
      expect(result, contains(testSapOrderNumber));
      expect(result, contains(testClientId.toString()));
      expect(result, contains(testClientName));
      expect(result, contains(testTotalAmount.toString()));
    });
  });

  group('ClientOrderItemModel', () {
    const testProductCode = '01101123';
    const testProductName = '갈릭 아이올리소스 240g';
    const testDeliveredQuantity = '10 BOX';
    const testDeliveryStatus = 'DELIVERED';

    final testJson = {
      'product_code': testProductCode,
      'product_name': testProductName,
      'delivered_quantity': testDeliveredQuantity,
      'delivery_status': testDeliveryStatus,
    };

    final testModel = ClientOrderItemModel(
      productCode: testProductCode,
      productName: testProductName,
      deliveredQuantity: testDeliveredQuantity,
      deliveryStatus: testDeliveryStatus,
    );

    final testEntity = ClientOrderItem(
      productCode: testProductCode,
      productName: testProductName,
      deliveredQuantity: testDeliveredQuantity,
      deliveryStatus: DeliveryStatus.delivered,
    );

    test('fromJson should parse snake_case JSON correctly', () {
      final result = ClientOrderItemModel.fromJson(testJson);

      expect(result.productCode, testProductCode);
      expect(result.productName, testProductName);
      expect(result.deliveredQuantity, testDeliveredQuantity);
      expect(result.deliveryStatus, testDeliveryStatus);
    });

    test('toJson should output snake_case JSON correctly', () {
      final result = testModel.toJson();

      expect(result['product_code'], testProductCode);
      expect(result['product_name'], testProductName);
      expect(result['delivered_quantity'], testDeliveredQuantity);
      expect(result['delivery_status'], testDeliveryStatus);
    });

    test('toEntity should convert to domain entity correctly', () {
      final result = testModel.toEntity();

      expect(result, isA<ClientOrderItem>());
      expect(result.productCode, testProductCode);
      expect(result.productName, testProductName);
      expect(result.deliveredQuantity, testDeliveredQuantity);
      expect(result.deliveryStatus, DeliveryStatus.delivered);
    });

    test('toEntity should handle WAITING status correctly', () {
      final model = ClientOrderItemModel(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: '0 BOX',
        deliveryStatus: 'WAITING',
      );

      final result = model.toEntity();

      expect(result.deliveryStatus, DeliveryStatus.waiting);
    });

    test('toEntity should handle SHIPPING status correctly', () {
      final model = ClientOrderItemModel(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: '5 BOX',
        deliveryStatus: 'SHIPPING',
      );

      final result = model.toEntity();

      expect(result.deliveryStatus, DeliveryStatus.shipping);
    });

    test('fromEntity should convert from domain entity correctly', () {
      final result = ClientOrderItemModel.fromEntity(testEntity);

      expect(result, isA<ClientOrderItemModel>());
      expect(result.productCode, testProductCode);
      expect(result.productName, testProductName);
      expect(result.deliveredQuantity, testDeliveredQuantity);
      expect(result.deliveryStatus, 'DELIVERED');
    });

    test('fromEntity should handle different delivery statuses', () {
      final waitingEntity = ClientOrderItem(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: '0 BOX',
        deliveryStatus: DeliveryStatus.waiting,
      );

      final result = ClientOrderItemModel.fromEntity(waitingEntity);
      expect(result.deliveryStatus, 'WAITING');
    });

    test('round-trip: entity -> model -> json -> model -> entity should equal',
        () {
      final model1 = ClientOrderItemModel.fromEntity(testEntity);
      final json = model1.toJson();
      final model2 = ClientOrderItemModel.fromJson(json);
      final entity2 = model2.toEntity();

      expect(entity2, testEntity);
    });

    test('equality should work correctly', () {
      final model1 = ClientOrderItemModel(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      final model2 = ClientOrderItemModel(
        productCode: testProductCode,
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      final model3 = ClientOrderItemModel(
        productCode: 'different',
        productName: testProductName,
        deliveredQuantity: testDeliveredQuantity,
        deliveryStatus: testDeliveryStatus,
      );

      expect(model1, model2);
      expect(model1, isNot(model3));
      expect(model1.hashCode, model2.hashCode);
      expect(model1.hashCode, isNot(model3.hashCode));
    });

    test('toString should return formatted string', () {
      final result = testModel.toString();

      expect(result, contains('ClientOrderItemModel'));
      expect(result, contains(testProductCode));
      expect(result, contains(testProductName));
      expect(result, contains(testDeliveredQuantity));
      expect(result, contains(testDeliveryStatus));
    });
  });

  group('ClientOrderDetailModel', () {
    const testSapOrderNumber = '300011396';
    const testClientId = 12345;
    const testClientName = '서울마트';
    const testClientDeadlineTime = '14:00';
    const testOrderDate = '2025-01-15';
    const testDeliveryDate = '2025-01-17';
    const testTotalApprovedAmount = 2000000;
    const testOrderedItemCount = 3;

    final testItemJson = {
      'product_code': '01101123',
      'product_name': '갈릭 아이올리소스 240g',
      'delivered_quantity': '10 BOX',
      'delivery_status': 'DELIVERED',
    };

    final testJsonWithoutWrapper = {
      'sap_order_number': testSapOrderNumber,
      'client_id': testClientId,
      'client_name': testClientName,
      'client_deadline_time': testClientDeadlineTime,
      'order_date': testOrderDate,
      'delivery_date': testDeliveryDate,
      'total_approved_amount': testTotalApprovedAmount,
      'ordered_item_count': testOrderedItemCount,
      'ordered_items': [testItemJson],
    };

    final testJsonWithWrapper = {
      'data': testJsonWithoutWrapper,
    };

    final testJsonNullDeadline = {
      'sap_order_number': testSapOrderNumber,
      'client_id': testClientId,
      'client_name': testClientName,
      'client_deadline_time': null,
      'order_date': testOrderDate,
      'delivery_date': testDeliveryDate,
      'total_approved_amount': testTotalApprovedAmount,
      'ordered_item_count': testOrderedItemCount,
      'ordered_items': [testItemJson],
    };

    final testItemModel = ClientOrderItemModel(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      deliveredQuantity: '10 BOX',
      deliveryStatus: 'DELIVERED',
    );

    final testModel = ClientOrderDetailModel(
      sapOrderNumber: testSapOrderNumber,
      clientId: testClientId,
      clientName: testClientName,
      clientDeadlineTime: testClientDeadlineTime,
      orderDate: testOrderDate,
      deliveryDate: testDeliveryDate,
      totalApprovedAmount: testTotalApprovedAmount,
      orderedItemCount: testOrderedItemCount,
      orderedItems: [testItemModel],
    );

    final testEntity = ClientOrderDetail(
      sapOrderNumber: testSapOrderNumber,
      clientId: testClientId,
      clientName: testClientName,
      clientDeadlineTime: testClientDeadlineTime,
      orderDate: DateTime.parse(testOrderDate),
      deliveryDate: DateTime.parse(testDeliveryDate),
      totalApprovedAmount: testTotalApprovedAmount,
      orderedItemCount: testOrderedItemCount,
      orderedItems: [testItemModel.toEntity()],
    );

    test('fromJson should parse snake_case JSON without wrapper', () {
      final result = ClientOrderDetailModel.fromJson(testJsonWithoutWrapper);

      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.clientDeadlineTime, testClientDeadlineTime);
      expect(result.orderDate, testOrderDate);
      expect(result.deliveryDate, testDeliveryDate);
      expect(result.totalApprovedAmount, testTotalApprovedAmount);
      expect(result.orderedItemCount, testOrderedItemCount);
      expect(result.orderedItems, hasLength(1));
      expect(result.orderedItems.first.productCode, '01101123');
    });

    test('fromJson should parse snake_case JSON with data wrapper', () {
      final result = ClientOrderDetailModel.fromJson(testJsonWithWrapper);

      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.clientDeadlineTime, testClientDeadlineTime);
      expect(result.orderDate, testOrderDate);
      expect(result.deliveryDate, testDeliveryDate);
      expect(result.totalApprovedAmount, testTotalApprovedAmount);
      expect(result.orderedItemCount, testOrderedItemCount);
      expect(result.orderedItems, hasLength(1));
    });

    test('fromJson should handle null clientDeadlineTime', () {
      final result = ClientOrderDetailModel.fromJson(testJsonNullDeadline);

      expect(result.clientDeadlineTime, isNull);
      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
    });

    test('fromJson should handle empty orderedItems array', () {
      final jsonWithEmptyItems = {
        'sap_order_number': testSapOrderNumber,
        'client_id': testClientId,
        'client_name': testClientName,
        'client_deadline_time': testClientDeadlineTime,
        'order_date': testOrderDate,
        'delivery_date': testDeliveryDate,
        'total_approved_amount': testTotalApprovedAmount,
        'ordered_item_count': 0,
        'ordered_items': [],
      };

      final result = ClientOrderDetailModel.fromJson(jsonWithEmptyItems);

      expect(result.orderedItems, isEmpty);
      expect(result.orderedItemCount, 0);
    });

    test('toJson should output snake_case JSON correctly', () {
      final result = testModel.toJson();

      expect(result['sap_order_number'], testSapOrderNumber);
      expect(result['client_id'], testClientId);
      expect(result['client_name'], testClientName);
      expect(result['client_deadline_time'], testClientDeadlineTime);
      expect(result['order_date'], testOrderDate);
      expect(result['delivery_date'], testDeliveryDate);
      expect(result['total_approved_amount'], testTotalApprovedAmount);
      expect(result['ordered_item_count'], testOrderedItemCount);
      expect(result['ordered_items'], isA<List>());
      expect(result['ordered_items'], hasLength(1));
    });

    test('toEntity should convert to domain entity correctly', () {
      final result = testModel.toEntity();

      expect(result, isA<ClientOrderDetail>());
      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.clientDeadlineTime, testClientDeadlineTime);
      expect(result.orderDate, DateTime.parse(testOrderDate));
      expect(result.deliveryDate, DateTime.parse(testDeliveryDate));
      expect(result.totalApprovedAmount, testTotalApprovedAmount);
      expect(result.orderedItemCount, testOrderedItemCount);
      expect(result.orderedItems, hasLength(1));
      expect(result.orderedItems.first.productCode, '01101123');
      expect(result.orderedItems.first.deliveryStatus, DeliveryStatus.delivered);
    });

    test('toEntity should handle null clientDeadlineTime', () {
      final modelWithNullDeadline = ClientOrderDetailModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: null,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: [testItemModel],
      );

      final result = modelWithNullDeadline.toEntity();

      expect(result.clientDeadlineTime, isNull);
    });

    test('fromEntity should convert from domain entity correctly', () {
      final result = ClientOrderDetailModel.fromEntity(testEntity);

      expect(result, isA<ClientOrderDetailModel>());
      expect(result.sapOrderNumber, testSapOrderNumber);
      expect(result.clientId, testClientId);
      expect(result.clientName, testClientName);
      expect(result.clientDeadlineTime, testClientDeadlineTime);
      expect(result.orderDate, testOrderDate);
      expect(result.deliveryDate, testDeliveryDate);
      expect(result.totalApprovedAmount, testTotalApprovedAmount);
      expect(result.orderedItemCount, testOrderedItemCount);
      expect(result.orderedItems, hasLength(1));
    });

    test('fromEntity should format dates as YYYY-MM-DD', () {
      final entityWithTime = ClientOrderDetail(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: DateTime(2025, 1, 15, 10, 30, 45),
        deliveryDate: DateTime(2025, 1, 17, 23, 59, 59),
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: [],
      );

      final result = ClientOrderDetailModel.fromEntity(entityWithTime);

      expect(result.orderDate, '2025-01-15');
      expect(result.deliveryDate, '2025-01-17');
    });

    test('round-trip: entity -> model -> json -> model -> entity should equal',
        () {
      final model1 = ClientOrderDetailModel.fromEntity(testEntity);
      final json = model1.toJson();
      final model2 = ClientOrderDetailModel.fromJson(json);
      final entity2 = model2.toEntity();

      expect(entity2, testEntity);
    });

    test(
        'round-trip with data wrapper: json -> model -> entity -> model -> json',
        () {
      final model1 = ClientOrderDetailModel.fromJson(testJsonWithWrapper);
      final entity = model1.toEntity();
      final model2 = ClientOrderDetailModel.fromEntity(entity);
      final json = model2.toJson();

      expect(json['sap_order_number'], testSapOrderNumber);
      expect(json['client_id'], testClientId);
    });

    test('equality should work correctly', () {
      final model1 = ClientOrderDetailModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: [testItemModel],
      );

      final model2 = ClientOrderDetailModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: [testItemModel],
      );

      final model3 = ClientOrderDetailModel(
        sapOrderNumber: 'different',
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: testOrderedItemCount,
        orderedItems: [testItemModel],
      );

      expect(model1, model2);
      expect(model1, isNot(model3));
      expect(model1.hashCode, model2.hashCode);
      expect(model1.hashCode, isNot(model3.hashCode));
    });

    test('equality should check orderedItems list', () {
      final model1 = ClientOrderDetailModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: 1,
        orderedItems: [testItemModel],
      );

      final model2 = ClientOrderDetailModel(
        sapOrderNumber: testSapOrderNumber,
        clientId: testClientId,
        clientName: testClientName,
        clientDeadlineTime: testClientDeadlineTime,
        orderDate: testOrderDate,
        deliveryDate: testDeliveryDate,
        totalApprovedAmount: testTotalApprovedAmount,
        orderedItemCount: 0,
        orderedItems: [],
      );

      expect(model1, isNot(model2));
    });

    test('toString should return formatted string', () {
      final result = testModel.toString();

      expect(result, contains('ClientOrderDetailModel'));
      expect(result, contains(testSapOrderNumber));
      expect(result, contains(testClientName));
      expect(result, contains(testOrderedItemCount.toString()));
    });
  });

  group('ClientOrderListResponseModel', () {
    const testTotalElements = 25;
    const testTotalPages = 3;
    const testNumber = 0;
    const testSize = 10;
    const testFirst = true;
    const testLast = false;

    final testOrderJson = {
      'sap_order_number': '300011396',
      'client_id': 12345,
      'client_name': '서울마트',
      'total_amount': 1500000,
    };

    final testJson = {
      'data': {
        'content': [testOrderJson],
        'total_elements': testTotalElements,
        'total_pages': testTotalPages,
        'number': testNumber,
        'size': testSize,
        'first': testFirst,
        'last': testLast,
      }
    };

    test('fromJson should parse paginated response correctly', () {
      final result = ClientOrderListResponseModel.fromJson(testJson);

      expect(result.content, hasLength(1));
      expect(result.content.first.sapOrderNumber, '300011396');
      expect(result.totalElements, testTotalElements);
      expect(result.totalPages, testTotalPages);
      expect(result.number, testNumber);
      expect(result.size, testSize);
      expect(result.first, testFirst);
      expect(result.last, testLast);
    });

    test('fromJson should handle empty content', () {
      final emptyJson = {
        'data': {
          'content': [],
          'total_elements': 0,
          'total_pages': 0,
          'number': 0,
          'size': 10,
          'first': true,
          'last': true,
        }
      };

      final result = ClientOrderListResponseModel.fromJson(emptyJson);

      expect(result.content, isEmpty);
      expect(result.totalElements, 0);
      expect(result.totalPages, 0);
      expect(result.first, true);
      expect(result.last, true);
    });

    test('fromJson should handle multiple items in content', () {
      final multipleItemsJson = {
        'data': {
          'content': [
            {
              'sap_order_number': '300011396',
              'client_id': 12345,
              'client_name': '서울마트',
              'total_amount': 1500000,
            },
            {
              'sap_order_number': '300011397',
              'client_id': 12346,
              'client_name': '부산마트',
              'total_amount': 2000000,
            },
          ],
          'total_elements': 2,
          'total_pages': 1,
          'number': 0,
          'size': 10,
          'first': true,
          'last': true,
        }
      };

      final result = ClientOrderListResponseModel.fromJson(multipleItemsJson);

      expect(result.content, hasLength(2));
      expect(result.content[0].sapOrderNumber, '300011396');
      expect(result.content[1].sapOrderNumber, '300011397');
      expect(result.content[0].clientName, '서울마트');
      expect(result.content[1].clientName, '부산마트');
    });

    test('fromJson should parse all pagination fields correctly', () {
      final result = ClientOrderListResponseModel.fromJson(testJson);

      expect(result.totalElements, 25);
      expect(result.totalPages, 3);
      expect(result.number, 0);
      expect(result.size, 10);
      expect(result.first, true);
      expect(result.last, false);
    });

    test('fromJson should handle last page correctly', () {
      final lastPageJson = {
        'data': {
          'content': [testOrderJson],
          'total_elements': 25,
          'total_pages': 3,
          'number': 2,
          'size': 10,
          'first': false,
          'last': true,
        }
      };

      final result = ClientOrderListResponseModel.fromJson(lastPageJson);

      expect(result.number, 2);
      expect(result.first, false);
      expect(result.last, true);
    });

    test('constructor should create instance correctly', () {
      final order = ClientOrderModel(
        sapOrderNumber: '300011396',
        clientId: 12345,
        clientName: '서울마트',
        totalAmount: 1500000,
      );

      final response = ClientOrderListResponseModel(
        content: [order],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 10,
        first: true,
        last: true,
      );

      expect(response.content, hasLength(1));
      expect(response.content.first, order);
      expect(response.totalElements, 1);
      expect(response.totalPages, 1);
      expect(response.number, 0);
      expect(response.size, 10);
      expect(response.first, true);
      expect(response.last, true);
    });
  });

  group('Integration Tests', () {
    test('Full workflow: JSON API response -> entities -> back to JSON', () {
      // Simulate API response
      final apiResponse = {
        'data': {
          'content': [
            {
              'sap_order_number': '300011396',
              'client_id': 12345,
              'client_name': '서울마트',
              'total_amount': 1500000,
            },
            {
              'sap_order_number': '300011397',
              'client_id': 12346,
              'client_name': '부산마트',
              'total_amount': 2000000,
            },
          ],
          'total_elements': 2,
          'total_pages': 1,
          'number': 0,
          'size': 10,
          'first': true,
          'last': true,
        }
      };

      // Parse response
      final response = ClientOrderListResponseModel.fromJson(apiResponse);

      // Convert to entities
      final entities = response.content.map((m) => m.toEntity()).toList();

      expect(entities, hasLength(2));
      expect(entities[0].sapOrderNumber, '300011396');
      expect(entities[1].sapOrderNumber, '300011397');

      // Convert back to models
      final models = entities.map((e) => ClientOrderModel.fromEntity(e)).toList();

      // Convert to JSON
      final jsonList = models.map((m) => m.toJson()).toList();

      expect(jsonList[0]['sap_order_number'], '300011396');
      expect(jsonList[1]['sap_order_number'], '300011397');
    });

    test(
        'Detail workflow: API response with data wrapper -> entity -> model -> JSON',
        () {
      final apiResponse = {
        'data': {
          'sap_order_number': '300011396',
          'client_id': 12345,
          'client_name': '서울마트',
          'client_deadline_time': '14:00',
          'order_date': '2025-01-15',
          'delivery_date': '2025-01-17',
          'total_approved_amount': 2000000,
          'ordered_item_count': 2,
          'ordered_items': [
            {
              'product_code': '01101123',
              'product_name': '갈릭 아이올리소스 240g',
              'delivered_quantity': '10 BOX',
              'delivery_status': 'DELIVERED',
            },
            {
              'product_code': '01101124',
              'product_name': '스위트 칠리소스 300g',
              'delivered_quantity': '5 BOX',
              'delivery_status': 'SHIPPING',
            },
          ],
        }
      };

      // Parse response
      final model = ClientOrderDetailModel.fromJson(apiResponse);

      // Convert to entity
      final entity = model.toEntity();

      expect(entity.sapOrderNumber, '300011396');
      expect(entity.orderedItems, hasLength(2));
      expect(entity.orderedItems[0].deliveryStatus, DeliveryStatus.delivered);
      expect(entity.orderedItems[1].deliveryStatus, DeliveryStatus.shipping);

      // Convert back to model
      final model2 = ClientOrderDetailModel.fromEntity(entity);

      // Convert to JSON
      final json = model2.toJson();

      expect(json['sap_order_number'], '300011396');
      expect(json['ordered_items'], hasLength(2));
      expect(json['ordered_items'][0]['delivery_status'], 'DELIVERED');
      expect(json['ordered_items'][1]['delivery_status'], 'SHIPPING');
    });

    test('Handle multiple delivery statuses in items', () {
      final items = [
        ClientOrderItemModel(
          productCode: 'P1',
          productName: 'Product 1',
          deliveredQuantity: '0 BOX',
          deliveryStatus: 'WAITING',
        ),
        ClientOrderItemModel(
          productCode: 'P2',
          productName: 'Product 2',
          deliveredQuantity: '5 BOX',
          deliveryStatus: 'SHIPPING',
        ),
        ClientOrderItemModel(
          productCode: 'P3',
          productName: 'Product 3',
          deliveredQuantity: '10 BOX',
          deliveryStatus: 'DELIVERED',
        ),
      ];

      final entities = items.map((m) => m.toEntity()).toList();

      expect(entities[0].deliveryStatus, DeliveryStatus.waiting);
      expect(entities[1].deliveryStatus, DeliveryStatus.shipping);
      expect(entities[2].deliveryStatus, DeliveryStatus.delivered);
    });
  });
}
