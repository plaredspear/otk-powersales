import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_cancel_model.dart';
import 'package:mobile/domain/entities/order_cancel.dart';

void main() {
  group('OrderCancelRequestModel', () {
    test('productCodes를 올바르게 생성한다', () {
      const model = OrderCancelRequestModel(
        productCodes: ['01101123', '01101222'],
      );

      expect(model.productCodes, ['01101123', '01101222']);
    });

    test('Entity에서 변환할 수 있다', () {
      const entity = OrderCancelRequest(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );

      final model = OrderCancelRequestModel.fromEntity(entity);

      expect(model.productCodes, ['01101123', '01101222']);
    });

    test('toJson이 올바른 형식을 반환한다', () {
      const model = OrderCancelRequestModel(
        productCodes: ['01101123', '01101222'],
      );

      final json = model.toJson();

      expect(json, {
        'productCodes': ['01101123', '01101222'],
      });
    });

    test('빈 productCodes도 처리한다', () {
      const model = OrderCancelRequestModel(productCodes: []);

      final json = model.toJson();
      expect(json['productCodes'], isEmpty);
    });
  });

  group('OrderCancelResponseModel', () {
    test('API 응답 JSON에서 올바르게 파싱한다', () {
      final json = {
        'success': true,
        'data': {
          'cancelledCount': 2,
          'cancelledProductCodes': ['01101123', '01101222'],
        },
        'message': '주문이 취소되었습니다',
      };

      final model = OrderCancelResponseModel.fromJson(json);

      expect(model.cancelledCount, 2);
      expect(model.cancelledProductCodes, ['01101123', '01101222']);
    });

    test('단일 제품 취소 응답을 파싱한다', () {
      final json = {
        'success': true,
        'data': {
          'cancelledCount': 1,
          'cancelledProductCodes': ['01101123'],
        },
        'message': '주문이 취소되었습니다',
      };

      final model = OrderCancelResponseModel.fromJson(json);

      expect(model.cancelledCount, 1);
      expect(model.cancelledProductCodes.length, 1);
    });

    test('toEntity가 올바른 도메인 엔티티를 반환한다', () {
      const model = OrderCancelResponseModel(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );

      final entity = model.toEntity();

      expect(entity, isA<OrderCancelResult>());
      expect(entity.cancelledCount, 2);
      expect(entity.cancelledProductCodes, ['01101123', '01101222']);
    });

    test('fromJson → toEntity 변환 체인이 올바르게 동작한다', () {
      final json = {
        'success': true,
        'data': {
          'cancelledCount': 3,
          'cancelledProductCodes': ['01101123', '01101222', '01101333'],
        },
        'message': '주문이 취소되었습니다',
      };

      final model = OrderCancelResponseModel.fromJson(json);
      final entity = model.toEntity();

      expect(entity.cancelledCount, 3);
      expect(entity.cancelledProductCodes.length, 3);
      expect(entity.cancelledProductCodes[0], '01101123');
      expect(entity.cancelledProductCodes[1], '01101222');
      expect(entity.cancelledProductCodes[2], '01101333');
    });
  });
}
