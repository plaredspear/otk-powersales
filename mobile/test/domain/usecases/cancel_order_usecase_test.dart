import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/cancel_order_usecase.dart';

/// Mock OrderRepository for testing CancelOrderUseCase
class _MockOrderRepository implements OrderRepository {
  Object? errorToThrow;
  bool cancelOrderCalled = false;

  // Capture call parameters
  int? lastOrderId;
  List<String>? lastProductCodes;

  // Return value
  OrderCancelResult resultToReturn = const OrderCancelResult(
    cancelledCount: 0,
    cancelledProductCodes: [],
  );

  @override
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) async {
    lastOrderId = orderId;
    lastProductCodes = productCodes;
    cancelOrderCalled = true;

    if (errorToThrow != null) {
      throw errorToThrow!;
    }

    return resultToReturn;
  }

  @override
  Future<OrderListResult> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError('getMyOrders not implemented in mock');
  }

  @override
  Future<OrderDetail> getOrderDetail({required int orderId}) {
    throw UnimplementedError('getOrderDetail not implemented in mock');
  }

  @override
  Future<void> resendOrder({required int orderId}) {
    throw UnimplementedError('resendOrder not implemented in mock');
  }
}

void main() {
  late _MockOrderRepository mockRepository;
  late CancelOrderUseCase useCase;

  setUp(() {
    mockRepository = _MockOrderRepository();
    useCase = CancelOrderUseCase(mockRepository);
  });

  group('CancelOrderUseCase', () {
    test('should call repository with correct orderId and productCodes',
        () async {
      // Arrange
      const testOrderId = 3;
      const testProductCodes = ['01101123', '01101222'];
      mockRepository.resultToReturn = const OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: testProductCodes,
      );

      // Act
      await useCase.call(
        orderId: testOrderId,
        productCodes: testProductCodes,
      );

      // Assert
      expect(mockRepository.cancelOrderCalled, isTrue);
      expect(mockRepository.lastOrderId, equals(testOrderId));
      expect(mockRepository.lastProductCodes, equals(testProductCodes));
    });

    test('should return OrderCancelResult from repository', () async {
      // Arrange
      const expectedResult = OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101123', '01101222'],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(
        orderId: 1,
        productCodes: ['01101123', '01101222'],
      );

      // Assert
      expect(result, equals(expectedResult));
      expect(result.cancelledCount, 2);
      expect(result.cancelledProductCodes.length, 2);
    });

    test('should throw ArgumentError when productCodes is empty', () async {
      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 1,
          productCodes: [],
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
          productCodes: ['01101123'],
        ),
        throwsA(equals(testException)),
      );
    });

    test('should propagate ORDER_ALREADY_CLOSED error', () async {
      // Arrange
      final testException = Exception('ORDER_ALREADY_CLOSED');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 1,
          productCodes: ['01101123'],
        ),
        throwsA(equals(testException)),
      );
    });

    test('should propagate ALREADY_CANCELLED error', () async {
      // Arrange
      final testException = Exception('ALREADY_CANCELLED');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(
          orderId: 3,
          productCodes: ['01101123'],
        ),
        throwsA(equals(testException)),
      );
    });

    test('should handle single product cancellation', () async {
      // Arrange
      const expectedResult = OrderCancelResult(
        cancelledCount: 1,
        cancelledProductCodes: ['01101123'],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(
        orderId: 1,
        productCodes: ['01101123'],
      );

      // Assert
      expect(result.cancelledCount, 1);
      expect(result.cancelledProductCodes, ['01101123']);
    });

    test('should handle multiple consecutive calls', () async {
      // Arrange
      mockRepository.resultToReturn = const OrderCancelResult(
        cancelledCount: 1,
        cancelledProductCodes: ['01101123'],
      );

      // Act & Assert - first call
      await useCase.call(orderId: 1, productCodes: ['01101123']);
      expect(mockRepository.lastOrderId, 1);
      expect(mockRepository.lastProductCodes, ['01101123']);

      // Act & Assert - second call
      mockRepository.resultToReturn = const OrderCancelResult(
        cancelledCount: 2,
        cancelledProductCodes: ['01101222', '01101333'],
      );
      final result = await useCase.call(
        orderId: 2,
        productCodes: ['01101222', '01101333'],
      );
      expect(mockRepository.lastOrderId, 2);
      expect(mockRepository.lastProductCodes, ['01101222', '01101333']);
      expect(result.cancelledCount, 2);
    });
  });
}
