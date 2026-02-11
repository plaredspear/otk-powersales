import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/get_client_orders_usecase.dart';

/// Mock OrderRepository for testing GetClientOrdersUseCase
///
/// Only implements getClientOrders and getClientOrderDetail with real logic.
/// All other methods throw UnimplementedError.
class MockOrderRepository implements OrderRepository {
  ClientOrderListResult? resultToReturn;
  Exception? errorToThrow;

  // Capture call parameters for getClientOrders
  int? lastClientId;
  String? lastDeliveryDate;
  int? lastPage;
  int? lastSize;

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async {
    // Capture parameters
    lastClientId = clientId;
    lastDeliveryDate = deliveryDate;
    lastPage = page;
    lastSize = size;

    // Throw error if set
    if (errorToThrow != null) {
      throw errorToThrow!;
    }

    // Return result if set
    if (resultToReturn != null) {
      return resultToReturn!;
    }

    // Default empty result
    return const ClientOrderListResult(
      orders: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: 20,
      isFirst: true,
      isLast: true,
    );
  }

  @override
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) {
    throw UnimplementedError('getClientOrderDetail not implemented in mock');
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

  @override
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) {
    throw UnimplementedError('cancelOrder not implemented in mock');
  }

  @override
  Future<int> getCreditBalance({required int clientId}) {
    throw UnimplementedError('getCreditBalance not implemented in mock');
  }

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() {
    throw UnimplementedError('getFavoriteProducts not implemented in mock');
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) {
    throw UnimplementedError('searchProductsForOrder not implemented in mock');
  }

  @override
  Future<ProductForOrder> getProductByBarcode({required String barcode}) {
    throw UnimplementedError('getProductByBarcode not implemented in mock');
  }

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) {
    throw UnimplementedError('saveDraftOrder not implemented in mock');
  }

  @override
  Future<OrderDraft?> loadDraftOrder() {
    throw UnimplementedError('loadDraftOrder not implemented in mock');
  }

  @override
  Future<void> deleteDraftOrder() {
    throw UnimplementedError('deleteDraftOrder not implemented in mock');
  }

  @override
  Future<ValidationResult> validateOrder({required OrderDraft orderDraft}) {
    throw UnimplementedError('validateOrder not implemented in mock');
  }

  @override
  Future<OrderSubmitResult> submitOrder({required OrderDraft orderDraft}) {
    throw UnimplementedError('submitOrder not implemented in mock');
  }

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) {
    throw UnimplementedError('updateOrder not implemented in mock');
  }

  @override
  Future<void> addToFavorites({required String productCode}) {
    throw UnimplementedError('addToFavorites not implemented in mock');
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) {
    throw UnimplementedError('removeFromFavorites not implemented in mock');
  }
}

void main() {
  late MockOrderRepository mockRepository;
  late GetClientOrdersUseCase useCase;

  setUp(() {
    mockRepository = MockOrderRepository();
    useCase = GetClientOrdersUseCase(mockRepository);
  });

  group('GetClientOrdersUseCase', () {
    test('should call repository with correct parameters', () async {
      // Arrange
      const clientId = 100;
      const deliveryDate = '2026-02-11';
      const page = 1;
      const size = 10;

      final expectedResult = ClientOrderListResult(
        orders: [
          const ClientOrder(
            sapOrderNumber: '300011396',
            clientId: 100,
            clientName: '천사푸드',
            totalAmount: 612000000,
          ),
        ],
        totalElements: 1,
        totalPages: 1,
        currentPage: 1,
        pageSize: 10,
        isFirst: false,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(
        clientId: clientId,
        deliveryDate: deliveryDate,
        page: page,
        size: size,
      );

      // Assert
      expect(result, equals(expectedResult));
      expect(mockRepository.lastClientId, equals(clientId));
      expect(mockRepository.lastDeliveryDate, equals(deliveryDate));
      expect(mockRepository.lastPage, equals(page));
      expect(mockRepository.lastSize, equals(size));
    });

    test('should return result from repository', () async {
      // Arrange
      const order1 = ClientOrder(
        sapOrderNumber: '300011396',
        clientId: 100,
        clientName: '천사푸드',
        totalAmount: 612000000,
      );

      const order2 = ClientOrder(
        sapOrderNumber: '300011397',
        clientId: 100,
        clientName: '천사푸드',
        totalAmount: 450000000,
      );

      final expectedResult = ClientOrderListResult(
        orders: [order1, order2],
        totalElements: 2,
        totalPages: 1,
        currentPage: 0,
        pageSize: 20,
        isFirst: true,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(clientId: 100);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.orders.length, equals(2));
      expect(result.orders[0], equals(order1));
      expect(result.orders[1], equals(order2));
      expect(result.totalElements, equals(2));
      expect(result.totalPages, equals(1));
      expect(result.currentPage, equals(0));
      expect(result.pageSize, equals(20));
      expect(result.isFirst, isTrue);
      expect(result.isLast, isTrue);
    });

    test('should use default page=0, size=20 when not specified', () async {
      // Arrange
      const clientId = 200;
      const expectedResult = ClientOrderListResult(
        orders: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 20,
        isFirst: true,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      await useCase.call(clientId: clientId);

      // Assert
      expect(mockRepository.lastClientId, equals(clientId));
      expect(mockRepository.lastDeliveryDate, isNull);
      expect(mockRepository.lastPage, equals(0));
      expect(mockRepository.lastSize, equals(20));
    });

    test('should pass null deliveryDate when not specified', () async {
      // Arrange
      const clientId = 300;
      const expectedResult = ClientOrderListResult(
        orders: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 20,
        isFirst: true,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      await useCase.call(
        clientId: clientId,
        page: 0,
        size: 20,
      );

      // Assert
      expect(mockRepository.lastClientId, equals(clientId));
      expect(mockRepository.lastDeliveryDate, isNull);
    });

    test('should propagate exceptions from repository', () async {
      // Arrange
      final testException = Exception('Network error');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(clientId: 100),
        throwsA(equals(testException)),
      );
    });

    test('should handle empty result from repository', () async {
      // Arrange
      const expectedResult = ClientOrderListResult(
        orders: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 20,
        isFirst: true,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(clientId: 999);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.orders, isEmpty);
      expect(result.totalElements, equals(0));
    });

    test('should handle multiple pages correctly', () async {
      // Arrange
      const clientId = 100;
      final firstPageResult = ClientOrderListResult(
        orders: [
          const ClientOrder(
            sapOrderNumber: '300011396',
            clientId: 100,
            clientName: '천사푸드',
            totalAmount: 612000000,
          ),
        ],
        totalElements: 25,
        totalPages: 3,
        currentPage: 0,
        pageSize: 10,
        isFirst: true,
        isLast: false,
      );
      mockRepository.resultToReturn = firstPageResult;

      // Act
      final result = await useCase.call(
        clientId: clientId,
        page: 0,
        size: 10,
      );

      // Assert
      expect(result.hasNextPage, isTrue);
      expect(result.hasPreviousPage, isFalse);
      expect(result.isFirst, isTrue);
      expect(result.isLast, isFalse);

      // Test second page
      final secondPageResult = ClientOrderListResult(
        orders: [
          const ClientOrder(
            sapOrderNumber: '300011397',
            clientId: 100,
            clientName: '천사푸드',
            totalAmount: 450000000,
          ),
        ],
        totalElements: 25,
        totalPages: 3,
        currentPage: 1,
        pageSize: 10,
        isFirst: false,
        isLast: false,
      );
      mockRepository.resultToReturn = secondPageResult;

      final result2 = await useCase.call(
        clientId: clientId,
        page: 1,
        size: 10,
      );

      expect(result2.hasNextPage, isTrue);
      expect(result2.hasPreviousPage, isTrue);
      expect(result2.isFirst, isFalse);
      expect(result2.isLast, isFalse);
    });

    test('should handle custom deliveryDate parameter', () async {
      // Arrange
      const clientId = 100;
      const deliveryDate = '2026-03-15';
      const expectedResult = ClientOrderListResult(
        orders: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 20,
        isFirst: true,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      await useCase.call(
        clientId: clientId,
        deliveryDate: deliveryDate,
      );

      // Assert
      expect(mockRepository.lastClientId, equals(clientId));
      expect(mockRepository.lastDeliveryDate, equals(deliveryDate));
      expect(mockRepository.lastPage, equals(0));
      expect(mockRepository.lastSize, equals(20));
    });

    test('should handle different error types from repository', () async {
      // Arrange - Network error
      final networkError = Exception('Connection timeout');
      mockRepository.errorToThrow = networkError;

      // Act & Assert
      expect(
        () async => await useCase.call(clientId: 100),
        throwsA(equals(networkError)),
      );

      // Arrange - Server error
      final serverError = Exception('Internal server error');
      mockRepository.errorToThrow = serverError;

      // Act & Assert
      expect(
        () async => await useCase.call(clientId: 100),
        throwsA(equals(serverError)),
      );

      // Arrange - Not found error
      final notFoundError = Exception('Client not found');
      mockRepository.errorToThrow = notFoundError;

      // Act & Assert
      expect(
        () async => await useCase.call(clientId: 999),
        throwsA(equals(notFoundError)),
      );
    });
  });
}
