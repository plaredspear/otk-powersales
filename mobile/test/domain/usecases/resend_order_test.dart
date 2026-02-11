import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/resend_order.dart';

/// Mock OrderRepository for testing ResendOrder
class _MockOrderRepository implements OrderRepository {
  Object? errorToThrow;
  bool resendOrderCalled = false;

  // Capture call parameters
  int? lastOrderId;

  @override
  Future<void> resendOrder({required int orderId}) async {
    // Capture parameters
    lastOrderId = orderId;
    resendOrderCalled = true;

    // Throw error if set
    if (errorToThrow != null) {
      throw errorToThrow!;
    }

    // Successfully completes if no error
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
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) {
    throw UnimplementedError('cancelOrder not implemented in mock');
  }

  @override
  Future<int> getCreditBalance({required int clientId}) {
    throw UnimplementedError();
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
  Future<ProductForOrder> getProductByBarcode({required String barcode}) {
    throw UnimplementedError();
  }

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderDraft?> loadDraftOrder() {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteDraftOrder() {
    throw UnimplementedError();
  }

  @override
  Future<ValidationResult> validateOrder({required OrderDraft orderDraft}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderSubmitResult> submitOrder({required OrderDraft orderDraft}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderSubmitResult> updateOrder({required int orderId, required OrderDraft orderDraft}) {
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
}

void main() {
  late _MockOrderRepository mockRepository;
  late ResendOrder useCase;

  setUp(() {
    mockRepository = _MockOrderRepository();
    useCase = ResendOrder(mockRepository);
  });

  group('ResendOrder', () {
    test('should call repository with correct orderId', () async {
      // Arrange
      const testOrderId = 123;

      // Act
      await useCase.call(orderId: testOrderId);

      // Assert
      expect(mockRepository.resendOrderCalled, isTrue);
      expect(mockRepository.lastOrderId, equals(testOrderId));
    });

    test('should complete successfully when repository succeeds', () async {
      // Arrange
      const testOrderId = 456;

      // Act & Assert
      await expectLater(
        useCase.call(orderId: testOrderId),
        completes,
      );
      expect(mockRepository.resendOrderCalled, isTrue);
    });

    test('should propagate exceptions from repository', () async {
      // Arrange
      const testOrderId = 789;
      final testException = Exception('Network error');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(orderId: testOrderId),
        throwsA(equals(testException)),
      );
      expect(mockRepository.resendOrderCalled, isTrue);
    });

    test('should handle different error types from repository', () async {
      // Arrange
      const testOrderId = 999;
      final testError = ArgumentError('Invalid order ID');
      mockRepository.errorToThrow = testError;

      // Act & Assert
      expect(
        () async => await useCase.call(orderId: testOrderId),
        throwsA(equals(testError)),
      );
    });

    test('should propagate server error from repository', () async {
      // Arrange
      const testOrderId = 111;
      final testException = Exception('Server error: 500');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(orderId: testOrderId),
        throwsA(equals(testException)),
      );
    });

    test('should not throw when repository completes successfully', () async {
      // Arrange
      const testOrderId = 222;

      // Act
      await expectLater(
        useCase.call(orderId: testOrderId),
        completes,
      );

      // Assert
      expect(mockRepository.lastOrderId, equals(testOrderId));
    });

    test('should handle multiple consecutive calls with different orderIds', () async {
      // Arrange & Act
      await useCase.call(orderId: 1);
      expect(mockRepository.lastOrderId, equals(1));

      await useCase.call(orderId: 2);
      expect(mockRepository.lastOrderId, equals(2));

      await useCase.call(orderId: 3);
      expect(mockRepository.lastOrderId, equals(3));

      // Assert
      expect(mockRepository.resendOrderCalled, isTrue);
    });
  });
}
