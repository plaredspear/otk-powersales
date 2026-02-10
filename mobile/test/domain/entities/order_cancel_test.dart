import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_cancel.dart';

void main() {
  group('OrderCancelRequest', () {
    test('올바르게 생성된다', () {
      const request = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );

      expect(request.orderId, 1);
      expect(request.productCodes, ['01101123', '01101222']);
      expect(request.productCodes.length, 2);
    });

    test('빈 productCodes로 생성 가능하다', () {
      const request = OrderCancelRequest(
        orderId: 1,
        productCodes: [],
      );

      expect(request.orderId, 1);
      expect(request.productCodes, isEmpty);
    });

    test('copyWith이 올바르게 동작한다', () {
      const original = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123'],
      );

      final copied = original.copyWith(
        orderId: 2,
        productCodes: ['01101222', '01101333'],
      );

      expect(copied.orderId, 2);
      expect(copied.productCodes, ['01101222', '01101333']);

      // 원본은 변경되지 않음
      expect(original.orderId, 1);
      expect(original.productCodes, ['01101123']);
    });

    test('copyWith으로 일부 필드만 변경 가능하다', () {
      const original = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123'],
      );

      final copied = original.copyWith(orderId: 99);

      expect(copied.orderId, 99);
      expect(copied.productCodes, ['01101123']);
    });

    test('toJson이 올바르게 동작한다', () {
      const request = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );

      final json = request.toJson();

      expect(json['orderId'], 1);
      expect(json['productCodes'], ['01101123', '01101222']);
    });

    test('fromJson이 올바르게 동작한다', () {
      final json = {
        'orderId': 1,
        'productCodes': ['01101123', '01101222'],
      };

      final request = OrderCancelRequest.fromJson(json);

      expect(request.orderId, 1);
      expect(request.productCodes, ['01101123', '01101222']);
    });

    test('toJson과 fromJson이 대칭적으로 동작한다', () {
      const original = OrderCancelRequest(
        orderId: 5,
        productCodes: ['01101123', '01101222', '01101333'],
      );

      final json = original.toJson();
      final restored = OrderCancelRequest.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 인스턴스는 동일하다 (equality)', () {
      const request1 = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );
      const request2 = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );

      expect(request1, equals(request2));
      expect(request1.hashCode, equals(request2.hashCode));
    });

    test('다른 값을 가진 인스턴스는 동일하지 않다', () {
      const request1 = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123'],
      );
      const request2 = OrderCancelRequest(
        orderId: 2,
        productCodes: ['01101123'],
      );

      expect(request1, isNot(equals(request2)));
    });

    test('productCodes 순서가 다르면 동일하지 않다', () {
      const request1 = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );
      const request2 = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101222', '01101123'],
      );

      expect(request1, isNot(equals(request2)));
    });

    test('toString이 올바른 형식을 반환한다', () {
      const request = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123'],
      );

      expect(request.toString(), contains('OrderCancelRequest'));
      expect(request.toString(), contains('orderId: 1'));
      expect(request.toString(), contains('01101123'));
    });
  });

  group('OrderCancelResult', () {
    test('올바르게 생성된다', () {
      const result = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      expect(result.cancelledCount, 2);
      expect(result.cancelledProductCodes, ['01101123', '01101222']);
    });

    test('copyWith이 올바르게 동작한다', () {
      const original = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      final copied = original.copyWith(
        cancelledCount: 3,
        cancelledProductCodes: ['01101123', '01101222', '01101333'],
      );

      expect(copied.cancelledCount, 3);
      expect(copied.cancelledProductCodes.length, 3);

      // 원본 변경 없음
      expect(original.cancelledCount, 2);
      expect(original.cancelledProductCodes.length, 2);
    });

    test('copyWith으로 일부 필드만 변경 가능하다', () {
      const original = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      final copied = original.copyWith(cancelledCount: 5);

      expect(copied.cancelledCount, 5);
      expect(copied.cancelledProductCodes, ['01101123', '01101222']);
    });

    test('toJson이 올바르게 동작한다', () {
      const result = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      final json = result.toJson();

      expect(json['cancelledCount'], 2);
      expect(json['cancelledProductCodes'], ['01101123', '01101222']);
    });

    test('fromJson이 올바르게 동작한다', () {
      final json = {
        'cancelledCount': 2,
        'cancelledProductCodes': ['01101123', '01101222'],
      };

      final result = OrderCancelResult.fromJson(json);

      expect(result.cancelledCount, 2);
      expect(result.cancelledProductCodes, ['01101123', '01101222']);
    });

    test('toJson과 fromJson이 대칭적으로 동작한다', () {
      const original = OrderCancelResult(
        cancelledCount: 3,
        cancelledProductCodes: ['01101123', '01101222', '01101333'],
      );

      final json = original.toJson();
      final restored = OrderCancelResult.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 인스턴스는 동일하다 (equality)', () {
      const result1 = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );
      const result2 = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      expect(result1, equals(result2));
      expect(result1.hashCode, equals(result2.hashCode));
    });

    test('다른 값을 가진 인스턴스는 동일하지 않다', () {
      const result1 = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );
      const result2 = OrderCancelResult(
        cancelledCount: 1,
        cancelledProductCodes: ['01101123'],
      );

      expect(result1, isNot(equals(result2)));
    });

    test('toString이 올바른 형식을 반환한다', () {
      const result = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      expect(result.toString(), contains('OrderCancelResult'));
      expect(result.toString(), contains('cancelledCount: 2'));
    });
  });
}
