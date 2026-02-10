import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order.dart';

void main() {
  late OrderMockRepository repository;

  setUp(() {
    repository = OrderMockRepository();
  });

  group('OrderMockRepository - cancelOrder', () {
    test('마감전 주문 (orderId=3)의 취소 가능한 제품을 취소한다', () async {
      // orderId=3은 마감전(isClosed=false) 주문
      // 먼저 주문 상세를 조회하여 취소 가능한 제품 코드를 확인
      final detail = await repository.getOrderDetail(orderId: 3);
      expect(detail.isClosed, false);

      final cancellableItems =
          detail.orderedItems.where((item) => !item.isCancelled).toList();
      expect(cancellableItems, isNotEmpty);

      // 첫 번째 취소 가능한 제품 코드로 취소 요청
      final productCode = cancellableItems.first.productCode;
      final result = await repository.cancelOrder(
        orderId: 3,
        productCodes: [productCode],
      );

      expect(result.cancelledCount, 1);
      expect(result.cancelledProductCodes, [productCode]);
    });

    test('여러 제품을 한 번에 취소한다', () async {
      // orderId=3의 취소 가능한 제품 확인
      final detail = await repository.getOrderDetail(orderId: 3);
      final cancellableItems =
          detail.orderedItems.where((item) => !item.isCancelled).toList();

      // 2개 이상 있는지 확인
      expect(cancellableItems.length, greaterThanOrEqualTo(2));

      final productCodes =
          cancellableItems.take(2).map((item) => item.productCode).toList();
      final result = await repository.cancelOrder(
        orderId: 3,
        productCodes: productCodes,
      );

      expect(result.cancelledCount, 2);
      expect(result.cancelledProductCodes, productCodes);
    });

    test('마감된 주문 (orderId=1)은 취소할 수 없다', () async {
      // orderId=1은 마감된(isClosed=true) 주문
      expect(
        () async => await repository.cancelOrder(
          orderId: 1,
          productCodes: ['01101123'],
        ),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('ORDER_ALREADY_CLOSED'),
          ),
        ),
      );
    });

    test('존재하지 않는 주문은 취소할 수 없다', () async {
      expect(
        () async => await repository.cancelOrder(
          orderId: 999,
          productCodes: ['01101123'],
        ),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('ORDER_NOT_FOUND'),
          ),
        ),
      );
    });

    test('이미 취소된 제품을 다시 취소하면 에러가 발생한다', () async {
      // orderId=4 (전송실패)의 경우 첫 번째 제품이 이미 취소됨
      final detail = await repository.getOrderDetail(orderId: 4);
      final cancelledItems =
          detail.orderedItems.where((item) => item.isCancelled).toList();

      expect(cancelledItems, isNotEmpty,
          reason: 'orderId=4는 이미 취소된 제품이 있어야 합니다');

      final cancelledCode = cancelledItems.first.productCode;

      expect(
        () async => await repository.cancelOrder(
          orderId: 4,
          productCodes: [cancelledCode],
        ),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('ALREADY_CANCELLED'),
          ),
        ),
      );
    });

    test('취소 가능한 제품만 포함된 요청은 성공한다', () async {
      // orderId=5는 마감전(isClosed=false) 주문
      final detail = await repository.getOrderDetail(orderId: 5);
      expect(detail.isClosed, false);

      final cancellableItems =
          detail.orderedItems.where((item) => !item.isCancelled).toList();
      expect(cancellableItems, isNotEmpty);

      final productCodes =
          cancellableItems.map((item) => item.productCode).toList();
      final result = await repository.cancelOrder(
        orderId: 5,
        productCodes: productCodes,
      );

      expect(result.cancelledCount, productCodes.length);
      expect(result.cancelledProductCodes, productCodes);
    });

    test('전송실패 상태(orderId=4)의 마감전 주문도 취소 가능하다', () async {
      // orderId=4는 전송실패(sendFailed) + 마감전(isClosed=false)
      final detail = await repository.getOrderDetail(orderId: 4);
      expect(detail.isClosed, false);
      expect(detail.approvalStatus, ApprovalStatus.sendFailed);

      final cancellableItems =
          detail.orderedItems.where((item) => !item.isCancelled).toList();
      expect(cancellableItems, isNotEmpty);

      final productCode = cancellableItems.first.productCode;
      final result = await repository.cancelOrder(
        orderId: 4,
        productCodes: [productCode],
      );

      expect(result.cancelledCount, 1);
      expect(result.cancelledProductCodes, [productCode]);
    });

    test('네트워크 지연이 시뮬레이션된다 (300ms 이상)', () async {
      final detail = await repository.getOrderDetail(orderId: 3);
      final cancellableItems =
          detail.orderedItems.where((item) => !item.isCancelled).toList();
      final productCode = cancellableItems.first.productCode;

      final stopwatch = Stopwatch()..start();
      await repository.cancelOrder(
        orderId: 3,
        productCodes: [productCode],
      );
      stopwatch.stop();

      expect(stopwatch.elapsedMilliseconds, greaterThanOrEqualTo(250));
    });
  });
}
