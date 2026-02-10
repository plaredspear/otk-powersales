import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_detail.dart';

void main() {
  late OrderMockRepository repository;

  setUp(() {
    repository = OrderMockRepository();
  });

  group('OrderMockRepository - getOrderDetail', () {
    test('마감전 주문 (orderId=3) - orderedItems 있음, processingStatus null, rejectedItems null',
        () async {
      final detail = await repository.getOrderDetail(orderId: 3);

      expect(detail.id, 3);
      expect(detail.orderRequestNumber, 'OP00000072');
      expect(detail.clientId, 3);
      expect(detail.clientName, '대한식품유통');
      expect(detail.clientDeadlineTime, '15:30');
      expect(detail.orderDate, DateTime(2026, 2, 4));
      expect(detail.deliveryDate, DateTime(2026, 2, 7));
      expect(detail.totalAmount, 180500000);
      expect(detail.approvalStatus, ApprovalStatus.pending);
      expect(detail.isClosed, false);

      // 마감전: orderedItems는 있어야 함
      expect(detail.orderedItems, isNotEmpty);
      expect(detail.orderedItemCount, detail.orderedItems.length);

      // 마감전: totalApprovedAmount, processingStatus, rejectedItems는 null
      expect(detail.totalApprovedAmount, isNull);
      expect(detail.orderProcessingStatus, isNull);
      expect(detail.rejectedItems, isNull);
    });

    test('마감후 주문 (orderId=2) - processingStatus 있음', () async {
      final detail = await repository.getOrderDetail(orderId: 2);

      expect(detail.id, 2);
      expect(detail.orderRequestNumber, 'OP00000073');
      expect(detail.clientId, 2);
      expect(detail.clientName, '(유)경산식품');
      expect(detail.clientDeadlineTime, '14:00');
      expect(detail.orderDate, DateTime(2026, 2, 4));
      expect(detail.deliveryDate, DateTime(2026, 2, 7));
      expect(detail.totalAmount, 245000000);
      expect(detail.approvalStatus, ApprovalStatus.approved);
      expect(detail.isClosed, true);

      // 마감후: totalApprovedAmount가 있어야 함
      expect(detail.totalApprovedAmount, isNotNull);
      expect(detail.totalApprovedAmount, (detail.totalAmount * 0.85).toInt());

      // 마감후: orderProcessingStatus가 있어야 함
      expect(detail.orderProcessingStatus, isNotNull);
      expect(detail.orderProcessingStatus!.sapOrderNumber, isNotEmpty);
      expect(detail.orderProcessingStatus!.sapOrderNumber, startsWith('0300013'));
      expect(detail.orderProcessingStatus!.items, isNotEmpty);

      // processingStatus의 items 검증
      for (final item in detail.orderProcessingStatus!.items) {
        expect(item.productCode, isNotEmpty);
        expect(item.productName, isNotEmpty);
        expect(item.deliveredQuantity, endsWith(' EA'));
        expect(
          item.deliveryStatus,
          isIn([
            DeliveryStatus.waiting,
            DeliveryStatus.shipping,
            DeliveryStatus.delivered,
          ]),
        );
      }

      // id=2는 rejectedItems가 없는 주문
      expect(detail.rejectedItems, isNull);
    });

    test('마감후 반려 주문 (orderId=1) - rejectedItems 5개 있음', () async {
      final detail = await repository.getOrderDetail(orderId: 1);

      expect(detail.id, 1);
      expect(detail.orderRequestNumber, 'OP00000074');
      expect(detail.clientId, 1);
      expect(detail.clientName, '천사푸드');
      expect(detail.clientDeadlineTime, '13:40');
      expect(detail.orderDate, DateTime(2026, 2, 5));
      expect(detail.deliveryDate, DateTime(2026, 2, 8));
      expect(detail.totalAmount, 612000000);
      expect(detail.approvalStatus, ApprovalStatus.approved);
      expect(detail.isClosed, true);

      // 마감후: totalApprovedAmount와 orderProcessingStatus 있음
      expect(detail.totalApprovedAmount, isNotNull);
      expect(detail.orderProcessingStatus, isNotNull);

      // 반려 제품 검증 (5개)
      expect(detail.rejectedItems, isNotNull);
      expect(detail.rejectedItems!.length, 5);

      // 반려 제품 상세 검증
      final rejectedItem1 = detail.rejectedItems![0];
      expect(rejectedItem1.productCode, '23010011');
      expect(rejectedItem1.productName, '오감포차_크림새우180G');
      expect(rejectedItem1.orderQuantityBoxes, 1);
      expect(rejectedItem1.rejectionReason, '납품일자가 업무일이 아닙니다.');

      final rejectedItem2 = detail.rejectedItems![1];
      expect(rejectedItem2.productCode, '11110003');
      expect(rejectedItem2.productName, '토마토케찹500G');
      expect(rejectedItem2.orderQuantityBoxes, 2);

      final rejectedItem3 = detail.rejectedItems![2];
      expect(rejectedItem3.productCode, '01202001');
      expect(rejectedItem3.rejectionReason, '재고 부족');

      final rejectedItem4 = detail.rejectedItems![3];
      expect(rejectedItem4.productCode, '04101002');
      expect(rejectedItem4.rejectionReason, '단가 불일치');

      final rejectedItem5 = detail.rejectedItems![4];
      expect(rejectedItem5.productCode, '03201001');
      expect(rejectedItem5.rejectionReason, '최소 주문 수량 미달');

      // hasRejectedItems getter 검증
      expect(detail.hasRejectedItems, true);
    });

    test('마감후 반려 주문 (orderId=7) - rejectedItems 2개 있음', () async {
      final detail = await repository.getOrderDetail(orderId: 7);

      expect(detail.id, 7);
      expect(detail.orderRequestNumber, 'OP00000068');
      expect(detail.clientId, 2);
      expect(detail.clientName, '(유)경산식품');
      expect(detail.clientDeadlineTime, '14:00');
      expect(detail.orderDate, DateTime(2026, 2, 1));
      expect(detail.deliveryDate, DateTime(2026, 2, 4));
      expect(detail.totalAmount, 89000000);
      expect(detail.approvalStatus, ApprovalStatus.approved);
      expect(detail.isClosed, true);

      // 마감후: totalApprovedAmount와 orderProcessingStatus 있음
      expect(detail.totalApprovedAmount, isNotNull);
      expect(detail.orderProcessingStatus, isNotNull);

      // 반려 제품 검증 (2개)
      expect(detail.rejectedItems, isNotNull);
      expect(detail.rejectedItems!.length, 2);

      // 반려 제품 상세 검증
      final rejectedItem1 = detail.rejectedItems![0];
      expect(rejectedItem1.productCode, '23010011');
      expect(rejectedItem1.productName, '오감포차_크림새우180G');
      expect(rejectedItem1.orderQuantityBoxes, 1);
      expect(rejectedItem1.rejectionReason, '납품일자가 업무일이 아닙니다.');

      final rejectedItem2 = detail.rejectedItems![1];
      expect(rejectedItem2.productCode, '11110003');
      expect(rejectedItem2.productName, '토마토케찹500G');
      expect(rejectedItem2.orderQuantityBoxes, 2);
      expect(rejectedItem2.rejectionReason, '납품일자가 업무일이 아닙니다.');

      // hasRejectedItems getter 검증
      expect(detail.hasRejectedItems, true);
    });

    test('존재하지 않는 orderId - Exception 발생', () async {
      expect(
        () => repository.getOrderDetail(orderId: 9999),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('ORDER_NOT_FOUND'),
        )),
      );
    });

    test('전송실패 주문 (orderId=4) - 마감전, sendFailed 상태', () async {
      final detail = await repository.getOrderDetail(orderId: 4);

      expect(detail.id, 4);
      expect(detail.orderRequestNumber, 'OP00000071');
      expect(detail.approvalStatus, ApprovalStatus.sendFailed);
      expect(detail.isClosed, false);

      // 마감전이므로 processingStatus, rejectedItems, totalApprovedAmount 없음
      expect(detail.totalApprovedAmount, isNull);
      expect(detail.orderProcessingStatus, isNull);
      expect(detail.rejectedItems, isNull);

      // orderedItems에는 취소된 제품이 있을 수 있음
      expect(detail.orderedItems, isNotEmpty);
      final cancelledItems =
          detail.orderedItems.where((item) => item.isCancelled).toList();
      expect(cancelledItems, isNotEmpty);
    });

    test('orderedItems 상세 검증 - 제품 정보 올바름', () async {
      final detail = await repository.getOrderDetail(orderId: 1);

      expect(detail.orderedItems, isNotEmpty);

      for (final item in detail.orderedItems) {
        // 필수 필드가 채워져 있는지 검증
        expect(item.productCode, isNotEmpty);
        expect(item.productName, isNotEmpty);
        expect(item.totalQuantityBoxes, greaterThan(0));
        expect(item.totalQuantityPieces, greaterThan(0));

        // 제품 코드 형식 검증 (8자리 숫자)
        expect(item.productCode.length, 8);
        expect(int.tryParse(item.productCode), isNotNull);

        // 수량 관계 검증
        expect(item.totalQuantityPieces, greaterThan(item.totalQuantityBoxes));
      }
    });

    test('거래처 마감시간 매핑 검증', () async {
      final testCases = [
        (1, '13:40'),
        (2, '14:00'),
        (3, '15:30'),
        (4, '13:00'),
        (5, '14:30'),
        (6, '16:00'),
        (7, '13:30'),
        (8, '15:00'),
      ];

      for (final (clientId, expectedDeadline) in testCases) {
        // 해당 거래처의 주문을 찾음
        final orders = await repository.getMyOrders(
          clientId: clientId,
          size: 1,
        );

        if (orders.orders.isNotEmpty) {
          final orderId = orders.orders.first.id;
          final detail = await repository.getOrderDetail(orderId: orderId);

          expect(detail.clientId, clientId);
          expect(detail.clientDeadlineTime, expectedDeadline);
        }
      }
    });

    test('마감후 주문 - totalApprovedAmount는 totalAmount의 85%', () async {
      // 마감후 주문 여러 개 검증
      final closedOrderIds = [1, 2, 6, 7, 8, 10, 11, 12, 14, 16, 17, 18, 20];

      for (final orderId in closedOrderIds) {
        final detail = await repository.getOrderDetail(orderId: orderId);

        if (detail.isClosed) {
          expect(detail.totalApprovedAmount, isNotNull);
          final expectedApprovedAmount =
              (detail.totalAmount * 0.85).toInt();
          expect(detail.totalApprovedAmount, expectedApprovedAmount);
        }
      }
    });

    test('orderProcessingStatus - SAP 주문번호 형식 검증', () async {
      final detail = await repository.getOrderDetail(orderId: 1);

      expect(detail.orderProcessingStatus, isNotNull);
      final sapNumber = detail.orderProcessingStatus!.sapOrderNumber;

      // SAP 주문번호는 10자리 숫자로 시작
      expect(sapNumber.length, greaterThanOrEqualTo(10));
      expect(sapNumber, startsWith('0300013'));
    });

    test('네트워크 지연 시뮬레이션 검증', () async {
      final stopwatch = Stopwatch()..start();

      await repository.getOrderDetail(orderId: 1);

      stopwatch.stop();

      // 300ms 지연이 시뮬레이션되어야 함 (여유를 두고 250ms 이상)
      expect(stopwatch.elapsedMilliseconds, greaterThanOrEqualTo(250));
    });

    test('allItemsCancelled getter - 모든 제품이 취소된 경우', () async {
      // orderId=9는 여러 취소 제품이 있음
      final detail = await repository.getOrderDetail(orderId: 9);

      final allCancelled = detail.orderedItems.every((item) => item.isCancelled);

      expect(detail.allItemsCancelled, allCancelled);
    });

    test('orderedItemCount와 실제 orderedItems 길이 일치', () async {
      final testOrderIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

      for (final orderId in testOrderIds) {
        final detail = await repository.getOrderDetail(orderId: orderId);

        expect(
          detail.orderedItemCount,
          detail.orderedItems.length,
          reason: 'orderId=$orderId에서 orderedItemCount가 일치하지 않음',
        );
      }
    });
  });

  group('OrderMockRepository - resendOrder', () {
    test('전송실패 주문 (orderId=4) 재전송 성공', () async {
      // orderId=4는 approvalStatus=sendFailed
      await expectLater(
        repository.resendOrder(orderId: 4),
        completes,
      );

      // 재전송 후 상태는 실제로는 변경되지 않음 (Mock)
      // 실제 구현에서는 API 호출 후 상태가 변경됨
    });

    test('전송실패 주문 (orderId=13) 재전송 성공', () async {
      // orderId=13도 approvalStatus=sendFailed
      await expectLater(
        repository.resendOrder(orderId: 13),
        completes,
      );
    });

    test('전송실패 주문 (orderId=23) 재전송 성공', () async {
      // orderId=23도 approvalStatus=sendFailed
      await expectLater(
        repository.resendOrder(orderId: 23),
        completes,
      );
    });

    test('전송실패가 아닌 주문 (orderId=1) - INVALID_STATUS Exception 발생',
        () async {
      // orderId=1은 approvalStatus=approved
      expect(
        () => repository.resendOrder(orderId: 1),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('INVALID_STATUS'),
        )),
      );
    });

    test('대기중 주문 (orderId=3) - INVALID_STATUS Exception 발생', () async {
      // orderId=3은 approvalStatus=pending
      expect(
        () => repository.resendOrder(orderId: 3),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('INVALID_STATUS'),
        )),
      );
    });

    test('재전송 주문 (orderId=5) - INVALID_STATUS Exception 발생', () async {
      // orderId=5는 approvalStatus=resend
      expect(
        () => repository.resendOrder(orderId: 5),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('INVALID_STATUS'),
        )),
      );
    });

    test('존재하지 않는 orderId - ORDER_NOT_FOUND Exception 발생', () async {
      expect(
        () => repository.resendOrder(orderId: 9999),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('ORDER_NOT_FOUND'),
        )),
      );
    });

    test('음수 orderId - ORDER_NOT_FOUND Exception 발생', () async {
      expect(
        () => repository.resendOrder(orderId: -1),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('ORDER_NOT_FOUND'),
        )),
      );
    });

    test('0 orderId - ORDER_NOT_FOUND Exception 발생', () async {
      expect(
        () => repository.resendOrder(orderId: 0),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('ORDER_NOT_FOUND'),
        )),
      );
    });

    test('네트워크 지연 시뮬레이션 검증', () async {
      final stopwatch = Stopwatch()..start();

      await repository.resendOrder(orderId: 4);

      stopwatch.stop();

      // 300ms 지연이 시뮬레이션되어야 함 (여유를 두고 250ms 이상)
      expect(stopwatch.elapsedMilliseconds, greaterThanOrEqualTo(250));
    });

    test('모든 sendFailed 주문 재전송 가능 확인', () async {
      // Mock 데이터에서 모든 sendFailed 주문 찾기
      final result = await repository.getMyOrders(
        status: 'SEND_FAILED',
        size: 100,
      );

      expect(result.orders, isNotEmpty);

      // 모든 sendFailed 주문 재전송 테스트
      for (final order in result.orders) {
        await expectLater(
          repository.resendOrder(orderId: order.id),
          completes,
        );
      }
    });

    test('재전송 후 주문 상세 조회 가능', () async {
      // 재전송 실행
      await repository.resendOrder(orderId: 4);

      // 재전송 후에도 주문 상세 조회 가능해야 함
      final detail = await repository.getOrderDetail(orderId: 4);

      expect(detail.id, 4);
      expect(detail.approvalStatus, ApprovalStatus.sendFailed);
    });
  });

  group('OrderMockRepository - 통합 시나리오', () {
    test('마감전 주문 → 주문 상세 조회 시나리오', () async {
      // 1. 마감전 주문 조회
      final result = await repository.getMyOrders(
        status: 'PENDING',
        size: 1,
      );

      expect(result.orders, isNotEmpty);
      final order = result.orders.first;
      expect(order.isClosed, false);

      // 2. 주문 상세 조회
      final detail = await repository.getOrderDetail(orderId: order.id);

      expect(detail.id, order.id);
      expect(detail.orderRequestNumber, order.orderRequestNumber);
      expect(detail.isClosed, false);
      expect(detail.orderedItems, isNotEmpty);
      expect(detail.orderProcessingStatus, isNull);
    });

    test('마감후 주문 → 주문 상세 조회 시나리오', () async {
      // 1. 마감후 주문 조회
      final result = await repository.getMyOrders(
        status: 'APPROVED',
        size: 1,
      );

      expect(result.orders, isNotEmpty);

      // 마감된 주문 찾기
      final order = result.orders.firstWhere((o) => o.isClosed);
      expect(order.isClosed, true);

      // 2. 주문 상세 조회
      final detail = await repository.getOrderDetail(orderId: order.id);

      expect(detail.id, order.id);
      expect(detail.isClosed, true);
      expect(detail.totalApprovedAmount, isNotNull);
      expect(detail.orderProcessingStatus, isNotNull);
    });

    test('전송실패 주문 → 주문 상세 조회 → 재전송 시나리오', () async {
      // 1. 전송실패 주문 조회
      final result = await repository.getMyOrders(
        status: 'SEND_FAILED',
        size: 1,
      );

      expect(result.orders, isNotEmpty);
      final order = result.orders.first;
      expect(order.approvalStatus, ApprovalStatus.sendFailed);

      // 2. 주문 상세 조회
      final detail = await repository.getOrderDetail(orderId: order.id);

      expect(detail.id, order.id);
      expect(detail.approvalStatus, ApprovalStatus.sendFailed);

      // 3. 재전송 실행
      await expectLater(
        repository.resendOrder(orderId: order.id),
        completes,
      );
    });

    test('반려 제품 있는 주문 → 상세 조회 시나리오', () async {
      // 1. 마감후 주문 중 반려 제품이 있는 주문 조회
      final detail = await repository.getOrderDetail(orderId: 1);

      expect(detail.isClosed, true);
      expect(detail.hasRejectedItems, true);
      expect(detail.rejectedItems, isNotEmpty);

      // 2. 반려 제품 상세 확인
      for (final rejectedItem in detail.rejectedItems!) {
        expect(rejectedItem.productCode, isNotEmpty);
        expect(rejectedItem.productName, isNotEmpty);
        expect(rejectedItem.orderQuantityBoxes, greaterThan(0));
        expect(rejectedItem.rejectionReason, isNotEmpty);
      }

      // 3. 주문 처리 현황도 함께 존재해야 함
      expect(detail.orderProcessingStatus, isNotNull);
    });

    test('거래처별 주문 목록 → 각 주문 상세 조회 시나리오', () async {
      // 1. 특정 거래처(clientId=1)의 주문 목록 조회
      final result = await repository.getMyOrders(
        clientId: 1,
        size: 5,
      );

      expect(result.orders, isNotEmpty);

      // 2. 각 주문의 상세 정보 조회 및 거래처 정보 일치 확인
      for (final order in result.orders) {
        final detail = await repository.getOrderDetail(orderId: order.id);

        expect(detail.clientId, 1);
        expect(detail.clientName, '천사푸드');
        expect(detail.clientDeadlineTime, '13:40');
      }
    });
  });
}
