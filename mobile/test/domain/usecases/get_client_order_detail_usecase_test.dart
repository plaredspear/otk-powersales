import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/get_client_order_detail_usecase.dart';

/// Mock OrderRepository for testing GetClientOrderDetailUseCase
///
/// Only implements getClientOrderDetail with real logic.
/// All other methods throw UnimplementedError.
class MockOrderRepository implements OrderRepository {
  ClientOrderDetail? resultToReturn;
  Exception? errorToThrow;

  // Capture call parameters for getClientOrderDetail
  String? lastSapOrderNumber;

  @override
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) async {
    // Capture parameters
    lastSapOrderNumber = sapOrderNumber;

    // Throw error if set
    if (errorToThrow != null) {
      throw errorToThrow!;
    }

    // Return result if set
    if (resultToReturn != null) {
      return resultToReturn!;
    }

    // Default result
    return ClientOrderDetail(
      sapOrderNumber: sapOrderNumber,
      clientId: 100,
      clientName: 'Default Client',
      clientDeadlineTime: null,
      orderDate: DateTime(2026, 2, 11),
      deliveryDate: DateTime(2026, 2, 14),
      totalApprovedAmount: 0,
      orderedItemCount: 0,
      orderedItems: [],
    );
  }

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError('getClientOrders not implemented in mock');
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
  late GetClientOrderDetailUseCase useCase;

  setUp(() {
    mockRepository = MockOrderRepository();
    useCase = GetClientOrderDetailUseCase(mockRepository);
  });

  group('GetClientOrderDetailUseCase', () {
    test('should call repository with correct sapOrderNumber', () async {
      // Arrange
      const sapOrderNumber = '300011396';
      final expectedResult = ClientOrderDetail(
        sapOrderNumber: sapOrderNumber,
        clientId: 100,
        clientName: '천사푸드',
        clientDeadlineTime: '13:00',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 612000000,
        orderedItemCount: 2,
        orderedItems: [
          const ClientOrderItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            deliveredQuantity: '5 BOX',
            deliveryStatus: DeliveryStatus.delivered,
          ),
          const ClientOrderItem(
            productCode: '01101124',
            productName: '마요네즈 500g',
            deliveredQuantity: '10 BOX',
            deliveryStatus: DeliveryStatus.shipping,
          ),
        ],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(sapOrderNumber: sapOrderNumber);

      // Assert
      expect(result, equals(expectedResult));
      expect(mockRepository.lastSapOrderNumber, equals(sapOrderNumber));
    });

    test('should return result from repository', () async {
      // Arrange
      const sapOrderNumber = '300011396';
      final item1 = const ClientOrderItem(
        productCode: '01101123',
        productName: '갈릭 아이올리소스 240g',
        deliveredQuantity: '5 BOX',
        deliveryStatus: DeliveryStatus.delivered,
      );

      final item2 = const ClientOrderItem(
        productCode: '01101124',
        productName: '마요네즈 500g',
        deliveredQuantity: '10 BOX',
        deliveryStatus: DeliveryStatus.shipping,
      );

      final expectedResult = ClientOrderDetail(
        sapOrderNumber: sapOrderNumber,
        clientId: 100,
        clientName: '천사푸드',
        clientDeadlineTime: '13:00',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 612000000,
        orderedItemCount: 2,
        orderedItems: [item1, item2],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(sapOrderNumber: sapOrderNumber);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.sapOrderNumber, equals(sapOrderNumber));
      expect(result.clientId, equals(100));
      expect(result.clientName, equals('천사푸드'));
      expect(result.clientDeadlineTime, equals('13:00'));
      expect(result.totalApprovedAmount, equals(612000000));
      expect(result.orderedItemCount, equals(2));
      expect(result.orderedItems.length, equals(2));
      expect(result.orderedItems[0], equals(item1));
      expect(result.orderedItems[1], equals(item2));
    });

    test('should propagate exceptions from repository', () async {
      // Arrange
      final testException = Exception('Network error');
      mockRepository.errorToThrow = testException;

      // Act & Assert
      expect(
        () async => await useCase.call(sapOrderNumber: '300011396'),
        throwsA(equals(testException)),
      );
    });

    test('should handle order detail without clientDeadlineTime', () async {
      // Arrange
      const sapOrderNumber = '300011397';
      final expectedResult = ClientOrderDetail(
        sapOrderNumber: sapOrderNumber,
        clientId: 200,
        clientName: '경산식품',
        clientDeadlineTime: null, // No deadline time
        orderDate: DateTime(2026, 2, 4),
        deliveryDate: DateTime(2026, 2, 7),
        totalApprovedAmount: 245000000,
        orderedItemCount: 1,
        orderedItems: [
          const ClientOrderItem(
            productCode: '01101125',
            productName: '케첩 500g',
            deliveredQuantity: '3 BOX',
            deliveryStatus: DeliveryStatus.waiting,
          ),
        ],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(sapOrderNumber: sapOrderNumber);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.clientDeadlineTime, isNull);
    });

    test('should handle order detail with empty items list', () async {
      // Arrange
      const sapOrderNumber = '300011398';
      final expectedResult = ClientOrderDetail(
        sapOrderNumber: sapOrderNumber,
        clientId: 300,
        clientName: '서울식품',
        clientDeadlineTime: '14:00',
        orderDate: DateTime(2026, 2, 10),
        deliveryDate: DateTime(2026, 2, 13),
        totalApprovedAmount: 0,
        orderedItemCount: 0,
        orderedItems: [], // Empty items
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(sapOrderNumber: sapOrderNumber);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.orderedItems, isEmpty);
      expect(result.orderedItemCount, equals(0));
    });

    test('should handle different delivery statuses', () async {
      // Arrange
      const sapOrderNumber = '300011399';
      final expectedResult = ClientOrderDetail(
        sapOrderNumber: sapOrderNumber,
        clientId: 100,
        clientName: '천사푸드',
        clientDeadlineTime: '13:00',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 1000000,
        orderedItemCount: 3,
        orderedItems: [
          const ClientOrderItem(
            productCode: '01101123',
            productName: '제품A',
            deliveredQuantity: '5 BOX',
            deliveryStatus: DeliveryStatus.waiting,
          ),
          const ClientOrderItem(
            productCode: '01101124',
            productName: '제품B',
            deliveredQuantity: '10 BOX',
            deliveryStatus: DeliveryStatus.shipping,
          ),
          const ClientOrderItem(
            productCode: '01101125',
            productName: '제품C',
            deliveredQuantity: '3 BOX',
            deliveryStatus: DeliveryStatus.delivered,
          ),
        ],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(sapOrderNumber: sapOrderNumber);

      // Assert
      expect(result.orderedItems[0].deliveryStatus, equals(DeliveryStatus.waiting));
      expect(result.orderedItems[1].deliveryStatus, equals(DeliveryStatus.shipping));
      expect(result.orderedItems[2].deliveryStatus, equals(DeliveryStatus.delivered));
    });

    test('should handle different error types from repository', () async {
      // Arrange - Network error
      final networkError = Exception('Connection timeout');
      mockRepository.errorToThrow = networkError;

      // Act & Assert
      expect(
        () async => await useCase.call(sapOrderNumber: '300011396'),
        throwsA(equals(networkError)),
      );

      // Arrange - Not found error
      final notFoundError = Exception('Order not found');
      mockRepository.errorToThrow = notFoundError;

      // Act & Assert
      expect(
        () async => await useCase.call(sapOrderNumber: '999999999'),
        throwsA(equals(notFoundError)),
      );

      // Arrange - Unauthorized error
      final unauthorizedError = Exception('Unauthorized access');
      mockRepository.errorToThrow = unauthorizedError;

      // Act & Assert
      expect(
        () async => await useCase.call(sapOrderNumber: '300011396'),
        throwsA(equals(unauthorizedError)),
      );
    });

    test('should handle different SAP order number formats', () async {
      // Test numeric string
      const numericSapOrderNumber = '300011396';
      mockRepository.resultToReturn = ClientOrderDetail(
        sapOrderNumber: numericSapOrderNumber,
        clientId: 100,
        clientName: '천사푸드',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 612000000,
        orderedItemCount: 0,
        orderedItems: [],
      );

      await useCase.call(sapOrderNumber: numericSapOrderNumber);
      expect(mockRepository.lastSapOrderNumber, equals(numericSapOrderNumber));

      // Test alphanumeric string
      const alphanumericSapOrderNumber = 'SAP-2026-001';
      mockRepository.resultToReturn = ClientOrderDetail(
        sapOrderNumber: alphanumericSapOrderNumber,
        clientId: 200,
        clientName: '경산식품',
        orderDate: DateTime(2026, 2, 4),
        deliveryDate: DateTime(2026, 2, 7),
        totalApprovedAmount: 245000000,
        orderedItemCount: 0,
        orderedItems: [],
      );

      await useCase.call(sapOrderNumber: alphanumericSapOrderNumber);
      expect(mockRepository.lastSapOrderNumber, equals(alphanumericSapOrderNumber));
    });

    test('should handle large number of ordered items', () async {
      // Arrange
      const sapOrderNumber = '300011400';
      final largeItemsList = List.generate(
        100,
        (index) => ClientOrderItem(
          productCode: 'PROD${index.toString().padLeft(5, '0')}',
          productName: '제품 $index',
          deliveredQuantity: '${index + 1} BOX',
          deliveryStatus: DeliveryStatus.values[index % 3],
        ),
      );

      final expectedResult = ClientOrderDetail(
        sapOrderNumber: sapOrderNumber,
        clientId: 100,
        clientName: '천사푸드',
        clientDeadlineTime: '13:00',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 10000000,
        orderedItemCount: 100,
        orderedItems: largeItemsList,
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(sapOrderNumber: sapOrderNumber);

      // Assert
      expect(result.orderedItems.length, equals(100));
      expect(result.orderedItemCount, equals(100));
    });
  });
}
