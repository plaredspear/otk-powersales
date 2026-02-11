import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/presentation/providers/client_order_list_state.dart';

void main() {
  group('ClientOrderListState', () {
    test('initial() sets today\'s date as selectedDeliveryDate', () {
      // Arrange
      final now = DateTime.now();
      final expectedDate = '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

      // Act
      final state = ClientOrderListState.initial();

      // Assert
      expect(state.selectedDeliveryDate, expectedDate);
    });

    test('initial() has correct default values', () {
      // Act
      final state = ClientOrderListState.initial();

      // Assert
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
      expect(state.stores, isEmpty);
    });

    test('toLoading() sets isLoading true and clears error', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        errorMessage: 'Previous error',
      );

      // Act
      final loadingState = state.toLoading();

      // Assert
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, isNull);
    });

    test('toError() sets errorMessage and isLoading false', () {
      // Arrange
      final state = ClientOrderListState.initial().toLoading();
      const errorMessage = 'Something went wrong';

      // Act
      final errorState = state.toError(errorMessage);

      // Assert
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, errorMessage);
    });

    test('canSearch returns false when selectedStoreId is null', () {
      // Arrange
      final state = ClientOrderListState.initial();

      // Assert
      expect(state.canSearch, false);
    });

    test('canSearch returns true when selectedStoreId is set', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        selectedStoreId: 1,
        selectedStoreName: 'Test Store',
      );

      // Assert
      expect(state.canSearch, true);
    });

    test('hasResults returns true when orders is not empty', () {
      // Arrange
      const order = ClientOrder(
        sapOrderNumber: '300011396',
        clientId: 2,
        clientName: '(유)경산식품',
        totalAmount: 3763740,
      );
      final state = ClientOrderListState.initial().copyWith(
        orders: [order],
      );

      // Assert
      expect(state.hasResults, true);
    });

    test('hasResults returns false when orders is empty', () {
      // Arrange
      final state = ClientOrderListState.initial();

      // Assert
      expect(state.hasResults, false);
    });

    test('isEmpty returns true when hasSearched and orders empty', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        hasSearched: true,
        orders: [],
      );

      // Assert
      expect(state.isEmpty, true);
    });

    test('isEmpty returns false when not yet searched', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        hasSearched: false,
        orders: [],
      );

      // Assert
      expect(state.isEmpty, false);
    });

    test('hasNextPage returns true when isLast is false', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        isLast: false,
      );

      // Assert
      expect(state.hasNextPage, true);
    });

    test('hasPreviousPage returns true when isFirst is false', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        isFirst: false,
      );

      // Assert
      expect(state.hasPreviousPage, true);
    });

    test('copyWith preserves all fields', () {
      // Arrange
      const order = ClientOrder(
        sapOrderNumber: '300011396',
        clientId: 2,
        clientName: '(유)경산식품',
        totalAmount: 3763740,
      );
      final state = ClientOrderListState.initial().copyWith(
        isLoading: true,
        orders: [order],
        totalElements: 10,
        totalPages: 2,
        currentPage: 1,
        isFirst: false,
        isLast: false,
        hasSearched: true,
        selectedStoreId: 1,
        selectedStoreName: 'Test Store',
        selectedDeliveryDate: '2026-02-08',
        stores: {1: 'Test Store'},
      );

      // Act
      final copiedState = state.copyWith();

      // Assert
      expect(copiedState.isLoading, state.isLoading);
      expect(copiedState.errorMessage, isNull); // errorMessage is not preserved by design
      expect(copiedState.orders, state.orders);
      expect(copiedState.totalElements, state.totalElements);
      expect(copiedState.totalPages, state.totalPages);
      expect(copiedState.currentPage, state.currentPage);
      expect(copiedState.isFirst, state.isFirst);
      expect(copiedState.isLast, state.isLast);
      expect(copiedState.hasSearched, state.hasSearched);
      expect(copiedState.selectedStoreId, state.selectedStoreId);
      expect(copiedState.selectedStoreName, state.selectedStoreName);
      expect(copiedState.selectedDeliveryDate, state.selectedDeliveryDate);
      expect(copiedState.stores, state.stores);
    });

    test('copyWith with clearStoreFilter clears store fields', () {
      // Arrange
      final state = ClientOrderListState.initial().copyWith(
        selectedStoreId: 1,
        selectedStoreName: 'Test Store',
      );

      // Act
      final clearedState = state.copyWith(clearStoreFilter: true);

      // Assert
      expect(clearedState.selectedStoreId, isNull);
      expect(clearedState.selectedStoreName, isNull);
    });
  });
}
