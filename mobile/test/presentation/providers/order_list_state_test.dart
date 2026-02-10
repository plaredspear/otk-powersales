import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/presentation/providers/order_list_state.dart';

void main() {
  group('OrderListState', () {
    group('initial()', () {
      test('should set default values correctly', () {
        final state = OrderListState.initial();

        expect(state.isLoading, false);
        expect(state.isLoadingMore, false);
        expect(state.errorMessage, null);
        expect(state.orders, isEmpty);
        expect(state.totalElements, 0);
        expect(state.currentPage, 0);
        expect(state.isLastPage, false);
        expect(state.hasSearched, false);
        expect(state.selectedClientId, null);
        expect(state.selectedClientName, null);
        expect(state.selectedStatus, null);
        expect(state.sortType, OrderSortType.latestOrder);
        expect(state.clients, isEmpty);
      });

      test('should set deliveryDateFrom to 7 days ago', () {
        final state = OrderListState.initial();
        final now = DateTime.now();
        final sevenDaysAgo = now.subtract(const Duration(days: 7));
        final expectedFrom = '${sevenDaysAgo.year}-${sevenDaysAgo.month.toString().padLeft(2, '0')}-${sevenDaysAgo.day.toString().padLeft(2, '0')}';

        expect(state.deliveryDateFrom, expectedFrom);
      });

      test('should set deliveryDateTo to today', () {
        final state = OrderListState.initial();
        final now = DateTime.now();
        final expectedTo = '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

        expect(state.deliveryDateTo, expectedTo);
      });
    });

    group('toLoading()', () {
      test('should set isLoading to true', () {
        const state = OrderListState();
        final loadingState = state.toLoading();

        expect(loadingState.isLoading, true);
      });

      test('should clear errorMessage', () {
        const state = OrderListState(errorMessage: 'Previous error');
        final loadingState = state.toLoading();

        expect(loadingState.errorMessage, null);
      });

      test('should preserve other fields', () {
        final orders = [
          Order(
            id: 1,
            orderRequestNumber: 'OP00000074',
            clientId: 1,
            clientName: '천사푸드',
            orderDate: DateTime(2026, 2, 5),
            deliveryDate: DateTime(2026, 2, 8),
            totalAmount: 612000000,
            approvalStatus: ApprovalStatus.approved,
            isClosed: true,
          ),
        ];
        final state = OrderListState(
          orders: orders,
          totalElements: 10,
          currentPage: 1,
          selectedClientId: 123,
        );
        final loadingState = state.toLoading();

        expect(loadingState.orders, orders);
        expect(loadingState.totalElements, 10);
        expect(loadingState.currentPage, 1);
        expect(loadingState.selectedClientId, 123);
      });
    });

    group('toLoadingMore()', () {
      test('should set isLoadingMore to true', () {
        const state = OrderListState();
        final loadingMoreState = state.toLoadingMore();

        expect(loadingMoreState.isLoadingMore, true);
      });

      test('should clear errorMessage', () {
        const state = OrderListState(errorMessage: 'Previous error');
        final loadingMoreState = state.toLoadingMore();

        expect(loadingMoreState.errorMessage, null);
      });

      test('should preserve other fields', () {
        final orders = [
          Order(
            id: 1,
            orderRequestNumber: 'OP00000074',
            clientId: 1,
            clientName: '천사푸드',
            orderDate: DateTime(2026, 2, 5),
            deliveryDate: DateTime(2026, 2, 8),
            totalAmount: 612000000,
            approvalStatus: ApprovalStatus.approved,
            isClosed: true,
          ),
        ];
        final state = OrderListState(
          orders: orders,
          currentPage: 1,
          isLoading: true,
        );
        final loadingMoreState = state.toLoadingMore();

        expect(loadingMoreState.orders, orders);
        expect(loadingMoreState.currentPage, 1);
        expect(loadingMoreState.isLoading, true);
      });
    });

    group('toError()', () {
      test('should set errorMessage', () {
        const state = OrderListState();
        final errorState = state.toError('Network error');

        expect(errorState.errorMessage, 'Network error');
      });

      test('should set isLoading to false', () {
        const state = OrderListState(isLoading: true);
        final errorState = state.toError('Error occurred');

        expect(errorState.isLoading, false);
      });

      test('should set isLoadingMore to false', () {
        const state = OrderListState(isLoadingMore: true);
        final errorState = state.toError('Error occurred');

        expect(errorState.isLoadingMore, false);
      });

      test('should set both loading flags to false', () {
        const state = OrderListState(isLoading: true, isLoadingMore: true);
        final errorState = state.toError('Error occurred');

        expect(errorState.isLoading, false);
        expect(errorState.isLoadingMore, false);
      });

      test('should preserve other fields', () {
        final orders = [
          Order(
            id: 1,
            orderRequestNumber: 'OP00000074',
            clientId: 1,
            clientName: '천사푸드',
            orderDate: DateTime(2026, 2, 5),
            deliveryDate: DateTime(2026, 2, 8),
            totalAmount: 612000000,
            approvalStatus: ApprovalStatus.approved,
            isClosed: true,
          ),
        ];
        final state = OrderListState(
          orders: orders,
          totalElements: 5,
          selectedStatus: 'APPROVED',
        );
        final errorState = state.toError('Error');

        expect(errorState.orders, orders);
        expect(errorState.totalElements, 5);
        expect(errorState.selectedStatus, 'APPROVED');
      });
    });

    group('hasResults', () {
      test('should return true when orders is not empty', () {
        final state = OrderListState(
          orders: [
            Order(
              id: 1,
              orderRequestNumber: 'OP00000074',
              clientId: 1,
              clientName: '천사푸드',
              orderDate: DateTime(2026, 2, 5),
              deliveryDate: DateTime(2026, 2, 8),
              totalAmount: 612000000,
              approvalStatus: ApprovalStatus.approved,
              isClosed: true,
            ),
          ],
        );

        expect(state.hasResults, true);
      });

      test('should return false when orders is empty', () {
        const state = OrderListState(orders: []);

        expect(state.hasResults, false);
      });
    });

    group('isEmpty', () {
      test('should return true when hasSearched is true and orders is empty', () {
        const state = OrderListState(hasSearched: true, orders: []);

        expect(state.isEmpty, true);
      });

      test('should return false when hasSearched is false', () {
        const state = OrderListState(hasSearched: false, orders: []);

        expect(state.isEmpty, false);
      });

      test('should return false when orders is not empty', () {
        final state = OrderListState(
          hasSearched: true,
          orders: [
            Order(
              id: 1,
              orderRequestNumber: 'OP00000074',
              clientId: 1,
              clientName: '천사푸드',
              orderDate: DateTime(2026, 2, 5),
              deliveryDate: DateTime(2026, 2, 8),
              totalAmount: 612000000,
              approvalStatus: ApprovalStatus.approved,
              isClosed: true,
            ),
          ],
        );

        expect(state.isEmpty, false);
      });
    });

    group('hasNextPage', () {
      test('should return true when isLastPage is false', () {
        const state = OrderListState(isLastPage: false);

        expect(state.hasNextPage, true);
      });

      test('should return false when isLastPage is true', () {
        const state = OrderListState(isLastPage: true);

        expect(state.hasNextPage, false);
      });
    });

    group('hasActiveFilter', () {
      test('should return true when selectedClientId is not null', () {
        const state = OrderListState(selectedClientId: 123);

        expect(state.hasActiveFilter, true);
      });

      test('should return true when selectedStatus is not null', () {
        const state = OrderListState(selectedStatus: 'APPROVED');

        expect(state.hasActiveFilter, true);
      });

      test('should return true when both filters are set', () {
        const state = OrderListState(
          selectedClientId: 123,
          selectedStatus: 'APPROVED',
        );

        expect(state.hasActiveFilter, true);
      });

      test('should return false when both filters are null', () {
        const state = OrderListState(
          selectedClientId: null,
          selectedStatus: null,
        );

        expect(state.hasActiveFilter, false);
      });
    });

    group('copyWith()', () {
      test('should update isLoading', () {
        const state = OrderListState(isLoading: false);
        final newState = state.copyWith(isLoading: true);

        expect(newState.isLoading, true);
      });

      test('should update isLoadingMore', () {
        const state = OrderListState(isLoadingMore: false);
        final newState = state.copyWith(isLoadingMore: true);

        expect(newState.isLoadingMore, true);
      });

      test('should update errorMessage', () {
        const state = OrderListState(errorMessage: null);
        final newState = state.copyWith(errorMessage: 'New error');

        expect(newState.errorMessage, 'New error');
      });

      test('should update orders', () {
        const state = OrderListState(orders: []);
        final newOrders = [
          Order(
            id: 1,
            orderRequestNumber: 'OP00000074',
            clientId: 1,
            clientName: '천사푸드',
            orderDate: DateTime(2026, 2, 5),
            deliveryDate: DateTime(2026, 2, 8),
            totalAmount: 612000000,
            approvalStatus: ApprovalStatus.approved,
            isClosed: true,
          ),
        ];
        final newState = state.copyWith(orders: newOrders);

        expect(newState.orders, newOrders);
      });

      test('should update totalElements', () {
        const state = OrderListState(totalElements: 0);
        final newState = state.copyWith(totalElements: 10);

        expect(newState.totalElements, 10);
      });

      test('should update currentPage', () {
        const state = OrderListState(currentPage: 0);
        final newState = state.copyWith(currentPage: 2);

        expect(newState.currentPage, 2);
      });

      test('should update isLastPage', () {
        const state = OrderListState(isLastPage: false);
        final newState = state.copyWith(isLastPage: true);

        expect(newState.isLastPage, true);
      });

      test('should update hasSearched', () {
        const state = OrderListState(hasSearched: false);
        final newState = state.copyWith(hasSearched: true);

        expect(newState.hasSearched, true);
      });

      test('should update selectedClientId', () {
        const state = OrderListState(selectedClientId: null);
        final newState = state.copyWith(selectedClientId: 123);

        expect(newState.selectedClientId, 123);
      });

      test('should update selectedClientName', () {
        const state = OrderListState(selectedClientName: null);
        final newState = state.copyWith(selectedClientName: '천사푸드');

        expect(newState.selectedClientName, '천사푸드');
      });

      test('should update selectedStatus', () {
        const state = OrderListState(selectedStatus: null);
        final newState = state.copyWith(selectedStatus: 'APPROVED');

        expect(newState.selectedStatus, 'APPROVED');
      });

      test('should update deliveryDateFrom', () {
        const state = OrderListState(deliveryDateFrom: null);
        final newState = state.copyWith(deliveryDateFrom: '2026-02-01');

        expect(newState.deliveryDateFrom, '2026-02-01');
      });

      test('should update deliveryDateTo', () {
        const state = OrderListState(deliveryDateTo: null);
        final newState = state.copyWith(deliveryDateTo: '2026-02-10');

        expect(newState.deliveryDateTo, '2026-02-10');
      });

      test('should update sortType', () {
        const state = OrderListState(sortType: OrderSortType.latestOrder);
        final newState = state.copyWith(sortType: OrderSortType.amountHigh);

        expect(newState.sortType, OrderSortType.amountHigh);
      });

      test('should update clients', () {
        const state = OrderListState(clients: {});
        final newClients = {1: '천사푸드', 2: '행복마트'};
        final newState = state.copyWith(clients: newClients);

        expect(newState.clients, newClients);
      });

      test('should preserve fields not being updated', () {
        final state = OrderListState(
          isLoading: false,
          totalElements: 5,
          selectedClientId: 123,
          sortType: OrderSortType.latestOrder,
        );
        final newState = state.copyWith(isLoading: true);

        expect(newState.isLoading, true);
        expect(newState.totalElements, 5);
        expect(newState.selectedClientId, 123);
        expect(newState.sortType, OrderSortType.latestOrder);
      });

      test('should update multiple fields at once', () {
        const state = OrderListState(
          isLoading: false,
          currentPage: 0,
          totalElements: 0,
        );
        final newState = state.copyWith(
          isLoading: true,
          currentPage: 1,
          totalElements: 20,
        );

        expect(newState.isLoading, true);
        expect(newState.currentPage, 1);
        expect(newState.totalElements, 20);
      });
    });

    group('copyWith() with clearClientFilter', () {
      test('should clear selectedClientId when clearClientFilter is true', () {
        const state = OrderListState(selectedClientId: 123);
        final newState = state.copyWith(clearClientFilter: true);

        expect(newState.selectedClientId, null);
      });

      test('should clear selectedClientName when clearClientFilter is true', () {
        const state = OrderListState(selectedClientName: '천사푸드');
        final newState = state.copyWith(clearClientFilter: true);

        expect(newState.selectedClientName, null);
      });

      test('should clear both client filters when clearClientFilter is true', () {
        const state = OrderListState(
          selectedClientId: 123,
          selectedClientName: '천사푸드',
        );
        final newState = state.copyWith(clearClientFilter: true);

        expect(newState.selectedClientId, null);
        expect(newState.selectedClientName, null);
      });

      test('should preserve other fields when clearing client filter', () {
        const state = OrderListState(
          selectedClientId: 123,
          selectedStatus: 'APPROVED',
          totalElements: 10,
        );
        final newState = state.copyWith(clearClientFilter: true);

        expect(newState.selectedClientId, null);
        expect(newState.selectedStatus, 'APPROVED');
        expect(newState.totalElements, 10);
      });

      test('should not clear client filters when clearClientFilter is false', () {
        const state = OrderListState(
          selectedClientId: 123,
          selectedClientName: '천사푸드',
        );
        final newState = state.copyWith(clearClientFilter: false);

        expect(newState.selectedClientId, 123);
        expect(newState.selectedClientName, '천사푸드');
      });
    });

    group('copyWith() with clearStatusFilter', () {
      test('should clear selectedStatus when clearStatusFilter is true', () {
        const state = OrderListState(selectedStatus: 'APPROVED');
        final newState = state.copyWith(clearStatusFilter: true);

        expect(newState.selectedStatus, null);
      });

      test('should preserve other fields when clearing status filter', () {
        const state = OrderListState(
          selectedStatus: 'APPROVED',
          selectedClientId: 123,
          totalElements: 10,
        );
        final newState = state.copyWith(clearStatusFilter: true);

        expect(newState.selectedStatus, null);
        expect(newState.selectedClientId, 123);
        expect(newState.totalElements, 10);
      });

      test('should not clear status filter when clearStatusFilter is false', () {
        const state = OrderListState(selectedStatus: 'APPROVED');
        final newState = state.copyWith(clearStatusFilter: false);

        expect(newState.selectedStatus, 'APPROVED');
      });
    });

    group('copyWith() with both clear flags', () {
      test('should clear both filters when both flags are true', () {
        const state = OrderListState(
          selectedClientId: 123,
          selectedClientName: '천사푸드',
          selectedStatus: 'APPROVED',
        );
        final newState = state.copyWith(
          clearClientFilter: true,
          clearStatusFilter: true,
        );

        expect(newState.selectedClientId, null);
        expect(newState.selectedClientName, null);
        expect(newState.selectedStatus, null);
      });

      test('should preserve other fields when clearing all filters', () {
        const state = OrderListState(
          selectedClientId: 123,
          selectedStatus: 'APPROVED',
          totalElements: 10,
          currentPage: 2,
          sortType: OrderSortType.amountHigh,
        );
        final newState = state.copyWith(
          clearClientFilter: true,
          clearStatusFilter: true,
        );

        expect(newState.selectedClientId, null);
        expect(newState.selectedStatus, null);
        expect(newState.totalElements, 10);
        expect(newState.currentPage, 2);
        expect(newState.sortType, OrderSortType.amountHigh);
      });
    });
  });
}
