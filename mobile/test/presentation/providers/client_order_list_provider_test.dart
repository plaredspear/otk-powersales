import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/presentation/providers/client_order_list_provider.dart';
import 'package:mobile/presentation/providers/order_list_provider.dart';

void main() {
  group('ClientOrderListNotifier', () {
    late ProviderContainer container;
    late OrderMockRepository mockRepository;

    setUp(() {
      mockRepository = OrderMockRepository();
      container = ProviderContainer(
        overrides: [
          orderRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('initial state has correct defaults', () {
      final state = container.read(clientOrderListProvider);

      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.orders, isEmpty);
      expect(state.totalElements, 0);
      expect(state.totalPages, 0);
      expect(state.currentPage, 0);
      expect(state.isFirst, true);
      expect(state.isLast, true);
      expect(state.hasSearched, false);
      expect(state.selectedStoreId, isNull);
      expect(state.selectedStoreName, isNull);
      expect(state.selectedDeliveryDate, isNotNull);
      expect(state.stores, isEmpty);
      expect(state.canSearch, false);
    });

    test('initialize() loads store data', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      await notifier.initialize();

      final state = container.read(clientOrderListProvider);
      expect(state.stores, isNotEmpty);
      expect(state.stores.length, 8); // mockClients has 8 stores
      expect(state.stores[1], '천사푸드');
      expect(state.stores[2], '(유)경산식품');
    });

    test('selectStore() sets store filter', () {
      final notifier = container.read(clientOrderListProvider.notifier);

      notifier.selectStore(2, '(유)경산식품');

      final state = container.read(clientOrderListProvider);
      expect(state.selectedStoreId, 2);
      expect(state.selectedStoreName, '(유)경산식품');
      expect(state.canSearch, true);
    });

    test('selectStore(null) clears store filter', () {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Set filter first
      notifier.selectStore(2, '(유)경산식품');
      expect(container.read(clientOrderListProvider).selectedStoreId, 2);

      // Clear filter
      notifier.selectStore(null, null);

      final state = container.read(clientOrderListProvider);
      expect(state.selectedStoreId, isNull);
      expect(state.selectedStoreName, isNull);
      expect(state.canSearch, false);
    });

    test('updateDeliveryDate() sets date', () {
      final notifier = container.read(clientOrderListProvider.notifier);

      notifier.updateDeliveryDate('2026-02-15');

      final state = container.read(clientOrderListProvider);
      expect(state.selectedDeliveryDate, '2026-02-15');
    });

    test('searchOrders() skips if no store selected', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Should skip without throwing error
      await notifier.searchOrders();

      final state = container.read(clientOrderListProvider);
      expect(state.hasSearched, false);
      expect(state.orders, isEmpty);
    });

    test('searchOrders() loads orders for selected store', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Initialize to get stores
      await notifier.initialize();

      // Select store 2 ((유)경산식품)
      notifier.selectStore(2, '(유)경산식품');
      notifier.updateDeliveryDate('2026-02-08');

      await notifier.searchOrders();

      final state = container.read(clientOrderListProvider);
      expect(state.orders, isNotEmpty);
      expect(state.hasSearched, true);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);

      // All orders should be from clientId 2
      for (final order in state.orders) {
        expect(order.clientId, 2);
      }
    });

    test('searchOrders() sets hasSearched true', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Initialize and select a store
      await notifier.initialize();
      notifier.selectStore(1, '천사푸드');

      expect(container.read(clientOrderListProvider).hasSearched, false);

      await notifier.searchOrders();

      final state = container.read(clientOrderListProvider);
      expect(state.hasSearched, true);
    });

    test('goToPage() navigates to specific page', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Initialize and select store
      await notifier.initialize();
      notifier.selectStore(1, '천사푸드');
      await notifier.searchOrders();

      final firstPageState = container.read(clientOrderListProvider);

      // Only try to go to next page if there is one
      if (!firstPageState.isLast) {
        await notifier.goToPage(1);

        final state = container.read(clientOrderListProvider);
        expect(state.currentPage, 1);
        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
      }
    });

    test('goToPage() skips if no store selected', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Should skip without throwing error
      await notifier.goToPage(1);

      final state = container.read(clientOrderListProvider);
      expect(state.currentPage, 0);
      expect(state.orders, isEmpty);
    });

    test('clearError() clears error message', () {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Manually set error state
      container.read(clientOrderListProvider.notifier).state =
          container.read(clientOrderListProvider).toError('Test error');

      expect(container.read(clientOrderListProvider).errorMessage, 'Test error');

      // Clear error
      notifier.clearError();

      final state = container.read(clientOrderListProvider);
      expect(state.errorMessage, isNull);
    });

    test('canSearch getter', () {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Initially false (no store selected)
      expect(container.read(clientOrderListProvider).canSearch, false);

      // True after selecting store
      notifier.selectStore(1, '천사푸드');
      expect(container.read(clientOrderListProvider).canSearch, true);

      // False after clearing store
      notifier.selectStore(null, null);
      expect(container.read(clientOrderListProvider).canSearch, false);
    });

    test('error handling - searchOrders with error', () async {
      // Create a mock repository that throws an error
      final errorRepository = _ErrorOrderRepository();

      final errorContainer = ProviderContainer(
        overrides: [
          orderRepositoryProvider.overrideWithValue(errorRepository),
        ],
      );

      final notifier = errorContainer.read(clientOrderListProvider.notifier);

      // Select a store to trigger search
      notifier.selectStore(1, '천사푸드');
      await notifier.searchOrders();

      final state = errorContainer.read(clientOrderListProvider);
      expect(state.errorMessage, isNotNull);
      expect(state.errorMessage, 'Failed to fetch client orders');
      expect(state.isLoading, false);

      errorContainer.dispose();
    });

    test('full workflow: initialize → selectStore → search → goToPage → clearStore',
        () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Step 1: Initialize
      await notifier.initialize();
      var state = container.read(clientOrderListProvider);
      expect(state.stores, isNotEmpty);
      expect(state.stores.length, 8);

      // Step 2: Select store
      notifier.selectStore(2, '(유)경산식품');
      state = container.read(clientOrderListProvider);
      expect(state.selectedStoreId, 2);
      expect(state.selectedStoreName, '(유)경산식품');
      expect(state.canSearch, true);

      // Step 3: Update delivery date
      notifier.updateDeliveryDate('2026-02-08');
      state = container.read(clientOrderListProvider);
      expect(state.selectedDeliveryDate, '2026-02-08');

      // Step 4: Search
      await notifier.searchOrders();
      state = container.read(clientOrderListProvider);
      expect(state.orders, isNotEmpty);
      expect(state.hasSearched, true);
      expect(state.currentPage, 0);
      for (final order in state.orders) {
        expect(order.clientId, 2);
      }

      // Step 5: Go to page (if available)
      final ordersOnFirstPage = state.orders.length;
      if (!state.isLast && state.totalPages > 1) {
        await notifier.goToPage(1);
        state = container.read(clientOrderListProvider);
        expect(state.currentPage, 1);
        expect(state.orders, isNotEmpty);
      }

      // Step 6: Clear store filter
      notifier.selectStore(null, null);
      state = container.read(clientOrderListProvider);
      expect(state.selectedStoreId, isNull);
      expect(state.selectedStoreName, isNull);
      expect(state.canSearch, false);
    });

    test('hasResults getter returns correct values', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Initially no results
      expect(container.read(clientOrderListProvider).hasResults, false);

      // After search, has results
      await notifier.initialize();
      notifier.selectStore(1, '천사푸드');
      await notifier.searchOrders();

      final state = container.read(clientOrderListProvider);
      if (state.orders.isNotEmpty) {
        expect(state.hasResults, true);
      }
    });

    test('isEmpty getter returns correct values', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      // Initially not isEmpty (because hasSearched is false)
      expect(container.read(clientOrderListProvider).isEmpty, false);

      // After search with results, not isEmpty
      await notifier.initialize();
      notifier.selectStore(1, '천사푸드');
      await notifier.searchOrders();

      var state = container.read(clientOrderListProvider);
      if (state.orders.isNotEmpty) {
        expect(state.isEmpty, false);
      }

      // If we search for a non-existent date, might be empty
      notifier.updateDeliveryDate('2020-01-01');
      await notifier.searchOrders();
      state = container.read(clientOrderListProvider);
      if (state.orders.isEmpty) {
        expect(state.isEmpty, true);
      }
    });

    test('hasNextPage and hasPreviousPage getters', () async {
      final notifier = container.read(clientOrderListProvider.notifier);

      await notifier.initialize();
      notifier.selectStore(1, '천사푸드');
      await notifier.searchOrders();

      final state = container.read(clientOrderListProvider);

      // First page should have no previous page
      expect(state.hasPreviousPage, false);

      // hasNextPage depends on data
      expect(state.hasNextPage, !state.isLast);
    });
  });
}

/// Mock repository that always throws an error for testing error handling
class _ErrorOrderRepository implements OrderRepository {
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
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async {
    throw Exception('Failed to fetch client orders');
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
