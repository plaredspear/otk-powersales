import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/client_order_model.dart';
import 'package:mobile/domain/entities/order_detail.dart';

void main() {
  group('ClientOrderItemModel.fromJson', () {
    test('영문 코드 4종 → 문자열 그대로 보유', () {
      final pending = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '0 BOX',
        'deliveryStatus': 'PENDING',
      });
      expect(pending.toEntity().deliveryStatus, OrderDeliveryStatus.pending);

      final shipping = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '5 BOX',
        'deliveryStatus': 'SHIPPING',
      });
      expect(shipping.toEntity().deliveryStatus, OrderDeliveryStatus.shipping);

      final delivered = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '10 BOX',
        'deliveryStatus': 'DELIVERED',
      });
      expect(
          delivered.toEntity().deliveryStatus, OrderDeliveryStatus.delivered);

      final outOfStock = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '0 BOX',
        'deliveryStatus': 'OUT_OF_STOCK',
      });
      expect(
          outOfStock.toEntity().deliveryStatus, OrderDeliveryStatus.outOfStock);
    });

    test('미정의 코드 → 문자열 그대로 보유 (crash 없이 안전)', () {
      // enum fromCode fallback 제거 — 서버 미정의 코드도 그대로 보유해 파싱 예외를 원천 차단.
      final unknown = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '0 BOX',
        'deliveryStatus': 'UNKNOWN_CODE',
      });
      expect(unknown.toEntity().deliveryStatus, 'UNKNOWN_CODE');
      // 표시명은 미정의 코드에 대해 빈 문자열(crash 대신).
      expect(OrderDeliveryStatus.displayName('UNKNOWN_CODE'), '');
    });
  });

  group('ClientOrderDetailModel.fromJson', () {
    test('전체 필드 매핑 — sapAccountCode/Name 포함', () {
      final json = {
        'data': {
          'sapOrderNumber': '0300011396',
          'sapAccountCode': '0001234567',
          'sapAccountName': '홍길동마트',
          'clientDeadlineTime': '13:50',
          'orderDate': '2026-05-04',
          'deliveryDate': '2026-05-06',
          'totalApprovedAmount': 1250000,
          'orderedItemCount': 1,
          'orderedItems': [
            {
              'productCode': 'P001',
              'productName': '예시 상품',
              'deliveredQuantity': '10 BOX',
              'deliveryStatus': 'DELIVERED',
            }
          ],
        }
      };

      final entity = ClientOrderDetailModel.fromJson(json).toEntity();

      expect(entity.sapOrderNumber, '0300011396');
      expect(entity.sapAccountCode, '0001234567');
      expect(entity.sapAccountName, '홍길동마트');
      expect(entity.clientDeadlineTime, '13:50');
      expect(entity.orderDate, DateTime(2026, 5, 4));
      expect(entity.deliveryDate, DateTime(2026, 5, 6));
      expect(entity.totalApprovedAmount, 1250000);
      expect(entity.orderedItemCount, 1);
      expect(entity.orderedItems.first.deliveryStatus,
          OrderDeliveryStatus.delivered);
    });

    test('nullable 필드 누락 허용', () {
      final json = {
        'sapOrderNumber': '0300011396',
        'sapAccountCode': null,
        'sapAccountName': null,
        'clientDeadlineTime': null,
        'orderDate': null,
        'deliveryDate': null,
        'totalApprovedAmount': null,
        'orderedItemCount': 0,
        'orderedItems': [],
      };

      final entity = ClientOrderDetailModel.fromJson(json).toEntity();

      expect(entity.sapAccountCode, isNull);
      expect(entity.orderDate, isNull);
      expect(entity.totalApprovedAmount, isNull);
      expect(entity.orderedItems, isEmpty);
    });

    test('relatedOrders 매핑 — 취소 후속 주문 요약(제품표 없음)', () {
      final json = {
        'data': {
          'sapOrderNumber': '330884720',
          'orderedItemCount': 0,
          'orderedItems': [],
          'relatedOrders': [
            {
              'sapOrderNumber': '604311314',
              'orderTypeCode': 'ZRE1',
              'orderTypeName': 'Standard Cancel',
              'orderDate': '2026-07-21',
              'deliveryDate': '2026-07-22',
              'totalApprovedAmount': 84000,
              'ordererName': '황동영',
              'ordererCode': '20180073',
            }
          ],
        }
      };

      final entity = ClientOrderDetailModel.fromJson(json).toEntity();

      expect(entity.relatedOrders, hasLength(1));
      final related = entity.relatedOrders.first;
      expect(related.sapOrderNumber, '604311314');
      expect(related.orderTypeCode, 'ZRE1');
      expect(related.orderTypeName, 'Standard Cancel');
      expect(related.orderDate, DateTime(2026, 7, 21));
      expect(related.deliveryDate, DateTime(2026, 7, 22));
      expect(related.totalApprovedAmount, 84000);
      expect(related.ordererName, '황동영');
      expect(related.ordererCode, '20180073');
    });

    test('relatedOrders 누락 시 빈 배열', () {
      final json = {
        'sapOrderNumber': '330884720',
        'orderedItemCount': 0,
        'orderedItems': [],
      };

      final entity = ClientOrderDetailModel.fromJson(json).toEntity();

      expect(entity.relatedOrders, isEmpty);
    });
  });

  group('OrderDeliveryStatus.displayName', () {
    test('영문 4종 → heroku 권위 한글', () {
      expect(OrderDeliveryStatus.displayName('PENDING'), '대기');
      expect(OrderDeliveryStatus.displayName('SHIPPING'), '배송중');
      expect(OrderDeliveryStatus.displayName('DELIVERED'), '배송 완료');
      expect(OrderDeliveryStatus.displayName('OUT_OF_STOCK'), '결품');
    });

    test('빈상태(UNKNOWN)/미정의/null → 빈 문자열 (crash 대신)', () {
      expect(OrderDeliveryStatus.displayName('UNKNOWN'), '');
      expect(OrderDeliveryStatus.displayName('XXX'), '');
      expect(OrderDeliveryStatus.displayName(null), '');
    });
  });
}
