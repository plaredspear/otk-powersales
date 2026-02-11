import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/get_order_detail.dart';

/// Mock OrderRepository for testing GetOrderDetail
class _MockOrderRepository implements OrderRepository {
  OrderDetail? resultToReturn;
  Object? errorToThrow;

  // Capture call parameters
  int? lastOrderId;

  @override
  Future<OrderDetail> getOrderDetail({required int orderId}) async {
    // Capture parameters
    lastOrderId = orderId;

    // Throw error if set
    if (errorToThrow != null) {
      throw errorToThrow!;
    }

    // Return result if set
    if (resultToReturn != null) {
      return resultToReturn!;
    }

    // Default test data
    return OrderDetail(
      id: orderId,
      orderRequestNumber: 'OP00000001',
      clientId: 1,
      clientName: '테스트거래처',
      orderDate: DateTime(2026, 2, 10),
      deliveryDate: DateTime(2026, 2, 11),
      totalAmount: 100000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: false,
      orderedItemCount: 1,
      orderedItems: const [
        OrderedItem(
          productCode: '01101123',
          productName: '갈릭 아이올리소스',
          totalQuantityBoxes: 5,
          totalQuantityPieces: 100,
          isCancelled: false,
        ),
      ],
    );
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
  late _MockOrderRepository mockRepository;
  late GetOrderDetail useCase;

  setUp(() {
    mockRepository = _MockOrderRepository();
    useCase = GetOrderDetail(mockRepository);
  });

  group('GetOrderDetail', () {
    test('should call repository with correct orderId', () async {
      // Arrange
      const testOrderId = 123;
      final expectedResult = OrderDetail(
        id: testOrderId,
        orderRequestNumber: 'OP00000001',
        clientId: 1,
        clientName: '테스트거래처',
        orderDate: DateTime(2026, 2, 10),
        deliveryDate: DateTime(2026, 2, 11),
        totalAmount: 100000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: false,
        orderedItemCount: 1,
        orderedItems: const [
          OrderedItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스',
            totalQuantityBoxes: 5,
            totalQuantityPieces: 100,
            isCancelled: false,
          ),
        ],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      await useCase.call(orderId: testOrderId);

      // Assert
      expect(mockRepository.lastOrderId, equals(testOrderId));
    });

    test('should return OrderDetail from repository', () async {
      // Arrange
      const testOrderId = 456;
      final expectedResult = OrderDetail(
        id: testOrderId,
        orderRequestNumber: 'OP00000002',
        clientId: 2,
        clientName: '거래처2',
        clientDeadlineTime: '15:00',
        orderDate: DateTime(2026, 2, 9),
        deliveryDate: DateTime(2026, 2, 12),
        totalAmount: 500000,
        totalApprovedAmount: 480000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: true,
        orderedItemCount: 2,
        orderedItems: const [
          OrderedItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스',
            totalQuantityBoxes: 5,
            totalQuantityPieces: 100,
            isCancelled: false,
          ),
          OrderedItem(
            productCode: '02201234',
            productName: '케첩',
            totalQuantityBoxes: 3,
            totalQuantityPieces: 50,
            isCancelled: true,
          ),
        ],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(orderId: testOrderId);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.id, equals(testOrderId));
      expect(result.orderRequestNumber, equals('OP00000002'));
      expect(result.clientName, equals('거래처2'));
      expect(result.totalAmount, equals(500000));
      expect(result.orderedItemCount, equals(2));
      expect(result.orderedItems.length, equals(2));
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

    test('should return OrderDetail with processing status when closed', () async {
      // Arrange
      const testOrderId = 111;
      final expectedResult = OrderDetail(
        id: testOrderId,
        orderRequestNumber: 'OP00000003',
        clientId: 3,
        clientName: '거래처3',
        orderDate: DateTime(2026, 2, 8),
        deliveryDate: DateTime(2026, 2, 11),
        totalAmount: 300000,
        totalApprovedAmount: 300000,
        approvalStatus: ApprovalStatus.approved,
        isClosed: true,
        orderedItemCount: 1,
        orderedItems: const [
          OrderedItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스',
            totalQuantityBoxes: 5,
            totalQuantityPieces: 100,
            isCancelled: false,
          ),
        ],
        orderProcessingStatus: const OrderProcessingStatus(
          sapOrderNumber: '0300013650',
          items: [
            ProcessingItem(
              productCode: '01101123',
              productName: '갈릭 아이올리소스',
              deliveredQuantity: '100 EA',
              deliveryStatus: DeliveryStatus.delivered,
            ),
          ],
        ),
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(orderId: testOrderId);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.isClosed, isTrue);
      expect(result.orderProcessingStatus, isNotNull);
      expect(result.orderProcessingStatus?.sapOrderNumber, equals('0300013650'));
      expect(result.orderProcessingStatus?.items.length, equals(1));
    });

    test('should return OrderDetail with rejected items when present', () async {
      // Arrange
      const testOrderId = 222;
      final expectedResult = OrderDetail(
        id: testOrderId,
        orderRequestNumber: 'OP00000004',
        clientId: 4,
        clientName: '거래처4',
        orderDate: DateTime(2026, 2, 7),
        deliveryDate: DateTime(2026, 2, 10),
        totalAmount: 400000,
        totalApprovedAmount: 300000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: true,
        orderedItemCount: 2,
        orderedItems: const [
          OrderedItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스',
            totalQuantityBoxes: 5,
            totalQuantityPieces: 100,
            isCancelled: false,
          ),
          OrderedItem(
            productCode: '02201234',
            productName: '케첩',
            totalQuantityBoxes: 3,
            totalQuantityPieces: 50,
            isCancelled: true,
          ),
        ],
        rejectedItems: const [
          RejectedItem(
            productCode: '02201234',
            productName: '케첩',
            orderQuantityBoxes: 3,
            rejectionReason: '재고 부족',
          ),
        ],
      );
      mockRepository.resultToReturn = expectedResult;

      // Act
      final result = await useCase.call(orderId: testOrderId);

      // Assert
      expect(result, equals(expectedResult));
      expect(result.hasRejectedItems, isTrue);
      expect(result.rejectedItems, isNotNull);
      expect(result.rejectedItems!.length, equals(1));
      expect(result.rejectedItems![0].rejectionReason, equals('재고 부족'));
    });
  });
}
