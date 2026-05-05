import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/client_order_model.dart';
import 'package:mobile/domain/entities/order_detail.dart';

void main() {
  group('ClientOrderItemModel.fromJson', () {
    test('영문 코드 4종 → enum 매핑', () {
      final pending = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '0 BOX',
        'deliveryStatus': 'PENDING',
      });
      expect(pending.toEntity().deliveryStatus, DeliveryStatus.pending);

      final shipping = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '5 BOX',
        'deliveryStatus': 'SHIPPING',
      });
      expect(shipping.toEntity().deliveryStatus, DeliveryStatus.shipping);

      final delivered = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '10 BOX',
        'deliveryStatus': 'DELIVERED',
      });
      expect(delivered.toEntity().deliveryStatus, DeliveryStatus.delivered);

      final outOfStock = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '0 BOX',
        'deliveryStatus': 'OUT_OF_STOCK',
      });
      expect(outOfStock.toEntity().deliveryStatus, DeliveryStatus.outOfStock);
    });

    test('미정의 코드 → pending fallback', () {
      final unknown = ClientOrderItemModel.fromJson({
        'productCode': 'P001',
        'productName': '예시',
        'deliveredQuantity': '0 BOX',
        'deliveryStatus': 'UNKNOWN_CODE',
      });
      expect(unknown.toEntity().deliveryStatus, DeliveryStatus.pending);
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
          DeliveryStatus.delivered);
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
  });

  group('DeliveryStatus.fromCode', () {
    test('영문 4종 + 미정의 → pending fallback', () {
      expect(DeliveryStatus.fromCode('PENDING'), DeliveryStatus.pending);
      expect(DeliveryStatus.fromCode('SHIPPING'), DeliveryStatus.shipping);
      expect(DeliveryStatus.fromCode('DELIVERED'), DeliveryStatus.delivered);
      expect(DeliveryStatus.fromCode('OUT_OF_STOCK'),
          DeliveryStatus.outOfStock);
      expect(DeliveryStatus.fromCode('XXX'), DeliveryStatus.pending);
    });

    test('displayName heroku 권위 한글 4종', () {
      expect(DeliveryStatus.pending.displayName, '대기');
      expect(DeliveryStatus.shipping.displayName, '배송중');
      expect(DeliveryStatus.delivered.displayName, '배송 완료');
      expect(DeliveryStatus.outOfStock.displayName, '결품');
    });
  });
}
