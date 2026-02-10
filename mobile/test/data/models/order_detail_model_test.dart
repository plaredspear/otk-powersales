import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_detail_model.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_detail.dart';

void main() {
  group('OrderedItemModel', () {
    final sampleJson = {
      'product_code': '01101123',
      'product_name': '갈릭 아이올리소스 240g',
      'total_quantity_boxes': 5.0,
      'total_quantity_pieces': 100,
      'is_cancelled': false,
    };

    final sampleModel = OrderedItemModel(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      totalQuantityBoxes: 5.0,
      totalQuantityPieces: 100,
      isCancelled: false,
    );

    group('fromJson', () {
      test('parses snake_case JSON correctly', () {
        final model = OrderedItemModel.fromJson(sampleJson);

        expect(model.productCode, '01101123');
        expect(model.productName, '갈릭 아이올리소스 240g');
        expect(model.totalQuantityBoxes, 5.0);
        expect(model.totalQuantityPieces, 100);
        expect(model.isCancelled, false);
      });

      test('handles integer quantity_boxes as double', () {
        final json = {
          ...sampleJson,
          'total_quantity_boxes': 10, // int
        };

        final model = OrderedItemModel.fromJson(json);
        expect(model.totalQuantityBoxes, 10.0);
      });

      test('handles cancelled item', () {
        final json = {
          ...sampleJson,
          'is_cancelled': true,
        };

        final model = OrderedItemModel.fromJson(json);
        expect(model.isCancelled, true);
      });
    });

    group('toJson', () {
      test('serializes to snake_case JSON', () {
        final json = sampleModel.toJson();

        expect(json['product_code'], '01101123');
        expect(json['product_name'], '갈릭 아이올리소스 240g');
        expect(json['total_quantity_boxes'], 5.0);
        expect(json['total_quantity_pieces'], 100);
        expect(json['is_cancelled'], false);
      });
    });

    group('toEntity', () {
      test('converts to OrderedItem entity correctly', () {
        final entity = sampleModel.toEntity();

        expect(entity.productCode, sampleModel.productCode);
        expect(entity.productName, sampleModel.productName);
        expect(entity.totalQuantityBoxes, sampleModel.totalQuantityBoxes);
        expect(entity.totalQuantityPieces, sampleModel.totalQuantityPieces);
        expect(entity.isCancelled, sampleModel.isCancelled);
      });
    });

    group('fromEntity', () {
      test('creates model from OrderedItem entity', () {
        final entity = OrderedItem(
          productCode: '23010011',
          productName: '오감포차_크림새우180G',
          totalQuantityBoxes: 3.5,
          totalQuantityPieces: 70,
          isCancelled: true,
        );

        final model = OrderedItemModel.fromEntity(entity);

        expect(model.productCode, entity.productCode);
        expect(model.productName, entity.productName);
        expect(model.totalQuantityBoxes, entity.totalQuantityBoxes);
        expect(model.totalQuantityPieces, entity.totalQuantityPieces);
        expect(model.isCancelled, entity.isCancelled);
      });
    });
  });

  group('ProcessingItemModel', () {
    final sampleJson = {
      'product_code': '01101123',
      'product_name': '갈릭 아이올리소스 240g',
      'delivered_quantity': '100 EA',
      'delivery_status': 'DELIVERED',
    };

    final sampleModel = ProcessingItemModel(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      deliveredQuantity: '100 EA',
      deliveryStatus: 'DELIVERED',
    );

    group('fromJson', () {
      test('parses snake_case JSON correctly', () {
        final model = ProcessingItemModel.fromJson(sampleJson);

        expect(model.productCode, '01101123');
        expect(model.productName, '갈릭 아이올리소스 240g');
        expect(model.deliveredQuantity, '100 EA');
        expect(model.deliveryStatus, 'DELIVERED');
      });

      test('handles WAITING status', () {
        final json = {
          ...sampleJson,
          'delivery_status': 'WAITING',
          'delivered_quantity': '0 EA',
        };

        final model = ProcessingItemModel.fromJson(json);
        expect(model.deliveryStatus, 'WAITING');
        expect(model.deliveredQuantity, '0 EA');
      });

      test('handles SHIPPING status', () {
        final json = {
          ...sampleJson,
          'delivery_status': 'SHIPPING',
        };

        final model = ProcessingItemModel.fromJson(json);
        expect(model.deliveryStatus, 'SHIPPING');
      });
    });

    group('toJson', () {
      test('serializes to snake_case JSON', () {
        final json = sampleModel.toJson();

        expect(json['product_code'], '01101123');
        expect(json['product_name'], '갈릭 아이올리소스 240g');
        expect(json['delivered_quantity'], '100 EA');
        expect(json['delivery_status'], 'DELIVERED');
      });
    });

    group('toEntity', () {
      test('converts to ProcessingItem entity with enum', () {
        final entity = sampleModel.toEntity();

        expect(entity.productCode, sampleModel.productCode);
        expect(entity.productName, sampleModel.productName);
        expect(entity.deliveredQuantity, sampleModel.deliveredQuantity);
        expect(entity.deliveryStatus, DeliveryStatus.delivered);
      });

      test('converts WAITING status to enum', () {
        final model = ProcessingItemModel(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          deliveredQuantity: '0 EA',
          deliveryStatus: 'WAITING',
        );

        final entity = model.toEntity();
        expect(entity.deliveryStatus, DeliveryStatus.waiting);
      });

      test('converts SHIPPING status to enum', () {
        final model = ProcessingItemModel(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          deliveredQuantity: '50 EA',
          deliveryStatus: 'SHIPPING',
        );

        final entity = model.toEntity();
        expect(entity.deliveryStatus, DeliveryStatus.shipping);
      });
    });
  });

  group('OrderProcessingStatusModel', () {
    final itemJson = {
      'product_code': '01101123',
      'product_name': '갈릭 아이올리소스 240g',
      'delivered_quantity': '0 EA',
      'delivery_status': 'WAITING',
    };

    final sampleJson = {
      'sap_order_number': '0300013650',
      'items': [itemJson],
    };

    final sampleModel = OrderProcessingStatusModel(
      sapOrderNumber: '0300013650',
      items: [
        ProcessingItemModel(
          productCode: '01101123',
          productName: '갈릭 아이올리소스 240g',
          deliveredQuantity: '0 EA',
          deliveryStatus: 'WAITING',
        ),
      ],
    );

    group('fromJson', () {
      test('parses snake_case JSON with nested items', () {
        final model = OrderProcessingStatusModel.fromJson(sampleJson);

        expect(model.sapOrderNumber, '0300013650');
        expect(model.items.length, 1);
        expect(model.items[0].productCode, '01101123');
        expect(model.items[0].deliveryStatus, 'WAITING');
      });

      test('handles multiple items', () {
        final json = {
          'sap_order_number': '0300013651',
          'items': [
            itemJson,
            {
              'product_code': '23010011',
              'product_name': '오감포차_크림새우180G',
              'delivered_quantity': '70 EA',
              'delivery_status': 'DELIVERED',
            },
          ],
        };

        final model = OrderProcessingStatusModel.fromJson(json);
        expect(model.items.length, 2);
        expect(model.items[1].productCode, '23010011');
        expect(model.items[1].deliveryStatus, 'DELIVERED');
      });

      test('handles empty items list', () {
        final json = {
          'sap_order_number': '0300013652',
          'items': [],
        };

        final model = OrderProcessingStatusModel.fromJson(json);
        expect(model.items, isEmpty);
      });
    });

    group('toJson', () {
      test('serializes to snake_case JSON with nested items', () {
        final json = sampleModel.toJson();

        expect(json['sap_order_number'], '0300013650');
        expect(json['items'], isList);
        expect((json['items'] as List).length, 1);
        expect((json['items'] as List)[0]['product_code'], '01101123');
      });
    });

    group('toEntity', () {
      test('converts to OrderProcessingStatus entity', () {
        final entity = sampleModel.toEntity();

        expect(entity.sapOrderNumber, sampleModel.sapOrderNumber);
        expect(entity.items.length, sampleModel.items.length);
        expect(entity.items[0].productCode, sampleModel.items[0].productCode);
        expect(entity.items[0].deliveryStatus, DeliveryStatus.waiting);
      });

      test('converts nested items with correct enums', () {
        final model = OrderProcessingStatusModel(
          sapOrderNumber: '0300013653',
          items: [
            ProcessingItemModel(
              productCode: '01101123',
              productName: '갈릭 아이올리소스 240g',
              deliveredQuantity: '0 EA',
              deliveryStatus: 'WAITING',
            ),
            ProcessingItemModel(
              productCode: '23010011',
              productName: '오감포차_크림새우180G',
              deliveredQuantity: '50 EA',
              deliveryStatus: 'SHIPPING',
            ),
            ProcessingItemModel(
              productCode: '11110003',
              productName: '토마토케찹500G',
              deliveredQuantity: '100 EA',
              deliveryStatus: 'DELIVERED',
            ),
          ],
        );

        final entity = model.toEntity();
        expect(entity.items.length, 3);
        expect(entity.items[0].deliveryStatus, DeliveryStatus.waiting);
        expect(entity.items[1].deliveryStatus, DeliveryStatus.shipping);
        expect(entity.items[2].deliveryStatus, DeliveryStatus.delivered);
      });
    });
  });

  group('RejectedItemModel', () {
    final sampleJson = {
      'product_code': '23010011',
      'product_name': '오감포차_크림새우180G',
      'order_quantity_boxes': 1,
      'rejection_reason': '납품일자가 업무일이 아닙니다.',
    };

    final sampleModel = RejectedItemModel(
      productCode: '23010011',
      productName: '오감포차_크림새우180G',
      orderQuantityBoxes: 1,
      rejectionReason: '납품일자가 업무일이 아닙니다.',
    );

    group('fromJson', () {
      test('parses snake_case JSON correctly', () {
        final model = RejectedItemModel.fromJson(sampleJson);

        expect(model.productCode, '23010011');
        expect(model.productName, '오감포차_크림새우180G');
        expect(model.orderQuantityBoxes, 1);
        expect(model.rejectionReason, '납품일자가 업무일이 아닙니다.');
      });

      test('handles different rejection reasons', () {
        final json = {
          ...sampleJson,
          'order_quantity_boxes': 5,
          'rejection_reason': '재고 부족',
        };

        final model = RejectedItemModel.fromJson(json);
        expect(model.orderQuantityBoxes, 5);
        expect(model.rejectionReason, '재고 부족');
      });
    });

    group('toJson', () {
      test('serializes to snake_case JSON', () {
        final json = sampleModel.toJson();

        expect(json['product_code'], '23010011');
        expect(json['product_name'], '오감포차_크림새우180G');
        expect(json['order_quantity_boxes'], 1);
        expect(json['rejection_reason'], '납품일자가 업무일이 아닙니다.');
      });
    });

    group('toEntity', () {
      test('converts to RejectedItem entity correctly', () {
        final entity = sampleModel.toEntity();

        expect(entity.productCode, sampleModel.productCode);
        expect(entity.productName, sampleModel.productName);
        expect(entity.orderQuantityBoxes, sampleModel.orderQuantityBoxes);
        expect(entity.rejectionReason, sampleModel.rejectionReason);
      });
    });
  });

  group('OrderDetailModel', () {
    final orderedItemJson = {
      'product_code': '01101123',
      'product_name': '갈릭 아이올리소스 240g',
      'total_quantity_boxes': 5.0,
      'total_quantity_pieces': 100,
      'is_cancelled': false,
    };

    final processingStatusJson = {
      'sap_order_number': '0300013650',
      'items': [
        {
          'product_code': '01101123',
          'product_name': '갈릭 아이올리소스 240g',
          'delivered_quantity': '0 EA',
          'delivery_status': 'WAITING',
        },
      ],
    };

    final rejectedItemJson = {
      'product_code': '23010011',
      'product_name': '오감포차_크림새우180G',
      'order_quantity_boxes': 1,
      'rejection_reason': '납품일자가 업무일이 아닙니다.',
    };

    group('fromJson', () {
      test('parses full JSON with all optional fields', () {
        final json = {
          'id': 1,
          'order_request_number': 'OP00000001',
          'client_id': 1,
          'client_name': '테스트거래처',
          'client_deadline_time': '13:40',
          'order_date': '2026-02-10',
          'delivery_date': '2026-02-11',
          'total_amount': 100000,
          'total_approved_amount': 85000,
          'approval_status': 'APPROVED',
          'is_closed': true,
          'ordered_item_count': 2,
          'ordered_items': [orderedItemJson],
          'order_processing_status': processingStatusJson,
          'rejected_items': [rejectedItemJson],
        };

        final model = OrderDetailModel.fromJson(json);

        expect(model.id, 1);
        expect(model.orderRequestNumber, 'OP00000001');
        expect(model.clientId, 1);
        expect(model.clientName, '테스트거래처');
        expect(model.clientDeadlineTime, '13:40');
        expect(model.orderDate, '2026-02-10');
        expect(model.deliveryDate, '2026-02-11');
        expect(model.totalAmount, 100000);
        expect(model.totalApprovedAmount, 85000);
        expect(model.approvalStatus, 'APPROVED');
        expect(model.isClosed, true);
        expect(model.orderedItemCount, 2);
        expect(model.orderedItems.length, 1);
        expect(model.orderProcessingStatus, isNotNull);
        expect(model.rejectedItems, isNotNull);
        expect(model.rejectedItems!.length, 1);
      });

      test('parses minimal JSON without optional fields', () {
        final json = {
          'id': 2,
          'order_request_number': 'OP00000002',
          'client_id': 2,
          'client_name': '거래처2',
          'order_date': '2026-02-09',
          'delivery_date': '2026-02-10',
          'total_amount': 50000,
          'approval_status': 'PENDING',
          'is_closed': false,
          'ordered_item_count': 1,
          'ordered_items': [orderedItemJson],
        };

        final model = OrderDetailModel.fromJson(json);

        expect(model.id, 2);
        expect(model.clientDeadlineTime, isNull);
        expect(model.totalApprovedAmount, isNull);
        expect(model.orderProcessingStatus, isNull);
        expect(model.rejectedItems, isNull);
        expect(model.isClosed, false);
      });

      test('parses JSON with data wrapper', () {
        final json = {
          'data': {
            'id': 3,
            'order_request_number': 'OP00000003',
            'client_id': 3,
            'client_name': '거래처3',
            'order_date': '2026-02-08',
            'delivery_date': '2026-02-09',
            'total_amount': 75000,
            'approval_status': 'APPROVED',
            'is_closed': false,
            'ordered_item_count': 1,
            'ordered_items': [orderedItemJson],
          },
        };

        final model = OrderDetailModel.fromJson(json);

        expect(model.id, 3);
        expect(model.orderRequestNumber, 'OP00000003');
        expect(model.clientName, '거래처3');
      });

      test('handles empty ordered_items list', () {
        final json = {
          'id': 4,
          'order_request_number': 'OP00000004',
          'client_id': 4,
          'client_name': '거래처4',
          'order_date': '2026-02-07',
          'delivery_date': '2026-02-08',
          'total_amount': 0,
          'approval_status': 'PENDING',
          'is_closed': false,
          'ordered_item_count': 0,
          'ordered_items': [],
        };

        final model = OrderDetailModel.fromJson(json);
        expect(model.orderedItems, isEmpty);
        expect(model.orderedItemCount, 0);
      });

      test('handles null rejected_items field', () {
        final json = {
          'id': 5,
          'order_request_number': 'OP00000005',
          'client_id': 5,
          'client_name': '거래처5',
          'order_date': '2026-02-06',
          'delivery_date': '2026-02-07',
          'total_amount': 60000,
          'approval_status': 'APPROVED',
          'is_closed': true,
          'ordered_item_count': 1,
          'ordered_items': [orderedItemJson],
          'rejected_items': null,
        };

        final model = OrderDetailModel.fromJson(json);
        expect(model.rejectedItems, isNull);
      });

      test('handles multiple rejected items', () {
        final json = {
          'id': 6,
          'order_request_number': 'OP00000006',
          'client_id': 6,
          'client_name': '거래처6',
          'order_date': '2026-02-05',
          'delivery_date': '2026-02-06',
          'total_amount': 120000,
          'approval_status': 'APPROVED',
          'is_closed': true,
          'ordered_item_count': 3,
          'ordered_items': [orderedItemJson],
          'rejected_items': [
            rejectedItemJson,
            {
              'product_code': '11110003',
              'product_name': '토마토케찹500G',
              'order_quantity_boxes': 2,
              'rejection_reason': '재고 부족',
            },
            {
              'product_code': '04101002',
              'product_name': '마요네즈 500g',
              'order_quantity_boxes': 5,
              'rejection_reason': '단가 불일치',
            },
          ],
        };

        final model = OrderDetailModel.fromJson(json);
        expect(model.rejectedItems!.length, 3);
        expect(model.rejectedItems![1].productCode, '11110003');
        expect(model.rejectedItems![2].rejectionReason, '단가 불일치');
      });
    });

    group('toJson', () {
      test('serializes full model to snake_case JSON', () {
        final model = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '테스트거래처',
          clientDeadlineTime: '13:40',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          totalApprovedAmount: 85000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 1,
          orderedItems: [
            OrderedItemModel(
              productCode: '01101123',
              productName: '갈릭 아이올리소스 240g',
              totalQuantityBoxes: 5.0,
              totalQuantityPieces: 100,
              isCancelled: false,
            ),
          ],
          orderProcessingStatus: OrderProcessingStatusModel(
            sapOrderNumber: '0300013650',
            items: [
              ProcessingItemModel(
                productCode: '01101123',
                productName: '갈릭 아이올리소스 240g',
                deliveredQuantity: '0 EA',
                deliveryStatus: 'WAITING',
              ),
            ],
          ),
          rejectedItems: [
            RejectedItemModel(
              productCode: '23010011',
              productName: '오감포차_크림새우180G',
              orderQuantityBoxes: 1,
              rejectionReason: '납품일자가 업무일이 아닙니다.',
            ),
          ],
        );

        final json = model.toJson();

        expect(json['id'], 1);
        expect(json['order_request_number'], 'OP00000001');
        expect(json['client_id'], 1);
        expect(json['client_name'], '테스트거래처');
        expect(json['client_deadline_time'], '13:40');
        expect(json['order_date'], '2026-02-10');
        expect(json['delivery_date'], '2026-02-11');
        expect(json['total_amount'], 100000);
        expect(json['total_approved_amount'], 85000);
        expect(json['approval_status'], 'APPROVED');
        expect(json['is_closed'], true);
        expect(json['ordered_item_count'], 1);
        expect(json['ordered_items'], isList);
        expect(json['order_processing_status'], isNotNull);
        expect(json['rejected_items'], isNotNull);
      });

      test('serializes model with null optional fields', () {
        final model = OrderDetailModel(
          id: 2,
          orderRequestNumber: 'OP00000002',
          clientId: 2,
          clientName: '거래처2',
          orderDate: '2026-02-09',
          deliveryDate: '2026-02-10',
          totalAmount: 50000,
          approvalStatus: 'PENDING',
          isClosed: false,
          orderedItemCount: 1,
          orderedItems: [
            OrderedItemModel(
              productCode: '01101123',
              productName: '갈릭 아이올리소스 240g',
              totalQuantityBoxes: 5.0,
              totalQuantityPieces: 100,
              isCancelled: false,
            ),
          ],
        );

        final json = model.toJson();

        expect(json['client_deadline_time'], isNull);
        expect(json['total_approved_amount'], isNull);
        expect(json['order_processing_status'], isNull);
        expect(json['rejected_items'], isNull);
      });
    });

    group('toEntity', () {
      test('converts to OrderDetail entity with all fields', () {
        final model = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '테스트거래처',
          clientDeadlineTime: '13:40',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          totalApprovedAmount: 85000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 1,
          orderedItems: [
            OrderedItemModel(
              productCode: '01101123',
              productName: '갈릭 아이올리소스 240g',
              totalQuantityBoxes: 5.0,
              totalQuantityPieces: 100,
              isCancelled: false,
            ),
          ],
          orderProcessingStatus: OrderProcessingStatusModel(
            sapOrderNumber: '0300013650',
            items: [
              ProcessingItemModel(
                productCode: '01101123',
                productName: '갈릭 아이올리소스 240g',
                deliveredQuantity: '0 EA',
                deliveryStatus: 'WAITING',
              ),
            ],
          ),
          rejectedItems: [
            RejectedItemModel(
              productCode: '23010011',
              productName: '오감포차_크림새우180G',
              orderQuantityBoxes: 1,
              rejectionReason: '납품일자가 업무일이 아닙니다.',
            ),
          ],
        );

        final entity = model.toEntity();

        expect(entity.id, model.id);
        expect(entity.orderRequestNumber, model.orderRequestNumber);
        expect(entity.clientId, model.clientId);
        expect(entity.clientName, model.clientName);
        expect(entity.clientDeadlineTime, model.clientDeadlineTime);
        expect(entity.orderDate, DateTime.parse(model.orderDate));
        expect(entity.deliveryDate, DateTime.parse(model.deliveryDate));
        expect(entity.totalAmount, model.totalAmount);
        expect(entity.totalApprovedAmount, model.totalApprovedAmount);
        expect(entity.approvalStatus, ApprovalStatus.approved);
        expect(entity.isClosed, model.isClosed);
        expect(entity.orderedItemCount, model.orderedItemCount);
        expect(entity.orderedItems.length, 1);
        expect(entity.orderProcessingStatus, isNotNull);
        expect(entity.rejectedItems, isNotNull);
        expect(entity.rejectedItems!.length, 1);
      });

      test('converts approval status code to enum correctly', () {
        final testCases = [
          ('APPROVED', ApprovalStatus.approved),
          ('PENDING', ApprovalStatus.pending),
          ('SEND_FAILED', ApprovalStatus.sendFailed),
          ('RESEND', ApprovalStatus.resend),
        ];

        for (final (code, expectedStatus) in testCases) {
          final model = OrderDetailModel(
            id: 1,
            orderRequestNumber: 'OP00000001',
            clientId: 1,
            clientName: '테스트거래처',
            orderDate: '2026-02-10',
            deliveryDate: '2026-02-11',
            totalAmount: 100000,
            approvalStatus: code,
            isClosed: false,
            orderedItemCount: 0,
            orderedItems: [],
          );

          final entity = model.toEntity();
          expect(entity.approvalStatus, expectedStatus);
        }
      });

      test('converts date strings to DateTime objects', () {
        final model = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '테스트거래처',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-15',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: false,
          orderedItemCount: 0,
          orderedItems: [],
        );

        final entity = model.toEntity();
        expect(entity.orderDate, DateTime(2026, 2, 10));
        expect(entity.deliveryDate, DateTime(2026, 2, 15));
      });

      test('converts nested orderedItems correctly', () {
        final model = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '테스트거래처',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: false,
          orderedItemCount: 2,
          orderedItems: [
            OrderedItemModel(
              productCode: '01101123',
              productName: '갈릭 아이올리소스 240g',
              totalQuantityBoxes: 5.0,
              totalQuantityPieces: 100,
              isCancelled: false,
            ),
            OrderedItemModel(
              productCode: '23010011',
              productName: '오감포차_크림새우180G',
              totalQuantityBoxes: 3.0,
              totalQuantityPieces: 60,
              isCancelled: true,
            ),
          ],
        );

        final entity = model.toEntity();
        expect(entity.orderedItems.length, 2);
        expect(entity.orderedItems[0].productCode, '01101123');
        expect(entity.orderedItems[0].isCancelled, false);
        expect(entity.orderedItems[1].productCode, '23010011');
        expect(entity.orderedItems[1].isCancelled, true);
      });

      test('handles null optional fields in entity conversion', () {
        final model = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '테스트거래처',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'PENDING',
          isClosed: false,
          orderedItemCount: 0,
          orderedItems: [],
        );

        final entity = model.toEntity();
        expect(entity.clientDeadlineTime, isNull);
        expect(entity.totalApprovedAmount, isNull);
        expect(entity.orderProcessingStatus, isNull);
        expect(entity.rejectedItems, isNull);
      });
    });

    group('equality and hashCode', () {
      test('two models with same id and orderRequestNumber are equal', () {
        final model1 = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '거래처1',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 1,
          orderedItems: [
            OrderedItemModel(
              productCode: '01101123',
              productName: '갈릭 아이올리소스 240g',
              totalQuantityBoxes: 5.0,
              totalQuantityPieces: 100,
              isCancelled: false,
            ),
          ],
        );

        final model2 = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 2, // different clientId
          clientName: '거래처2', // different clientName
          orderDate: '2026-02-09', // different date
          deliveryDate: '2026-02-10',
          totalAmount: 50000, // different amount
          approvalStatus: 'PENDING',
          isClosed: false,
          orderedItemCount: 0,
          orderedItems: [],
        );

        expect(model1, equals(model2));
        expect(model1.hashCode, equals(model2.hashCode));
      });

      test('models with different id are not equal', () {
        final model1 = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '거래처1',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 0,
          orderedItems: [],
        );

        final model2 = OrderDetailModel(
          id: 2,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '거래처1',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 0,
          orderedItems: [],
        );

        expect(model1, isNot(equals(model2)));
      });

      test('models with different orderRequestNumber are not equal', () {
        final model1 = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000001',
          clientId: 1,
          clientName: '거래처1',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 0,
          orderedItems: [],
        );

        final model2 = OrderDetailModel(
          id: 1,
          orderRequestNumber: 'OP00000002',
          clientId: 1,
          clientName: '거래처1',
          orderDate: '2026-02-10',
          deliveryDate: '2026-02-11',
          totalAmount: 100000,
          approvalStatus: 'APPROVED',
          isClosed: true,
          orderedItemCount: 0,
          orderedItems: [],
        );

        expect(model1, isNot(equals(model2)));
      });
    });
  });
}
