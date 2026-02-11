import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/get_my_orders.dart';

/// Mock OrderRepository for testing
class MockOrderRepository implements OrderRepository {
  OrderListResult? resultToReturn;
  Exception? errorToThrow;

  // Capture call parameters
  int? lastClientId;
  String? lastStatus;
  String? lastDeliveryDateFrom;
  String? lastDeliveryDateTo;
  String? lastSortBy;
  String? lastSortDir;
  int? lastPage;
  int? lastSize;

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
  }) async {
    // Capture parameters
    lastClientId = clientId;
    lastStatus = status;
    lastDeliveryDateFrom = deliveryDateFrom;
    lastDeliveryDateTo = deliveryDateTo;
    lastSortBy = sortBy;
    lastSortDir = sortDir;
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
    return const OrderListResult(
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
  late MockOrderRepository mockRepository;
  late GetMyOrders useCase;

  setUp(() {
    mockRepository = MockOrderRepository();
    useCase = GetMyOrders(mockRepository);
  });

  group('GetMyOrders', () {
    test('should call repository with default parameters', () async {
      // Arrange
      final expectedResult = OrderListResult(
        orders: [
          Order(
            id: 1,
            orderRequestNumber: 'OP00000074',
            clientId: 100,
            clientName: '천사푸드',
            orderDate: DateTime(2026, 2, 5),
            deliveryDate: DateTime(2026, 2, 8),
            totalAmount: 612000000,
            approvalStatus: ApprovalStatus.approved,
            isClosed: true,
          ),
        ],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 20,
        isFirst: true,
        isLast: true,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call();

      // Assert
      expect(result, equals(expectedResult));
      expect(mockRepository.lastClientId, isNull);
      expect(mockRepository.lastStatus, isNull);
      expect(mockRepository.lastDeliveryDateFrom, isNull);
      expect(mockRepository.lastDeliveryDateTo, isNull);
      expect(mockRepository.lastSortBy, equals('orderDate'));
      expect(mockRepository.lastSortDir, equals('DESC'));
      expect(mockRepository.lastPage, equals(0));
      expect(mockRepository.lastSize, equals(20));
    });

    test('should pass all custom parameters to repository', () async {
      // Arrange
      final expectedResult = OrderListResult(
        orders: [
          Order(
            id: 2,
            orderRequestNumber: 'OP00000073',
            clientId: 200,
            clientName: '경산식품',
            orderDate: DateTime(2026, 2, 4),
            deliveryDate: DateTime(2026, 2, 7),
            totalAmount: 245000000,
            approvalStatus: ApprovalStatus.pending,
            isClosed: false,
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
        clientId: 200,
        status: 'PENDING',
        deliveryDateFrom: '2026-02-01',
        deliveryDateTo: '2026-02-28',
        sortBy: 'totalAmount',
        sortDir: 'ASC',
        page: 1,
        size: 10,
      );

      // Assert
      expect(result, equals(expectedResult));
      expect(mockRepository.lastClientId, equals(200));
      expect(mockRepository.lastStatus, equals('PENDING'));
      expect(mockRepository.lastDeliveryDateFrom, equals('2026-02-01'));
      expect(mockRepository.lastDeliveryDateTo, equals('2026-02-28'));
      expect(mockRepository.lastSortBy, equals('totalAmount'));
      expect(mockRepository.lastSortDir, equals('ASC'));
      expect(mockRepository.lastPage, equals(1));
      expect(mockRepository.lastSize, equals(10));
    });

    test('should pass null values for optional parameters when not provided', () async {
      // Arrange
      final expectedResult = const OrderListResult(
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
        sortBy: 'deliveryDate',
        sortDir: 'ASC',
      );

      // Assert
      expect(mockRepository.lastClientId, isNull);
      expect(mockRepository.lastStatus, isNull);
      expect(mockRepository.lastDeliveryDateFrom, isNull);
      expect(mockRepository.lastDeliveryDateTo, isNull);
      expect(mockRepository.lastSortBy, equals('deliveryDate'));
      expect(mockRepository.lastSortDir, equals('ASC'));
    });

    test('should propagate repository errors', () async {
      // Arrange
      final testException = Exception('Network error');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(),
        throwsA(equals(testException)),
      );
    });

    test('should return OrderListResult from repository', () async {
      // Arrange
      final order1 = Order(
        id: 1,
        orderRequestNumber: 'OP00000074',
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalAmount: 612000000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
      );

      final order2 = Order(
        id: 2,
        orderRequestNumber: 'OP00000073',
        clientId: 200,
        clientName: '경산식품',
        orderDate: DateTime(2026, 2, 4),
        deliveryDate: DateTime(2026, 2, 7),
        totalAmount: 245000000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: false,
      );

      final expectedResult = OrderListResult(
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
      final result = await useCase.call();

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

    test('should handle empty result from repository', () async {
      // Arrange
      const expectedResult = OrderListResult(
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
  });
}
