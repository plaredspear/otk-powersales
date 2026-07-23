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
          'orderRequestStatusName': '승인완료',
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

  group('OrderedItem 결품 플래그 (레거시 view.jsp:414 동등)', () {
    Map<String, dynamic> orderedItemJson({
      bool isOutOfStock = false,
      String? outOfStockReason,
    }) {
      return {
        'orderProductId': 101,
        'productCode': '1000023',
        'productName': '진라면 매운맛',
        'totalQuantityBoxes': 10.0,
        'totalQuantityPieces': 300,
        'isCancelled': false,
        'isOutOfStock': isOutOfStock,
        'outOfStockReason': outOfStockReason,
      };
    }

    test('결품 제품 — isOutOfStock=true + outOfStockReason 파싱', () {
      final model = OrderedItemModel.fromJson(
        orderedItemJson(isOutOfStock: true, outOfStockReason: '재고부족'),
      );
      expect(model.isOutOfStock, isTrue);
      expect(model.outOfStockReason, '재고부족');
      final entity = model.toEntity();
      expect(entity.isOutOfStock, isTrue);
      expect(entity.outOfStockReason, '재고부족');
    });

    test('정상 제품 — 필드 부재 시 isOutOfStock=false 기본값 (하위호환)', () {
      final json = orderedItemJson();
      json.remove('isOutOfStock');
      json.remove('outOfStockReason');
      final model = OrderedItemModel.fromJson(json);
      expect(model.isOutOfStock, isFalse);
      expect(model.outOfStockReason, isNull);
    });

    test('toJson ↔ fromJson 왕복 — 결품 필드 보존', () {
      final original = OrderedItemModel.fromJson(
        orderedItemJson(isOutOfStock: true, outOfStockReason: '재고부족'),
      );
      final roundTrip = OrderedItemModel.fromJson(original.toJson());
      expect(roundTrip.isOutOfStock, isTrue);
      expect(roundTrip.outOfStockReason, '재고부족');
    });
  });

  group('OrderedItem 취소요청/실제취소 비교 필드 (Spec #845 P2-M)', () {
    // 신규 필드를 모두 담은 완성 JSON. '누락 방어' 테스트는 여기서 key 를 remove 한다.
    Map<String, dynamic> itemJson({
      bool isCancelled = false,
      bool isCancelRequested = false,
      bool isOutOfStock = false,
      String? outOfStockReason,
      bool isCancelledBySap = false,
      String? cancelReason,
    }) {
      return {
        'orderProductId': 101,
        'productCode': '26010007',
        'productName': '콩기름 0.9L',
        'totalQuantityBoxes': 1.0,
        'totalQuantityPieces': 12,
        'isCancelled': isCancelled,
        'isCancelRequested': isCancelRequested,
        'isOutOfStock': isOutOfStock,
        'outOfStockReason': outOfStockReason,
        'isCancelledBySap': isCancelledBySap,
        'cancelReason': cancelReason,
      };
    }

    test('요청+SAP취소 반영 — isCancelRequested/isCancelledBySap/cancelReason 파싱', () {
      final model = OrderedItemModel.fromJson(itemJson(
        isCancelRequested: true,
        isCancelledBySap: true,
        cancelReason: 'S2 [영업] 고객사정에 의한 취소',
      ));
      expect(model.isCancelRequested, isTrue);
      expect(model.isCancelledBySap, isTrue);
      expect(model.cancelReason, 'S2 [영업] 고객사정에 의한 취소');

      final entity = model.toEntity();
      expect(entity.isCancelRequested, isTrue);
      expect(entity.isCancelledBySap, isTrue);
      expect(entity.cancelReason, 'S2 [영업] 고객사정에 의한 취소');
    });

    test('요청만(미반영) — isCancelRequested=true, 나머지 기본값', () {
      final entity =
          OrderedItemModel.fromJson(itemJson(isCancelRequested: true))
              .toEntity();
      expect(entity.isCancelRequested, isTrue);
      expect(entity.isCancelledBySap, isFalse);
      expect(entity.cancelReason, isNull);
    });

    test('파싱 누락 방어 — 신규 필드 없는 JSON → 기본값(false/null), 예외 없음', () {
      final json = itemJson()
        ..remove('isCancelRequested')
        ..remove('isCancelledBySap')
        ..remove('cancelReason');
      final model = OrderedItemModel.fromJson(json);
      expect(model.isCancelRequested, isFalse);
      expect(model.isCancelledBySap, isFalse);
      expect(model.cancelReason, isNull);
    });

    test('toJson ↔ fromJson 왕복 — 신규 필드 보존', () {
      final original = OrderedItemModel.fromJson(itemJson(
        isCancelRequested: true,
        isCancelledBySap: true,
        cancelReason: 'S2 [영업] 고객사정에 의한 취소',
      ));
      final roundTrip = OrderedItemModel.fromJson(original.toJson());
      expect(roundTrip.isCancelRequested, isTrue);
      expect(roundTrip.isCancelledBySap, isTrue);
      expect(roundTrip.cancelReason, 'S2 [영업] 고객사정에 의한 취소');
    });
  });

  group('OrderRequestDetailModel.fromJson - 상태 null (SF nillable NULL row)', () {
    Map<String, dynamic> data({
      dynamic status = 'APPROVED',
      dynamic statusName = '승인완료',
    }) {
      return {
        'data': {
          'id': 1,
          'orderRequestNumber': 'OR-0001234',
          'clientId': 5678,
          'clientName': '홍길동상회',
          'clientDeadlineTime': '13:50',
          'orderDate': '2026-05-04T10:00:00',
          'deliveryDate': '2026-05-06',
          'totalAmount': 1000,
          'totalApprovedAmount': null,
          'orderRequestStatus': status,
          'orderRequestStatusName': statusName,
          'isClosed': false,
          'orderedItemCount': 0,
          'orderedItems': <dynamic>[],
          'orderProcessingStatusList': null,
          'rejectedItems': null,
        },
      };
    }

    test('상태 코드/표시명이 null 이어도 파싱 예외 없이 null 로 매핑 (크래시 방지)', () {
      final model =
          OrderRequestDetailModel.fromJson(data(status: null, statusName: null));
      expect(model.orderRequestStatus, isNull);
      expect(model.orderRequestStatusName, isNull);
      // 엔티티 변환도 예외 없이 통과.
      final entity = model.toEntity();
      expect(entity.orderRequestStatus, isNull);
      expect(entity.orderRequestStatusName, isNull);
    });

    test('상태가 정상 값이면 그대로 매핑', () {
      final model = OrderRequestDetailModel.fromJson(data());
      expect(model.orderRequestStatus, 'APPROVED');
      expect(model.orderRequestStatusName, '승인완료');
    });
  });

  group('RejectedItemModel.fromJson - 소수 박스 (BigDecimal 정합)', () {
    test('정수 박스 → double 로 파싱', () {
      final model = RejectedItemModel.fromJson({
        'productCode': 'P1',
        'productName': '진라면',
        'orderQuantityBoxes': 3,
        'rejectionReason': '재고 부족',
      });
      expect(model.orderQuantityBoxes, 3.0);
      expect(model.toEntity().orderQuantityBoxes, 3.0);
    });

    test('소수 박스 → 절단 없이 double 로 파싱 (기존 int 캐스팅이면 예외)', () {
      final model = RejectedItemModel.fromJson({
        'productCode': 'P1',
        'productName': '진라면',
        'orderQuantityBoxes': 2.5,
        'rejectionReason': '단가 불일치',
      });
      expect(model.orderQuantityBoxes, 2.5);
      expect(model.toEntity().orderQuantityBoxes, 2.5);
    });
  });

  group('UnfulfilledItemModel.fromJson - 미납 제품 (신규 정책, LineItemStatus != OK)', () {
    Map<String, dynamic> detailData({List<dynamic>? unfulfilledItems}) {
      return {
        'data': {
          'id': 12345,
          'orderRequestNumber': 'OR-0001234',
          'clientId': 5678,
          'clientName': '홍길동상회',
          'orderDate': '2026-05-04T10:00:00',
          'deliveryDate': '2026-05-06',
          'totalAmount': 1234567,
          'orderRequestStatus': 'APPROVED',
          'orderRequestStatusName': '승인완료',
          'isClosed': false,
          'orderedItemCount': 0,
          'orderedItems': <dynamic>[],
          'orderProcessingStatusList': null,
          'rejectedItems': null,
          'unfulfilledItems': unfulfilledItems,
        },
      };
    }

    test('unfulfilledItems 배열 → 파싱 + 엔티티 변환 (사유/소수 박스 보존)', () {
      final model = OrderRequestDetailModel.fromJson(detailData(
        unfulfilledItems: [
          {
            'productCode': '1000023',
            'productName': '진라면 매운맛',
            'orderQuantityBoxes': 2.5,
            'reason': '배차 미확정',
          },
        ],
      ));

      expect(model.unfulfilledItems, hasLength(1));
      expect(model.unfulfilledItems![0].reason, '배차 미확정');

      final entity = model.toEntity();
      expect(entity.hasUnfulfilledItems, isTrue);
      expect(entity.unfulfilledItems![0].productCode, '1000023');
      expect(entity.unfulfilledItems![0].orderQuantityBoxes, 2.5);
      expect(entity.unfulfilledItems![0].reason, '배차 미확정');
    });

    test('unfulfilledItems 부재/null → null 매핑 (하위호환, 크래시 없음)', () {
      final model = OrderRequestDetailModel.fromJson(detailData());
      expect(model.unfulfilledItems, isNull);
      expect(model.toEntity().hasUnfulfilledItems, isFalse);
    });
  });

  group('relatedOrders — 역참조 후속 주문 요약 (내 주문 상세)', () {
    Map<String, dynamic> detailData({List<dynamic>? relatedOrders}) {
      return {
        'data': {
          'id': 1,
          'orderRequestNumber': 'OR-0001234',
          'clientId': 1,
          'clientName': '홍길동상회',
          'orderDate': '2026-07-21T10:00:00',
          'deliveryDate': '2026-07-30',
          'totalAmount': 114600,
          'orderRequestStatus': 'APPROVED',
          'orderRequestStatusName': '승인완료',
          'isClosed': false,
          'orderedItemCount': 0,
          'orderedItems': <dynamic>[],
          'rejectedItems': null,
          if (relatedOrders != null) 'relatedOrders': relatedOrders,
        },
      };
    }

    test('취소 후속 주문 요약 파싱 (제품표 없음)', () {
      final entity = OrderRequestDetailModel.fromJson(detailData(relatedOrders: [
        {
          'sapOrderNumber': '604311314',
          'orderTypeCode': 'ZRE1',
          'orderTypeName': 'Standard Cancel',
          'orderDate': '2026-07-21',
          'deliveryDate': '2026-07-22',
          'totalApprovedAmount': 84000,
          'ordererName': '황동영',
          'ordererCode': '20180073',
        },
      ])).toEntity();

      expect(entity.hasRelatedOrders, isTrue);
      expect(entity.relatedOrders, hasLength(1));
      final related = entity.relatedOrders.first;
      expect(related.sapOrderNumber, '604311314');
      expect(related.orderTypeCode, 'ZRE1');
      expect(related.orderTypeName, 'Standard Cancel');
      expect(related.totalApprovedAmount, 84000);
      expect(related.ordererName, '황동영');
    });

    test('relatedOrders 부재/null → 빈 배열 (하위호환, 크래시 없음)', () {
      final entity = OrderRequestDetailModel.fromJson(detailData()).toEntity();
      expect(entity.hasRelatedOrders, isFalse);
      expect(entity.relatedOrders, isEmpty);
    });
  });
}
