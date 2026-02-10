import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/validation_result_model.dart';
import 'package:mobile/domain/entities/validation_error.dart';

void main() {
  group('ValidationErrorModel', () {
    test('should create ValidationErrorModel from snake_case JSON', () {
      // Arrange
      final json = {
        'error_type': 'MIN_ORDER_QUANTITY',
        'message': '최소 주문 수량은 10박스입니다',
        'min_order_quantity': 10,
        'supply_quantity': null,
        'dc_quantity': null,
      };

      // Act
      final model = ValidationErrorModel.fromJson(json);

      // Assert
      expect(model.errorType, equals('MIN_ORDER_QUANTITY'));
      expect(model.message, equals('최소 주문 수량은 10박스입니다'));
      expect(model.minOrderQuantity, equals(10));
      expect(model.supplyQuantity, isNull);
      expect(model.dcQuantity, isNull);
    });

    test('should serialize to snake_case JSON', () {
      // Arrange
      const model = ValidationErrorModel(
        errorType: 'MIN_ORDER_QUANTITY',
        message: '최소 주문 수량은 10박스입니다',
        minOrderQuantity: 10,
        supplyQuantity: null,
        dcQuantity: null,
      );

      // Act
      final json = model.toJson();

      // Assert
      expect(json['error_type'], equals('MIN_ORDER_QUANTITY'));
      expect(json['message'], equals('최소 주문 수량은 10박스입니다'));
      expect(json['min_order_quantity'], equals(10));
      expect(json['supply_quantity'], isNull);
      expect(json['dc_quantity'], isNull);
    });

    test('should convert to ValidationError entity correctly', () {
      // Arrange
      const model = ValidationErrorModel(
        errorType: 'MIN_ORDER_QUANTITY',
        message: '최소 주문 수량은 10박스입니다',
        minOrderQuantity: 10,
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.errorType, equals(ValidationErrorType.minOrderQuantity));
      expect(entity.message, equals('최소 주문 수량은 10박스입니다'));
      expect(entity.minOrderQuantity, equals(10));
      expect(entity.supplyQuantity, isNull);
      expect(entity.dcQuantity, isNull);
    });

    test('should handle all error types correctly', () {
      // Test all error type conversions
      const testCases = [
        ('MIN_ORDER_QUANTITY', ValidationErrorType.minOrderQuantity),
        ('SUPPLY_QUANTITY', ValidationErrorType.supplyQuantity),
        ('DC_QUANTITY', ValidationErrorType.dcQuantity),
        ('CREDIT_EXCEEDED', ValidationErrorType.creditExceeded),
      ];

      for (final (errorCode, expectedType) in testCases) {
        final model = ValidationErrorModel(
          errorType: errorCode,
          message: '에러 메시지',
        );

        final entity = model.toEntity();

        expect(entity.errorType, equals(expectedType),
            reason: 'Failed for error type: $errorCode');
      }
    });

    test('should compare ValidationErrorModels correctly with equality operator',
        () {
      // Arrange
      const model1 = ValidationErrorModel(
        errorType: 'MIN_ORDER_QUANTITY',
        message: '최소 주문 수량은 10박스입니다',
        minOrderQuantity: 10,
      );

      const model2 = ValidationErrorModel(
        errorType: 'MIN_ORDER_QUANTITY',
        message: '최소 주문 수량은 10박스입니다',
        minOrderQuantity: 10,
      );

      const model3 = ValidationErrorModel(
        errorType: 'SUPPLY_QUANTITY',
        message: '공급 수량이 부족합니다',
        supplyQuantity: 50,
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });
  });

  group('ValidationErrorWithProductModel', () {
    test('should create from JSON with product_code and error fields combined',
        () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'error_type': 'MIN_ORDER_QUANTITY',
        'message': '최소 주문 수량은 10박스입니다',
        'min_order_quantity': 10,
      };

      // Act
      final model = ValidationErrorWithProductModel.fromJson(json);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.error.errorType, equals('MIN_ORDER_QUANTITY'));
      expect(model.error.message, equals('최소 주문 수량은 10박스입니다'));
      expect(model.error.minOrderQuantity, equals(10));
    });

    test('should handle all optional error fields', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'error_type': 'SUPPLY_QUANTITY',
        'message': '공급 수량이 부족합니다',
        'min_order_quantity': null,
        'supply_quantity': 50,
        'dc_quantity': 30,
      };

      // Act
      final model = ValidationErrorWithProductModel.fromJson(json);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.error.errorType, equals('SUPPLY_QUANTITY'));
      expect(model.error.message, equals('공급 수량이 부족합니다'));
      expect(model.error.minOrderQuantity, isNull);
      expect(model.error.supplyQuantity, equals(50));
      expect(model.error.dcQuantity, equals(30));
    });

    test(
        'should compare ValidationErrorWithProductModels correctly with equality operator',
        () {
      // Arrange
      const model1 = ValidationErrorWithProductModel(
        productCode: '01234567',
        error: ValidationErrorModel(
          errorType: 'MIN_ORDER_QUANTITY',
          message: '최소 주문 수량은 10박스입니다',
          minOrderQuantity: 10,
        ),
      );

      const model2 = ValidationErrorWithProductModel(
        productCode: '01234567',
        error: ValidationErrorModel(
          errorType: 'MIN_ORDER_QUANTITY',
          message: '최소 주문 수량은 10박스입니다',
          minOrderQuantity: 10,
        ),
      );

      const model3 = ValidationErrorWithProductModel(
        productCode: '89012345',
        error: ValidationErrorModel(
          errorType: 'SUPPLY_QUANTITY',
          message: '공급 수량이 부족합니다',
          supplyQuantity: 50,
        ),
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });
  });

  group('ValidationResultModel', () {
    test('should create from JSON with valid=true and no errors', () {
      // Arrange
      final json = {
        'valid': true,
        'errors': [],
      };

      // Act
      final model = ValidationResultModel.fromJson(json);

      // Assert
      expect(model.isValid, equals(true));
      expect(model.errors, isEmpty);
    });

    test('should create from JSON with valid=false and errors list', () {
      // Arrange
      final json = {
        'valid': false,
        'errors': [
          {
            'product_code': '01234567',
            'error_type': 'MIN_ORDER_QUANTITY',
            'message': '최소 주문 수량은 10박스입니다',
            'min_order_quantity': 10,
          },
          {
            'product_code': '89012345',
            'error_type': 'SUPPLY_QUANTITY',
            'message': '공급 수량이 부족합니다',
            'supply_quantity': 50,
          }
        ],
      };

      // Act
      final model = ValidationResultModel.fromJson(json);

      // Assert
      expect(model.isValid, equals(false));
      expect(model.errors.length, equals(2));
      expect(model.errors[0].productCode, equals('01234567'));
      expect(model.errors[0].error.errorType, equals('MIN_ORDER_QUANTITY'));
      expect(model.errors[1].productCode, equals('89012345'));
      expect(model.errors[1].error.errorType, equals('SUPPLY_QUANTITY'));
    });

    test('should create from JSON with data wrapper', () {
      // Arrange
      final json = {
        'data': {
          'valid': false,
          'errors': [
            {
              'product_code': '01234567',
              'error_type': 'MIN_ORDER_QUANTITY',
              'message': '최소 주문 수량은 10박스입니다',
              'min_order_quantity': 10,
            }
          ],
        }
      };

      // Act
      final model = ValidationResultModel.fromJson(json);

      // Assert
      expect(model.isValid, equals(false));
      expect(model.errors.length, equals(1));
      expect(model.errors[0].productCode, equals('01234567'));
    });

    test('should default to valid=true when not provided', () {
      // Arrange
      final json = {
        'errors': [],
      };

      // Act
      final model = ValidationResultModel.fromJson(json);

      // Assert
      expect(model.isValid, equals(true));
    });

    test('should convert to ValidationResult entity with error Map', () {
      // Arrange
      final model = ValidationResultModel(
        isValid: false,
        errors: const [
          ValidationErrorWithProductModel(
            productCode: '01234567',
            error: ValidationErrorModel(
              errorType: 'MIN_ORDER_QUANTITY',
              message: '최소 주문 수량은 10박스입니다',
              minOrderQuantity: 10,
            ),
          ),
          ValidationErrorWithProductModel(
            productCode: '89012345',
            error: ValidationErrorModel(
              errorType: 'SUPPLY_QUANTITY',
              message: '공급 수량이 부족합니다',
              supplyQuantity: 50,
            ),
          ),
        ],
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.isValid, equals(false));
      expect(entity.errors.length, equals(2));
      expect(entity.errors['01234567'], isNotNull);
      expect(entity.errors['01234567']!.errorType,
          equals(ValidationErrorType.minOrderQuantity));
      expect(entity.errors['89012345'], isNotNull);
      expect(entity.errors['89012345']!.errorType,
          equals(ValidationErrorType.supplyQuantity));
    });

    test('should convert to entity with empty error Map when no errors', () {
      // Arrange
      const model = ValidationResultModel(
        isValid: true,
        errors: [],
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.isValid, equals(true));
      expect(entity.errors, isEmpty);
    });

    test(
        'should compare ValidationResultModels correctly with equality operator',
        () {
      // Arrange
      const model1 = ValidationResultModel(
        isValid: false,
        errors: [
          ValidationErrorWithProductModel(
            productCode: '01234567',
            error: ValidationErrorModel(
              errorType: 'MIN_ORDER_QUANTITY',
              message: '최소 주문 수량은 10박스입니다',
              minOrderQuantity: 10,
            ),
          ),
        ],
      );

      const model2 = ValidationResultModel(
        isValid: false,
        errors: [
          ValidationErrorWithProductModel(
            productCode: '01234567',
            error: ValidationErrorModel(
              errorType: 'MIN_ORDER_QUANTITY',
              message: '최소 주문 수량은 10박스입니다',
              minOrderQuantity: 10,
            ),
          ),
        ],
      );

      const model3 = ValidationResultModel(
        isValid: true,
        errors: [],
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });
  });

  group('OrderSubmitResultModel', () {
    test('should create from JSON with data wrapper', () {
      // Arrange
      final json = {
        'data': {
          'order_id': 123,
          'order_request_number': 'OP00000074',
          'status': 'PENDING',
        }
      };

      // Act
      final model = OrderSubmitResultModel.fromJson(json);

      // Assert
      expect(model.orderId, equals(123));
      expect(model.orderRequestNumber, equals('OP00000074'));
      expect(model.status, equals('PENDING'));
    });

    test('should create from JSON without data wrapper', () {
      // Arrange
      final json = {
        'order_id': 123,
        'order_request_number': 'OP00000074',
        'status': 'PENDING',
      };

      // Act
      final model = OrderSubmitResultModel.fromJson(json);

      // Assert
      expect(model.orderId, equals(123));
      expect(model.orderRequestNumber, equals('OP00000074'));
      expect(model.status, equals('PENDING'));
    });

    test('should convert to OrderSubmitResult entity correctly', () {
      // Arrange
      const model = OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000074',
        status: 'PENDING',
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.orderId, equals(123));
      expect(entity.orderRequestNumber, equals('OP00000074'));
      expect(entity.status, equals('PENDING'));
    });

    test('should handle different status values', () {
      // Test different status values
      const testCases = [
        'PENDING',
        'APPROVED',
        'REJECTED',
        'SEND_FAILED',
      ];

      for (final status in testCases) {
        final model = OrderSubmitResultModel(
          orderId: 123,
          orderRequestNumber: 'OP00000074',
          status: status,
        );

        final entity = model.toEntity();

        expect(entity.status, equals(status),
            reason: 'Failed for status: $status');
      }
    });

    test(
        'should compare OrderSubmitResultModels correctly with equality operator',
        () {
      // Arrange
      const model1 = OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000074',
        status: 'PENDING',
      );

      const model2 = OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000074',
        status: 'PENDING',
      );

      const model3 = OrderSubmitResultModel(
        orderId: 124,
        orderRequestNumber: 'OP00000075',
        status: 'APPROVED',
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });

    test('should generate consistent hashCode for equal models', () {
      // Arrange
      const model1 = OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000074',
        status: 'PENDING',
      );

      const model2 = OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000074',
        status: 'PENDING',
      );

      // Assert
      expect(model1.hashCode, equals(model2.hashCode));
    });

    test('should generate toString with all fields', () {
      // Arrange
      const model = OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000074',
        status: 'PENDING',
      );

      // Act
      final str = model.toString();

      // Assert
      expect(str, contains('OrderSubmitResultModel('));
      expect(str, contains('orderId: 123'));
      expect(str, contains('orderRequestNumber: OP00000074'));
      expect(str, contains('status: PENDING'));
    });
  });
}
