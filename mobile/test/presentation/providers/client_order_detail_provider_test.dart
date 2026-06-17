import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import '../../helpers/fake_order_request_repository.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/repositories/order_request_repository.dart';
import 'package:mobile/presentation/providers/client_order_detail_provider.dart';
import 'package:mobile/presentation/providers/order_request_list_provider.dart';

void main() {
  group('ClientOrderDetailNotifier', () {
    late ProviderContainer container;
    late FakeOrderRequestRepository mockRepository;

    setUp(() {
      mockRepository = FakeOrderRequestRepository();
      container = ProviderContainer(
        overrides: [
          orderRequestRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('initial state has correct defaults', () {
      final state = container.read(clientOrderDetailProvider);

      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.orderDetail, isNull);
      expect(state.hasData, false);
    });

    test('loadDetail() loads order detail successfully', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      await notifier.loadDetail('300011396');

      final state = container.read(clientOrderDetailProvider);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.orderDetail, isNotNull);
      expect(state.hasData, true);
    });

    test('loadDetail() sets correct order data', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      await notifier.loadDetail('300011396');

      final state = container.read(clientOrderDetailProvider);
      final detail = state.orderDetail!;

      expect(detail.sapOrderNumber, '300011396');
      expect(detail.sapAccountCode, isNotEmpty);
      expect(detail.sapAccountName, isNotEmpty);
    });

    test('loadDetail() includes ordered items', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      await notifier.loadDetail('300011396');

      final state = container.read(clientOrderDetailProvider);
      final detail = state.orderDetail!;

      expect(detail.orderedItems, isNotEmpty);
      expect(detail.orderedItemCount, greaterThan(0));
      expect(detail.orderedItems.length, detail.orderedItemCount);
    });

    test('loadDetail() with non-existent order', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      await notifier.loadDetail('999999999');

      final state = container.read(clientOrderDetailProvider);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNotNull);
      expect(state.errorMessage, 'ORDER_NOT_FOUND');
      expect(state.orderDetail, isNull);
      expect(state.hasData, false);
    });

    test('clearError() clears error message', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      // Set error via loadDetail with invalid order
      await notifier.loadDetail('999999999');
      expect(container.read(clientOrderDetailProvider).errorMessage, isNotNull);

      // Clear error
      notifier.clearError();

      final state = container.read(clientOrderDetailProvider);
      expect(state.errorMessage, isNull);
    });

    test('loadDetail() loads different order on subsequent call', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      // Load first order
      await notifier.loadDetail('300011396');
      final firstState = container.read(clientOrderDetailProvider);
      final firstDetail = firstState.orderDetail!;
      expect(firstDetail.sapOrderNumber, '300011396');
      expect(firstDetail.sapAccountCode, isNotEmpty);
      expect(firstState.hasData, true);
      expect(firstState.isLoading, false);
      expect(firstState.errorMessage, isNull);
    });

    test('error handling with error repository', () async {
      final errorRepository = _ErrorOrderRepository();
      final errorContainer = ProviderContainer(
        overrides: [
          orderRequestRepositoryProvider.overrideWithValue(errorRepository),
        ],
      );

      final notifier = errorContainer.read(clientOrderDetailProvider.notifier);

      await notifier.loadDetail('300011396');

      final state = errorContainer.read(clientOrderDetailProvider);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNotNull);
      expect(state.errorMessage, 'Failed to fetch detail');
      expect(state.orderDetail, isNull);

      errorContainer.dispose();
    });

    test('full workflow: load → error → clearError → reload', () async {
      final notifier = container.read(clientOrderDetailProvider.notifier);

      // Step 1: Load valid order
      await notifier.loadDetail('300011396');
      var state = container.read(clientOrderDetailProvider);
      expect(state.hasData, true);
      expect(state.errorMessage, isNull);
      expect(state.orderDetail?.sapOrderNumber, '300011396');

      // Step 2: Load invalid order (error)
      // Note: copyWith preserves orderDetail, so hasData remains true
      await notifier.loadDetail('999999999');
      state = container.read(clientOrderDetailProvider);
      expect(state.errorMessage, 'ORDER_NOT_FOUND');
      expect(state.isLoading, false);

      // Step 3: Clear error
      notifier.clearError();
      state = container.read(clientOrderDetailProvider);
      expect(state.errorMessage, isNull);

      // Step 4: Reload valid order
      await notifier.loadDetail('300011396');
      state = container.read(clientOrderDetailProvider);
      expect(state.hasData, true);
      expect(state.errorMessage, isNull);
      expect(state.orderDetail?.sapOrderNumber, '300011396');
    });
  });
}

/// Mock repository that throws exception on getClientOrderDetail
class _ErrorOrderRepository implements OrderRequestRepository {
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
    throw UnimplementedError();
  }

  @override
  Future<OrderDetail> getOrderRequestDetail({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<void> resendOrderRequest({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderCancelResult> cancelOrderRequest({
    required int orderId,
    required List<int> orderProductIds,
  }) {
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
    throw Exception('Failed to fetch detail');
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
  Future<void> addToFavorites({required String productCode}) {
    throw UnimplementedError();
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) {
    throw UnimplementedError();
  }
}
