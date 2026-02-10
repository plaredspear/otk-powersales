import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/validation_error.dart';

void main() {
  group('ValidationErrorType', () {
    test('enum 값 테스트', () {
      expect(ValidationErrorType.minOrderQuantity.code, 'MIN_ORDER_QUANTITY');
      expect(ValidationErrorType.minOrderQuantity.displayName, '최소 주문 수량 미달');

      expect(ValidationErrorType.supplyQuantity.code, 'SUPPLY_QUANTITY');
      expect(ValidationErrorType.supplyQuantity.displayName, '공급 수량 부족');

      expect(ValidationErrorType.dcQuantity.code, 'DC_QUANTITY');
      expect(ValidationErrorType.dcQuantity.displayName, 'DC 수량 부족');

      expect(ValidationErrorType.creditExceeded.code, 'CREDIT_EXCEEDED');
      expect(ValidationErrorType.creditExceeded.displayName, '여신 잔액 초과');
    });

    test('fromCode 테스트 - 유효한 코드', () {
      expect(
        ValidationErrorType.fromCode('MIN_ORDER_QUANTITY'),
        ValidationErrorType.minOrderQuantity,
      );
      expect(
        ValidationErrorType.fromCode('SUPPLY_QUANTITY'),
        ValidationErrorType.supplyQuantity,
      );
      expect(
        ValidationErrorType.fromCode('DC_QUANTITY'),
        ValidationErrorType.dcQuantity,
      );
      expect(
        ValidationErrorType.fromCode('CREDIT_EXCEEDED'),
        ValidationErrorType.creditExceeded,
      );
    });

    test('fromCode 테스트 - 무효한 코드는 기본값 반환', () {
      expect(
        ValidationErrorType.fromCode('INVALID_CODE'),
        ValidationErrorType.minOrderQuantity,
      );
    });

    test('toJson 테스트', () {
      expect(ValidationErrorType.minOrderQuantity.toJson(), 'MIN_ORDER_QUANTITY');
      expect(ValidationErrorType.supplyQuantity.toJson(), 'SUPPLY_QUANTITY');
      expect(ValidationErrorType.dcQuantity.toJson(), 'DC_QUANTITY');
      expect(ValidationErrorType.creditExceeded.toJson(), 'CREDIT_EXCEEDED');
    });

    test('fromJson 테스트', () {
      expect(
        ValidationErrorType.fromJson('MIN_ORDER_QUANTITY'),
        ValidationErrorType.minOrderQuantity,
      );
      expect(
        ValidationErrorType.fromJson('SUPPLY_QUANTITY'),
        ValidationErrorType.supplyQuantity,
      );
    });
  });

  group('ValidationError', () {
    test('ValidationError 생성 테스트 - 최소 주문 수량', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량은 10박스입니다',
        minOrderQuantity: 10,
      );

      expect(error.errorType, ValidationErrorType.minOrderQuantity);
      expect(error.message, '최소 주문 수량은 10박스입니다');
      expect(error.minOrderQuantity, 10);
      expect(error.supplyQuantity, null);
      expect(error.dcQuantity, null);
    });

    test('ValidationError 생성 테스트 - 공급 수량', () {
      final error = ValidationError(
        errorType: ValidationErrorType.supplyQuantity,
        message: '공급 가능 수량이 부족합니다',
        supplyQuantity: 50,
      );

      expect(error.errorType, ValidationErrorType.supplyQuantity);
      expect(error.message, '공급 가능 수량이 부족합니다');
      expect(error.minOrderQuantity, null);
      expect(error.supplyQuantity, 50);
      expect(error.dcQuantity, null);
    });

    test('ValidationError 생성 테스트 - DC 수량', () {
      final error = ValidationError(
        errorType: ValidationErrorType.dcQuantity,
        message: 'DC 재고가 부족합니다',
        dcQuantity: 30,
      );

      expect(error.errorType, ValidationErrorType.dcQuantity);
      expect(error.message, 'DC 재고가 부족합니다');
      expect(error.minOrderQuantity, null);
      expect(error.supplyQuantity, null);
      expect(error.dcQuantity, 30);
    });

    test('ValidationError 생성 테스트 - 여신 초과', () {
      final error = ValidationError(
        errorType: ValidationErrorType.creditExceeded,
        message: '여신 잔액을 초과하였습니다',
      );

      expect(error.errorType, ValidationErrorType.creditExceeded);
      expect(error.message, '여신 잔액을 초과하였습니다');
      expect(error.minOrderQuantity, null);
      expect(error.supplyQuantity, null);
      expect(error.dcQuantity, null);
    });

    test('copyWith 테스트', () {
      final original = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      final copied = original.copyWith(
        message: '수정된 메시지',
        minOrderQuantity: 20,
      );

      expect(copied.errorType, ValidationErrorType.minOrderQuantity);
      expect(copied.message, '수정된 메시지');
      expect(copied.minOrderQuantity, 20);
    });

    test('toJson 테스트', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
        supplyQuantity: 50,
        dcQuantity: 30,
      );

      final json = error.toJson();

      expect(json['errorType'], 'MIN_ORDER_QUANTITY');
      expect(json['message'], '최소 주문 수량 미달');
      expect(json['minOrderQuantity'], 10);
      expect(json['supplyQuantity'], 50);
      expect(json['dcQuantity'], 30);
    });

    test('fromJson 테스트', () {
      final json = {
        'errorType': 'SUPPLY_QUANTITY',
        'message': '공급 수량 부족',
        'minOrderQuantity': null,
        'supplyQuantity': 100,
        'dcQuantity': null,
      };

      final error = ValidationError.fromJson(json);

      expect(error.errorType, ValidationErrorType.supplyQuantity);
      expect(error.message, '공급 수량 부족');
      expect(error.minOrderQuantity, null);
      expect(error.supplyQuantity, 100);
      expect(error.dcQuantity, null);
    });

    test('toJson/fromJson 왕복 변환 테스트', () {
      final original = ValidationError(
        errorType: ValidationErrorType.dcQuantity,
        message: 'DC 수량 부족',
        dcQuantity: 25,
      );

      final json = original.toJson();
      final restored = ValidationError.fromJson(json);

      expect(restored, original);
    });

    test('equality 테스트 - 동일한 객체', () {
      final error1 = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      final error2 = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      expect(error1, error2);
      expect(error1.hashCode, error2.hashCode);
    });

    test('equality 테스트 - 다른 객체', () {
      final error1 = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      final error2 = ValidationError(
        errorType: ValidationErrorType.supplyQuantity,
        message: '공급 수량 부족',
        supplyQuantity: 50,
      );

      expect(error1, isNot(error2));
    });

    test('toString 테스트', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      final str = error.toString();
      expect(str, contains('ValidationError'));
      expect(str, contains('minOrderQuantity'));
      expect(str, contains('10'));
    });
  });

  group('ValidationResult', () {
    test('ValidationResult 생성 테스트 - 유효한 경우', () {
      final result = ValidationResult(
        isValid: true,
        errors: {},
      );

      expect(result.isValid, true);
      expect(result.errors, isEmpty);
      expect(result.hasErrors, false);
    });

    test('ValidationResult 생성 테스트 - 에러가 있는 경우', () {
      final error1 = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      final error2 = ValidationError(
        errorType: ValidationErrorType.supplyQuantity,
        message: '공급 수량 부족',
        supplyQuantity: 50,
      );

      final result = ValidationResult(
        isValid: false,
        errors: {
          'P001': error1,
          'P002': error2,
        },
      );

      expect(result.isValid, false);
      expect(result.errors.length, 2);
      expect(result.hasErrors, true);
    });

    test('hasErrors getter 테스트 - 에러 없음', () {
      final result = ValidationResult(
        isValid: true,
        errors: {},
      );

      expect(result.hasErrors, false);
    });

    test('hasErrors getter 테스트 - 에러 있음', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
      );

      final result = ValidationResult(
        isValid: false,
        errors: {'P001': error},
      );

      expect(result.hasErrors, true);
    });

    test('getError 테스트 - 존재하는 제품 코드', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
        minOrderQuantity: 10,
      );

      final result = ValidationResult(
        isValid: false,
        errors: {'P001': error},
      );

      expect(result.getError('P001'), error);
    });

    test('getError 테스트 - 존재하지 않는 제품 코드', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
      );

      final result = ValidationResult(
        isValid: false,
        errors: {'P001': error},
      );

      expect(result.getError('P999'), null);
    });

    test('equality 테스트 - 동일한 객체', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
      );

      final result1 = ValidationResult(
        isValid: false,
        errors: {'P001': error},
      );

      final result2 = ValidationResult(
        isValid: false,
        errors: {'P001': error},
      );

      expect(result1, result2);
      expect(result1.hashCode, result2.hashCode);
    });

    test('equality 테스트 - 다른 객체', () {
      final error1 = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
      );

      final error2 = ValidationError(
        errorType: ValidationErrorType.supplyQuantity,
        message: '공급 수량 부족',
      );

      final result1 = ValidationResult(
        isValid: false,
        errors: {'P001': error1},
      );

      final result2 = ValidationResult(
        isValid: false,
        errors: {'P001': error2},
      );

      expect(result1, isNot(result2));
    });

    test('toString 테스트', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 주문 수량 미달',
      );

      final result = ValidationResult(
        isValid: false,
        errors: {'P001': error, 'P002': error},
      );

      final str = result.toString();
      expect(str, contains('ValidationResult'));
      expect(str, contains('isValid: false'));
      expect(str, contains('errors: 2'));
    });
  });

  group('OrderSubmitResult', () {
    test('OrderSubmitResult 생성 테스트', () {
      final result = OrderSubmitResult(
        orderId: 12345,
        orderRequestNumber: 'ORD-2024-001',
        status: 'PENDING',
      );

      expect(result.orderId, 12345);
      expect(result.orderRequestNumber, 'ORD-2024-001');
      expect(result.status, 'PENDING');
    });

    test('toJson 테스트', () {
      final result = OrderSubmitResult(
        orderId: 12345,
        orderRequestNumber: 'ORD-2024-001',
        status: 'CONFIRMED',
      );

      final json = result.toJson();

      expect(json['orderId'], 12345);
      expect(json['orderRequestNumber'], 'ORD-2024-001');
      expect(json['status'], 'CONFIRMED');
    });

    test('fromJson 테스트', () {
      final json = {
        'orderId': 67890,
        'orderRequestNumber': 'ORD-2024-002',
        'status': 'SHIPPED',
      };

      final result = OrderSubmitResult.fromJson(json);

      expect(result.orderId, 67890);
      expect(result.orderRequestNumber, 'ORD-2024-002');
      expect(result.status, 'SHIPPED');
    });

    test('toJson/fromJson 왕복 변환 테스트', () {
      final original = OrderSubmitResult(
        orderId: 11111,
        orderRequestNumber: 'ORD-2024-003',
        status: 'DELIVERED',
      );

      final json = original.toJson();
      final restored = OrderSubmitResult.fromJson(json);

      expect(restored, original);
    });

    test('equality 테스트 - 동일한 객체', () {
      final result1 = OrderSubmitResult(
        orderId: 12345,
        orderRequestNumber: 'ORD-2024-001',
        status: 'PENDING',
      );

      final result2 = OrderSubmitResult(
        orderId: 12345,
        orderRequestNumber: 'ORD-2024-001',
        status: 'PENDING',
      );

      expect(result1, result2);
      expect(result1.hashCode, result2.hashCode);
    });

    test('equality 테스트 - 다른 객체', () {
      final result1 = OrderSubmitResult(
        orderId: 12345,
        orderRequestNumber: 'ORD-2024-001',
        status: 'PENDING',
      );

      final result2 = OrderSubmitResult(
        orderId: 67890,
        orderRequestNumber: 'ORD-2024-002',
        status: 'CONFIRMED',
      );

      expect(result1, isNot(result2));
    });

    test('toString 테스트', () {
      final result = OrderSubmitResult(
        orderId: 12345,
        orderRequestNumber: 'ORD-2024-001',
        status: 'PENDING',
      );

      final str = result.toString();
      expect(str, contains('OrderSubmitResult'));
      expect(str, contains('12345'));
      expect(str, contains('ORD-2024-001'));
      expect(str, contains('PENDING'));
    });
  });
}
