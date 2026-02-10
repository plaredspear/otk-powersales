import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/presentation/providers/order_list_provider.dart';

void main() {
  group('OrderListNotifier', () {
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
      final state = container.read(orderListProvider);

      expect(state.orders, isEmpty);
      expect(state.clients, isEmpty);
      expect(state.isLoading, false);
      expect(state.isLoadingMore, false);
      expect(state.hasSearched, false);
      expect(state.selectedClientId, isNull);
      expect(state.selectedClientName, isNull);
      expect(state.selectedStatus, isNull);
      expect(state.deliveryDateFrom, isNotNull); // initial() sets 7-day range
      expect(state.deliveryDateTo, isNotNull);
      expect(state.sortType, OrderSortType.latestOrder);
      expect(state.currentPage, 0);
      expect(state.isLastPage, false);
      expect(state.totalElements, 0);
      expect(state.errorMessage, isNull);
    });

    test('initialize() loads clients and orders', () async {
      final notifier = container.read(orderListProvider.notifier);

      await notifier.initialize();

      final state = container.read(orderListProvider);
      expect(state.clients, isNotEmpty);
      expect(state.clients.length, 8); // mockClients has 8 clients
      expect(state.orders, isNotEmpty);
      expect(state.hasSearched, true);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
    });

    test('searchOrders() loads first page and sets hasSearched true', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Clear date filter to get all orders
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      final state = container.read(orderListProvider);
      expect(state.orders, isNotEmpty);
      expect(state.hasSearched, true);
      expect(state.currentPage, 0);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.totalElements, greaterThan(0));
    });

    test('searchOrders() with client filter', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Initialize to get clients
      await notifier.initialize();

      // Use clientId: 1 (천사푸드)
      notifier.updateClientFilter(1, '천사푸드');
      // Clear date range to get all orders for this client
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      final state = container.read(orderListProvider);
      expect(state.selectedClientId, 1);
      expect(state.selectedClientName, '천사푸드');
      expect(state.orders, isNotEmpty);

      // All orders should be from the selected client
      for (final order in state.orders) {
        expect(order.clientId, 1);
        expect(order.clientName, '천사푸드');
      }
    });

    test('searchOrders() with status filter', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Update status filter to APPROVED
      notifier.updateStatusFilter('APPROVED');
      // Clear date range to get all
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      final state = container.read(orderListProvider);
      expect(state.selectedStatus, 'APPROVED');
      expect(state.orders, isNotEmpty);

      // All orders should have approved status
      for (final order in state.orders) {
        expect(order.approvalStatus, ApprovalStatus.approved);
      }
    });

    test('searchOrders() with delivery date filter', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Update delivery date range
      notifier.updateDeliveryDateRange('2026-01-15', '2026-02-08');
      await notifier.searchOrders();

      final state = container.read(orderListProvider);
      expect(state.deliveryDateFrom, '2026-01-15');
      expect(state.deliveryDateTo, '2026-02-08');
      expect(state.orders, isNotEmpty);

      // All orders should be within the date range
      for (final order in state.orders) {
        expect(
          !order.deliveryDate.isBefore(DateTime(2026, 1, 15)) &&
              !order.deliveryDate.isAfter(DateTime(2026, 2, 8)),
          true,
          reason:
              'Order ${order.id} deliveryDate ${order.deliveryDate} out of range',
        );
      }
    });

    test('loadNextPage() appends results and updates page number', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Clear date filter and search with small page to ensure pagination
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      final firstPageState = container.read(orderListProvider);
      final firstPageOrdersCount = firstPageState.orders.length;

      // Only try if there is a next page
      if (!firstPageState.isLastPage) {
        await notifier.loadNextPage();

        final state = container.read(orderListProvider);
        expect(state.currentPage, 1);
        expect(state.orders.length, greaterThan(firstPageOrdersCount));
        expect(state.isLoadingMore, false);
        expect(state.errorMessage, isNull);
      }
    });

    test('loadNextPage() skips if already loading', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Clear date filter and initialize
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      final stateBefore = container.read(orderListProvider);

      // Manually set loading state
      container.read(orderListProvider.notifier).state =
          stateBefore.toLoading();

      // Try to load next page while loading
      await notifier.loadNextPage();

      // State should still be in loading state, page should not change
      final state = container.read(orderListProvider);
      expect(state.currentPage, 0);
    });

    test('loadNextPage() skips if last page', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Clear date filter and initialize
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      // Manually set to last page
      final stateBefore = container.read(orderListProvider);
      container.read(orderListProvider.notifier).state =
          stateBefore.copyWith(isLastPage: true);

      final ordersCountBefore = stateBefore.orders.length;

      // Try to load next page when already last page
      await notifier.loadNextPage();

      final state = container.read(orderListProvider);
      expect(state.orders.length, ordersCountBefore);
      expect(state.isLastPage, true);
    });

    test('updateClientFilter() sets client filter', () {
      final notifier = container.read(orderListProvider.notifier);

      notifier.updateClientFilter(100, '테스트 거래처');

      final state = container.read(orderListProvider);
      expect(state.selectedClientId, 100);
      expect(state.selectedClientName, '테스트 거래처');
    });

    test('updateClientFilter() clears client filter when null', () {
      final notifier = container.read(orderListProvider.notifier);

      // Set filter first
      notifier.updateClientFilter(100, '테스트 거래처');

      // Clear filter
      notifier.updateClientFilter(null, null);

      final state = container.read(orderListProvider);
      expect(state.selectedClientId, isNull);
      expect(state.selectedClientName, isNull);
    });

    test('updateStatusFilter() sets status filter', () {
      final notifier = container.read(orderListProvider.notifier);

      notifier.updateStatusFilter('APPROVED');

      final state = container.read(orderListProvider);
      expect(state.selectedStatus, 'APPROVED');
    });

    test('updateStatusFilter() clears status filter when null', () {
      final notifier = container.read(orderListProvider.notifier);

      // Set filter first
      notifier.updateStatusFilter('APPROVED');

      // Clear filter
      notifier.updateStatusFilter(null);

      final state = container.read(orderListProvider);
      expect(state.selectedStatus, isNull);
    });

    test('updateDeliveryDateRange() sets date range', () {
      final notifier = container.read(orderListProvider.notifier);

      notifier.updateDeliveryDateRange('2026-01-01', '2026-01-31');

      final state = container.read(orderListProvider);
      expect(state.deliveryDateFrom, '2026-01-01');
      expect(state.deliveryDateTo, '2026-01-31');
    });

    test('updateSortType() changes sort and re-searches', () async {
      final notifier = container.read(orderListProvider.notifier);

      // Clear date filter and initialize
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      // Change sort type
      await notifier.updateSortType(OrderSortType.amountHigh);

      final state = container.read(orderListProvider);
      expect(state.sortType, OrderSortType.amountHigh);
      expect(state.orders, isNotEmpty);
      expect(state.hasSearched, true);
      expect(state.currentPage, 0); // Should reset to first page

      // Verify sort order (descending totalAmount)
      for (var i = 0; i < state.orders.length - 1; i++) {
        expect(
          state.orders[i].totalAmount >= state.orders[i + 1].totalAmount,
          true,
          reason:
              'Order ${state.orders[i].id} amount ${state.orders[i].totalAmount} should be >= ${state.orders[i + 1].totalAmount}',
        );
      }
    });

    test('clearError() clears error message', () {
      final notifier = container.read(orderListProvider.notifier);

      // Manually set error state
      container.read(orderListProvider.notifier).state =
          container.read(orderListProvider).toError('Test error');

      expect(container.read(orderListProvider).errorMessage, 'Test error');

      // Clear error
      notifier.clearError();

      final state = container.read(orderListProvider);
      expect(state.errorMessage, isNull);
    });

    test('error handling - searchOrders with error', () async {
      // Create a mock repository that throws an error
      final errorRepository = _ErrorOrderRepository();

      final errorContainer = ProviderContainer(
        overrides: [
          orderRepositoryProvider.overrideWithValue(errorRepository),
        ],
      );

      final notifier = errorContainer.read(orderListProvider.notifier);

      await notifier.searchOrders();

      final state = errorContainer.read(orderListProvider);
      expect(state.errorMessage, isNotNull);
      expect(state.errorMessage, 'Failed to fetch orders');
      expect(state.isLoading, false);

      errorContainer.dispose();
    });

    test('full workflow: initialize → filter → search → sort → clear',
        () async {
      final notifier = container.read(orderListProvider.notifier);

      // Step 1: Initialize
      await notifier.initialize();
      var state = container.read(orderListProvider);
      expect(state.clients, isNotEmpty);
      expect(state.orders, isNotEmpty);
      expect(state.hasSearched, true);

      // Step 2: Apply filters
      notifier.updateClientFilter(1, '천사푸드');
      notifier.updateStatusFilter('APPROVED');
      notifier.updateDeliveryDateRange('2026-01-01', '2026-02-28');

      // Step 3: Search with filters
      await notifier.searchOrders();
      state = container.read(orderListProvider);
      expect(state.selectedClientId, 1);
      expect(state.selectedStatus, 'APPROVED');
      expect(state.deliveryDateFrom, '2026-01-01');
      expect(state.deliveryDateTo, '2026-02-28');
      expect(state.currentPage, 0);
      expect(state.orders, isNotEmpty);
      for (final order in state.orders) {
        expect(order.clientId, 1);
        expect(order.approvalStatus, ApprovalStatus.approved);
      }

      // Step 4: Change sort
      await notifier.updateSortType(OrderSortType.oldestDelivery);
      state = container.read(orderListProvider);
      expect(state.sortType, OrderSortType.oldestDelivery);
      expect(state.currentPage, 0); // Should reset to first page

      // Step 5: Clear filters
      notifier.updateClientFilter(null, null);
      notifier.updateStatusFilter(null);

      state = container.read(orderListProvider);
      expect(state.selectedClientId, isNull);
      expect(state.selectedStatus, isNull);
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
  }) async {
    throw Exception('Failed to fetch orders');
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
}
