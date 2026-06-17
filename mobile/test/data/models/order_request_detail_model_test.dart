import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_request_detail_model.dart';

void main() {
  group('OrderRequestDetailModel.fromJson - orderProcessingStatusList (Spec #595 P2-M)', () {
    Map<String, dynamic> baseData({
      List<dynamic>? processingList,
    }) {
      return {
        'data': {
          'id': 12345,
          'orderRequestNumber': 'OR-0001234',
          'clientId': 5678,
          'clientName': '홍길동상회',
          'clientDeadlineTime': '13:50',
          'orderDate': '2026-05-04T10:00:00',
          'deliveryDate': '2026-05-06',
          'totalAmount': 1234567,
          'totalApprovedAmount': 1200000,
          'orderRequestStatus': 'APPROVED',
          'isClosed': true,
          'orderedItemCount': 1,
          'orderedItems': <dynamic>[
            {
              'orderProductId': 101,
              'productCode': '1000023',
              'productName': '진라면 매운맛',
              'totalQuantityBoxes': 10.0,
              'totalQuantityPieces': 300,
              'isCancelled': false,
            },
          ],
          'orderProcessingStatusList': processingList,
          'rejectedItems': null,
        },
      };
    }

    Map<String, dynamic> processingItem({
      String productCode = '1000023',
      String? driverName,
      String? vehicle,
      String? driverPhone,
      String? scheduleTime,
      String? completeTime,
    }) {
      return {
        'productCode': productCode,
        'productName': '진라면 매운맛',
        'deliveredQuantity': '10 BOX (300 EA)',
        'deliveryStatus': 'DELIVERED',
        'driverName': driverName,
        'vehicle': vehicle,
        'driverPhone': driverPhone,
        'scheduleTime': scheduleTime,
        'completeTime': completeTime,
      };
    }

    test('MT1 — null 응답 → orderProcessingStatusList == null', () {
      final model = OrderRequestDetailModel.fromJson(baseData(processingList: null));
      expect(model.orderProcessingStatusList, isNull);
    });

    test('MT2 — 길이 1 배열 → 단일 그룹 매핑', () {
      final json = baseData(processingList: [
        {
          'sapOrderNumber': '0300004993',
          'items': [processingItem()],
        },
      ]);
      final model = OrderRequestDetailModel.fromJson(json);
      expect(model.orderProcessingStatusList!.length, 1);
      expect(model.orderProcessingStatusList![0].sapOrderNumber, '0300004993');
    });

    test('MT3 — 길이 2 배열 → 다중 그룹, 응답 순서 유지', () {
      final json = baseData(processingList: [
        {
          'sapOrderNumber': 'AAA',
          'items': [processingItem(productCode: 'P_A1')],
        },
        {
          'sapOrderNumber': 'BBB',
          'items': [processingItem(productCode: 'P_B1')],
        },
      ]);
      final model = OrderRequestDetailModel.fromJson(json);
      expect(model.orderProcessingStatusList!.length, 2);
      expect(model.orderProcessingStatusList![0].sapOrderNumber, 'AAA');
      expect(model.orderProcessingStatusList![1].sapOrderNumber, 'BBB');
    });

    test('MT4 — toEntity → OrderProcessingStatus 그룹 N개 생성', () {
      final json = baseData(processingList: [
        {
          'sapOrderNumber': 'AAA',
          'items': [processingItem(productCode: 'P_A1')],
        },
        {
          'sapOrderNumber': 'BBB',
          'items': [processingItem(productCode: 'P_B1')],
        },
      ]);
      final entity = OrderRequestDetailModel.fromJson(json).toEntity();
      expect(entity.orderProcessingStatusList!.length, 2);
      expect(entity.orderProcessingStatusList![0].sapOrderNumber, 'AAA');
      expect(entity.orderProcessingStatusList![0].items[0].productCode, 'P_A1');
    });

    test('MT5 — copyWith 로 orderProcessingStatusList 교체 — 새 인스턴스', () {
      final json = baseData(processingList: [
        {
          'sapOrderNumber': 'AAA',
          'items': [processingItem()],
        },
      ]);
      final entity = OrderRequestDetailModel.fromJson(json).toEntity();
      final updated = entity.copyWith(orderProcessingStatusList: []);
      expect(updated.orderProcessingStatusList, isEmpty);
      expect(entity.orderProcessingStatusList!.length, 1); // 원본 불변
    });

    test('MT6 — 차량/기사 5필드 매핑 (Q5) + null 매핑', () {
      final json = baseData(processingList: [
        {
          'sapOrderNumber': 'AAA',
          'items': [
            processingItem(
              driverName: '홍길동',
              vehicle: '12가3456',
              driverPhone: '010-1234-5678',
              scheduleTime: '12:00',
              completeTime: '14:30',
            ),
            processingItem(productCode: 'P2'), // 5필드 모두 null
          ],
        },
      ]);
      final entity = OrderRequestDetailModel.fromJson(json).toEntity();
      final items = entity.orderProcessingStatusList![0].items;
      expect(items[0].driverName, '홍길동');
      expect(items[0].vehicle, '12가3456');
      expect(items[0].driverPhone, '010-1234-5678');
      expect(items[0].scheduleTime, '12:00');
      expect(items[0].completeTime, '14:30');
      expect(items[1].driverName, isNull);
      expect(items[1].vehicle, isNull);
      expect(items[1].driverPhone, isNull);
      expect(items[1].scheduleTime, isNull);
      expect(items[1].completeTime, isNull);
    });

    test('MT7 — 마감 전 응답 (orderProcessingStatusList = null, Q6)', () {
      final json = baseData(processingList: null);
      json['data']['isClosed'] = false;
      final entity = OrderRequestDetailModel.fromJson(json).toEntity();
      expect(entity.isClosed, isFalse);
      expect(entity.orderProcessingStatusList, isNull);
    });
  });
}
