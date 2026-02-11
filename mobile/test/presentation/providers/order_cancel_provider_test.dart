import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/cancel_order_usecase.dart';
import 'package:mobile/presentation/providers/order_cancel_provider.dart';
import 'package:mobile/presentation/providers/order_cancel_state.dart';
import 'package:mobile/presentation/providers/order_list_provider.dart';

// --- Test Data ---

final _item1 = OrderedItem(
  productCode: '01101123',
  productName: '갈릭 아이올리소스 240g',
  totalQuantityBoxes: 5,
  totalQuantityPieces: 100,
  isCancelled: false,
);

final _item2 = OrderedItem(
  productCode: '01101456',
  productName: '토마토 케첩 500g',
  totalQuantityBoxes: 10,
  totalQuantityPieces: 200,
  isCancelled: false,
);

final _cancelledItem = OrderedItem(
  productCode: '01101789',
  productName: '마요네즈 300g',
  totalQuantityBoxes: 3,
  totalQuantityPieces: 60,
  isCancelled: true,
);

// --- Mock Repository ---

class _MockOrderRepository implements OrderRepository {
  OrderCancelResult? cancelResult;
  Exception? errorToThrow;
  int cancelCallCount = 0;
  int? lastOrderId;
  List<String>? lastProductCodes;

  @override
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) async {
    cancelCallCount++;
    lastOrderId = orderId;
    lastProductCodes = productCodes;

    if (errorToThrow != null) throw errorToThrow!;

    return cancelResult ??
        OrderCancelResult(
          cancelledCount: productCodes.length,
          cancelledProductCodes: productCodes,
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
    throw UnimplementedError();
  }

  @override
  Future<OrderDetail> getOrderDetail({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<void> resendOrder({required int orderId}) {
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
  Future<int> getCreditBalance({required int clientId}) {
    throw UnimplementedError();
  }

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() {
    throw UnimplementedError();
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) {
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
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) {
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
}

void main() {
  group('OrderCancelNotifier', () {
    late _MockOrderRepository mockRepository;
    late ProviderContainer container;
    late OrderCancelParams params;

    setUp(() {
      mockRepository = _MockOrderRepository();
      params = OrderCancelParams(
        orderId: 1,
        allItems: [_item1, _cancelledItem, _item2],
      );

      container = ProviderContainer(
        overrides: [
          orderRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('initial state filters out cancelled items', () {
      final state = container.read(orderCancelProvider(params));

      expect(state.orderId, 1);
      expect(state.cancellableItems.length, 2);
      expect(state.selectedProductCodes, isEmpty);
      expect(state.isCancelling, false);
      expect(state.cancelSuccess, false);
      expect(state.errorMessage, isNull);
    });

    test('toggleProduct selects an unselected product', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleProduct('01101123');

      final state = container.read(orderCancelProvider(params));
      expect(state.selectedProductCodes, {'01101123'});
      expect(state.selectedCount, 1);
    });

    test('toggleProduct deselects a selected product', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleProduct('01101123');
      notifier.toggleProduct('01101123');

      final state = container.read(orderCancelProvider(params));
      expect(state.selectedProductCodes, isEmpty);
      expect(state.selectedCount, 0);
    });

    test('toggleProduct can select multiple products', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleProduct('01101123');
      notifier.toggleProduct('01101456');

      final state = container.read(orderCancelProvider(params));
      expect(state.selectedProductCodes, {'01101123', '01101456'});
      expect(state.selectedCount, 2);
      expect(state.isAllSelected, true);
    });

    test('toggleSelectAll selects all when none selected', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleSelectAll();

      final state = container.read(orderCancelProvider(params));
      expect(state.selectedProductCodes, {'01101123', '01101456'});
      expect(state.isAllSelected, true);
    });

    test('toggleSelectAll deselects all when all selected', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleSelectAll(); // Select all
      notifier.toggleSelectAll(); // Deselect all

      final state = container.read(orderCancelProvider(params));
      expect(state.selectedProductCodes, isEmpty);
      expect(state.isAllSelected, false);
    });

    test('toggleSelectAll selects all when partially selected', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleProduct('01101123'); // Select one
      notifier.toggleSelectAll(); // Should select all

      final state = container.read(orderCancelProvider(params));
      expect(state.selectedProductCodes, {'01101123', '01101456'});
      expect(state.isAllSelected, true);
    });

    test('cancelOrder returns false when canCancel is false', () async {
      final notifier = container.read(orderCancelProvider(params).notifier);

      // No products selected → canCancel is false
      final result = await notifier.cancelOrder();

      expect(result, false);
      expect(mockRepository.cancelCallCount, 0);
    });

    test('cancelOrder succeeds with selected products', () async {
      final notifier = container.read(orderCancelProvider(params).notifier);

      notifier.toggleProduct('01101123');
      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, true);
      expect(state.cancelSuccess, true);
      expect(state.isCancelling, false);
      expect(state.errorMessage, isNull);
      expect(mockRepository.lastOrderId, 1);
      expect(mockRepository.lastProductCodes, ['01101123']);
    });

    test('cancelOrder handles ALREADY_CANCELLED error', () async {
      mockRepository.errorToThrow = Exception('ALREADY_CANCELLED');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.cancelSuccess, false);
      expect(state.isCancelling, false);
      expect(state.errorMessage, '이미 취소된 제품이 포함되어 있습니다');
    });

    test('cancelOrder handles ORDER_ALREADY_CLOSED error', () async {
      mockRepository.errorToThrow = Exception('ORDER_ALREADY_CLOSED');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '마감된 주문은 취소할 수 없습니다');
    });

    test('cancelOrder handles ORDER_NOT_FOUND error', () async {
      mockRepository.errorToThrow = Exception('ORDER_NOT_FOUND');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '주문을 찾을 수 없습니다');
    });

    test('cancelOrder handles INVALID_PARAMETER error', () async {
      mockRepository.errorToThrow = Exception('INVALID_PARAMETER');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '취소할 제품을 선택해주세요');
    });

    test('cancelOrder handles UNAUTHORIZED error', () async {
      mockRepository.errorToThrow = Exception('UNAUTHORIZED');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '인증이 만료되었습니다. 다시 로그인해주세요');
    });

    test('cancelOrder handles FORBIDDEN error', () async {
      mockRepository.errorToThrow = Exception('FORBIDDEN');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '접근 권한이 없습니다');
    });

    test('cancelOrder handles SERVER_ERROR error', () async {
      mockRepository.errorToThrow = Exception('SERVER_ERROR');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요');
    });

    test('cancelOrder handles network error', () async {
      mockRepository.errorToThrow = Exception('네트워크 연결 오류');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, '네트워크 연결을 확인해주세요');
    });

    test('cancelOrder handles unknown error', () async {
      mockRepository.errorToThrow = Exception('Unknown error');

      final notifier = container.read(orderCancelProvider(params).notifier);
      notifier.toggleProduct('01101123');

      final result = await notifier.cancelOrder();

      final state = container.read(orderCancelProvider(params));
      expect(result, false);
      expect(state.errorMessage, 'Unknown error');
    });

    test('clearError clears the error message', () {
      final notifier = container.read(orderCancelProvider(params).notifier);

      // Manually set error
      notifier.state = notifier.state.toError('test error');
      expect(container.read(orderCancelProvider(params)).errorMessage,
          'test error');

      notifier.clearError();

      final state = container.read(orderCancelProvider(params));
      expect(state.errorMessage, isNull);
    });

    test('full workflow: select → cancel → success', () async {
      final notifier = container.read(orderCancelProvider(params).notifier);

      // Step 1: Select products
      notifier.toggleSelectAll();
      var state = container.read(orderCancelProvider(params));
      expect(state.isAllSelected, true);
      expect(state.selectedCount, 2);
      expect(state.canCancel, true);

      // Step 2: Cancel
      final result = await notifier.cancelOrder();

      // Step 3: Verify
      state = container.read(orderCancelProvider(params));
      expect(result, true);
      expect(state.cancelSuccess, true);
      expect(state.isCancelling, false);
      expect(mockRepository.cancelCallCount, 1);
      expect(mockRepository.lastOrderId, 1);
      expect(
        mockRepository.lastProductCodes!.toSet(),
        {'01101123', '01101456'},
      );
    });
  });

  group('OrderCancelParams', () {
    test('equality based on orderId', () {
      final params1 = OrderCancelParams(
        orderId: 1,
        allItems: [_item1],
      );
      final params2 = OrderCancelParams(
        orderId: 1,
        allItems: [_item1, _item2],
      );

      expect(params1, equals(params2));
      expect(params1.hashCode, equals(params2.hashCode));
    });

    test('inequality based on different orderId', () {
      final params1 = OrderCancelParams(
        orderId: 1,
        allItems: [_item1],
      );
      final params2 = OrderCancelParams(
        orderId: 2,
        allItems: [_item1],
      );

      expect(params1, isNot(equals(params2)));
    });
  });
}
