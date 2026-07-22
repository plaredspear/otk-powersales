import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/product_order_history_group.dart';
import 'package:mobile/domain/repositories/order_request_repository.dart';
import 'package:mobile/domain/usecases/cancel_order_usecase.dart';

/// Mock OrderRequestRepository for testing CancelOrderUseCase
class _MockOrderRepository implements OrderRequestRepository {
  Object? errorToThrow;
  bool cancelOrderCalled = false;

  // Capture call parameters
  int? lastOrderId;
  List<int>? lastOrderProductIds;

  // Return value
  OrderCancelResult resultToReturn = const OrderCancelResult(
    orderRequestId: 0,
    orderRequestNumber: '',
    orderRequestStatus: 'CANCEL_REQUESTED',
    cancelledLines: [],
  );

  @override
  Future<OrderCancelResult> cancelOrderRequest({
    required int orderId,
    required List<int> orderProductIds,
  }) async {
    lastOrderId = orderId;
    lastOrderProductIds = orderProductIds;
    cancelOrderCalled = true;

    if (errorToThrow != null) {
      throw errorToThrow!;
    }

    return resultToReturn;
  }

  @override
  Future<OrderRequestListResult> getMyOrderRequests({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError('getMyOrderRequests not implemented in mock');
  }

  @override
  Future<OrderDetail> getOrderRequestDetail({required int orderId}) {
    throw UnimplementedError('getOrderRequestDetail not implemented in mock');
  }

  @override
  Future<void> resendOrderRequest({required int orderId}) {
    throw UnimplementedError('resendOrderRequest not implemented in mock');
  }

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() {
    throw UnimplementedError();
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({required String query, String? categoryMid, String? categorySub}) {
    throw UnimplementedError();
  }

  @override
  Future<void> addToFavorites({required String productCode}) {
    throw UnimplementedError();
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) {
    throw UnimplementedError();
  }

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<List<ProductOrderHistoryGroup>> getAccountOrderHistory({
    required String accountCode,
    required DateTime startDate,
    required DateTime endDate,
  }) {
    throw UnimplementedError();
  }
}

void main() {
  late _MockOrderRepository mockRepository;
  late CancelOrderUseCase useCase;

  setUp(() {
    mockRepository = _MockOrderRepository();
    useCase = CancelOrderUseCase(mockRepository);
  });

  OrderCancelResult buildResult(List<int> ids) => OrderCancelResult(
        orderRequestId: 1,
        orderRequestNumber: 'OP20260301',
        orderRequestStatus: 'CANCEL_REQUESTED',
        cancelledLines: [
          for (final id in ids)
            CancelledLine(
              orderProductId: id,
              lineNumber: 1,
              productCode: '$id',
            ),
        ],
      );

  group('CancelOrderUseCase', () {
    test('should call repository with correct orderId and orderProductIds',
        () async {
      // Arrange
      const testOrderId = 3;
      const testOrderProductIds = [101, 102];
      mockRepository.resultToReturn = buildResult(testOrderProductIds);

      // Act
      await useCase.call(
        orderId: testOrderId,
        orderProductIds: testOrderProductIds,
      );

      // Assert
      expect(mockRepository.cancelOrderCalled, isTrue);
      expect(mockRepository.lastOrderId, equals(testOrderId));
      expect(mockRepository.lastOrderProductIds, equals(testOrderProductIds));
    });

    test('should return OrderCancelResult from repository', () async {
      // Arrange
      final expectedResult = buildResult([101, 102]);
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(
        orderId: 1,
        orderProductIds: [101, 102],
      );

      // Assert
      expect(result, equals(expectedResult));
      expect(result.cancelledCount, 2);
      expect(result.cancelledLines.length, 2);
    });

    test('should throw ArgumentError when orderProductIds is empty', () async {
      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 1,
          orderProductIds: [],
        ),
        throwsA(isA<ArgumentError>()),
      );

      // Repository should not be called
      expect(mockRepository.cancelOrderCalled, isFalse);
    });

    test('should propagate exceptions from repository', () async {
      // Arrange
      final testException = Exception('ORDER_NOT_FOUND');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 999,
          orderProductIds: [101],
        ),
        throwsA(equals(testException)),
      );
    });

    test('should propagate ORD_CANCEL_INVALID_STATUS error', () async {
      // Arrange
      final testException = Exception('ORD_CANCEL_INVALID_STATUS');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 1,
          orderProductIds: [101],
        ),
        throwsA(equals(testException)),
      );
    });

    test('should propagate ORD_CANCEL_LINE_NOT_FOUND error', () async {
      // Arrange
      final testException = Exception('ORD_CANCEL_LINE_NOT_FOUND');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 3,
          orderProductIds: [999],
        ),
        throwsA(equals(testException)),
      );
    });

    test('should handle single line cancellation', () async {
      // Arrange
      final expectedResult = buildResult([101]);
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(
        orderId: 1,
        orderProductIds: [101],
      );

      // Assert
      expect(result.cancelledCount, 1);
      expect(result.cancelledLines.single.orderProductId, 101);
    });

    test('should handle multiple consecutive calls', () async {
      // Arrange
      mockRepository.resultToReturn = buildResult([101]);

      // Act & Assert - first call
      await useCase.call(orderId: 1, orderProductIds: [101]);
      expect(mockRepository.lastOrderId, 1);
      expect(mockRepository.lastOrderProductIds, [101]);

      // Act & Assert - second call
      mockRepository.resultToReturn = buildResult([102, 103]);
      final result = await useCase.call(
        orderId: 2,
        orderProductIds: [102, 103],
      );
      expect(mockRepository.lastOrderId, 2);
      expect(mockRepository.lastOrderProductIds, [102, 103]);
      expect(result.cancelledCount, 2);
    });
  });
}
