import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_draft_model.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/validation_error.dart';

void main() {
  group('OrderDraftItemModel', () {
    test('should create OrderDraftItemModel from snake_case JSON', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'quantity_boxes': 10.5,
        'quantity_pieces': 5,
        'unit_price': 5000,
        'box_size': 20,
        'total_price': 1051250,
        'is_selected': true,
      };

      // Act
      final model = OrderDraftItemModel.fromJson(json);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.productName, equals('진라면'));
      expect(model.quantityBoxes, equals(10.5));
      expect(model.quantityPieces, equals(5));
      expect(model.unitPrice, equals(5000));
      expect(model.boxSize, equals(20));
      expect(model.totalPrice, equals(1051250));
      expect(model.isSelected, equals(true));
      expect(model.validationError, isNull);
    });

    test('should serialize to snake_case JSON', () {
      // Arrange
      const model = OrderDraftItemModel(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      // Act
      final json = model.toJson();

      // Assert
      expect(json['product_code'], equals('01234567'));
      expect(json['product_name'], equals('진라면'));
      expect(json['quantity_boxes'], equals(10.5));
      expect(json['quantity_pieces'], equals(5));
      expect(json['unit_price'], equals(5000));
      expect(json['box_size'], equals(20));
      expect(json['total_price'], equals(1051250));
      expect(json['is_selected'], equals(true));
      expect(json['validation_error'], isNull);
    });

    test('should convert to OrderDraftItem entity correctly', () {
      // Arrange
      const model = OrderDraftItemModel(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.productCode, equals('01234567'));
      expect(entity.productName, equals('진라면'));
      expect(entity.quantityBoxes, equals(10.5));
      expect(entity.quantityPieces, equals(5));
      expect(entity.unitPrice, equals(5000));
      expect(entity.boxSize, equals(20));
      expect(entity.totalPrice, equals(1051250));
      expect(entity.isSelected, equals(true));
      expect(entity.validationError, isNull);
    });

    test('should create model from OrderDraftItem entity', () {
      // Arrange
      const entity = OrderDraftItem(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      // Act
      final model = OrderDraftItemModel.fromEntity(entity);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.productName, equals('진라면'));
      expect(model.quantityBoxes, equals(10.5));
      expect(model.quantityPieces, equals(5));
      expect(model.unitPrice, equals(5000));
      expect(model.boxSize, equals(20));
      expect(model.totalPrice, equals(1051250));
      expect(model.isSelected, equals(true));
      expect(model.validationError, isNull);
    });

    test('should handle null validation_error in fromJson', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'quantity_boxes': 10.0,
        'quantity_pieces': 0,
        'unit_price': 5000,
        'box_size': 20,
        'total_price': 1000000,
        'is_selected': false,
        'validation_error': null,
      };

      // Act
      final model = OrderDraftItemModel.fromJson(json);

      // Assert
      expect(model.validationError, isNull);
    });

    test('should parse validation_error when present in fromJson', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'quantity_boxes': 5.0,
        'quantity_pieces': 0,
        'unit_price': 5000,
        'box_size': 20,
        'total_price': 500000,
        'is_selected': false,
        'validation_error': {
          'errorType': 'MIN_ORDER_QUANTITY',
          'message': '최소 주문 수량은 10박스입니다',
          'minOrderQuantity': 10,
        },
      };

      // Act
      final model = OrderDraftItemModel.fromJson(json);

      // Assert
      expect(model.validationError, isNotNull);
      expect(model.validationError!.errorType,
          equals(ValidationErrorType.minOrderQuantity));
      expect(model.validationError!.message, equals('최소 주문 수량은 10박스입니다'));
      expect(model.validationError!.minOrderQuantity, equals(10));
    });

    test('should default isSelected to false when not provided in JSON', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'quantity_boxes': 10.0,
        'quantity_pieces': 0,
        'unit_price': 5000,
        'box_size': 20,
        'total_price': 1000000,
      };

      // Act
      final model = OrderDraftItemModel.fromJson(json);

      // Assert
      expect(model.isSelected, equals(false));
    });

    test('should compare OrderDraftItemModels correctly with equality operator',
        () {
      // Arrange
      const model1 = OrderDraftItemModel(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      const model2 = OrderDraftItemModel(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      const model3 = OrderDraftItemModel(
        productCode: '89012345',
        productName: '육개장',
        quantityBoxes: 5.0,
        quantityPieces: 0,
        unitPrice: 6000,
        boxSize: 15,
        totalPrice: 450000,
        isSelected: false,
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });

    test('should generate consistent hashCode for equal OrderDraftItemModels',
        () {
      // Arrange
      const model1 = OrderDraftItemModel(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      const model2 = OrderDraftItemModel(
        productCode: '01234567',
        productName: '진라면',
        quantityBoxes: 10.5,
        quantityPieces: 5,
        unitPrice: 5000,
        boxSize: 20,
        totalPrice: 1051250,
        isSelected: true,
      );

      // Assert
      expect(model1.hashCode, equals(model2.hashCode));
    });
  });

  group('OrderDraftModel', () {
    test('should create OrderDraftModel from snake_case JSON (direct format)',
        () {
      // Arrange
      final json = {
        'id': 1,
        'client_id': 100,
        'client_name': '천사푸드',
        'credit_balance': 50000000,
        'delivery_date': '2026-02-10',
        'items': [
          {
            'product_code': '01234567',
            'product_name': '진라면',
            'quantity_boxes': 10.0,
            'quantity_pieces': 0,
            'unit_price': 5000,
            'box_size': 20,
            'total_price': 1000000,
            'is_selected': true,
          }
        ],
        'total_amount': 1000000,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00',
      };

      // Act
      final model = OrderDraftModel.fromJson(json);

      // Assert
      expect(model.id, equals(1));
      expect(model.clientId, equals(100));
      expect(model.clientName, equals('천사푸드'));
      expect(model.creditBalance, equals(50000000));
      expect(model.deliveryDate, equals('2026-02-10'));
      expect(model.items.length, equals(1));
      expect(model.items[0].productCode, equals('01234567'));
      expect(model.totalAmount, equals(1000000));
      expect(model.isDraft, equals(true));
      expect(model.lastModified, equals('2026-02-10T10:30:00'));
    });

    test('should create OrderDraftModel from API response with data wrapper',
        () {
      // Arrange
      final json = {
        'data': {
          'id': 1,
          'client_id': 100,
          'client_name': '천사푸드',
          'credit_balance': 50000000,
          'delivery_date': '2026-02-10',
          'items': [
            {
              'product_code': '01234567',
              'product_name': '진라면',
              'quantity_boxes': 10.0,
              'quantity_pieces': 0,
              'unit_price': 5000,
              'box_size': 20,
              'total_price': 1000000,
              'is_selected': true,
            }
          ],
          'total_amount': 1000000,
          'is_draft': true,
          'last_modified': '2026-02-10T10:30:00',
        }
      };

      // Act
      final model = OrderDraftModel.fromJson(json);

      // Assert
      expect(model.id, equals(1));
      expect(model.clientId, equals(100));
      expect(model.clientName, equals('천사푸드'));
      expect(model.creditBalance, equals(50000000));
      expect(model.deliveryDate, equals('2026-02-10'));
      expect(model.items.length, equals(1));
      expect(model.totalAmount, equals(1000000));
      expect(model.isDraft, equals(true));
      expect(model.lastModified, equals('2026-02-10T10:30:00'));
    });

    test('should serialize to snake_case JSON', () {
      // Arrange
      final model = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      // Act
      final json = model.toJson();

      // Assert
      expect(json['id'], equals(1));
      expect(json['client_id'], equals(100));
      expect(json['client_name'], equals('천사푸드'));
      expect(json['credit_balance'], equals(50000000));
      expect(json['delivery_date'], equals('2026-02-10'));
      expect(json['items'], isA<List>());
      expect(json['items'].length, equals(1));
      expect(json['items'][0]['product_code'], equals('01234567'));
      expect(json['total_amount'], equals(1000000));
      expect(json['is_draft'], equals(true));
      expect(json['last_modified'], equals('2026-02-10T10:30:00'));
    });

    test('should produce API submit format with toRequestJson', () {
      // Arrange
      final model = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 5,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      // Act
      final json = model.toRequestJson();

      // Assert
      expect(json['client_id'], equals(100));
      expect(json['delivery_date'], equals('2026-02-10'));
      expect(json['items'], isA<List>());
      expect(json['items'].length, equals(1));
      expect(json['items'][0]['product_code'], equals('01234567'));
      expect(json['items'][0]['quantity_boxes'], equals(10.0));
      expect(json['items'][0]['quantity_pieces'], equals(5));
      // Should NOT include UI fields
      expect(json.containsKey('id'), isFalse);
      expect(json.containsKey('client_name'), isFalse);
      expect(json.containsKey('total_amount'), isFalse);
      expect(json.containsKey('is_draft'), isFalse);
      expect(json['items'][0].containsKey('product_name'), isFalse);
      expect(json['items'][0].containsKey('unit_price'), isFalse);
      expect(json['items'][0].containsKey('is_selected'), isFalse);
    });

    test('should convert to OrderDraft entity correctly', () {
      // Arrange
      final model = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.id, equals(1));
      expect(entity.clientId, equals(100));
      expect(entity.clientName, equals('천사푸드'));
      expect(entity.creditBalance, equals(50000000));
      expect(entity.deliveryDate, equals(DateTime(2026, 2, 10)));
      expect(entity.items.length, equals(1));
      expect(entity.items[0].productCode, equals('01234567'));
      expect(entity.totalAmount, equals(1000000));
      expect(entity.isDraft, equals(true));
      expect(entity.lastModified, equals(DateTime(2026, 2, 10, 10, 30, 0)));
    });

    test('should create model from OrderDraft entity', () {
      // Arrange
      final entity = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: DateTime(2026, 2, 10, 14, 30, 45),
        items: const [
          OrderDraftItem(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: DateTime(2026, 2, 10, 10, 30, 0),
      );

      // Act
      final model = OrderDraftModel.fromEntity(entity);

      // Assert
      expect(model.id, equals(1));
      expect(model.clientId, equals(100));
      expect(model.clientName, equals('천사푸드'));
      expect(model.creditBalance, equals(50000000));
      // Should extract date only (YYYY-MM-DD)
      expect(model.deliveryDate, equals('2026-02-10'));
      expect(model.items.length, equals(1));
      expect(model.items[0].productCode, equals('01234567'));
      expect(model.totalAmount, equals(1000000));
      expect(model.isDraft, equals(true));
      expect(model.lastModified, equals('2026-02-10T10:30:00.000'));
    });

    test('should handle null optional fields in fromJson', () {
      // Arrange
      final json = {
        'items': [],
        'total_amount': 0,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00',
      };

      // Act
      final model = OrderDraftModel.fromJson(json);

      // Assert
      expect(model.id, isNull);
      expect(model.clientId, isNull);
      expect(model.clientName, isNull);
      expect(model.creditBalance, isNull);
      expect(model.deliveryDate, isNull);
      expect(model.items, isEmpty);
      expect(model.totalAmount, equals(0));
      expect(model.isDraft, equals(true));
    });

    test('should compare OrderDraftModels correctly with equality operator',
        () {
      // Arrange
      final model1 = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      final model2 = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      final model3 = OrderDraftModel(
        id: 2,
        clientId: 200,
        clientName: '경산식품',
        creditBalance: 30000000,
        deliveryDate: '2026-02-11',
        items: const [],
        totalAmount: 0,
        isDraft: false,
        lastModified: '2026-02-11T09:00:00',
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });

    test('should generate consistent hashCode for equal OrderDraftModels', () {
      // Arrange
      final model1 = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      final model2 = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      // Assert
      expect(model1.hashCode, equals(model2.hashCode));
    });

    test('should generate toString with all fields', () {
      // Arrange
      final model = OrderDraftModel(
        id: 1,
        clientId: 100,
        clientName: '천사푸드',
        creditBalance: 50000000,
        deliveryDate: '2026-02-10',
        items: const [
          OrderDraftItemModel(
            productCode: '01234567',
            productName: '진라면',
            quantityBoxes: 10.0,
            quantityPieces: 0,
            unitPrice: 5000,
            boxSize: 20,
            totalPrice: 1000000,
            isSelected: true,
          ),
        ],
        totalAmount: 1000000,
        isDraft: true,
        lastModified: '2026-02-10T10:30:00',
      );

      // Act
      final str = model.toString();

      // Assert
      expect(str, contains('OrderDraftModel('));
      expect(str, contains('id: 1'));
      expect(str, contains('clientId: 100'));
      expect(str, contains('clientName: 천사푸드'));
      expect(str, contains('deliveryDate: 2026-02-10'));
      expect(str, contains('items: 1'));
      expect(str, contains('totalAmount: 1000000'));
      expect(str, contains('isDraft: true'));
      expect(str, contains('lastModified: 2026-02-10T10:30:00'));
    });
  });
}
